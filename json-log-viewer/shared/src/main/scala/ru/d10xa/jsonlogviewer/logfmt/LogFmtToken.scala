package ru.d10xa.jsonlogviewer.logfmt

sealed trait LogFmtToken

case class Text(value: String) extends LogFmtToken
case class DoubleQuote() extends LogFmtToken
case class EscapedDoubleQuote() extends LogFmtToken
case class EqualSign() extends LogFmtToken
case class Spaces(s: String) extends LogFmtToken

