package ru.d10xa.jsonlogviewer.decline

import cats.effect.std.Supervisor
import cats.effect.IO
import cats.effect.Ref
import cats.effect.Resource
import ru.d10xa.jsonlogviewer.cache.CachedResolvedState
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml

final case class ConfigRefs(
  configYamlRef: Ref[IO, Option[ConfigYaml]],
  cacheRef: Ref[IO, CachedResolvedState]
)

trait ConfigInit {
  def initConfigRefs(
    c: Config,
    supervisor: Supervisor[IO]
  ): Resource[IO, ConfigRefs]
}
