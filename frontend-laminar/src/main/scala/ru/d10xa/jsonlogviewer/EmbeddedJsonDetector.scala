package ru.d10xa.jsonlogviewer

import scala.scalajs.js

case class JsonFragment(
  startIndex: Int,
  endIndex: Int,
  normalizedJson: String
)

object EmbeddedJsonDetector {

  def detect(text: String): List[JsonFragment] = {
    val result = List.newBuilder[JsonFragment]
    var i = 0
    while (i < text.length) {
      val ch = text.charAt(i)
      if (ch == '{' || ch == '[') {
        findClosing(text, i) match {
          case Some(endIdx) =>
            val raw = text.substring(i, endIdx + 1)
            val normalized =
              raw.replace('\u00a0', ' ')
            tryParse(normalized) match {
              case Some(json) =>
                result += JsonFragment(i, endIdx + 1, json)
                i = endIdx + 1
              case None =>
                i += 1
            }
          case None =>
            i += 1
        }
      } else {
        i += 1
      }
    }
    result.result()
  }

  private def findClosing(text: String, start: Int): Option[Int] = {
    val open = text.charAt(start)
    val close = if (open == '{') '}' else ']'
    var depth = 0
    var inString = false
    var escape = false
    var i = start
    while (i < text.length) {
      val ch = text.charAt(i)
      if (escape) {
        escape = false
      } else if (inString) {
        if (ch == '\\') escape = true
        else if (ch == '"') inString = false
      } else {
        if (ch == '"') inString = true
        else if (ch == open) depth += 1
        else if (ch == close) {
          depth -= 1
          if (depth == 0) return Some(i)
        }
      }
      i += 1
    }
    None
  }

  private def tryParse(s: String): Option[String] =
    try {
      val parsed = js.JSON.parse(s)
      Some(js.JSON.stringify(parsed))
    } catch {
      case _: Throwable => None
    }
}
