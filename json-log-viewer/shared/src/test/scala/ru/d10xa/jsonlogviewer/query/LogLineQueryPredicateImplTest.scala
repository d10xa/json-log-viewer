package ru.d10xa.jsonlogviewer.query

import ru.d10xa.jsonlogviewer.Config
import ru.d10xa.jsonlogviewer.LogLineQueryPredicateImpl
import ru.d10xa.jsonlogviewer.ParseResult
import ru.d10xa.jsonlogviewer.ParseResultKeys
import ru.d10xa.jsonlogviewer.ParsedLine
import ru.d10xa.jsonlogviewer.TimestampConfig
import LogLineQueryPredicateImpl.likeContains

class LogLineQueryPredicateImplTest extends munit.FunSuite {

  test("startsWith") {
    assert(likeContains("abc", "a%"))
    assert(likeE("a%").test(msg("abc")))
    assert(!likeE("a%").test(msg("cba")))
  }
  test("endsWith") {
    assert(likeContains("abc", "%c"))
    assert(likeE("%c").test(msg("abc")))
    assert(!likeE("%c").test(msg("cba")))
  }
  test("contains") {
    assert(likeContains("abc", "%b%"))
    assert(likeE("%b%").test(msg("abc")))
    assert(!likeE("%b%").test(msg("ac")))
  }
  private val config: Config = Config(
    TimestampConfig(
      fieldName = "@timestamp",
      None,
      None
    ),
    List.empty,
    None
  )
  private def likeE(s: String): LogLineQueryPredicateImpl =
    val le = LikeExpr(StrIdentifier("message"), StrLiteral(s), false)
    val qp = new LogLineQueryPredicateImpl(
      le,
      new ParseResultKeys(config = config)
    )
    qp

  private def msg(m: String) = ParseResult(
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
