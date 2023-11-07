package ru.d10xa.jsonlogviewer.query

sealed trait QueryToken {
  def logicalExpr(lhs: SqlExpr, rhs: SqlExpr): LogicalExpr = ???
  def compareOp(lhs: SqlExpr, rhs: SqlExpr): CompareExpr = ???
}

case class IDENTIFIER(str: String) extends QueryToken
case class LITERAL(str: String) extends QueryToken
case object LPAREN extends QueryToken
case object RPAREN extends QueryToken
case object EQUAL extends QueryToken {
  override def compareOp(lhs: SqlExpr, rhs: SqlExpr): CompareExpr =
    Eq(lhs, rhs)
}
case object NOT_EQUAL extends QueryToken {
  override def compareOp(lhs: SqlExpr, rhs: SqlExpr): CompareExpr =
    Neq(lhs, rhs)
}
case object LIKE extends QueryToken {
  override def compareOp(lhs: SqlExpr, rhs: SqlExpr): CompareExpr = LikeExpr(lhs, rhs, false)
}
case object NOTLIKE extends QueryToken {
  override def compareOp(lhs: SqlExpr, rhs: SqlExpr): CompareExpr = LikeExpr(lhs, rhs, true)
}
sealed trait LogicalExprQueryToken extends QueryToken
case object OR extends LogicalExprQueryToken {
  override def logicalExpr(lhs: SqlExpr, rhs: SqlExpr): LogicalExpr =
    OrExpr(lhs, rhs)
}

case object AND extends LogicalExprQueryToken {
  override def logicalExpr(lhs: SqlExpr, rhs: SqlExpr): LogicalExpr =
    AndExpr(lhs, rhs)
}
