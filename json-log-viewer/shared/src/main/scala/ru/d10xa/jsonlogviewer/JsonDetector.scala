package ru.d10xa.jsonlogviewer

import scala.annotation.tailrec

class JsonDetector {
  def detectJson(s: String): Option[(Int, Int)] =
    @tailrec
    def loop(i: Int, start: Int, end: Int): (Int, Int) =
      val ri = s.length - i - 1
      if (ri < 0) {
        (-1, -1)
      } else {
        val rchar = s.charAt(ri)
        val lchar = s.charAt(i)
        val nextEnd = if (end == -1 && rchar == '}') ri else end
        val nextStart = if (start == -1 && lchar == '{') i else start
        if (s.length / 2 < i || (nextStart != -1 && nextEnd != -1)) {
          (nextStart, nextEnd)
        } else {
          loop(i + 1, nextStart, nextEnd)
        }
      }
    val res = loop(0, -1, -1)
    res match
      case (-1, _) => None
      case (_, -1) => None
      case (i, j)  => Some(i, j)
}
