package ru.d10xa.jsonlogviewer.cache

import ru.d10xa.jsonlogviewer.config.ConfigResolver
import ru.d10xa.jsonlogviewer.config.ResolvedConfig
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.FilterComponents
import ru.d10xa.jsonlogviewer.LogLineParserFactory
import scala.util.Try

/** Manages caching of resolved configurations and filter components
  *
  * This reduces the overhead of ConfigResolver.resolve calls from once per log
  * line to only when configuration actually changes
  */
object FilterCacheManager {

  def buildCache(
    config: Config,
    configYaml: Option[ConfigYaml]
  ): Either[String, CachedResolvedState] =
    Try {
      val resolvedConfigs = ConfigResolver.resolve(config, configYaml)
      val filterSets = resolvedConfigs.map(buildFilterSet)
      CachedResolvedState(config, configYaml, filterSets)
    }.toEither.left.map(_.getMessage)

  /** @return (cache, wasRebuilt) */
  def updateCacheIfNeeded(
    existingCache: Option[CachedResolvedState],
    config: Config,
    configYaml: Option[ConfigYaml]
  ): (CachedResolvedState, Boolean) =
    existingCache match {
      case Some(cache) if cache.isValid(config, configYaml) =>
        (cache, false)
      case _ =>
        buildCache(config, configYaml) match {
          case Right(newCache) => (newCache, true)
          case Left(_) =>
            existingCache match {
              case Some(cache) => (cache, false)
              case None =>
                val fallback = CachedResolvedState.noFilters(config, configYaml)
                (fallback, true)
            }
        }
    }

  def buildFilterSet(resolvedConfig: ResolvedConfig): CachedFilterSet = {
    val components = FilterComponents.fromConfig(resolvedConfig)
    val parser =
      if (resolvedConfig.formatIn.contains(FormatIn.Csv)) {
        None // CSV parser needs header line, created dynamically
      } else {
        Some(LogLineParserFactory.createNonCsvParser(resolvedConfig))
      }
    CachedFilterSet(resolvedConfig, components, parser)
  }
}
