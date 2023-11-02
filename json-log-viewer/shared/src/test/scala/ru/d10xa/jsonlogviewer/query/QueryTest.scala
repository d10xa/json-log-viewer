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
}
