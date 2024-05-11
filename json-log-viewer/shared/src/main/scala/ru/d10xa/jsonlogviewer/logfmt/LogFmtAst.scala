package ru.d10xa.jsonlogviewer.logfmt

trait LogFmtAst

case class TextAst(s: String) extends LogFmtAst {
  override def toString: String = s
}
case class EqualSignAst() extends LogFmtAst {
  override def toString: String = "="
}
case class SpacesAst(s: String) extends LogFmtAst {
  override def toString: String = s
}
case class DoubleQuoteAst() extends LogFmtAst {
  override def toString: String = "\""
}
case class EscapedDoubleQuoteAst() extends LogFmtAst {
  override def toString: String = "\\\""
  def unescaped = "\""
}

case class StatementsAst(asts: Vector[LogFmtAst]) extends LogFmtAst {
  override def toString: String = asts.mkString
}

case class PairAst(key: String, value: LogFmtAst) extends LogFmtAst {
  override def toString: String = s"$key=${value.toString}"
}
case class QuotedValueAst(asts: Vector[LogFmtAst]) extends LogFmtAst {
  override def toString: String = s"\"${asts.map(_.toString).mkString}\""
  def unquoted: String = asts.map {
    case e: EscapedDoubleQuoteAst => e.unescaped
    case s                        => s.toString
  }.mkString
}
case class UnquotedValueAst(v: String) extends LogFmtAst {
  override def toString: String = v
}
case class AstString(s: String) extends LogFmtAst {
  override def toString: String = s
}
