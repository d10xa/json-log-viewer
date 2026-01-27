package ru.d10xa.jsonlogviewer

import cats.effect.IO
import cats.effect.Ref
import fs2.*
import fs2.Pull
import ru.d10xa.jsonlogviewer.cache.CachedFilterSet
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig

/** Context for CSV stream processing with cache support. */
final case class CsvProcessingContext(
  initialFilterSet: CachedFilterSet,
  feedName: Option[String],
  getCachedFilterSet: IO[CachedFilterSet]
)

/** Processes CSV log streams with header-based parser creation and live reload support. */
object CsvStreamProcessor {

  def process(
    lines: Stream[IO, String],
    context: CsvProcessingContext
  ): Stream[IO, String] =
    lines.pull.uncons1.flatMap {
      case Some((headerLine, rest)) =>
        processWithHeader(headerLine, rest, context)
      case None =>
        Pull.done
    }.stream

  private def processWithHeader(
    headerLine: String,
    rest: Stream[IO, String],
    context: CsvProcessingContext
  ): Pull[IO, String, Unit] = {
    val initialParser =
      LogLineParserFactory.createCsvParser(
        context.initialFilterSet.resolvedConfig,
        headerLine
      )
    val initialFieldNames = context.initialFilterSet.resolvedConfig.fieldNames

    Stream
      .eval(
        Ref.of[IO, (LogLineParser, FieldNamesConfig)](
          (initialParser, initialFieldNames)
        )
      )
      .flatMap { parserRef =>
        rest.flatMap { line =>
          processLine(line, headerLine, parserRef, context)
        }
      }
      .pull
      .echo
  }

  private def processLine(
    line: String,
    headerLine: String,
    parserRef: Ref[IO, (LogLineParser, FieldNamesConfig)],
    context: CsvProcessingContext
  ): Stream[IO, String] =
    Stream
      .eval(
        for {
          filterSet <- context.getCachedFilterSet
          currentFieldNames = filterSet.resolvedConfig.fieldNames
          parserAndFieldNames <- parserRef.get
          (currentParser, prevFieldNames) = parserAndFieldNames
          updatedParser <- updateParserIfNeeded(
            currentFieldNames,
            prevFieldNames,
            currentParser,
            filterSet,
            headerLine,
            parserRef
          )
        } yield (filterSet, updatedParser)
      )
      .flatMap { case (filterSet, parser) =>
        FilterPipeline.applyFilters(
          Stream.emit(line),
          parser,
          filterSet.components,
          filterSet.resolvedConfig
        )
      }

  private def updateParserIfNeeded(
    currentFieldNames: FieldNamesConfig,
    prevFieldNames: FieldNamesConfig,
    currentParser: LogLineParser,
    filterSet: CachedFilterSet,
    headerLine: String,
    parserRef: Ref[IO, (LogLineParser, FieldNamesConfig)]
  ): IO[LogLineParser] =
    if (currentFieldNames != prevFieldNames) {
      val newParser = LogLineParserFactory.createCsvParser(
        filterSet.resolvedConfig,
        headerLine
      )
      parserRef
        .set((newParser, currentFieldNames))
        .as(newParser)
    } else {
      IO.pure(currentParser)
    }
}
