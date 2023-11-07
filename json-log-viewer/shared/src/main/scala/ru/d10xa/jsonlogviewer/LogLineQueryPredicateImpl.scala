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
  import LogLineQueryPredicateImpl.*

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
      case like @ LikeExpr(_, _, _) =>
        val x = like0(like, parseResult)
        println(
          s"like = ${like}, message = ${parseResult.parsed.get.message.get}, like=$x"
        )
        x

  def like0(likeExpr: LikeExpr, parseResult: ParseResult): Boolean =
    likeExpr match
      case LikeExpr(StrIdentifier(key), StrLiteral(lit), false) =>
        parseResultKeys
          .getByKey(parseResult, key)
          .exists(k => likeContains(k, lit))
      case LikeExpr(StrIdentifier(key), StrLiteral(lit), true) =>
        parseResultKeys
          .getByKey(parseResult, key)
          .exists(k => !likeContains(k, lit))
      case _ => false

}

object LogLineQueryPredicateImpl:
  def likeContains(line: String, lit: String): Boolean =
    lit match
      case v if v.startsWith("%") && v.endsWith("%") =>
        val str = v.substring(1, v.length - 1)
        line.contains(str)
      case v if v.startsWith("%") =>
        val str = v.substring(1)
        line.endsWith(str)
      case v if v.endsWith("%") =>
        val str = v.substring(0, v.length - 1)
        line.startsWith(str)
      case v =>
        line == v
end LogLineQueryPredicateImpl
