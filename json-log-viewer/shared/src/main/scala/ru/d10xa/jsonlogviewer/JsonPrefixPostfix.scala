package ru.d10xa.jsonlogviewer

class JsonPrefixPostfix(jsonDetector: JsonDetector) {

  /** @param s
    *   logline with json inside
    * @return
    *   (json or full line, prefix, postfix)
    */
  def detectJson(s: String): (String, Option[String], Option[String]) =
    jsonDetector.detectJson(s) match
      case Some((start, end)) =>
        val prefix = s.substring(0, start)
        val middle = s.substring(start, end + 1)
        val postfix = s.substring(end + 1, s.length)
        (
          middle,
          if (prefix.nonEmpty) Some(prefix) else None,
          if (postfix.nonEmpty) Some(postfix) else None
        )
      case None => (s, None, None)
}
