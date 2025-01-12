package ru.d10xa.jsonlogviewer.decline

import cats.effect.{IO, Ref, Resource}
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml

trait ConfigInit {
  def initConfigYaml(c: Config): Resource[IO, Ref[IO, Option[ConfigYaml]]]
}
