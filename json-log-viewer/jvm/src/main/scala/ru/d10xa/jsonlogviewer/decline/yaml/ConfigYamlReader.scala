package ru.d10xa.jsonlogviewer.decline.yaml

import cats.data.ValidatedNel
import cats.effect.IO
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYamlLoader
import scala.io.Source

object ConfigYamlReader {
  val configYamlLoader: ConfigYamlLoader = ConfigYamlLoaderImpl()
  def readFile(filePath: String): IO[String] =
    IO.blocking(Source.fromFile(filePath))
      .bracket { source =>
        IO(source.mkString)
      } { source =>
        IO(source.close())
      }

  def fromYamlFile(filePath: String): IO[ValidatedNel[String, ConfigYaml]] =
    readFile(filePath).map(configYamlLoader.parseYamlFile)
}
