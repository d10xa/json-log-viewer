package ru.d10xa.jsonlogviewer.query

import ru.d10xa.jsonlogviewer.Config
import ru.d10xa.jsonlogviewer.LogLineQueryPredicateImpl
import ru.d10xa.jsonlogviewer.ParseResult
import ru.d10xa.jsonlogviewer.ParseResultKeys
import ru.d10xa.jsonlogviewer.ParsedLine
import ru.d10xa.jsonlogviewer.TimestampConfig

class LogLineQueryPredicateImplTest extends munit.FunSuite {
  val config = Config(
    TimestampConfig(
      fieldName = "@timestamp",
      None,
      None
    ),
    List.empty,
    None
  )
  test("startsWith") {
    assert(likeE("a*").test(msg("abc")))
  }
  test("endsWith") {
    assert(likeE("*c").test(msg("abc")))
  }
  test("contains") {
    assert(likeE("*b*").test(msg("abc")))
  }

  def likeE(s: String) =
    val le = LikeExpr(StrIdentifier("message"), StrLiteral(s), false)
    val qp = new LogLineQueryPredicateImpl(
      le,
      new ParseResultKeys(config = config)
    )
    qp

  def msg(m: String) = ParseResult(
    "",
    parsed = Some(
      ParsedLine(
        "",
        level = Some(""),
        message = Some(m),
        stackTrace = Some(""),
        loggerName = Some(""),
        threadName = Some(""),
        otherAttributes = Map.empty
      )
    ),
    "",
    None,
    None
  )
}
