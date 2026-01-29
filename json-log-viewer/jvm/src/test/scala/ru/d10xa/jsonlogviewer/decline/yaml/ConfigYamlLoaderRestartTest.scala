package ru.d10xa.jsonlogviewer.decline.yaml

import munit.FunSuite

class ConfigYamlLoaderRestartTest extends FunSuite {

  private val configYamlLoader: ConfigYamlLoader = new ConfigYamlLoaderImpl

  test("parse yaml with restart enabled") {
    val yaml =
      """|feeds:
         |  - name: "pod-logs"
         |    commands:
         |      - "kubectl logs -f deployment/my-app"
         |    restart: true
         |    restartDelayMs: 2000
         |    maxRestarts: 10
         |""".stripMargin

    val result = configYamlLoader.parseYamlFile(yaml)
    assert(result.isValid, s"Result should be valid: $result")

    val config = result.toOption.get
    val feeds = config.feeds.get
    assertEquals(feeds.size, 1)

    val feed = feeds.head
    assertEquals(feed.name, Some("pod-logs"))
    assertEquals(feed.restart, Some(true))
    assertEquals(feed.restartDelayMs, Some(2000L))
    assertEquals(feed.maxRestarts, Some(10))
  }

  test("parse yaml with restart disabled (default)") {
    val yaml =
      """|feeds:
         |  - name: "service-logs"
         |    commands:
         |      - "./mock-logs.sh service1"
         |""".stripMargin

    val result = configYamlLoader.parseYamlFile(yaml)
    assert(result.isValid, s"Result should be valid: $result")

    val config = result.toOption.get
    val feeds = config.feeds.get
    assertEquals(feeds.size, 1)

    val feed = feeds.head
    assertEquals(feed.restart, None)
    assertEquals(feed.restartDelayMs, None)
    assertEquals(feed.maxRestarts, None)
  }

  test("parse yaml with restart but no maxRestarts (unlimited)") {
    val yaml =
      """|feeds:
         |  - name: "unlimited-restarts"
         |    commands:
         |      - "tail -f /var/log/syslog"
         |    restart: true
         |    restartDelayMs: 5000
         |""".stripMargin

    val result = configYamlLoader.parseYamlFile(yaml)
    assert(result.isValid, s"Result should be valid: $result")

    val config = result.toOption.get
    val feeds = config.feeds.get
    assertEquals(feeds.size, 1)

    val feed = feeds.head
    assertEquals(feed.restart, Some(true))
    assertEquals(feed.restartDelayMs, Some(5000L))
    assertEquals(feed.maxRestarts, None)
  }

  test("parse yaml with multiple feeds with different restart configs") {
    val yaml =
      """|feeds:
         |  - name: "kubernetes-logs"
         |    commands:
         |      - "kubectl logs -f deployment/app"
         |    restart: true
         |    restartDelayMs: 2000
         |    maxRestarts: 5
         |  - name: "static-logs"
         |    commands:
         |      - "cat /var/log/app.log"
         |    restart: false
         |  - name: "unlimited-restart"
         |    commands:
         |      - "tail -f /var/log/syslog"
         |    restart: true
         |""".stripMargin

    val result = configYamlLoader.parseYamlFile(yaml)
    assert(result.isValid, s"Result should be valid: $result")

    val config = result.toOption.get
    val feeds = config.feeds.get
    assertEquals(feeds.size, 3)

    val k8sFeed = feeds.find(_.name.contains("kubernetes-logs")).get
    assertEquals(k8sFeed.restart, Some(true))
    assertEquals(k8sFeed.restartDelayMs, Some(2000L))
    assertEquals(k8sFeed.maxRestarts, Some(5))

    val staticFeed = feeds.find(_.name.contains("static-logs")).get
    assertEquals(staticFeed.restart, Some(false))
    assertEquals(staticFeed.restartDelayMs, None)
    assertEquals(staticFeed.maxRestarts, None)

    val unlimitedFeed = feeds.find(_.name.contains("unlimited-restart")).get
    assertEquals(unlimitedFeed.restart, Some(true))
    assertEquals(unlimitedFeed.restartDelayMs, None)
    assertEquals(unlimitedFeed.maxRestarts, None)
  }
}
