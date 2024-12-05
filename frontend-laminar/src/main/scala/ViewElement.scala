import App.textVar
import com.monovore.decline.Help
import com.raquo.airstream.core.Signal
import com.raquo.laminar.DomApi
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.api.L
import ru.d10xa.jsonlogviewer.Application
import ru.d10xa.jsonlogviewer.JsonDetector
import ru.d10xa.jsonlogviewer.JsonPrefixPostfix
import ru.d10xa.jsonlogviewer.JsonLogLineParser
import ru.d10xa.jsonlogviewer.TimestampFilter
import ru.d10xa.jsonlogviewer.ColorLineFormatter
import ru.d10xa.jsonlogviewer.LogViewerStream
import ru.d10xa.jsonlogviewer.LogLineFilter
import fs2.*
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.TimestampConfig
import ru.d10xa.jsonlogviewer.decline.TimestampConfig
import ru.d10xa.jsonlogviewer.logfmt.LogfmtLogLineParser

import scala.util.chaining.scalaUtilChainingOps
object ViewElement {

  def runApp(
    logLinesSignal: Signal[String],
    configSignal: Signal[Either[Help, Config]]
  ): Signal[HtmlElement] =
    logLinesSignal.combineWith(configSignal).map {
      case (string, Right(c)) =>
        val jsonPrefixPostfix = JsonPrefixPostfix(JsonDetector())
        val logLineParser = c.formatIn match
          case Some(FormatIn.Logfmt) => LogfmtLogLineParser(c)
          case Some(FormatIn.Json)   => JsonLogLineParser(c, jsonPrefixPostfix)
          case other =>
            throw new IllegalStateException(
              s"Unsupported format-in value: $other"
            )

        fs2.Stream
          .emits(string.split("\n"))
          .filter(_.trim.nonEmpty)
          .through(LogViewerStream.stream(c, logLineParser))
          .map(Ansi2HtmlWithClasses.apply)
          .toList
          .mkString("<div>", "", "</div>")
          .pipe(DomApi.unsafeParseHtmlString)
          .pipe(foreignHtmlElement)
      case (string, Left(help)) =>
        pre(cls := "text-light", help.toString)
    }
  def render(
    logLinesSignal: Signal[String],
    configSignal: Signal[Either[Help, Config]]
  ): HtmlElement =
    pre(
      cls := "bg-dark font-monospace",
      child <-- runApp(logLinesSignal, configSignal)
    )

}
