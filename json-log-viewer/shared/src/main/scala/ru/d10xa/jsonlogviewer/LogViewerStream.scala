package ru.d10xa.jsonlogviewer

import cats.effect.IO
import cats.effect.Ref
import fs2.*
import fs2.Pull
import ru.d10xa.jsonlogviewer.cache.CachedResolvedState
import ru.d10xa.jsonlogviewer.cache.FilterCacheManager
import ru.d10xa.jsonlogviewer.cache.FilterSet
import ru.d10xa.jsonlogviewer.config.ConfigResolver
import ru.d10xa.jsonlogviewer.config.ResolvedConfig
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.shell.Shell
import ru.d10xa.jsonlogviewer.shell.ShellImpl

object LogViewerStream {

  def stream(
    config: Config,
    configYamlRef: Ref[IO, Option[ConfigYaml]],
    cacheRef: Ref[IO, CachedResolvedState],
    stdinStream: StdInLinesStream,
    shell: Shell
  ): Stream[IO, String] = {

    def getOrUpdateCache(
      currentConfigYaml: Option[ConfigYaml]
    ): IO[CachedResolvedState] =
      cacheRef.get.flatMap { currentCache =>
        val (newCache, wasUpdated) = FilterCacheManager.updateCacheIfNeeded(
          Some(currentCache),
          config,
          currentConfigYaml
        )
        if (wasUpdated) cacheRef.set(newCache).as(newCache)
        else IO.pure(currentCache)
      }

    def findFilterSet(
      cache: CachedResolvedState,
      feedName: Option[String]
    ): Option[FilterSet] =
      cache.filterSets.find(_.resolvedConfig.feedName == feedName)

    def processStreamWithCache(
      inputStream: Stream[IO, String],
      initialFilterSet: FilterSet
    ): Stream[IO, String] = {
      val feedName = initialFilterSet.resolvedConfig.feedName

      if (initialFilterSet.resolvedConfig.formatIn.contains(FormatIn.Csv)) {
        createCsvProcessStreamCached(
          initialFilterSet,
          inputStream,
          configYamlRef,
          cacheRef,
          config,
          feedName
        )
      } else {
        inputStream.flatMap { line =>
          Stream
            .eval(for {
              currentConfigYaml <- configYamlRef.get
              cache <- getOrUpdateCache(currentConfigYaml)
            } yield findFilterSet(cache, feedName).getOrElse(initialFilterSet))
            .flatMap { filterSet =>
              processLineWithFilterSet(line, filterSet)
            }
        }
      }
    }

    Stream.eval(cacheRef.get).flatMap { initialCache =>
      val filterSets = initialCache.filterSets

      val finalStream = if (filterSets.isEmpty) {
        Stream.empty
      } else if (filterSets.length > 1) {
        val feedStreams = filterSets.map { filterSet =>
          val feedStream = shell.mergeCommandsAndInlineInput(
            filterSet.resolvedConfig.commands,
            filterSet.resolvedConfig.inlineInput
          )
          processStreamWithCache(feedStream, filterSet)
        }
        Stream.emits(feedStreams).parJoin(feedStreams.size)
      } else {
        val filterSet = filterSets.head
        val inputStream =
          if (
            filterSet.resolvedConfig.inlineInput.isDefined ||
            filterSet.resolvedConfig.commands.nonEmpty
          ) {
            shell.mergeCommandsAndInlineInput(
              filterSet.resolvedConfig.commands,
              filterSet.resolvedConfig.inlineInput
            )
          } else {
            stdinStream.stdinLinesStream
          }
        processStreamWithCache(inputStream, filterSet)
      }

      finalStream.intersperse("\n").append(Stream.emit("\n"))
    }
  }

  def processLineWithRef(
    line: String,
    configYamlRef: Ref[IO, Option[ConfigYaml]],
    config: Config
  ): Stream[IO, String] =
    Stream.eval(configYamlRef.get).flatMap { configYaml =>
      processLineWithConfig(line, configYaml, config)
    }

  def processLineWithConfig(
    line: String,
    configYaml: Option[ConfigYaml],
    config: Config
  ): Stream[IO, String] = {
    val resolvedConfigs = ConfigResolver.resolve(config, configYaml)

    if (resolvedConfigs.isEmpty) {
      Stream.empty
    } else if (resolvedConfigs.length > 1) {
      val results = resolvedConfigs.map { resolvedConfig =>
        processLineWithResolvedConfig(line, resolvedConfig)
      }
      Stream.emits(results).parJoinUnbounded
    } else {
      processLineWithResolvedConfig(line, resolvedConfigs.head)
    }
  }

  def processLineWithResolvedConfig(
    line: String,
    resolvedConfig: ResolvedConfig
  ): Stream[IO, String] = {
    val getParser: IO[LogLineParser] =
      if (resolvedConfig.formatIn.contains(FormatIn.Csv)) {
        IO.raiseError(
          new IllegalStateException(
            "CSV format requires header line, cannot process a single line"
          )
        )
      } else {
        IO.pure(LogLineParserFactory.createNonCsvParser(resolvedConfig))
      }

    Stream.eval(getParser).flatMap { parser =>
      val components = FilterComponents.fromConfig(resolvedConfig)

      FilterPipeline.applyFilters(
        Stream.emit(line),
        parser,
        components,
        resolvedConfig
      )
    }
  }

  def processLineWithFilterSet(
    line: String,
    filterSet: FilterSet
  ): Stream[IO, String] =
    filterSet.parser match {
      case Some(parser) =>
        FilterPipeline.applyFilters(
          Stream.emit(line),
          parser,
          filterSet.components,
          filterSet.resolvedConfig
        )
      case None =>
        // CSV: parser=None, will throw error (CSV needs header for parser creation)
        processLineWithResolvedConfig(line, filterSet.resolvedConfig)
    }

  private def createCsvProcessStream(
    resolvedConfig: ResolvedConfig,
    lines: Stream[IO, String]
  ): Stream[IO, String] =
    lines.pull.uncons1.flatMap {
      case Some((headerLine, rest)) =>
        val csvHeaderParser =
          LogLineParserFactory.createCsvParser(resolvedConfig, headerLine)
        val components = FilterComponents.fromConfig(resolvedConfig)

        FilterPipeline
          .applyFilters(rest, csvHeaderParser, components, resolvedConfig)
          .pull
          .echo
      case None =>
        Pull.done
    }.stream

  private def createCsvProcessStreamCached(
    initialFilterSet: FilterSet,
    lines: Stream[IO, String],
    configYamlRef: Ref[IO, Option[ConfigYaml]],
    cacheRef: Ref[IO, CachedResolvedState],
    config: Config,
    feedName: Option[String]
  ): Stream[IO, String] = {

    def getOrUpdateCache(
      currentConfigYaml: Option[ConfigYaml]
    ): IO[CachedResolvedState] =
      cacheRef.get.flatMap { currentCache =>
        val (newCache, wasUpdated) = FilterCacheManager.updateCacheIfNeeded(
          Some(currentCache),
          config,
          currentConfigYaml
        )
        if (wasUpdated) cacheRef.set(newCache).as(newCache)
        else IO.pure(currentCache)
      }

    def findFilterSet(
      cache: CachedResolvedState,
      name: Option[String]
    ): Option[FilterSet] =
      cache.filterSets.find(_.resolvedConfig.feedName == name)

    lines.pull.uncons1.flatMap {
      case Some((headerLine, rest)) =>
        val resolvedConfig = initialFilterSet.resolvedConfig
        val csvHeaderParser =
          LogLineParserFactory.createCsvParser(resolvedConfig, headerLine)

        // CSV parser created from header, but components from cache support live reload
        rest
          .flatMap { line =>
            Stream
              .eval(
                for {
                  currentConfigYaml <- configYamlRef.get
                  cache <- getOrUpdateCache(currentConfigYaml)
                } yield findFilterSet(cache, feedName).getOrElse(
                  initialFilterSet
                )
              )
              .flatMap { filterSet =>
                FilterPipeline.applyFilters(
                  Stream.emit(line),
                  csvHeaderParser,
                  filterSet.components,
                  filterSet.resolvedConfig
                )
              }
          }
          .pull
          .echo
      case None =>
        Pull.done
    }.stream
  }
}
