package ru.d10xa.jsonlogviewer.decline

import cats.data.Validated
import cats.effect.IO
import ru.d10xa.jsonlogviewer.ConfigYamlReader
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import cats.syntax.all._

class ConfigInitImpl extends ConfigInit {

  override def initConfig(c: Config): IO[Config] = {
    val f = c.configFile.map(_.file).getOrElse("json-log-viewer.yml")
    val configIO: IO[ConfigYaml] = ConfigYamlReader.fromYamlFile(f).flatMap {
      case Validated.Valid(config) =>
        config.pure[IO]
      case Validated.Invalid(errors) =>
        IO.raiseError(
          new IllegalArgumentException(errors.toList.mkString(", "))
        )
    }
    configIO.map { config =>
      c.copy(
        filter = c.filter.orElse(config.filter),
        formatIn =
          c.formatIn.orElse(config.formatIn).orElse(Some(FormatIn.Json))
      )
    }
  }
}
