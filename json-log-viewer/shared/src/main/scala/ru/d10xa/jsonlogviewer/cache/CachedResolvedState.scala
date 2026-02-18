package ru.d10xa.jsonlogviewer.cache

import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.Config

/** Cached filter sets with configuration snapshot for cache invalidation. */
final case class CachedResolvedState(
  config: Config,
  configYaml: Option[ConfigYaml],
  filterSets: List[CachedFilterSet]
) {

  def isValid(
    currentConfig: Config,
    currentConfigYaml: Option[ConfigYaml]
  ): Boolean =
    config == currentConfig && configYaml == currentConfigYaml
}

object CachedResolvedState {
  def noFilters(
    config: Config,
    configYaml: Option[ConfigYaml]
  ): CachedResolvedState =
    CachedResolvedState(config, configYaml, Nil)
}
