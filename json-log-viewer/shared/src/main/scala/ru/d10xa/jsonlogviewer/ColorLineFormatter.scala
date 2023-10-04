package ru.d10xa.jsonlogviewer

import cats.syntax.all.*
import fansi.EscapeAttr
import fansi.Str

import scala.collection.immutable.AbstractMap
import scala.collection.immutable.SeqMap
import scala.collection.immutable.SortedMap

class ColorLineFormatter(c: Config) extends OutputLineFormatter:
  private val strSpace: Str = Str(" ")

  def levelToColor(level: String): EscapeAttr =
    level match
      case "DEBUG"   => fansi.Color.LightGray
      case "WARNING" | "WARN" => fansi.Color.Yellow
      case "ERROR"   => fansi.Color.Red
      case _         => fansi.Color.Reset

  def strLevel(levelOpt: Option[String], colorAttr: EscapeAttr): Seq[Str] =
    levelOpt match
      case Some(level) => strSpace :: colorAttr(s"[$level]") :: Nil
      case None        => Nil

  def strMessage(messageOpt: Option[String], colorAttr: EscapeAttr): Seq[Str] =
    messageOpt match
      case Some(message) => strSpace :: colorAttr(message) :: Nil
      case None          => Nil

  def strStackTrace(
    stackTraceOpt: Option[String],
    colorAttr: EscapeAttr
  ): Seq[Str] =
    stackTraceOpt match
      case Some(s) => Str("\n") :: colorAttr(s) :: Nil
      case None    => Nil

  def strLoggerName(
    loggerNameOpt: Option[String],
    colorAttr: EscapeAttr
  ): Seq[Str] =
    loggerNameOpt match
      case Some(loggerName) => strSpace :: colorAttr(loggerName) :: Nil
      case None             => Nil

  def strThreadName(
    threadNameOpt: Option[String],
    colorAttr: EscapeAttr
  ): Seq[Str] =
    threadNameOpt match
      case Some(threadName) => strSpace :: colorAttr(s"[$threadName]") :: Nil
      case None             => Nil

  def mapAsString(m: Map[String, String]): String =
    m.map { case (k, v) => s"$k:$v" }.mkString("{", ",", "}")

  def strOtherAttributes(
    otherAttributes: Map[String, String],
    colorAttr: EscapeAttr
  ): Seq[Str] =
    otherAttributes match
      case m if m.isEmpty => Nil
      case m =>
        val formattedAttrs = colorAttr(mapAsString(m))
        strSpace :: formattedAttrs :: Nil

  override def formatLine(p: ParseResult): Str =
    p.parsed match
      case Some(line) =>
        val color = line.level.map(levelToColor).getOrElse(fansi.Color.Reset)
        Str.join(
          Seq(
            Seq(fansi.Color.Green(line.timestamp)),
            strThreadName(line.threadName, color),
            strLevel(line.level, color),
            strLoggerName(line.loggerName, color),
            strMessage(line.message, color),
            strStackTrace(line.stackTrace, color),
            strOtherAttributes(line.otherAttributes, color)
          ).flatten
        )
      case None => Str(p.raw)
