package ru.d10xa.jsonlogviewer

import munit.FunSuite
import ru.d10xa.jsonlogviewer.decline.yaml.FieldNames
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig
import ru.d10xa.jsonlogviewer.config.ConfigResolver

class FieldNamesMergeTest extends FunSuite {

  val globalFieldNames: FieldNamesConfig = FieldNamesConfig(
    timestampFieldName = "@timestamp",
    levelFieldName = "level",
    messageFieldName = "message",
    stackTraceFieldName = "stack_trace",
    loggerNameFieldName = "logger_name",
    threadNameFieldName = "thread_name"
  )

  test("mergeFieldNames combines global and None") {
    val noFeedFieldNames =
      ConfigResolver.mergeFieldNames(globalFieldNames, None)
    assertEquals(noFeedFieldNames, globalFieldNames)
  }

  test("mergeFieldNames combines global and feed configs") {
    val partialFeedFieldNames = ConfigResolver.mergeFieldNames(
      globalFieldNames,
      Some(
        FieldNames(
          timestamp = Some("ts"),
          level = Some("severity"),
          message = None,
          stackTrace = None,
          loggerName = None,
          threadName = None
        )
      )
    )

    assertEquals(partialFeedFieldNames.timestampFieldName, "ts")
    assertEquals(partialFeedFieldNames.levelFieldName, "severity")
    assertEquals(
      partialFeedFieldNames.messageFieldName,
      "message"
    )
    assertEquals(
      partialFeedFieldNames.stackTraceFieldName,
      "stack_trace"
    )
    assertEquals(
      partialFeedFieldNames.loggerNameFieldName,
      "logger_name"
    )
    assertEquals(
      partialFeedFieldNames.threadNameFieldName,
      "thread_name"
    )
  }
}