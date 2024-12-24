package ru.d10xa.jsonlogviewer

import cats.effect.IO
import fs2.Stream
import fs2.io.*
import cats.effect.syntax.all.*
import munit.FunSuite
import cats.effect.unsafe.implicits.*
import scala.concurrent.duration.*
import java.time.ZonedDateTime
import scala.concurrent.Await

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
      ).compile.toList.unsafeToFuture()
   
    assertEquals( Await.result(list, 1.second), List(t2))
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
