package ru.d10xa.jsonlogviewer

import cats.effect.IO
import cats.effect.Ref
import fs2.*
import fs2.Pull
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
    stdinStream: StdInLinesStream,
    shell: Shell
  ): Stream[IO, String] = {
    def processStreamWithConfig(
      inputStream: Stream[IO, String],
      resolvedConfig: ResolvedConfig
    ): Stream[IO, String] =
      if (resolvedConfig.formatIn.contains(FormatIn.Csv)) {
        createCsvProcessStream(resolvedConfig, inputStream)
      } else {
        inputStream.flatMap { line =>
          Stream.eval(configYamlRef.get).flatMap { currentConfigYaml =>
            processLineWithConfig(line, currentConfigYaml, config)
          }
        }
      }

    Stream.eval(configYamlRef.get).flatMap { initialConfigYaml =>
      val resolvedConfigs = ConfigResolver.resolve(config, initialConfigYaml)

      val finalStream = if (resolvedConfigs.isEmpty) {
        Stream.empty
      } else if (resolvedConfigs.length > 1) {
        val feedStreams = resolvedConfigs.map { resolvedConfig =>
          val feedStream = shell.mergeCommandsAndInlineInput(
            resolvedConfig.commands,
            resolvedConfig.inlineInput
          )
          processStreamWithConfig(feedStream, resolvedConfig)
        }
        Stream.emits(feedStreams).parJoin(feedStreams.size)
      } else {
        val resolvedConfig = resolvedConfigs.head
        val inputStream =
          if (
            resolvedConfig.inlineInput.isDefined || resolvedConfig.commands.nonEmpty
          ) {
            shell.mergeCommandsAndInlineInput(
              resolvedConfig.commands,
              resolvedConfig.inlineInput
            )
          } else {
            stdinStream.stdinLinesStream
          }
        processStreamWithConfig(inputStream, resolvedConfig)
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

  private def createCsvProcessStream(
    resolvedConfig: ResolvedConfig,
    lines: Stream[IO, String]
  ): Stream[IO, String] =
    lines.pull.uncons1.flatMap {
      case Some((headerLine, rest)) =>
        val csvHeaderParser = LogLineParserFactory.createCsvParser(resolvedConfig, headerLine)
        val components = FilterComponents.fromConfig(resolvedConfig)

        FilterPipeline
          .applyFilters(rest, csvHeaderParser, components, resolvedConfig)
          .pull
          .echo
      case None =>
        Pull.done
    }.stream
}
