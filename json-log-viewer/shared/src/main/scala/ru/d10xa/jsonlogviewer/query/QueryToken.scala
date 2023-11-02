package ru.d10xa.jsonlogviewer.query

sealed trait QueryToken

case class IDENTIFIER(str: String) extends QueryToken
case class LITERAL(str: String) extends QueryToken
case object EQUAL extends QueryToken
case object NOT_EQUAL extends QueryToken
