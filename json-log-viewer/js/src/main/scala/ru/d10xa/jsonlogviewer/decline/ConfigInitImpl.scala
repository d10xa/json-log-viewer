package ru.d10xa.jsonlogviewer.decline

import cats.effect.std.Supervisor
import cats.effect.IO
import cats.effect.Ref
import cats.effect.Resource
import ru.d10xa.jsonlogviewer.cache.CachedResolvedState
import ru.d10xa.jsonlogviewer.cache.FilterCacheManager
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn

class ConfigInitImpl extends ConfigInit {

  override def initConfigRefs(
    c: Config,
    supervisor: Supervisor[IO]
  ): Resource[IO, ConfigRefs] = {
    val initialConfigYaml: Option[ConfigYaml] = None
    val initialCache = FilterCacheManager.buildCache(c, initialConfigYaml)
    Resource.eval(for {
      configYamlRef <- Ref.of[IO, Option[ConfigYaml]](initialConfigYaml)
      cacheRef <- Ref.of[IO, CachedResolvedState](initialCache)
    } yield ConfigRefs(configYamlRef, cacheRef))
  }
}
