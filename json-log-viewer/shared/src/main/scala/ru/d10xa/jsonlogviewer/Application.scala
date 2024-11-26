package ru.d10xa.jsonlogviewer

import cats.data.Validated
import cats.effect.*
import cats.effect.IO.IOCont
import cats.effect.IO.Uncancelable
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import fs2.*
import fs2.io.*
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.DeclineOpts
import ru.d10xa.jsonlogviewer.logfmt.LogfmtLogLineParser
import _root_.io.circe.yaml.scalayaml.parser
import cats.syntax.all.*
import ru.d10xa.jsonlogviewer.decline.ConfigYaml

object Application
  extends CommandIOApp(
    "json-log-viewer",
    "Print json logs in human-readable form"
  ):

  private val stdinLinesStream: Stream[IO, String] =
    stdinUtf8[IO](1024 * 1024 * 10)
      .repartition(s => Chunk.array(s.split("\n", -1)))
      .filter(_.nonEmpty)

  def initConfigYaml(c: Config): IO[Config] =
//    val f = config.configFile.map(_.file).getOrElse("json-log-viewer.yml")
    val f = c.configFile.map(_.file).getOrElse("json-log-viewer.yml")
    val configIO: IO[ConfigYaml] = ConfigYamlReader.fromYamlFile(f).flatMap {
      case Validated.Valid(config) =>
        config.pure[IO]
      case Validated.Invalid(errors) =>
        IO.raiseError(new IllegalArgumentException(errors.toList.mkString(", ")))
    }
    configIO.map { config =>
      c.copy(
        // TODO неправильный парсинг
        filter = c.filter.orElse(config.filter),
        formatIn = config.formatIn.getOrElse(c.formatIn)
      )
    }

  import cats.effect.unsafe.implicits._

  // TODO unsafe
  def main: Opts[IO[ExitCode]] = DeclineOpts.config.map(c => initConfigYaml(c).unsafeRunSync()).map { c =>
    val jsonPrefixPostfix = JsonPrefixPostfix(JsonDetector())
    val logLineParser = c.formatIn match
      case FormatIn.Json => JsonLogLineParser(c, jsonPrefixPostfix)
      case FormatIn.Logfmt => LogfmtLogLineParser(c)

    stdinLinesStream
      .through(LogViewerStream.stream[IO](c, logLineParser))
      .through(text.utf8.encode)
      .through(io.stdout)
      .compile
      .drain
      .as(ExitCode.Success)
  }
