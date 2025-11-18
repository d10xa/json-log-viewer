package ru.d10xa.jsonlogviewer

import cats.effect.IO
import cats.effect.Ref
import fs2.concurrent.Channel
import fs2.Stream
import munit.CatsEffectSuite
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.Feed
import ru.d10xa.jsonlogviewer.decline.yaml.FieldNames
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig
import ru.d10xa.jsonlogviewer.decline.TimestampConfig
import ru.d10xa.jsonlogviewer.query.QueryCompiler
import ru.d10xa.jsonlogviewer.shell.ShellImpl

import scala.concurrent.duration.*

class LogViewerStreamIntegrationTest extends CatsEffectSuite {

  // Sample log entries with different formats
  val infoLog =
    """{"@timestamp":"2023-01-01T10:00:00Z","level":"INFO","message":"Info message","logger_name":"TestLogger","thread_name":"main"}"""
  val errorLog =
    """{"@timestamp":"2023-01-01T12:00:00Z","level":"ERROR","message":"Error message","logger_name":"TestLogger","thread_name":"main"}"""
  val customLog =
    """{"ts":"2023-01-01T12:00:00Z","severity":"ERROR","msg":"Custom message","logger_name":"TestLogger","thread_name":"main"}"""

  // Default configuration
  val baseConfig = Config(
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
    formatIn = Some(Config.FormatIn.Json),
    formatOut = Some(Config.FormatOut.Raw),
    showEmptyFields = false
  )

  test("config filters should update during live reload") {
    // Create config with INFO filter
    val infoFilter = QueryCompiler("level = 'INFO'").toOption
    val initialConfig = ConfigYaml(
      showEmptyFields = None,
      fieldNames = None,
      feeds = Some(
        List(
          Feed(
            name = Some("test-feed"),
            commands = List.empty,
            inlineInput = None,
            filter = infoFilter,
            formatIn = None,
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

    // Create updated config with ERROR filter
    val errorFilter = QueryCompiler("level = 'ERROR'").toOption
    val updatedConfig = initialConfig.copy(
      feeds = initialConfig.feeds.map(_.map(_.copy(filter = errorFilter)))
    )

    // Results collector
    val results = scala.collection.mutable.ArrayBuffer.empty[String]

    for {
      // Initialize config reference with initial configuration
      configRef <- Ref.of[IO, Option[ConfigYaml]](Some(initialConfig))

      // Setup input channel for test logs
      logInputChannel <- Channel.unbounded[IO, String]

      // Create test stream implementation
      testStreamImpl = new StdInLinesStream {
        override def stdinLinesStream: Stream[IO, String] =
          logInputChannel.stream
      }

      // Start stream processing in background
      streamFiber <- LogViewerStream
        .stream(baseConfig, configRef, testStreamImpl, new ShellImpl)
        .evalTap(result => IO(results.append(result)))
        .compile
        .drain
        .start

      // Wait for stream initialization
      _ <- IO.sleep(500.millis)

      // Phase 1: Test initial INFO filter
      _ <- logInputChannel.send(infoLog)
      _ <- IO.sleep(100.millis)
      _ <- logInputChannel.send(errorLog)
      _ <- IO.sleep(500.millis)

      // Collect results and clear buffer
      initialResults = results.toList
      _ <- IO(results.clear())

      // Phase 2: Update configuration to ERROR filter
      _ <- configRef.set(Some(updatedConfig))
      _ <- IO.sleep(500.millis)

      // Send same logs with updated filter
      _ <- logInputChannel.send(infoLog)
      _ <- IO.sleep(100.millis)
      _ <- logInputChannel.send(errorLog)
      _ <- IO.sleep(500.millis)

      // Collect results after filter update
      updatedResults = results.toList

      // Cleanup
      _ <- streamFiber.cancel

    } yield {
      // Verify initial INFO filter
      assert(
        initialResults.exists(_.contains("Info message")),
        "INFO log should pass the initial INFO filter"
      )
      assert(
        !initialResults.exists(_.contains("Error message")),
        "ERROR log should not pass the initial INFO filter"
      )

      // Verify updated ERROR filter
      assert(
        !updatedResults.exists(_.contains("Info message")),
        "INFO log should not pass the updated ERROR filter"
      )
      assert(
        updatedResults.exists(_.contains("Error message")),
        "ERROR log should pass the updated ERROR filter"
      )
    }
  }

  test("field mappings should update during live reload") {
    // Initial configuration with standard field names
    val initialConfig = ConfigYaml(
      showEmptyFields = None,
      fieldNames = None,
      feeds = Some(
        List(
          Feed(
            name = Some("test-feed"),
            commands = List.empty,
            inlineInput = None,
            filter = None,
            formatIn = None,
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

    // Standard config with ERROR filter (works with standard field names)
    val errorFilterConfig = baseConfig.copy(
      filter = QueryCompiler("level = 'ERROR'").toOption
    )

    // Updated config with custom field names mapping
    val updatedConfig = ConfigYaml(
      showEmptyFields = None,
      fieldNames = Some(
        FieldNames(
          timestamp = Some("ts"),
          level = Some("severity"),
          message = Some("msg"),
          stackTrace = None,
          loggerName = None,
          threadName = None
        )
      ),
      feeds = initialConfig.feeds
    )

    // Results collector
    val results = scala.collection.mutable.ArrayBuffer.empty[String]

    for {
      // Initialize config reference with initial configuration
      configRef <- Ref.of[IO, Option[ConfigYaml]](Some(initialConfig))

      // Setup input channel for test logs
      logInputChannel <- Channel.unbounded[IO, String]

      // Create test stream implementation
      testStreamImpl = new StdInLinesStream {
        override def stdinLinesStream: Stream[IO, String] =
          logInputChannel.stream
      }

      // Start stream processing in background
      streamFiber <- LogViewerStream
        .stream(errorFilterConfig, configRef, testStreamImpl, new ShellImpl)
        .evalTap(result => IO(results.append(result)))
        .compile
        .drain
        .start

      // Wait for stream initialization
      _ <- IO.sleep(100.millis)

      // Phase 1: Test with standard field names mapping
      _ <- logInputChannel.send(infoLog)
      _ <- logInputChannel.send(errorLog)
      _ <- logInputChannel.send(customLog)
      _ <- IO.sleep(200.millis)

      // Collect results and clear buffer
      initialResults = results.toList
      _ <- IO(results.clear())

      // Phase 2: Update configuration to use custom field names
      _ <- configRef.set(Some(updatedConfig))
      _ <- IO.sleep(100.millis)

      // Send same logs with updated field mappings
      _ <- logInputChannel.send(infoLog)
      _ <- logInputChannel.send(errorLog)
      _ <- logInputChannel.send(customLog)
      _ <- IO.sleep(200.millis)

      // Collect results after field mapping update
      updatedResults = results.toList

      // Cleanup
      _ <- streamFiber.cancel

    } yield {
      // Verify initial field name mapping
      assert(
        !initialResults.exists(_.contains("Info message")),
        "INFO log should not pass ERROR filter"
      )
      assert(
        initialResults.exists(_.contains("Error message")),
        "Standard ERROR log should pass the filter"
      )
      assert(
        !initialResults.exists(_.contains("Custom message")),
        "Custom log should not pass filter with initial mapping"
      )

      // Verify updated field name mapping
      assert(
        !updatedResults.exists(_.contains("Info message")),
        "INFO log should still not pass ERROR filter"
      )
      assert(
        updatedResults.exists(_.contains("Error message")),
        "Standard ERROR log should still pass the filter"
      )
      assert(
        updatedResults.exists(_.contains("Custom message")),
        "Custom log should now pass filter with updated mapping"
      )
    }
  }
}
