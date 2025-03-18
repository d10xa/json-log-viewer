package ru.d10xa.jsonlogviewer.decline.yaml

import cats.data.Validated
import munit.FunSuite
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYamlLoader
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYamlLoaderImpl
import ru.d10xa.jsonlogviewer.query.QueryAST

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
  }

  test("parse invalid yaml") {
    val yaml =
      """feeds: ""
        |""".stripMargin
    val result = configYamlLoader.parseYamlFile(yaml)
    assert(result.isInvalid, s"Result should be invalid: $result")

    val errors = result.swap.toOption.get
    assert(errors.exists(_.contains("Invalid 'feeds' field format, should be a list")))
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
}
