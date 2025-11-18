package ru.d10xa.jsonlogviewer

class JsonPrefixPostfixTest extends munit.FunSuite {
  val jsonPrefixPostfix = new JsonPrefixPostfix(new JsonDetector)
  test("extract prefix and postfix") {
    val (json, Some(prefix), Some(postfix)) =
      jsonPrefixPostfix.detectJson("""a{"x":"y"}b"""): @unchecked
    assertEquals(json, """{"x":"y"}""")
    assertEquals(prefix, "a")
    assertEquals(postfix, "b")
  }
  test("extract prefix") {
    val (json, Some(prefix), postfixOpt) =
      jsonPrefixPostfix.detectJson("""a{"x":"y"}"""): @unchecked
    assertEquals(json, """{"x":"y"}""")
    assertEquals(prefix, "a")
    assertEquals(postfixOpt, None)
  }

  test(
    "detectJson should throw StringIndexOutOfBoundsException when jsonDetector returns invalid indices"
  ) {
    val jsonDetector = new JsonDetector()
    val jsonPrefixPostfix = new JsonPrefixPostfix(jsonDetector)
    val input = "{"
    val result = jsonPrefixPostfix.detectJson(input)
    assertEquals(result, (input, None, None))
  }

}
