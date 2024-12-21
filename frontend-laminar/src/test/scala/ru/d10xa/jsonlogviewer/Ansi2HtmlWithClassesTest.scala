package ru.d10xa.jsonlogviewer

import fansi.Attr
import fansi.Bold
import fansi.Color
import fansi.Underlined
import munit.FunSuite

class Ansi2HtmlWithClassesTest extends FunSuite {

    // Тест для функции transition
    test("transition correctly handles Bold.On to Bold.Off") {
        val from = Bold.On
        val to = Bold.Off
        val result = Ansi2HtmlWithClasses.transition(from, to)
        assertEquals(result, "</b>")
    }

    test("transition correctly handles Underlined.Off to Underlined.On") {
        val from = Underlined.Off
        val to = Underlined.On
        val result = Ansi2HtmlWithClasses.transition(from, to)
        assertEquals(result, "<u>")
    }

    test("transition returns empty string for unmatched attributes") {
        val from = Bold.Off
        val to = Color.Reset
        val result = Ansi2HtmlWithClasses.transition(from, to)
        assertEquals(result, "")
    }

    // Тест для функции apply
    test("apply correctly handles a simple ANSI string") {
        val input = "\u001b[1mBold\u001b[0m"
        val result = Ansi2HtmlWithClasses.apply(input)
        assert(result.contains("<b>Bold</b>"), "Expected bold tags around 'Bold'")
    }

    test("apply handles empty string gracefully") {
        val input = ""
        val result = Ansi2HtmlWithClasses.apply(input)
        assertEquals(result, "")
    }

}
