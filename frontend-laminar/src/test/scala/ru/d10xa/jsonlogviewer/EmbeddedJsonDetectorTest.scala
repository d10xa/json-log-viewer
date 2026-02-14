package ru.d10xa.jsonlogviewer

import munit.FunSuite

class EmbeddedJsonDetectorTest extends FunSuite {

  test("detect single JSON object") {
    val text = """prefix {"a":1} suffix"""
    val result = EmbeddedJsonDetector.detect(text)
    assertEquals(result.length, 1)
    assertEquals(result.head.startIndex, 7)
    assertEquals(result.head.endIndex, 14)
  }

  test("detect multiple JSON objects") {
    val text = """json1={"a":1} json2={"b":2}"""
    val result = EmbeddedJsonDetector.detect(text)
    assertEquals(result.length, 2)
    assertEquals(result(0).startIndex, 6)
    assertEquals(result(1).startIndex, 20)
  }

  test("detect nested JSON") {
    val text = """{"outer":{"inner":true}}"""
    val result = EmbeddedJsonDetector.detect(text)
    assertEquals(result.length, 1)
    assertEquals(result.head.normalizedJson, """{"outer":{"inner":true}}""")
  }

  test("detect JSON array") {
    val text = """data=[1,2,3] end"""
    val result = EmbeddedJsonDetector.detect(text)
    assertEquals(result.length, 1)
    assertEquals(result.head.normalizedJson, "[1,2,3]")
  }

  test("handle non-breaking spaces") {
    val text = "{\u00a0\"a\"\u00a0:\u00a0\"b\"\u00a0}"
    val result = EmbeddedJsonDetector.detect(text)
    assertEquals(result.length, 1)
    assertEquals(result.head.normalizedJson, """{"a":"b"}""")
  }

  test("handle braces inside strings") {
    val text = """{"msg":"value with {braces}"}"""
    val result = EmbeddedJsonDetector.detect(text)
    assertEquals(result.length, 1)
  }

  test("handle escaped quotes") {
    val text = """{"msg":"say \"hello\""}"""
    val result = EmbeddedJsonDetector.detect(text)
    assertEquals(result.length, 1)
  }

  test("no JSON in text") {
    val text = "just plain text without json"
    val result = EmbeddedJsonDetector.detect(text)
    assertEquals(result, List.empty)
  }

  test("invalid JSON skipped") {
    val text = "{not json} {\"valid\":true}"
    val result = EmbeddedJsonDetector.detect(text)
    assertEquals(result.length, 1)
    assertEquals(result.head.normalizedJson, """{"valid":true}""")
  }

  test("println pattern with two JSON objects") {
    val text = """json1={"a":1} json2={"b":2}"""
    val result = EmbeddedJsonDetector.detect(text)
    assertEquals(result.length, 2)
    assertEquals(result(0).normalizedJson, """{"a":1}""")
    assertEquals(result(1).normalizedJson, """{"b":2}""")
  }

  test("empty object and empty array") {
    val text = """empty={} arr=[]"""
    val result = EmbeddedJsonDetector.detect(text)
    assertEquals(result.length, 2)
  }

  test("unmatched opening brace") {
    val text = "prefix { no closing"
    val result = EmbeddedJsonDetector.detect(text)
    assertEquals(result, List.empty)
  }
}
