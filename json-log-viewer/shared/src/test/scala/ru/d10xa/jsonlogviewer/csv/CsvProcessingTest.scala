package ru.d10xa.jsonlogviewer.csv

import cats.effect.IO
import cats.effect.Ref
import fs2.Stream
import fs2.concurrent.Channel
import munit.CatsEffectSuite
import ru.d10xa.jsonlogviewer.cache.CachedResolvedState
import ru.d10xa.jsonlogviewer.cache.FilterCacheManager
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.FieldNames
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig
import ru.d10xa.jsonlogviewer.decline.TimestampConfig
import ru.d10xa.jsonlogviewer.query.QueryCompiler
import ru.d10xa.jsonlogviewer.shell.ShellImpl
import ru.d10xa.jsonlogviewer.LogViewerStream
import ru.d10xa.jsonlogviewer.StdInLinesStream
import ru.d10xa.jsonlogviewer.StreamContext

import scala.concurrent.duration.*

class CsvProcessingTest extends CatsEffectSuite {

  test("should process CSV format with header line") {
    val csvHeader = "@timestamp,level,message,logger_name,thread_name"
    val csvLine1 = "2023-01-01T10:00:00Z,INFO,Log message 1,TestLogger,main"
    val csvLine2 = "2023-01-01T11:00:00Z,WARN,Log message 2,TestLogger,main"

    val csvConfig = Config(
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
      formatIn = Some(Config.FormatIn.Csv),
      formatOut = Some(Config.FormatOut.Raw),
      showEmptyFields = false,
      commands = List.empty,
      restart = false,
      restartDelayMs = None,
      maxRestarts = None
    )

    val initialConfigYaml: Option[ConfigYaml] = None
    val initialCache = FilterCacheManager.buildCache(csvConfig, initialConfigYaml).fold(err => fail(s"buildCache failed: $err"), identity)

    for {
      configRef <- Ref.of[IO, Option[ConfigYaml]](initialConfigYaml)
      cacheRef <- Ref.of[IO, CachedResolvedState](initialCache)

      testStreamImpl = new StdInLinesStream {
        override def stdinLinesStream: fs2.Stream[IO, String] =
          Stream.emits(List(csvHeader, csvLine1, csvLine2))
      }

      ctx = StreamContext(
        config = csvConfig,
        configYamlRef = configRef,
        cacheRef = cacheRef,
        stdinStream = testStreamImpl,
        shell = new ShellImpl
      )
      results <- LogViewerStream
        .stream(ctx)
        .compile
        .toList

    } yield {
      assert(results.nonEmpty, "Results should not be empty")

      assert(
        results.exists(_.contains("Log message 1")),
        "Results should contain data from first CSV line"
      )
      assert(
        results.exists(_.contains("Log message 2")),
        "Results should contain data from second CSV line"
      )
    }
  }

  test(
    "should handle CSV with different column order than default field names"
  ) {
    val csvHeader =
      "message,level,@timestamp,thread_name,logger_name"
    val csvLine =
      "Custom log message,INFO,2023-01-01T12:00:00Z,worker-1,CustomLogger"

    val csvConfig = Config(
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
      formatIn = Some(Config.FormatIn.Csv),
      formatOut = Some(Config.FormatOut.Raw),
      showEmptyFields = false,
      commands = List.empty,
      restart = false,
      restartDelayMs = None,
      maxRestarts = None
    )

    val initialConfigYaml: Option[ConfigYaml] = None
    val initialCache = FilterCacheManager.buildCache(csvConfig, initialConfigYaml).fold(err => fail(s"buildCache failed: $err"), identity)

    for {
      configRef <- Ref.of[IO, Option[ConfigYaml]](initialConfigYaml)
      cacheRef <- Ref.of[IO, CachedResolvedState](initialCache)

      testStreamImpl = new StdInLinesStream {
        override def stdinLinesStream: fs2.Stream[IO, String] =
          Stream.emits(List(csvHeader, csvLine))
      }

      ctx = StreamContext(
        config = csvConfig,
        configYamlRef = configRef,
        cacheRef = cacheRef,
        stdinStream = testStreamImpl,
        shell = new ShellImpl
      )
      results <- LogViewerStream
        .stream(ctx)
        .compile
        .toList

    } yield {
      assert(results.nonEmpty, "Results should not be empty")
      assert(
        results.exists(_.contains("Custom log message")),
        "Results should contain the message despite different column order"
      )
    }
  }

  test("should update CSV field mappings during live reload") {
    // CSV with custom column names (severity, msg instead of level, message)
    val csvHeader = "ts,severity,msg,logger_name,thread_name"
    val csvLineError = "2023-01-01T10:00:00Z,ERROR,Error message,TestLogger,main"
    val csvLineInfo = "2023-01-01T11:00:00Z,INFO,Info message,TestLogger,main"

    // Config with standard field names and ERROR filter
    val csvConfig = Config(
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
      filter = QueryCompiler("level = 'ERROR'").toOption,
      formatIn = Some(Config.FormatIn.Csv),
      formatOut = Some(Config.FormatOut.Raw),
      showEmptyFields = false,
      commands = List.empty,
      restart = false,
      restartDelayMs = None,
      maxRestarts = None
    )

    // Initial config without field name mapping
    val initialConfigYaml: Option[ConfigYaml] = None
    val initialCache = FilterCacheManager.buildCache(csvConfig, initialConfigYaml).fold(err => fail(s"buildCache failed: $err"), identity)

    // Updated config with custom field names mapping
    val updatedConfigYaml = Some(
      ConfigYaml(
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
        feeds = None
      )
    )

    val results = scala.collection.mutable.ArrayBuffer.empty[String]

    for {
      configRef <- Ref.of[IO, Option[ConfigYaml]](initialConfigYaml)
      cacheRef <- Ref.of[IO, CachedResolvedState](initialCache)
      logInputChannel <- Channel.unbounded[IO, String]

      testStreamImpl = new StdInLinesStream {
        override def stdinLinesStream: Stream[IO, String] =
          logInputChannel.stream
      }

      ctx = StreamContext(
        config = csvConfig,
        configYamlRef = configRef,
        cacheRef = cacheRef,
        stdinStream = testStreamImpl,
        shell = new ShellImpl
      )
      streamFiber <- LogViewerStream
        .stream(ctx)
        .evalTap(result => IO(results.append(result)))
        .compile
        .drain
        .start

      // Wait for stream initialization
      _ <- IO.sleep(100.millis)

      // Send header first
      _ <- logInputChannel.send(csvHeader)
      _ <- IO.sleep(50.millis)

      // Phase 1: With standard field names, filter won't match 'severity' column
      _ <- logInputChannel.send(csvLineError)
      _ <- logInputChannel.send(csvLineInfo)
      _ <- IO.sleep(200.millis)

      initialResults = results.toList
      _ <- IO(results.clear())

      // Phase 2: Update config with custom field names mapping
      _ <- configRef.set(updatedConfigYaml)
      _ <- IO.sleep(100.millis)

      // Now filter should match 'severity' column as 'level'
      _ <- logInputChannel.send(csvLineError)
      _ <- logInputChannel.send(csvLineInfo)
      _ <- IO.sleep(200.millis)

      updatedResults = results.toList

      _ <- streamFiber.cancel

    } yield {
      // Before field mapping update: filter looks for 'level' column which doesn't exist
      assert(
        !initialResults.exists(_.contains("Error message")),
        s"Before mapping update: ERROR log should not pass filter (level column not found). Got: $initialResults"
      )

      // After field mapping update: 'severity' is now mapped to 'level'
      assert(
        updatedResults.exists(_.contains("Error message")),
        s"After mapping update: ERROR log should pass filter. Got: $updatedResults"
      )
      assert(
        !updatedResults.exists(_.contains("Info message")),
        s"After mapping update: INFO log should not pass ERROR filter. Got: $updatedResults"
      )
    }
  }
}
