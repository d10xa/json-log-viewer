package ru.d10xa.jsonlogviewer

import ru.d10xa.jsonlogviewer.config.ResolvedConfig

class LogLineFilter(config: ResolvedConfig, parseResultKeys: ParseResultKeys) {

  def logLineQueryPredicate(line: ParseResult): Boolean =
    config.filter match
      case Some(queryAST) =>
        new LogLineQueryPredicateImpl(queryAST, parseResultKeys).test(line)
      case None => true

  def grep(
    parseResult: ParseResult
  ): Boolean =
    config.grep
      .map { grepConfig =>
        parseResultKeys
          .getByKey(parseResult, grepConfig.key)
          .exists(grepConfig.value.matches)
      } match
      case Nil  => true
      case list => list.reduce(_ || _)
}
