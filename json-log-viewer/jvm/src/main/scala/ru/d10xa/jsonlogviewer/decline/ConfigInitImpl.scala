package ru.d10xa.jsonlogviewer.decline

import cats.data.Validated
import cats.effect.IO
import ru.d10xa.jsonlogviewer.ConfigYamlReader
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import cats.syntax.all.*

import java.io.File

class ConfigInitImpl extends ConfigInit {

  override def initConfig(c: Config): IO[Config] = {
    def findConfigFile(
      baseName: String,
      extensions: List[String]
    ): Option[String] = {
      extensions.collectFirst {
        case ext if new File(s"$baseName.$ext").exists() =>
          s"$baseName.$ext"
      }
    }

    val configFileOpt: Option[String] = c.configFile.map(_.file).orElse {
      findConfigFile("json-log-viewer", List("yml", "yaml", "YML", "YAML"))
    }

    val configIO: IO[Option[ConfigYaml]] = configFileOpt match {
      case Some(file) if new File(file).exists() =>
        ConfigYamlReader.fromYamlFile(file).flatMap {
          case Validated.Valid(config) =>
            config.some.pure[IO]
          case Validated.Invalid(errors) =>
            IO.raiseError(
              new IllegalArgumentException(errors.toList.mkString(", "))
            )
        }
      case Some(file) =>
        IO.raiseError(
          new IllegalArgumentException(s"Configuration file not found: $file")
        )
      case None =>
        None.pure[IO]
    }
    configIO.map {
      case Some(config) =>
        c.copy(
          filter = c.filter.orElse(config.filter),
          formatIn =
            c.formatIn.orElse(config.formatIn).orElse(Some(FormatIn.Json)),
          configYaml = Some(config)
        )
      case None => c
    }
  }
}
