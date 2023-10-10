import App.textVar
import com.raquo.airstream.core.Signal
import com.raquo.laminar.DomApi
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.api.L
import ru.d10xa.jsonlogviewer.Config
import ru.d10xa.jsonlogviewer.TimestampConfig
import ru.d10xa.jsonlogviewer.Config
import ru.d10xa.jsonlogviewer.JsonDetector
import ru.d10xa.jsonlogviewer.JsonPrefixPostfix
import ru.d10xa.jsonlogviewer.LogLineParser
import ru.d10xa.jsonlogviewer.TimestampConfig
import ru.d10xa.jsonlogviewer.TimestampFilter
import ru.d10xa.jsonlogviewer.ColorLineFormatter
import ru.d10xa.jsonlogviewer.LogLineFilter
object ViewElement {

  val c: Config = Config(
    TimestampConfig(
      "@timestamp",
      None,
      None
    ),
    Nil
  )

  val timestampFilter = TimestampFilter()
  val jsonPrefixPostfix = JsonPrefixPostfix(JsonDetector())
  val logLineParser = LogLineParser(c, jsonPrefixPostfix)
  val outputLineFormatter = ColorLineFormatter(c)
  val logLineFilter = LogLineFilter(c)

  def runApp(s: String): String =
    s
      .split("\n", -1)
      .toList
      .filter(_.nonEmpty)
      .map(logLineParser.parse)
      .filter(logLineFilter.grep)
      .map(outputLineFormatter.formatLine)
      .map(_.toString)
      .mkString("\n")
  def render(logLines: Signal[String]): HtmlElement =
    div(
      cls := "log-view",
      child <-- logLines.map(runApp).map(Ansi2HtmlWithClasses.apply).map(x => s"<div>$x</div>").map(x =>
        foreignHtmlElement(DomApi.unsafeParseHtmlString(x))
      )
    )

}
