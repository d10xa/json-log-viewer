package ru.d10xa.jsonlogviewer.decline

import cats.effect.std.Supervisor
import cats.effect.IO
import cats.effect.Ref
import cats.effect.Resource
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml

trait ConfigInit {
  def initConfigYaml(
    c: Config,
    supervisor: Supervisor[IO]
  ): Resource[IO, Ref[IO, Option[ConfigYaml]]]
}
