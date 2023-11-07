package ru.d10xa.jsonlogviewer.query

import scala.util.parsing.combinator.PackratParsers

object QueryParser extends PackratParsers:
  override type Elem = QueryToken
  private def literal0: Parser[SqlExpr] =
    accept("literal", { case lit @ LITERAL(name) => StrLiteral(name) })

  private def identifier0: Parser[SqlExpr] =
    accept("identifier", { case i @ IDENTIFIER(name) => StrIdentifier(name) })

  def statement: Parser[SqlExpr] = expr

  def expr = compExpr ~ opt(rep(exprRight)) ^^ {
    case l ~ None => l
    case l ~ Some(r) =>
      r.foldLeft(l) { case (left, (expr, right)) =>
        expr.logicalExpr(left, right)
      }
  }

  def compExpr: Parser[Binop] =
    (atomExpr ~ (EQUAL | NOT_EQUAL | NOTLIKE | LIKE) ~ atomExpr) ^^ { case l ~ op ~ r =>
      op.compareOp(l, r)
    }

  def atomExpr: QueryParser.Parser[SqlExpr] =
    (literal0 | identifier0 | (LPAREN ~> expr <~ RPAREN)) ^^ identity

  def exprRight: QueryParser.Parser[(QueryToken, Binop)] =
    (AND | OR) ~ compExpr ^^ { case op ~ e => (op, e) }

  def program: Parser[QueryAST] = phrase(statement)

  def apply(tokens: Seq[QueryToken]): Either[QueryParserError, QueryAST] =
    val reader = new PackratReader(new QueryTokenReader(tokens))
    program(reader) match {
      case NoSuccess(msg, next)  => Left(QueryParserError(msg))
      case Success(result, next) => Right(result)
    }
