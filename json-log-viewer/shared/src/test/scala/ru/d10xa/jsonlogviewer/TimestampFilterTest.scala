package ru.d10xa.jsonlogviewer

import fs2.Stream
import munit.FunSuite

import java.time.ZonedDateTime

class TimestampFilterTest extends FunSuite {
  test("filterTimestampAfter") {
    val filter = TimestampFilter()
    val t1 = pr("2023-09-17T19:10:01.132318Z")
    val t2 = pr("2023-09-19T19:10:03.132318Z")
    val stream = Stream.emits(Seq(t1, t2))
    val list = stream
      .through(
        filter.filterTimestampAfter(
          Some(ZonedDateTime.parse("2023-09-18T19:10:02Z"))
        )
      )
      .toList
    assertEquals(list, List(t2))
  }

  def pr(ts: String): ParseResult = ParseResult(
    raw = "",
    parsed = Some(
      ParsedLine(
        timestamp = Some(ts),
        level = None,
        message = None,
        stackTrace = None,
        loggerName = None,
        threadName = None,
        otherAttributes = Map.empty
      )
    ),
    middle = "",
    prefix = None,
    postfix = None
  )
}
