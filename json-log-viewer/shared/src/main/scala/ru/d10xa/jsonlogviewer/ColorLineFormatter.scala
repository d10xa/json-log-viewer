package ru.d10xa.jsonlogviewer

import fansi.ErrorMode.Strip
import fansi.EscapeAttr
import fansi.Str

class ColorLineFormatter(c: Config) extends OutputLineFormatter:
  private val strEmpty: Str = Str("")
  private val strSpace: Str = Str(" ")
  private val strNewLine: Str = Str("\n")
  extension (s: String) def ansiStrip: Str = Str(s, Strip)

  def levelToColor(level: String): EscapeAttr =
    level match
      case "DEBUG"            => fansi.Color.LightGray
      case "WARNING" | "WARN" => fansi.Color.Yellow
      case "ERROR"            => fansi.Color.Red
      case _                  => fansi.Color.White

  def strLevel(levelOpt: Option[String], colorAttr: EscapeAttr): Seq[Str] =
    levelOpt match
      case Some(level) => strSpace :: colorAttr(s"[${level.ansiStrip}]") :: Nil
      case None        => Nil

  def strMessage(messageOpt: Option[String], colorAttr: EscapeAttr): Seq[Str] =
    messageOpt match
      case Some(message) => strSpace :: colorAttr(message.ansiStrip) :: Nil
      case None          => Nil

  def strStackTrace(
    stackTraceOpt: Option[String],
    colorAttr: EscapeAttr
  ): Seq[Str] =
    stackTraceOpt match
      case Some(s) => strNewLine :: colorAttr(s.ansiStrip) :: Nil
      case None    => Nil

  def strLoggerName(
    loggerNameOpt: Option[String],
    colorAttr: EscapeAttr
  ): Seq[Str] =
    loggerNameOpt match
      case Some(loggerName) =>
        strSpace :: colorAttr(loggerName.ansiStrip) :: Nil
      case None => Nil

  def strThreadName(
    threadNameOpt: Option[String],
    colorAttr: EscapeAttr
  ): Seq[Str] =
    threadNameOpt match
      case Some(threadName) =>
        strSpace :: colorAttr(s"[${threadName.ansiStrip}]") :: Nil
      case None => Nil

  def strOtherAttributes(
    otherAttributes: Map[String, String],
    needNewLine: Boolean
  ): Seq[Str] =
    otherAttributes match
      case m if m.isEmpty => Nil
      case m =>
        val s = fansi.Str.join(
          m.map { case (k, v) =>
            fansi.Str.join(
              Seq(
                fansi.Color.Magenta(k.ansiStrip),
                fansi.Color.LightMagenta(":"),
                fansi.Color.Magenta(v.ansiStrip)
              )
            )
          }.toSeq,
          fansi.Color.LightMagenta("\n")
        )
        (if (needNewLine) strNewLine else strEmpty) :: s :: Nil

  def strPrefix(s: Option[String]): Seq[Str] =
    s match
      case Some(prefix) =>
        fansi.Color.White(prefix.ansiStrip) :: strSpace :: Nil
      case None => Nil

  def strPostfix(s: Option[String]): Seq[Str] =
    s match
      case Some(postfix) =>
        strSpace :: fansi.Color.White(postfix.ansiStrip) :: Nil
      case None => Nil

  override def formatLine(p: ParseResult): Str =
    p.parsed match
      case Some(line) =>
        val color = line.level.map(levelToColor).getOrElse(fansi.Color.White)
        val substrings1 = Seq(
          strPrefix(p.prefix),
          Seq(fansi.Color.Green(line.timestamp.ansiStrip)),
          strThreadName(line.threadName, color),
          strLevel(line.level, color),
          strLoggerName(line.loggerName, color),
          strMessage(line.message, color),
          strStackTrace(line.stackTrace, color)
        ).flatten
        Str.join(
          substrings1 ++ Seq(
            strOtherAttributes(
              line.otherAttributes,
              !substrings1.lastOption.exists(_.plainText.endsWith("\n"))
            ),
            strPostfix(p.postfix)
          ).flatten
        )
      case None => p.raw.ansiStrip
