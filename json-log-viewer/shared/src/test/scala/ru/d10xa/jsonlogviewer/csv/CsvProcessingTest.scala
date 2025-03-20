package ru.d10xa.jsonlogviewer.csv

import cats.effect.unsafe.implicits.global
import cats.effect.IO
import cats.effect.Ref
import fs2.Stream
import munit.CatsEffectSuite
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig
import ru.d10xa.jsonlogviewer.decline.TimestampConfig
import ru.d10xa.jsonlogviewer.LogViewerStream
import ru.d10xa.jsonlogviewer.StdInLinesStream

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
      formatOut = Some(Config.FormatOut.Raw)
    )

    for {
      configRef <- Ref.of[IO, Option[ConfigYaml]](None)

      testStreamImpl = new StdInLinesStream {
        override def stdinLinesStream: fs2.Stream[IO, String] =
          Stream.emits(List(csvHeader, csvLine1, csvLine2))
      }

      _ <- IO(LogViewerStream.setStdInLinesStreamImpl(testStreamImpl))

      results <- LogViewerStream.stream(csvConfig, configRef).compile.toList

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
      formatOut = Some(Config.FormatOut.Raw)
    )

    for {
      configRef <- Ref.of[IO, Option[ConfigYaml]](None)

      testStreamImpl = new StdInLinesStream {
        override def stdinLinesStream: fs2.Stream[IO, String] =
          Stream.emits(List(csvHeader, csvLine))
      }

      _ <- IO(LogViewerStream.setStdInLinesStreamImpl(testStreamImpl))

      results <- LogViewerStream.stream(csvConfig, configRef).compile.toList

    } yield {
      assert(results.nonEmpty, "Results should not be empty")
      assert(
        results.exists(_.contains("Custom log message")),
        "Results should contain the message despite different column order"
      )
    }
  }
}
