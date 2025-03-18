package ru.d10xa.jsonlogviewer.formatout

import fansi.ErrorMode.Strip
import fansi.EscapeAttr
import fansi.Str
import ru.d10xa.jsonlogviewer.HardcodedFieldNames._
import ru.d10xa.jsonlogviewer.OutputLineFormatter
import ru.d10xa.jsonlogviewer.ParseResult
import ru.d10xa.jsonlogviewer.decline.Config

class ColorLineFormatter(c: Config, feedName: Option[String], excludeFields: Option[List[String]])
  extends OutputLineFormatter:
  private val strEmpty: Str = Str("")
  private val strSpace: Str = Str(" ")
  private val strNewLine: Str = Str("\n")
  extension (s: String) def ansiStrip: Str = Str(s, Strip)

  def shouldExcludeField(fieldName: String): Boolean =
    excludeFields.exists(_.contains(fieldName))

  def levelToColor(level: String): EscapeAttr =
    level match
      case "DEBUG"            => fansi.Color.LightGray
      case "WARNING" | "WARN" => fansi.Color.Yellow
      case "ERROR"            => fansi.Color.Red
      case _                  => fansi.Color.White

  def strLevel(levelOpt: Option[String], colorAttr: EscapeAttr): Seq[Str] =
    if (shouldExcludeField(levelFieldName)) Nil
    else levelOpt match
      case Some(level) => strSpace :: colorAttr(s"[${level.ansiStrip}]") :: Nil
      case None        => Nil

  def strMessage(messageOpt: Option[String], colorAttr: EscapeAttr): Seq[Str] =
    if (shouldExcludeField(messageFieldName)) Nil
    else messageOpt match
      case Some(message) => strSpace :: colorAttr(message.ansiStrip) :: Nil
      case None          => Nil

  def strStackTrace(
    stackTraceOpt: Option[String],
    colorAttr: EscapeAttr
  ): Seq[Str] =
    if (shouldExcludeField(stackTraceFieldName)) Nil
    else stackTraceOpt match
      case Some(s) => strNewLine :: colorAttr(s.ansiStrip) :: Nil
      case None    => Nil

  def strLoggerName(
    loggerNameOpt: Option[String],
    colorAttr: EscapeAttr
  ): Seq[Str] =
    if (shouldExcludeField(loggerNameFieldName)) Nil
    else loggerNameOpt match
      case Some(loggerName) =>
        strSpace :: colorAttr(loggerName.ansiStrip) :: Nil
      case None => Nil

  def strTimestamp(
    timestampOpt: Option[String],
    colorAttr: EscapeAttr
  ): Seq[Str] =
    if (shouldExcludeField(c.timestamp.fieldName)) Nil
    else timestampOpt match
      case Some(timestamp) =>
        strSpace :: colorAttr(timestamp.ansiStrip) :: Nil
      case None => Nil

  def strThreadName(
    threadNameOpt: Option[String],
    colorAttr: EscapeAttr
  ): Seq[Str] =
    if (shouldExcludeField(threadNameFieldName)) Nil
    else threadNameOpt match
      case Some(threadName) =>
        strSpace :: colorAttr(s"[${threadName.ansiStrip}]") :: Nil
      case None => Nil

  def strOtherAttributes(
    otherAttributes: Map[String, String],
    needNewLine: Boolean
  ): Seq[Str] =
    val filteredAttributes = otherAttributes.filterNot { case (key, _) =>
      shouldExcludeField(key)
    }
    
    filteredAttributes match
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
    if (shouldExcludeField("prefix")) Nil
    else s match
      case Some(prefix) =>
        fansi.Color.White(prefix.ansiStrip) :: strSpace :: Nil
      case None => Nil

  def strFeedName(s: Option[String]): Seq[Str] =
    if (shouldExcludeField("feed_name")) Nil
    else s match
      case Some(feedName) =>
        fansi.Color.White(feedName.ansiStrip) :: strSpace :: Nil
      case None => Nil

  def strPostfix(s: Option[String]): Seq[Str] =
    if (shouldExcludeField("postfix")) Nil
    else s match
      case Some(postfix) =>
        strSpace :: fansi.Color.White(postfix.ansiStrip) :: Nil
      case None => Nil

  override def formatLine(p: ParseResult): Str =
    p.parsed match
      case Some(line) =>
        val color = line.level.map(levelToColor).getOrElse(fansi.Color.White)
        val substrings1 = Seq(
          strPrefix(feedName),
          strPrefix(p.prefix),
          strTimestamp(line.timestamp, fansi.Color.Green),
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
