package ru.d10xa.jsonlogviewer.logfmt

class LogFmtDecoderTest extends munit.FunSuite {

  test("logfmt decode") {
    val log =
      """some text before key1="some text" key2=text key3="escaped \" quote" some text after"""

    val (res, other) = LogFmtDecoder.decode(log)
    val List(kv1, kv2, kv3) = res.toList.sortBy(_._1)
    assertEquals(kv1._2, "some text")
    assertEquals(kv2._2, "text")
    assertEquals(kv3._2, "escaped \" quote")
    assertEquals(other, Vector("some", "text", "before", "some", "text", "after"))
  }

}
