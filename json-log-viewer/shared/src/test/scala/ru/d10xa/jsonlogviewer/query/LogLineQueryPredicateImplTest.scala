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
    assert(messageLike("a%").test(msg("abc")))
    assert(!messageLike("a%").test(msg("cba")))
  }
  test("endsWith") {
    assert(likeContains("abc", "%c"))
    assert(messageLike("%c").test(msg("abc")))
    assert(!messageLike("%c").test(msg("cba")))
  }
  test("contains") {
    assert(likeContains("abc", "%b%"))
    assert(messageLike("%b%").test(msg("abc")))
    assert(!messageLike("%b%").test(msg("ac")))
  }
  test("like empty") {
    assert(!stackTraceLike("", false).test(msg("")))
  }
  test("not like empty") {
    assert(stackTraceLike("", true).test(msg("")))
  }
  test("custom field") {
    assert(customFieldLike("custom", false).test(msg("custom")))
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

  private val parseResultKeys = new ParseResultKeys(config = config)

  private def messageLike(s: String): LogLineQueryPredicateImpl =
    val le = LikeExpr(StrIdentifier("message"), StrLiteral(s), false)
    new LogLineQueryPredicateImpl(le, parseResultKeys)


  private def stackTraceLike(s: String, negate: Boolean): LogLineQueryPredicateImpl =
    val le = LikeExpr(StrIdentifier("stack_trace"), StrLiteral(s), negate)
    new LogLineQueryPredicateImpl(le, parseResultKeys)
  private def customFieldLike(s: String, negate: Boolean): LogLineQueryPredicateImpl =
    val le = LikeExpr(StrIdentifier("custom_field"), StrLiteral(s), negate)
    new LogLineQueryPredicateImpl(le, parseResultKeys)

  private def msg(m: String) = ParseResult(
    "",
    parsed = Some(
      ParsedLine(
        timestamp = Some(""),
        level = Some(""),
        message = Some(m),
        stackTrace = None,
        loggerName = Some(""),
        threadName = Some(""),
        otherAttributes = Map(
          "custom_field" -> m
        )
      )
    ),
    "",
    None,
    None
  )
}
