package ru.d10xa.jsonlogviewer.decline.yaml

import munit.FunSuite
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYamlLoader
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn

class ConfigYamlLoaderTest extends FunSuite {

  private val configYamlLoader: ConfigYamlLoader = new ConfigYamlLoaderImpl

  test("parse valid yaml with feeds") {
    val yaml =
      """|feeds:
         |  - name: "pod-logs"
         |    commands:
         |      - "./mock-logs.sh pod1"
         |      - "./mock-logs.sh pod2"
         |    filter: |
         |      message = 'first line'
         |    formatIn: json
         |  - name: "service-logs"
         |    commands:
         |      - "./mock-logs.sh service1"
         |    filter: |
         |      message = 'first line'
         |    formatIn: logfmt
         |""".stripMargin

    val result = configYamlLoader.parseYamlFile(yaml)
    assert(result.isValid, s"Result should be valid: $result")

    val config = result.toOption.get

    val feeds = config.feeds.get
    assertEquals(feeds.size, 2)

    val feed1 = feeds.head
    assertEquals(feed1.name, Some("pod-logs"))
    assertEquals(
      feed1.commands,
      List("./mock-logs.sh pod1", "./mock-logs.sh pod2")
    )
    assertEquals(feed1.formatIn, Some(FormatIn.Json))

    val feed2 = feeds(1)
    assertEquals(feed2.name, Some("service-logs"))
    assertEquals(feed2.commands, List("./mock-logs.sh service1"))
    assertEquals(feed2.formatIn, Some(FormatIn.Logfmt))
  }

  test("parse empty yaml") {
    val yaml = ""
    val result = configYamlLoader.parseYamlFile(yaml)
    assert(result.isValid, s"Result should be valid for empty yaml: $result")

    val config = result.toOption.get
    assert(config.feeds.isEmpty)
    assert(config.fieldNames.isEmpty)
  }

  test("parse invalid yaml") {
    val yaml =
      """feeds: ""
        |""".stripMargin
    val result = configYamlLoader.parseYamlFile(yaml)
    assert(result.isInvalid, s"Result should be invalid: $result")

    val errors = result.swap.toOption.get
    assert(
      errors.exists(
        _.contains("Invalid 'feeds' field format, should be a list")
      )
    )
  }

  test("parse valid yaml with excludeFields") {
    val yaml =
      """|feeds:
         |  - name: "pod-logs"
         |    commands:
         |      - "./mock-logs.sh pod1"
         |    excludeFields:
         |      - "level"
         |      - "logger_name"
         |      - "thread_name"
         |  - name: "service-logs"
         |    commands:
         |      - "./mock-logs.sh service1"
         |    excludeFields:
         |      - "@timestamp"
         |""".stripMargin

    val result = configYamlLoader.parseYamlFile(yaml)
    assert(result.isValid, s"Result should be valid: $result")

    val config = result.toOption.get

    val feeds = config.feeds.get
    assertEquals(feeds.size, 2)

    val feed1 = feeds.head
    assertEquals(feed1.name, Some("pod-logs"))
    assertEquals(
      feed1.excludeFields,
      Some(List("level", "logger_name", "thread_name"))
    )

    val feed2 = feeds(1)
    assertEquals(feed2.name, Some("service-logs"))
    assertEquals(feed2.excludeFields, Some(List("@timestamp")))
  }

  test("parse yaml with fieldNames") {
    val yaml =
      """|fieldNames:
         |  timestamp: "ts"
         |  level: "severity"
         |  message: "text"
         |  stackTrace: "error"
         |  loggerName: "logger"
         |  threadName: "thread"
         |feeds:
         |  - name: "pod-logs"
         |    commands:
         |      - "./mock-logs.sh pod1"
         |  - name: "service-logs"
         |    commands:
         |      - "./mock-logs.sh service1"
         |    fieldNames:
         |      timestamp: "time"
         |      level: "priority"
         |""".stripMargin

    val result = configYamlLoader.parseYamlFile(yaml)
    assert(result.isValid, s"Result should be valid: $result")

    val config = result.toOption.get

    // Check global fieldNames
    val fieldNames = config.fieldNames.get
    assertEquals(fieldNames.timestamp, Some("ts"))
    assertEquals(fieldNames.level, Some("severity"))
    assertEquals(fieldNames.message, Some("text"))
    assertEquals(fieldNames.stackTrace, Some("error"))
    assertEquals(fieldNames.loggerName, Some("logger"))
    assertEquals(fieldNames.threadName, Some("thread"))

    // Check feed-specific fieldNames
    val feeds = config.feeds.get
    assertEquals(feeds.size, 2)

    val feed1 = feeds.head
    assertEquals(feed1.name, Some("pod-logs"))
    assertEquals(feed1.fieldNames, None)

    val feed2 = feeds(1)
    assertEquals(feed2.name, Some("service-logs"))
    assert(feed2.fieldNames.isDefined)
    assertEquals(feed2.fieldNames.get.timestamp, Some("time"))
    assertEquals(feed2.fieldNames.get.level, Some("priority"))
  }

  test("parse yaml with partial fieldNames") {
    val yaml =
      """|fieldNames:
         |  timestamp: "ts"
         |  level: "severity"
         |feeds:
         |  - name: "service-logs"
         |    commands:
         |      - "./mock-logs.sh service1"
         |    fieldNames:
         |      message: "content"
         |""".stripMargin

    val result = configYamlLoader.parseYamlFile(yaml)
    assert(result.isValid, s"Result should be valid: $result")

    val config = result.toOption.get

    // Check global fieldNames
    val fieldNames = config.fieldNames.get
    assertEquals(fieldNames.timestamp, Some("ts"))
    assertEquals(fieldNames.level, Some("severity"))
    assertEquals(fieldNames.message, None)

    // Check feed-specific fieldNames
    val feeds = config.feeds.get
    val feed = feeds.head
    assertEquals(feed.name, Some("service-logs"))
    assert(feed.fieldNames.isDefined)
    assertEquals(feed.fieldNames.get.message, Some("content"))
    assertEquals(
      feed.fieldNames.get.timestamp,
      None
    )
  }
}
