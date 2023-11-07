package ru.d10xa.jsonlogviewer.query

trait QueryAST

trait SqlExpr extends QueryAST
trait LogicalExpr extends Binop
trait Binop extends SqlExpr
trait CompareExpr extends Binop
trait LiteralExpr extends SqlExpr
case class Eq(lhs: SqlExpr, rhs: SqlExpr) extends CompareExpr
case class Neq(lhs: SqlExpr, rhs: SqlExpr) extends CompareExpr
case class LikeExpr(lhs: SqlExpr, pattern: SqlExpr, negate: Boolean) extends CompareExpr
case class OrExpr(lhs: SqlExpr, rhs: SqlExpr) extends LogicalExpr
case class AndExpr(lhs: SqlExpr, rhs: SqlExpr) extends LogicalExpr
case class StrLiteral(s: String) extends LiteralExpr
case class StrIdentifier(s: String) extends LiteralExpr
