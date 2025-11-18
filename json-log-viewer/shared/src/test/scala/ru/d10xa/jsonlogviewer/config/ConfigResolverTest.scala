package ru.d10xa.jsonlogviewer.config

import munit.FunSuite
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.Feed
import ru.d10xa.jsonlogviewer.decline.yaml.FieldNames
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig
import ru.d10xa.jsonlogviewer.decline.TimestampConfig

class ConfigResolverTest extends FunSuite {

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
    showEmptyFields = false
  )

  test("resolve returns single config when no ConfigYaml provided") {
    val resolvedConfigs = ConfigResolver.resolve(standardConfig, None)

    assertEquals(resolvedConfigs.length, 1)

    val resolved = resolvedConfigs.head
    assertEquals(resolved.feedName, None)
    assertEquals(resolved.commands, List.empty)
    assertEquals(resolved.fieldNames, standardConfig.fieldNames)
    assertEquals(resolved.filter, None)
    assertEquals(resolved.formatIn, None)
    assertEquals(resolved.formatOut, None)
  }

  test("resolve merges global fieldNames from ConfigYaml") {
    val configYaml = ConfigYaml(
      showEmptyFields = None,
      fieldNames = Some(
        FieldNames(
          timestamp = Some("ts"),
          level = Some("severity"),
          message = None,
          stackTrace = None,
          loggerName = None,
          threadName = None
        )
      ),
      feeds = None
    )

    val resolvedConfigs =
      ConfigResolver.resolve(standardConfig, Some(configYaml))

    assertEquals(resolvedConfigs.length, 1)

    val resolved = resolvedConfigs.head
    assertEquals(resolved.fieldNames.timestampFieldName, "ts")
    assertEquals(resolved.fieldNames.levelFieldName, "severity")
    assertEquals(
      resolved.fieldNames.messageFieldName,
      "message"
    ) // From standardConfig
  }

  test("resolve correctly handles multiple feeds") {
    val configYaml = ConfigYaml(
      showEmptyFields = None,
      fieldNames = Some(
        FieldNames(
          timestamp = Some("ts"),
          level = Some("severity"),
          message = None,
          stackTrace = None,
          loggerName = None,
          threadName = None
        )
      ),
      feeds = Some(
        List(
          Feed(
            name = Some("feed1"),
            commands = List("cmd1"),
            inlineInput = None,
            filter = None,
            formatIn = Some(Config.FormatIn.Json),
            fieldNames = Some(
              FieldNames(
                timestamp = None,
                level = None,
                message = Some("msg"),
                stackTrace = None,
                loggerName = None,
                threadName = None
              )
            ),
            rawInclude = None,
            rawExclude = None,
            fuzzyInclude = None,
            fuzzyExclude = None,
            excludeFields = None,
            showEmptyFields = None
          ),
          Feed(
            name = Some("feed2"),
            commands = List("cmd2"),
            inlineInput = None,
            filter = None,
            formatIn = Some(Config.FormatIn.Csv),
            fieldNames = None,
            rawInclude = None,
            rawExclude = None,
            fuzzyInclude = None,
            fuzzyExclude = None,
            excludeFields = None,
            showEmptyFields = None
          )
        )
      )
    )

    val resolvedConfigs =
      ConfigResolver.resolve(standardConfig, Some(configYaml))

    assertEquals(resolvedConfigs.length, 2)

    val feed1 = resolvedConfigs.find(_.feedName.contains("feed1")).get
    assertEquals(feed1.commands, List("cmd1"))
    assertEquals(feed1.formatIn, Some(Config.FormatIn.Json))
    assertEquals(feed1.fieldNames.timestampFieldName, "ts") // From global
    assertEquals(feed1.fieldNames.levelFieldName, "severity") // From global
    assertEquals(feed1.fieldNames.messageFieldName, "msg") // From feed1

    val feed2 = resolvedConfigs.find(_.feedName.contains("feed2")).get
    assertEquals(feed2.commands, List("cmd2"))
    assertEquals(feed2.formatIn, Some(Config.FormatIn.Csv))
    assertEquals(feed2.fieldNames.timestampFieldName, "ts") // From global
    assertEquals(feed2.fieldNames.levelFieldName, "severity") // From global
    assertEquals(
      feed2.fieldNames.messageFieldName,
      "message"
    ) // Default, not overridden
  }
}
