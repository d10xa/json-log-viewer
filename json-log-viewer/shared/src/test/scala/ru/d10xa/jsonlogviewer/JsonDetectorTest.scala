package ru.d10xa.jsonlogviewer

class JsonDetectorTest extends munit.FunSuite {
  val jsonDetector = new JsonDetector()

  test("valid json") {
    val stringWithJsonInside = """abc {"x":"y"}a"""
    val Some((start, end)) = jsonDetector.detectJson(stringWithJsonInside)
    assertEquals(stringWithJsonInside.charAt(start), '{')
    assertEquals(stringWithJsonInside.charAt(end), '}')
  }
  test("invalid json") {
    val stringWithJsonInside = """abc { xyz"""
    assert(jsonDetector.detectJson(stringWithJsonInside).isEmpty)
  }
}
