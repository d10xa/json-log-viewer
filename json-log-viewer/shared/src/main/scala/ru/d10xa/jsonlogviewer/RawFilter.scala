package ru.d10xa.jsonlogviewer

import scala.util.matching.Regex

class RawFilter(
  compiledInclude: List[Regex],
  compiledExclude: List[Regex]
) {
  def test(line: String): Boolean = {
    val includeMatches = compiledInclude.isEmpty ||
      compiledInclude.exists(_.findFirstIn(line).isDefined)
    val excludeMatches = compiledExclude.forall(_.findFirstIn(line).isEmpty)
    includeMatches && excludeMatches
  }
}

object RawFilter {
  val empty: RawFilter = new RawFilter(Nil, Nil)

  /** Throws PatternSyntaxException if any pattern is invalid. */
  def fromConfig(
    rawInclude: Option[List[String]],
    rawExclude: Option[List[String]]
  ): RawFilter = new RawFilter(
    rawInclude.getOrElse(Nil).map(_.r),
    rawExclude.getOrElse(Nil).map(_.r)
  )
}
