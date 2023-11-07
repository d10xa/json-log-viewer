package ru.d10xa.jsonlogviewer

import ru.d10xa.jsonlogviewer.query.AndExpr
import ru.d10xa.jsonlogviewer.query.Eq
import ru.d10xa.jsonlogviewer.query.LikeExpr
import ru.d10xa.jsonlogviewer.query.OrExpr
import ru.d10xa.jsonlogviewer.query.QueryAST
import ru.d10xa.jsonlogviewer.query.StrIdentifier
import ru.d10xa.jsonlogviewer.query.StrLiteral
import ru.d10xa.jsonlogviewer.query.Neq
class LogLineQueryPredicateImpl(q: QueryAST, parseResultKeys: ParseResultKeys) {

  def test(line: ParseResult): Boolean =
    ast(q, line)

  private def ast(q: QueryAST, parseResult: ParseResult): Boolean =
    q match
      case Eq(StrIdentifier(key), StrLiteral(lit)) =>
        parseResultKeys.getByKey(parseResult, key).contains(lit)
      case Neq(StrIdentifier(key), StrLiteral(lit)) =>
        !parseResultKeys.getByKey(parseResult, key).contains(lit)
      case AndExpr(l, r) => ast(l, parseResult) && ast(r, parseResult)
      case OrExpr(l, r)  => ast(l, parseResult) || ast(r, parseResult)
      case LikeExpr(StrIdentifier(key), StrLiteral(lit)) =>
        parseResultKeys.getByKey(parseResult, key).exists(_.contains(lit))

}
