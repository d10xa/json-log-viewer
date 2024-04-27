import scala.util.parsing.combinator.RegexParsers

trait TokenCompilationError

case class TokenLexerError(msg: String) extends TokenCompilationError
case class TokenParserError(msg: String) extends TokenCompilationError

sealed trait Token
case class Alphanumeric(value: String) extends Token
case class DoubleQuote() extends Token
case class EscapedDoubleQuote() extends Token
case class EqualSign() extends Token
case class Spaces(s: String) extends Token

object Lexer extends RegexParsers {
  override def skipWhitespace: Boolean = false

  def alphanumeric: Parser[Alphanumeric] = """[a-zA-Z0-9]+""".r ^^ { s =>
    Alphanumeric(s)
  }

  def escapedDoubleQuote: Parser[EscapedDoubleQuote] = "\\\"" ^^ { _ =>
    EscapedDoubleQuote()
  }

  def doubleQuote: Parser[DoubleQuote] = "\"" ^^ { _ => DoubleQuote() }

  def equalsSign: Parser[EqualSign] = "=" ^^ { _ => EqualSign() }
  def spaces: Parser[Spaces] = """\s+""".r ^^ { s => Spaces(s) }
  def tokens: Parser[List[Token]] = phrase(
    rep1(escapedDoubleQuote | doubleQuote | equalsSign | spaces | alphanumeric)
  )

//  def identifier: Parser[String] = rep1(letter | digit | "_").map(_.mkString)
//
//  def unquotedValue: Parser[String] = rep1(letter | digit | "_").map(_.mkString)
//
//  def quotedValue: Parser[String] = doubleQuote ~> rep(letter | digit | space | special | escapedQuote).map(_.mkString) <~ doubleQuote
//
//  def keyValue: Parser[(String, String)] =
//    identifier ~ (equalsSign ~> (quotedValue | unquotedValue)) ^^ {
//      case key ~ value => (key, value)
//    }
//  def freeText: Parser[String] = rep1(nonKeyValueChar | space).map(_.mkString.trim) <~ not(keyValue)

//  def entries: Parser[List[(String, String)]] =
//    rep1sep(keyValue, space)
//  def entries: Parser[List[Either[(String, String), String]]] =
//    repsep(keyValue, space) | opt(freeText) ^^ {
//      case k | v =>
////      case kvs | freeText => Right(text) :: kvs.map(Left.apply)
////      case None ~ kvs => kvs.map(Left.apply)
//    }

//    def parse(input: String): ParseResult[List[Either[(String, String), String]]] = parseAll(entries, input)

  def parse(input: String) = parseAll(tokens, input)

  def apply(code: String): Either[TokenLexerError, List[Token]] =
    parse(tokens, code) match {
      case NoSuccess(msg, next) =>
        Left(TokenLexerError(msg))
      case Success(result, next) =>
        Right(result)
    }
}

import scala.util.parsing.input.NoPosition
import scala.util.parsing.input.Position
import scala.util.parsing.input.Reader

class TokenReader(tokens: Seq[Token]) extends Reader[Token]:
  override def first: Token = tokens.head

  override def atEnd: Boolean = tokens.isEmpty

  override def pos: Position = NoPosition

  override def rest: Reader[Token] = new TokenReader(tokens.tail)

import scala.util.parsing.combinator.PackratParsers

trait Ast

case class AlphanumericAst(s: String) extends Ast {
  override def toString: String = s
}
case class EqualSignAst() extends Ast {
  override def toString: String = "="
}
case class SpacesAst(s: String) extends Ast {
  override def toString: String = s
}
case class DoubleQuoteAst() extends Ast {
  override def toString: String = "\""
}
case class EscapedDoubleQuoteAst() extends Ast {
  override def toString: String = "\\\""
}

case class LogFmtAst(asts: Vector[Ast]) extends Ast {
  override def toString: String = asts.mkString
}

case class PairAst(key: String, value: Ast) extends Ast {
  override def toString: String = s"$key=${value.toString}"
}
case class QuotedValueAst(v: String) extends Ast {
  override def toString: String = s"\"$v\""
}
case class UnquotedValueAst(v: String) extends Ast {
  override def toString: String = v
}
case class AstString(s: String) extends Ast {
  override def toString: String = s
}

object TokenParser extends PackratParsers:
  override type Elem = Token

  private def alphanumericAst: Parser[AlphanumericAst] =
    accept("alphanumeric", { case a @ Alphanumeric(v) => AlphanumericAst(v) })

  private def equalSignAst: Parser[EqualSignAst] =
    accept("equalSign", { case a @ EqualSign() => EqualSignAst() })

  private def spacesAst: Parser[SpacesAst] =
    accept("spaces", { case a @ Spaces(s) => SpacesAst(s) })

  private def doubleQuoteAst: Parser[DoubleQuoteAst] =
    accept("doubleQuote", { case a @ DoubleQuote() => DoubleQuoteAst() })

  private def valueAst: Parser[Ast] =
    quotedValueAst | unquotedValueAst

  private def unquotedValueAst: Parser[UnquotedValueAst] =
    alphanumericAst ^^ { case AlphanumericAst(value) =>
      UnquotedValueAst(value)
    }

//  private def quotedValueAst: Parser[QuotedValueAst] =
//    (doubleQuoteAst ~> rep(escapedDoubleQuoteAst | not(doubleQuoteAst) ~> alphanumericAst) <~ doubleQuoteAst) ^^ {
//      case chars => QuotedValueAst(chars.mkString)
//    }

  private def quotedValueAst: Parser[Ast] =
    (doubleQuoteAst ~> rep(
      escapedDoubleQuoteAst | spacesAst | alphanumericAst
    ) <~ doubleQuoteAst) ^^ {
      case segments: Seq[Ast] =>
        QuotedValueAst(segments.map {
          case EscapedDoubleQuoteAst() => "\""
          case SpacesAst(s)            => s
          case AlphanumericAst(value)  => value
//          case PairAst(k, v)  =>
          case other                   => other.toString
        }.mkString)
    }

  private def escapedDoubleQuoteAst: Parser[EscapedDoubleQuoteAst] =
    accept(
      "escapedDoubleQuote",
      { case a @ EscapedDoubleQuote() => EscapedDoubleQuoteAst() }
    )

//  private def valueAst: Parser[Ast] =
////    (rep1(doubleQuoteAst ~> escapedDoubleQuoteAst | alphanumericAst) <~ doubleQuoteAst) ^^ {
//    (rep1(doubleQuoteAst ~> alphanumericAst) <~ doubleQuoteAst) ^^ {
//      case list => AstImpl(list.map(_.toString).mkString)
//    } |
//      alphanumericAst

  def pairAst: Parser[PairAst] =
    (alphanumericAst ~ equalSignAst ~ valueAst) ^^ {
      case key ~ eq ~ value =>
        PairAst(key.toString, value)
    }

//  def pairAst: Parser[Ast] =
//     (alphanumericAst ~ equalSignAst) ^^ { case (alp ~ eq) => PairAst(alp.s, "=")}

  def simpleAst: TokenParser.Parser[Ast] = accept(
    "literal",
    {
      case lit @ Alphanumeric(name)                  => AstString(name)
      case space @ Spaces(s)                         => SpacesAst(s)
      case doubleQuote @ DoubleQuote()               => DoubleQuoteAst()
      case escapedDoubleQuote @ EscapedDoubleQuote() => EscapedDoubleQuoteAst()
      case equalSign @ EqualSign()                   => EqualSignAst()
    }
  )

  def statement: TokenParser.Parser[Ast] =
    pairAst | simpleAst

  def program: Parser[Ast] = phrase(
    rep1(statement).^^(list => LogFmtAst(list.toVector))
  )

  def apply(tokens: Seq[Token]): Either[TokenParserError, Ast] =
    val reader = new PackratReader(new TokenReader(tokens))
    program(reader) match {
      case NoSuccess(msg, next)  => Left(TokenParserError(msg))
      case Success(result, next) => Right(result)
    }
object Compiler:
  def apply(code: String): Either[TokenCompilationError, Ast] =
    for
      tokens <- Lexer(code)
      ast <- TokenParser(tokens)
    yield ast

object LexerRun extends PackratParsers {

  private val str = """text1      token="123abc \"hello"text2 text3"""
  val parsedLexer: Either[TokenLexerError, List[Token]] = Lexer(str)

  val parsed: Either[TokenCompilationError, Ast] = Compiler(str)

  def main(args: Array[String]): Unit = {

    val lexerPair = Lexer("""a="b \"c"""")
    val tokens = TokenParser.pairAst(
      new PackratReader(new TokenReader(lexerPair.right.get))
    )
    pprint.pprintln(tokens)
    println(tokens)

    println(str)
    parsedLexer match
      case Left(value)  => println(value)
      case Right(value) => println(value)
    parsed match
      case Left(value)  => println(value)
      case Right(value) =>
        pprint.pprintln(value)
        println(value)
  }

}
