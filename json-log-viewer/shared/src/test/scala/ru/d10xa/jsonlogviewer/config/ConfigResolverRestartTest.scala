package ru.d10xa.jsonlogviewer.config

import munit.FunSuite
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.Feed
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig
import ru.d10xa.jsonlogviewer.decline.TimestampConfig

class ConfigResolverRestartTest extends FunSuite {

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

  test("resolve sets default restart values when feed has no restart config") {
    val configYaml = ConfigYaml(
      showEmptyFields = None,
      fieldNames = None,
      feeds = Some(
        List(
          Feed(
            name = Some("test-feed"),
            commands = List("cmd1"),
            inlineInput = None,
            filter = None,
            formatIn = None,
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

    val resolvedConfigs = ConfigResolver.resolve(standardConfig, Some(configYaml))

    assertEquals(resolvedConfigs.length, 1)
    val resolved = resolvedConfigs.head
    assertEquals(resolved.restart, false)
    assertEquals(resolved.restartDelayMs, 1000L)
    assertEquals(resolved.maxRestarts, None)
  }

  test("resolve uses restart settings from feed") {
    val configYaml = ConfigYaml(
      showEmptyFields = None,
      fieldNames = None,
      feeds = Some(
        List(
          Feed(
            name = Some("restart-feed"),
            commands = List("cmd1"),
            inlineInput = None,
            filter = None,
            formatIn = None,
            fieldNames = None,
            rawInclude = None,
            rawExclude = None,
            fuzzyInclude = None,
            fuzzyExclude = None,
            excludeFields = None,
            showEmptyFields = None,
            restart = Some(true),
            restartDelayMs = Some(5000L),
            maxRestarts = Some(10)
          )
        )
      )
    )

    val resolvedConfigs = ConfigResolver.resolve(standardConfig, Some(configYaml))

    assertEquals(resolvedConfigs.length, 1)
    val resolved = resolvedConfigs.head
    assertEquals(resolved.restart, true)
    assertEquals(resolved.restartDelayMs, 5000L)
    assertEquals(resolved.maxRestarts, Some(10))
  }

  test("resolve handles multiple feeds with different restart configs") {
    val configYaml = ConfigYaml(
      showEmptyFields = None,
      fieldNames = None,
      feeds = Some(
        List(
          Feed(
            name = Some("restart-feed"),
            commands = List("cmd1"),
            inlineInput = None,
            filter = None,
            formatIn = None,
            fieldNames = None,
            rawInclude = None,
            rawExclude = None,
            fuzzyInclude = None,
            fuzzyExclude = None,
            excludeFields = None,
            showEmptyFields = None,
            restart = Some(true),
            restartDelayMs = Some(2000L),
            maxRestarts = Some(5)
          ),
          Feed(
            name = Some("no-restart-feed"),
            commands = List("cmd2"),
            inlineInput = None,
            filter = None,
            formatIn = None,
            fieldNames = None,
            rawInclude = None,
            rawExclude = None,
            fuzzyInclude = None,
            fuzzyExclude = None,
            excludeFields = None,
            showEmptyFields = None,
            restart = Some(false),
            restartDelayMs = None,
            maxRestarts = None
          ),
          Feed(
            name = Some("unlimited-restart-feed"),
            commands = List("cmd3"),
            inlineInput = None,
            filter = None,
            formatIn = None,
            fieldNames = None,
            rawInclude = None,
            rawExclude = None,
            fuzzyInclude = None,
            fuzzyExclude = None,
            excludeFields = None,
            showEmptyFields = None,
            restart = Some(true),
            restartDelayMs = None,
            maxRestarts = None
          )
        )
      )
    )

    val resolvedConfigs = ConfigResolver.resolve(standardConfig, Some(configYaml))

    assertEquals(resolvedConfigs.length, 3)

    val restartFeed = resolvedConfigs.find(_.feedName.contains("restart-feed")).get
    assertEquals(restartFeed.restart, true)
    assertEquals(restartFeed.restartDelayMs, 2000L)
    assertEquals(restartFeed.maxRestarts, Some(5))

    val noRestartFeed = resolvedConfigs.find(_.feedName.contains("no-restart-feed")).get
    assertEquals(noRestartFeed.restart, false)
    assertEquals(noRestartFeed.restartDelayMs, 1000L) // default
    assertEquals(noRestartFeed.maxRestarts, None)

    val unlimitedFeed = resolvedConfigs.find(_.feedName.contains("unlimited-restart-feed")).get
    assertEquals(unlimitedFeed.restart, true)
    assertEquals(unlimitedFeed.restartDelayMs, 1000L) // default
    assertEquals(unlimitedFeed.maxRestarts, None)
  }

  test("resolve returns default restart values when no ConfigYaml provided") {
    val resolvedConfigs = ConfigResolver.resolve(standardConfig, None)

    assertEquals(resolvedConfigs.length, 1)
    val resolved = resolvedConfigs.head
    assertEquals(resolved.restart, false)
    assertEquals(resolved.restartDelayMs, 1000L)
    assertEquals(resolved.maxRestarts, None)
  }
}
