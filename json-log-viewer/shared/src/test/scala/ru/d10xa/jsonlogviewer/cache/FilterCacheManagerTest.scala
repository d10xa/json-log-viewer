package ru.d10xa.jsonlogviewer.cache

import munit.FunSuite
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig
import ru.d10xa.jsonlogviewer.decline.TimestampConfig
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.Feed
import ru.d10xa.jsonlogviewer.decline.yaml.FieldNames

class FilterCacheManagerTest extends FunSuite {

  val standardConfig: Config = Config(
    configFile = None,
    fieldNames = FieldNamesConfig(
      timestampFieldName = "@timestamp",
      levelFieldName = "level",
      messageFieldName = "message",
      stackTraceFieldName = "stack_trace",
      loggerNameFieldName = "logger_name",
      threadNameFieldName = "thread_name"
    ),
    timestamp = TimestampConfig(None, None),
    grep = List.empty,
    filter = None,
    formatIn = None,
    formatOut = None,
    showEmptyFields = false,
    commands = List.empty,
    restart = false,
    restartDelayMs = None,
    maxRestarts = None
  )

  test("buildCache creates FilterSets from Config without YAML") {
    val cache = FilterCacheManager.buildCache(standardConfig, None)

    assertEquals(cache.filterSets.length, 1)
    assertEquals(cache.config, standardConfig)
    assertEquals(cache.configYaml, None)

    val cachedFilterSet = cache.filterSets.head
    assertEquals(cachedFilterSet.resolvedConfig.feedName, None)
    assert(cachedFilterSet.parser.isDefined, "Parser should be created for non-CSV format")
    assertNotEquals(cachedFilterSet.components, null, "FilterComponents should be created")
  }

  test("buildCache creates FilterSets from Config with YAML feeds") {
    val configYaml = ConfigYaml(
      showEmptyFields = None,
      fieldNames = None,
      feeds = Some(
        List(
          Feed(
            name = Some("feed1"),
            commands = List("cmd1"),
            inlineInput = None,
            filter = None,
            formatIn = Some(Config.FormatIn.Json),
            fieldNames = None,
            rawInclude = None,
            rawExclude = None,
            fuzzyInclude = None,
            fuzzyExclude = None,
            excludeFields = None,
            showEmptyFields = None,
            restart = None,
            restartDelayMs = None,
            maxRestarts = None
          ),
          Feed(
            name = Some("feed2"),
            commands = List("cmd2"),
            inlineInput = None,
            filter = None,
            formatIn = Some(Config.FormatIn.Logfmt),
            fieldNames = None,
            rawInclude = None,
            rawExclude = None,
            fuzzyInclude = None,
            fuzzyExclude = None,
            excludeFields = None,
            showEmptyFields = None,
            restart = None,
            restartDelayMs = None,
            maxRestarts = None
          )
        )
      )
    )

    val cache = FilterCacheManager.buildCache(standardConfig, Some(configYaml))

    assertEquals(cache.filterSets.length, 2)
    assertEquals(cache.configYaml, Some(configYaml))

    val feed1 = cache.filterSets.find(_.resolvedConfig.feedName.contains("feed1"))
    assert(feed1.isDefined, "feed1 FilterSet should exist")
    assert(feed1.get.parser.isDefined, "feed1 should have parser for JSON format")

    val feed2 = cache.filterSets.find(_.resolvedConfig.feedName.contains("feed2"))
    assert(feed2.isDefined, "feed2 FilterSet should exist")
    assert(feed2.get.parser.isDefined, "feed2 should have parser for Logfmt format")
  }

  test("buildCache creates FilterSet without parser for CSV format") {
    val configYaml = ConfigYaml(
      showEmptyFields = None,
      fieldNames = None,
      feeds = Some(
        List(
          Feed(
            name = Some("csv-feed"),
            commands = List("cmd"),
            inlineInput = None,
            filter = None,
            formatIn = Some(Config.FormatIn.Csv),
            fieldNames = None,
            rawInclude = None,
            rawExclude = None,
            fuzzyInclude = None,
            fuzzyExclude = None,
            excludeFields = None,
            showEmptyFields = None,
            restart = None,
            restartDelayMs = None,
            maxRestarts = None
          )
        )
      )
    )

    val cache = FilterCacheManager.buildCache(standardConfig, Some(configYaml))

    assertEquals(cache.filterSets.length, 1)
    val cachedFilterSet = cache.filterSets.head
    assertEquals(cachedFilterSet.parser, None, "CSV format should not have pre-built parser")
  }

  test("updateCacheIfNeeded returns existing cache when config unchanged") {
    val cache = FilterCacheManager.buildCache(standardConfig, None)

    val (updatedCache, wasRebuilt) =
      FilterCacheManager.updateCacheIfNeeded(Some(cache), standardConfig, None)

    assertEquals(wasRebuilt, false, "Cache should not be rebuilt")
    assert(updatedCache eq cache, "Should return same cache instance")
  }

  test("updateCacheIfNeeded rebuilds cache when CLI config changes") {
    val cache = FilterCacheManager.buildCache(standardConfig, None)

    val modifiedConfig = standardConfig.copy(showEmptyFields = true)
    val (updatedCache, wasRebuilt) =
      FilterCacheManager.updateCacheIfNeeded(Some(cache), modifiedConfig, None)

    assertEquals(wasRebuilt, true, "Cache should be rebuilt")
    assert(updatedCache ne cache, "Should return new cache instance")
    assertEquals(updatedCache.config, modifiedConfig)
  }

  test("updateCacheIfNeeded rebuilds cache when YAML config changes") {
    val cache = FilterCacheManager.buildCache(standardConfig, None)

    val configYaml = ConfigYaml(
      showEmptyFields = Some(true),
      fieldNames = None,
      feeds = None
    )
    val (updatedCache, wasRebuilt) =
      FilterCacheManager.updateCacheIfNeeded(Some(cache), standardConfig, Some(configYaml))

    assertEquals(wasRebuilt, true, "Cache should be rebuilt when YAML is added")
    assert(updatedCache ne cache, "Should return new cache instance")
    assertEquals(updatedCache.configYaml, Some(configYaml))
  }

  test("updateCacheIfNeeded builds cache when no existing cache") {
    val (cache, wasRebuilt) =
      FilterCacheManager.updateCacheIfNeeded(None, standardConfig, None)

    assertEquals(wasRebuilt, true, "Cache should be built")
    assertEquals(cache.config, standardConfig)
    assertEquals(cache.filterSets.length, 1)
  }

  test("CachedResolvedState.isValid returns true for identical config") {
    val cache = FilterCacheManager.buildCache(standardConfig, None)

    assert(
      cache.isValid(standardConfig, None),
      "isValid should return true for identical config"
    )
  }

  test("CachedResolvedState.isValid returns false when CLI config differs") {
    val cache = FilterCacheManager.buildCache(standardConfig, None)
    val modifiedConfig = standardConfig.copy(showEmptyFields = true)

    assert(
      !cache.isValid(modifiedConfig, None),
      "isValid should return false when CLI config differs"
    )
  }

  test("CachedResolvedState.isValid returns false when YAML config differs") {
    val cache = FilterCacheManager.buildCache(standardConfig, None)
    val configYaml = ConfigYaml(
      showEmptyFields = None,
      fieldNames = None,
      feeds = None
    )

    assert(
      !cache.isValid(standardConfig, Some(configYaml)),
      "isValid should return false when YAML config is added"
    )
  }

  test("CachedResolvedState.isValid returns false when YAML config content changes") {
    val configYaml1 = ConfigYaml(
      showEmptyFields = Some(false),
      fieldNames = None,
      feeds = None
    )
    val configYaml2 = ConfigYaml(
      showEmptyFields = Some(true),
      fieldNames = None,
      feeds = None
    )
    val cache = FilterCacheManager.buildCache(standardConfig, Some(configYaml1))

    assert(
      !cache.isValid(standardConfig, Some(configYaml2)),
      "isValid should return false when YAML content changes"
    )
  }

  test("buildFilterSet creates FilterComponents for resolved config") {
    val resolvedConfigs =
      ru.d10xa.jsonlogviewer.config.ConfigResolver.resolve(standardConfig, None)
    val resolvedConfig = resolvedConfigs.head

    val cachedFilterSet = FilterCacheManager.buildFilterSet(resolvedConfig)

    assertEquals(cachedFilterSet.resolvedConfig, resolvedConfig)
    assertNotEquals(cachedFilterSet.components, null)
    assertNotEquals(cachedFilterSet.components.timestampFilter, null)
    assertNotEquals(cachedFilterSet.components.logLineFilter, null)
    assertNotEquals(cachedFilterSet.components.fuzzyFilter, null)
    assertNotEquals(cachedFilterSet.components.outputLineFormatter, null)
  }
}
