package ru.d10xa.jsonlogviewer

import com.monovore.decline.Help
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.waypoint.*
import org.scalajs.dom
import org.scalajs.dom.HTMLButtonElement
import org.scalajs.dom.HTMLDivElement
import ru.d10xa.jsonlogviewer.decline.yaml.FieldNames
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

case class UiExtraConfig(
  fuzzyInclude: Option[List[String]],
  fuzzyExclude: Option[List[String]],
  showEmptyFields: Boolean,
  excludeFields: Option[List[String]],
  fieldNames: Option[FieldNames]
)

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
  val filterVar: Var[String] = Var("")
  val formatInVar: Var[FormatIn] = Var(
    FormatIn.Json
  )
  val formatOutVar: Var[FormatOut] = Var(
    FormatOut.Pretty
  )

  val fuzzyIncludeVar: Var[String] = Var("")
  val fuzzyExcludeVar: Var[String] = Var("")
  val showEmptyFieldsVar: Var[Boolean] = Var(false)
  val excludeFieldsVar: Var[String] = Var("")
  val timestampFieldVar: Var[String] = Var("")
  val levelFieldVar: Var[String] = Var("")
  val messageFieldVar: Var[String] = Var("")
  val stackTraceFieldVar: Var[String] = Var("")
  val loggerNameFieldVar: Var[String] = Var("")
  val threadNameFieldVar: Var[String] = Var("")

  private def nonEmptyToOption(s: String): Option[String] =
    Option(s.trim).filter(_.nonEmpty)

  private def commaSeparatedToOption(s: String): Option[List[String]] =
    Option(s.trim)
      .filter(_.nonEmpty)
      .map(_.split(",").map(_.trim).filter(_.nonEmpty).toList)
      .filter(_.nonEmpty)

  val uiExtraConfigSignal: Signal[UiExtraConfig] = for {
    fuzzyInc <- fuzzyIncludeVar.signal
    fuzzyExc <- fuzzyExcludeVar.signal
    showEmpty <- showEmptyFieldsVar.signal
    excludeF <- excludeFieldsVar.signal
    tsField <- timestampFieldVar.signal
    lvlField <- levelFieldVar.signal
    msgField <- messageFieldVar.signal
    stField <- stackTraceFieldVar.signal
    lnField <- loggerNameFieldVar.signal
    tnField <- threadNameFieldVar.signal
  } yield {
    val fieldNames = FieldNames(
      timestamp = nonEmptyToOption(tsField),
      level = nonEmptyToOption(lvlField),
      message = nonEmptyToOption(msgField),
      stackTrace = nonEmptyToOption(stField),
      loggerName = nonEmptyToOption(lnField),
      threadName = nonEmptyToOption(tnField)
    )
    val hasFieldNames = fieldNames.timestamp.isDefined ||
      fieldNames.level.isDefined ||
      fieldNames.message.isDefined ||
      fieldNames.stackTrace.isDefined ||
      fieldNames.loggerName.isDefined ||
      fieldNames.threadName.isDefined
    UiExtraConfig(
      fuzzyInclude =
        nonEmptyToOption(fuzzyInc).map(s => s.split("\\s+").toList),
      fuzzyExclude =
        nonEmptyToOption(fuzzyExc).map(s => s.split("\\s+").toList),
      showEmptyFields = showEmpty,
      excludeFields = commaSeparatedToOption(excludeF),
      fieldNames = if (hasFieldNames) Some(fieldNames) else None
    )
  }

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
        placeholder := "message LIKE '%first%' OR level = 'ERROR'",
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

  def fuzzyIncludeDiv: ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "row-fluid",
    div(
      label("--fuzzy-include", cls := "col-2"),
      input(
        cls := "col-10",
        typ := "text",
        placeholder := "token search (e.g., error timeout)",
        value <-- fuzzyIncludeVar,
        onInput.mapToValue --> fuzzyIncludeVar
      )
    )
  )

  def fuzzyExcludeDiv: ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "row-fluid",
    div(
      label("--fuzzy-exclude", cls := "col-2"),
      input(
        cls := "col-10",
        typ := "text",
        placeholder := "exclude tokens",
        value <-- fuzzyExcludeVar,
        onInput.mapToValue --> fuzzyExcludeVar
      )
    )
  )

  def showEmptyFieldsDiv: ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "row-fluid",
    div(
      cls := "form-check",
      input(
        cls := "form-check-input",
        typ := "checkbox",
        idAttr := "showEmptyFieldsCheck",
        checked <-- showEmptyFieldsVar.signal,
        onChange.mapToChecked --> showEmptyFieldsVar
      ),
      label(
        cls := "form-check-label",
        forId := "showEmptyFieldsCheck",
        "--show-empty-fields"
      )
    )
  )

  def excludeFieldsDiv: ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "row-fluid",
    div(
      label("--exclude-fields", cls := "col-2"),
      input(
        cls := "col-10",
        typ := "text",
        placeholder := "comma-separated: stack_trace,thread_name",
        value <-- excludeFieldsVar,
        onInput.mapToValue --> excludeFieldsVar
      )
    )
  )

  private val fieldNamesCollapsedVar: Var[Boolean] = Var(true)

  def fieldNamesDiv: ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "row-fluid",
    div(
      a(
        href := "#",
        child.text <-- fieldNamesCollapsedVar.signal.map(collapsed =>
          if (collapsed) "Field name overrides [+]"
          else "Field name overrides [-]"
        ),
        onClick.preventDefault --> { _ =>
          fieldNamesCollapsedVar.update(!_)
        }
      ),
      div(
        display <-- fieldNamesCollapsedVar.signal.map(collapsed =>
          if (collapsed) "none" else "block"
        ),
        fieldNameInput("timestamp", "@timestamp", timestampFieldVar),
        fieldNameInput("level", "level", levelFieldVar),
        fieldNameInput("message", "message", messageFieldVar),
        fieldNameInput("stackTrace", "stack_trace", stackTraceFieldVar),
        fieldNameInput("loggerName", "logger_name", loggerNameFieldVar),
        fieldNameInput("threadName", "thread_name", threadNameFieldVar)
      )
    )
  )

  private def fieldNameInput(
    labelText: String,
    placeholderText: String,
    v: Var[String]
  ): ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "row-fluid",
    label(labelText, cls := "col-2"),
    input(
      cls := "col-4",
      typ := "text",
      placeholder := placeholderText,
      value <-- v,
      onInput.mapToValue --> v
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
      fuzzyIncludeDiv,
      fuzzyExcludeDiv,
      showEmptyFieldsDiv,
      excludeFieldsDiv,
      fieldNamesDiv,
      additionalArgsDiv,
      buttonGenerateLogs,
      editElementDiv,
      div(
        cls := "row",
        ViewElement
          .render(textVar.signal, configSignal, uiExtraConfigSignal)
      )
    )
  }

  private def renderViewPage(): HtmlElement = {
    implicit val owner: Owner = new Owner {}
    div(
      ViewElement
        .render(textVar.signal, configSignal, uiExtraConfigSignal)
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
      fuzzyIncludeDiv,
      fuzzyExcludeDiv,
      showEmptyFieldsDiv,
      excludeFieldsDiv,
      fieldNamesDiv,
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
