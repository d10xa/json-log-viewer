package ru.d10xa.jsonlogviewer

import com.monovore.decline.Help
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.waypoint.*
import org.scalajs.dom
import org.scalajs.dom.HTMLButtonElement
import org.scalajs.dom.HTMLDivElement
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn.Csv
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn.Json
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn.Logfmt
import ru.d10xa.jsonlogviewer.decline.Config.FormatOut
import ru.d10xa.jsonlogviewer.decline.DeclineOpts
import ru.d10xa.jsonlogviewer.query.QueryCompiler
import ru.d10xa.jsonlogviewer.Router0.*
import ru.d10xa.jsonlogviewer.Router0.navigateTo
import ru.d10xa.jsonlogviewer.Router0.EditPage
import ru.d10xa.jsonlogviewer.Router0.HelpPage
import ru.d10xa.jsonlogviewer.Router0.LivePage
import ru.d10xa.jsonlogviewer.Router0.Page
import ru.d10xa.jsonlogviewer.Router0.ViewPage
import scala.util.matching.Regex

object App {

  val jsonLogSample: String =
    """
|{"@timestamp":"2023-09-18T19:10:10.123456Z","message":"first line","logger_name":"MakeLogs","thread_name":"main","level":"INFO", "custom_field": "custom"}
|{"@timestamp":"2023-09-18T19:11:20.132318Z","message":"test","logger_name":"MakeLogs","thread_name":"main","level":"INFO"}
|{"@timestamp":"2023-09-18T19:12:30.132319Z","message":"debug msg","logger_name":"MakeLogs","thread_name":"main","level":"DEBUG"}
|prefix before json {"@timestamp":"2023-09-18T19:11:42.132320Z","message":"warning msg","logger_name":"MakeLogs","thread_name":"main","level":"WARNING"}
|{"@timestamp":"2023-09-18T19:13:42.132321Z","message":"warn msg","logger_name":"MakeLogs","thread_name":"main","level":"WARN"}
|{"@timestamp":"2023-09-18T19:14:42.137207Z","message":"error message","logger_name":"MakeLogs","thread_name":"main","level":"ERROR","stack_trace":"java.lang.RuntimeException: java.lang.IllegalArgumentException: java.lang.ArithmeticException: hello\n\tat ru.d10xa.jsonlogviewer.MakeLogs$.main(MakeLogs.scala:9)\n\tat ru.d10xa.jsonlogviewer.MakeLogs.main(MakeLogs.scala)\nCaused by: java.lang.IllegalArgumentException: java.lang.ArithmeticException: hello\n\t... 2 common frames omitted\nCaused by: java.lang.ArithmeticException: hello\n\t... 2 common frames omitted\n", "duration": "30 seconds", "margin": "20px"}
|{"@timestamp":"2023-09-18T19:15:42.137207Z","message":"last line","logger_name":"MakeLogs","thread_name":"main","level":"INFO"}
|""".stripMargin

  val logfmtSample: String =
    """
      |@timestamp=2023-09-18T19:10:10.123456Z thread_name=main logger_name=MakeLogs first line custom_field=custom
      |@timestamp=2023-09-18T19:10:10.123456Z second line {"level":"INFO"}
      |""".stripMargin

  val csvSample: String =
    """@timestamp,level,logger_name,thread_name,message
      |2023-09-18T19:10:10.123456Z,INFO,MakeLogs,main,"first line, with comma"
      |2023-09-18T19:11:20.132318Z,INFO,MakeLogs,main,test
      |2023-09-18T19:12:30.132319Z,DEBUG,MakeLogs,main,debug msg
      |2023-09-18T19:13:42.132321Z,WARN,MakeLogs,main,warn msg
      |2023-09-18T19:14:42.137207Z,ERROR,MakeLogs,main,"error message,error details"
      |2023-09-18T19:15:42.137207Z,INFO,MakeLogs,main,last line
      |""".stripMargin

  val textVar: Var[String] = Var("")

  val cliVar: Var[String] = Var(
    """"""
  )
  val filterVar: Var[String] = Var(
    """message LIKE '%first%' OR level = 'ERROR'"""
  )
  val formatInVar: Var[FormatIn] = Var(
    FormatIn.Json
  )
  val formatOutVar: Var[FormatOut] = Var(
    FormatOut.Pretty
  )

  val splitPattern: Regex = "([^\"]\\S*|\".+?\")\\s*".r
  def splitArgs(s: String): Seq[String] =
    splitPattern
      .findAllMatchIn(s)
      .map(_.group(1).replace("\"", ""))
      .toSeq

  val configSignal: Signal[Either[Help, Config]] = for {
    cli <- cliVar.signal
    filterString <- filterVar.signal
    formatIn <- formatInVar.signal
    formatOut <- formatOutVar.signal
    filter = QueryCompiler(filterString) match
      case Left(value)  => None
      case Right(value) => Some(value)
  } yield DeclineOpts.command
    .parse(splitArgs(cli))
    .map(cfg =>
      cfg.copy(
        filter = filter,
        formatIn = Some(formatIn),
        formatOut = Some(formatOut)
      )
    )

  def main(args: Array[String]): Unit = {
    lazy val container = dom.document.getElementById("app-container")

    lazy val appElement =
      div(
        cls := "container-fluid",
        ul(
          cls := "bg-light bd-navbar nav nav-tabs sticky-top",
          linkPages.map { page =>
            li(
              cls := "nav-item p-1",
              a(
                cls := "nav-link",
                cls <-- Router0.router.currentPageSignal.map {
                  case currentPage if currentPage == page =>
                    Seq("active")
                  case _ =>
                    Seq.empty
                },
                navigateTo(page),
                page.title
              )
            )
          }
        ),
        div(
          child <-- selectedAppSignal
        )
      )
    renderOnDomContentLoaded(container, appElement)
  }

  private val selectedAppSignal =
    SplitRender(Router0.router.currentPageSignal)
      .collectStatic(LivePage)(renderLivePage())
      .collectStatic(EditPage)(renderEditPage())
      .collectStatic(ViewPage)(renderViewPage())
      .collectStatic(HelpPage)(renderHelpPage())
      .signal

  def formatInDiv: ReactiveHtmlElement[HTMLDivElement] = div(
    label("--format-in", cls := "col-2"),
    select(
      cls := "col-1",
      value <-- formatInVar.signal.map {
        case FormatIn.Json   => "json"
        case FormatIn.Logfmt => "logfmt"
        case FormatIn.Csv    => "csv"
      },
      onChange.mapToValue.map {
        case "json"   => FormatIn.Json
        case "logfmt" => FormatIn.Logfmt
        case "csv"    => FormatIn.Csv
      } --> formatInVar,
      option(value := "json", "json"),
      option(value := "logfmt", "logfmt"),
      option(value := "csv", "csv")
    )
  )
  def formatOutDiv: ReactiveHtmlElement[HTMLDivElement] = div(
    label("--format-out", cls := "col-2"),
    select(
      cls := "col-1",
      value <-- formatOutVar.signal.map {
        case FormatOut.Raw    => "raw"
        case FormatOut.Pretty => "pretty"
      },
      onChange.mapToValue.map {
        case "raw"    => FormatOut.Raw
        case "pretty" => FormatOut.Pretty
      } --> formatOutVar,
      option(value := "pretty", "pretty"),
      option(value := "raw", "raw")
    )
  )

  def filterDiv: ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "row-fluid",
    div(
      label("--filter", cls := "col-2"),
      input(
        cls := "col-10",
        typ := "text",
        value <-- filterVar,
        onInput.mapToValue --> filterVar
      )
    )
  )

  def additionalArgsDiv: ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "row-fluid",
    div(
      label("additional args", cls := "col-2"),
      input(
        cls := "col-10",
        typ := "text",
        value <-- cliVar,
        onInput.mapToValue --> cliVar
      )
    )
  )

  def editElementDiv: ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "row-fluid",
    EditElement.render(
      value <-- textVar,
      onInput.mapToValue --> textVar
    )
  )
  def buttonGenerateLogs: ReactiveHtmlElement[HTMLButtonElement] = button(
    cls := "btn btn-primary",
    child.text <-- formatInVar.signal.map {
      case Logfmt => "Generate logfmt logs"
      case Json   => "Generate json logs"
      case Csv    => "Generate csv logs"
    },
    onClick --> { _ =>
      formatInVar.now() match
        case Config.FormatIn.Json   => textVar.set(jsonLogSample)
        case Config.FormatIn.Logfmt => textVar.set(logfmtSample)
        case Config.FormatIn.Csv    => textVar.set(csvSample)
    }
  )
  private def renderLivePage(): HtmlElement = {
    implicit val owner: Owner = new Owner {}
    div(
      formatInDiv,
      formatOutDiv,
      filterDiv,
      additionalArgsDiv,
      buttonGenerateLogs,
      editElementDiv,
      div(cls := "row", ViewElement.render(textVar.signal, configSignal))
    )
  }

  private def renderViewPage(): HtmlElement = {
    implicit val owner: Owner = new Owner {}
    div(
      ViewElement.render(textVar.signal, configSignal)
    )
  }

  private def renderHelpPage(): HtmlElement =
    pre(
      cls := "bg-dark font-monospace text-light",
      Help.fromCommand(DeclineOpts.command).toString
    )

  private def renderEditPage(): HtmlElement =
    div(
      formatInDiv,
      formatOutDiv,
      filterDiv,
      additionalArgsDiv,
      buttonGenerateLogs,
      editElementDiv
    )

  val linkPages: List[Page] = List(
    LivePage,
    EditPage,
    ViewPage,
    HelpPage
  )
}
