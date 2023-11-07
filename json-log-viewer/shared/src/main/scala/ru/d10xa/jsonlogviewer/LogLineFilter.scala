package ru.d10xa.jsonlogviewer

import ru.d10xa.jsonlogviewer.Config.ConfigGrep

class LogLineFilter(config: Config, parseResultKeys: ParseResultKeys) {

  def logLineQueryPredicate(line: ParseResult): Boolean =
    config.filter match
      case Some(queryAST) =>
        new LogLineQueryPredicateImpl(queryAST, parseResultKeys).test(line)
      case None => true

  def grep(
    parseResult: ParseResult
  ): Boolean =
    config.grep
      .map { case ConfigGrep(grepKey, regex) =>
        parseResultKeys.getByKey(parseResult, grepKey).exists(regex.matches)
      } match
      case Nil  => true
      case list => list.reduce(_ || _)

}
