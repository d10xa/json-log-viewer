package ru.d10xa.jsonlogviewer.query

import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.LogLineQueryPredicateImpl
import ru.d10xa.jsonlogviewer.LogLineQueryPredicateImpl.likeContains
import ru.d10xa.jsonlogviewer.ParseResult
import ru.d10xa.jsonlogviewer.ParseResultKeys
import ru.d10xa.jsonlogviewer.ParsedLine
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.TimestampConfig

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
  test("(or) and (or)") {
    assert(
      testQ(
        parseResult(other = Map("a" -> "a", "b" -> "b", "c" -> "c")),
        "(a = 'a' OR d = '?') AND (b = 'b' OR c = '?')"
      )
    )
  }
  test("((or) and (or)) or") {
    assert(
      testQ(
        parseResult(other = Map("a" -> "a", "b" -> "b", "c" -> "c")),
        "((a = '?' OR d = '?') AND (b = '?' OR unknown_field = '?') OR a = 'a')"
      )
    )
  }

  private def testQ(pr: ParseResult, q: String): Boolean = {
    val e: Either[QueryCompilationError, QueryAST] = QueryCompiler(q)
    new LogLineQueryPredicateImpl(
      e.fold(e => throw new RuntimeException(e.toString), identity),
      parseResultKeys
    ).test(pr)
  }

  private def parseResult(
    message: Option[String] = None,
    other: Map[String, String] = Map.empty
  ) = ParseResult(
    raw = "",
    parsed = Some(
      value = ParsedLine(
        timestamp = Some(value = ""),
        level = Some(value = ""),
        message = message,
        stackTrace = None,
        loggerName = Some(value = ""),
        threadName = Some(value = ""),
        otherAttributes = other
      )
    ),
    middle = "",
    prefix = None,
    postfix = None
  )

  private val config: Config = Config(
    configFile = None,
    configYaml = None,
    timestamp = TimestampConfig(
      fieldName = "@timestamp",
      None,
      None
    ),
    grep = List.empty,
    filter = None,
    formatIn = None,
    formatOut = None
  )

  private lazy val parseResultKeys = new ParseResultKeys(config = config)

  private def messageLike(s: String): LogLineQueryPredicateImpl =
    val le = LikeExpr(StrIdentifier("message"), StrLiteral(s), false)
    new LogLineQueryPredicateImpl(le, parseResultKeys)

  private def stackTraceLike(
    s: String,
    negate: Boolean
  ): LogLineQueryPredicateImpl =
    val le = LikeExpr(StrIdentifier("stack_trace"), StrLiteral(s), negate)
    new LogLineQueryPredicateImpl(le, parseResultKeys)
  private def customFieldLike(
    s: String,
    negate: Boolean
  ): LogLineQueryPredicateImpl =
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
