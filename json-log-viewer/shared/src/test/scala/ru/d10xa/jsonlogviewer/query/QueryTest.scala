package ru.d10xa.jsonlogviewer.query

class QueryTest extends munit.FunSuite {
  test("eq") {
    val result = QueryCompiler("g = 'hello'")
    assertEquals(result, Right(Eq(StrIdentifier("g"), StrLiteral("hello"))))
  }
  test("neq") {
    val result = QueryCompiler("g != 'hello'")
    assertEquals(result, Right(Neq(StrIdentifier("g"), StrLiteral("hello"))))
  }
  test("like") {
    val result = QueryCompiler("g LIKE '*test*'")
    assertEquals(
      result,
      Right(LikeExpr(StrIdentifier("g"), StrLiteral("*test*"), false))
    )
  }
  test("not like") {
    val result = QueryCompiler("g NOT LIKE '*test*'")
    assertEquals(
      result,
      Right(LikeExpr(StrIdentifier("g"), StrLiteral("*test*"), true))
    )
  }
  test("or") {
    val result = QueryCompiler("a = 'a' OR b = 'b'")
    assertEquals(
      result,
      Right(
        OrExpr(
          Eq(StrIdentifier("a"), StrLiteral("a")),
          Eq(StrIdentifier("b"), StrLiteral("b"))
        )
      )
    )
  }
  test("or or") {
    val result = QueryCompiler("a = 'a' OR b = 'b' OR c = 'c'")
    assertEquals(
      result,
      Right(
        OrExpr(
          OrExpr(
            Eq(StrIdentifier("a"), StrLiteral("a")),
            Eq(StrIdentifier("b"), StrLiteral("b"))
          ),
          Eq(StrIdentifier("c"), StrLiteral("c"))
        )
      )
    )
  }
  test("and (or) and") {
    val result = QueryCompiler("a = 'a' AND (b = 'b' OR c = 'c') OR (d = 'd')")
    val expected = OrExpr(
      AndExpr(
        Eq(StrIdentifier("a"), StrLiteral("a")),
        OrExpr(
          Eq(StrIdentifier("b"), StrLiteral("b")),
          Eq(StrIdentifier("c"), StrLiteral("c"))
        )
      ),
      Eq(StrIdentifier("d"), StrLiteral("d"))
    )
    assertEquals(
      result.getOrElse(throw new RuntimeException(s"Result is left")),
      expected
    )
  }
  test("(or) and (or)") {
    val result = QueryCompiler("(a = 'a' OR b = 'b') AND (c = 'c' OR d = 'd')")
    val expected = AndExpr(
      OrExpr(
        Eq(StrIdentifier("a"), StrLiteral("a")),
        Eq(StrIdentifier("b"), StrLiteral("b"))
      ),
      OrExpr(
        Eq(StrIdentifier("c"), StrLiteral("c")),
        Eq(StrIdentifier("d"), StrLiteral("d"))
      )
    )
    assertEquals(
      result.getOrElse(throw new RuntimeException(s"Result is left")),
      expected
    )
  }

}
