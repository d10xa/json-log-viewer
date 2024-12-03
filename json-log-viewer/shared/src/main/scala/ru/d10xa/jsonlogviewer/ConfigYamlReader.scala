package ru.d10xa.jsonlogviewer

import cats.effect.IO
import cats.data.ValidatedNel
import ru.d10xa.jsonlogviewer.decline.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.ConfigYamlLoader

import scala.io.Source

object ConfigYamlReader {

  def readFile(filePath: String): IO[String] =
    IO.blocking(Source.fromFile(filePath)).bracket { source =>
      IO(source.mkString)
    } { source =>
      IO(source.close())
    }

  def fromYamlFile(filePath: String): IO[ValidatedNel[String, ConfigYaml]] =
    readFile(filePath).map(ConfigYamlLoader.parseYamlFile)
}