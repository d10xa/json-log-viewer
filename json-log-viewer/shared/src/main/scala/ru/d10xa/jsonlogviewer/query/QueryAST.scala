package ru.d10xa.jsonlogviewer.query

trait QueryAST

trait SqlExpr extends QueryAST
trait Binop extends SqlExpr
case class Eq(lhs: SqlExpr, rhs: SqlExpr) extends Binop
case class Neq(lhs: SqlExpr, rhs: SqlExpr) extends Binop
case class OrExpr(lhs: SqlExpr, rhs: SqlExpr) extends Binop

trait LiteralExpr extends SqlExpr
case class StrLiteral(s: String) extends LiteralExpr
case class StrIdentifier(s: String) extends LiteralExpr
