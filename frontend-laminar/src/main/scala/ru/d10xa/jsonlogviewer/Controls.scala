package ru.d10xa.jsonlogviewer

import com.raquo.airstream.core.Signal
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLButtonElement
import org.scalajs.dom.HTMLDivElement
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn.Csv
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn.Json
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn.Logfmt
import ru.d10xa.jsonlogviewer.decline.Config.FormatOut
import ru.d10xa.jsonlogviewer.query.QueryAST
import ru.d10xa.jsonlogviewer.query.QueryCompilationError
import ru.d10xa.jsonlogviewer.query.QueryLexerError
import ru.d10xa.jsonlogviewer.query.QueryParserError

object Controls {

  def formatsDiv(
    formatInVar: Var[FormatIn],
    formatOutVar: Var[FormatOut]
  ): ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "ctrl-row",
    label("--format-in", cls := "ctrl-label"),
    select(
      cls := "ctrl-input-narrow",
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
    ),
    label("--format-out", cls := "ctrl-label"),
    select(
      cls := "ctrl-input-narrow",
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

  def filterDiv(
    filterVar: Var[String],
    filterResultSignal: Signal[Either[QueryCompilationError, Option[QueryAST]]]
  ): ReactiveHtmlElement[HTMLDivElement] = div(
    div(
      cls := "ctrl-row",
      label("--filter", cls := "ctrl-label"),
      input(
        cls := "ctrl-input",
        typ := "text",
        placeholder := "message LIKE '%first%' OR level = 'ERROR'",
        value <-- filterVar,
        onInput.mapToValue --> filterVar
      )
    ),
    child <-- filterResultSignal.map {
      case Left(err) =>
        val msg = err match
          case QueryLexerError(m)  => s"Lexer error: $m"
          case QueryParserError(m) => s"Parser error: $m"
        div(
          styleAttr := "color: #f7768e; font-size: 12px; padding-left: 166px;",
          msg
        )
      case Right(_) =>
        emptyNode
    }
  )

  def fuzzyIncludeDiv(
    fuzzyIncludeVar: Var[String]
  ): ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "ctrl-row",
    label("--fuzzy-include", cls := "ctrl-label"),
    input(
      cls := "ctrl-input",
      typ := "text",
      placeholder := "token search (e.g., error timeout)",
      value <-- fuzzyIncludeVar,
      onInput.mapToValue --> fuzzyIncludeVar
    )
  )

  def fuzzyExcludeDiv(
    fuzzyExcludeVar: Var[String]
  ): ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "ctrl-row",
    label("--fuzzy-exclude", cls := "ctrl-label"),
    input(
      cls := "ctrl-input",
      typ := "text",
      placeholder := "exclude tokens",
      value <-- fuzzyExcludeVar,
      onInput.mapToValue --> fuzzyExcludeVar
    )
  )

  def showEmptyFieldsDiv(
    showEmptyFieldsVar: Var[Boolean]
  ): ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "ctrl-checkbox",
    input(
      typ := "checkbox",
      idAttr := "showEmptyFieldsCheck",
      checked <-- showEmptyFieldsVar.signal,
      onChange.mapToChecked --> showEmptyFieldsVar
    ),
    label(
      forId := "showEmptyFieldsCheck",
      "--show-empty-fields"
    )
  )

  def excludeFieldsDiv(
    excludeFieldsVar: Var[String]
  ): ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "ctrl-row",
    label("--exclude-fields", cls := "ctrl-label"),
    input(
      cls := "ctrl-input",
      typ := "text",
      placeholder := "comma-separated: stack_trace,thread_name",
      value <-- excludeFieldsVar,
      onInput.mapToValue --> excludeFieldsVar
    )
  )

  private val fieldNamesCollapsedVar: Var[Boolean] = Var(true)

  def fieldNamesDiv(
    timestampFieldVar: Var[String],
    levelFieldVar: Var[String],
    messageFieldVar: Var[String],
    stackTraceFieldVar: Var[String],
    loggerNameFieldVar: Var[String],
    threadNameFieldVar: Var[String]
  ): ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "field-toggle",
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

  private def fieldNameInput(
    labelText: String,
    placeholderText: String,
    v: Var[String]
  ): ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "ctrl-row",
    label(labelText, cls := "ctrl-label"),
    input(
      cls := "ctrl-input",
      typ := "text",
      placeholder := placeholderText,
      value <-- v,
      onInput.mapToValue --> v
    )
  )

  def additionalArgsDiv(
    cliVar: Var[String]
  ): ReactiveHtmlElement[HTMLDivElement] = div(
    cls := "ctrl-row",
    label("additional args", cls := "ctrl-label"),
    input(
      cls := "ctrl-input",
      typ := "text",
      value <-- cliVar,
      onInput.mapToValue --> cliVar
    )
  )

  def editElementDiv(
    textVar: Var[String]
  ): ReactiveHtmlElement[HTMLDivElement] = div(
    EditElement.renderWithDrop(textVar)
  )

  def buttonGenerateLogs(
    formatInVar: Var[FormatIn],
    textVar: Var[String]
  ): ReactiveHtmlElement[HTMLButtonElement] = button(
    cls := "term-btn",
    child.text <-- formatInVar.signal.map {
      case Logfmt => "Generate logfmt logs"
      case Json   => "Generate json logs"
      case Csv    => "Generate csv logs"
    },
    onClick --> { _ =>
      formatInVar.now() match
        case Config.FormatIn.Json   => textVar.set(SampleData.jsonLogSample)
        case Config.FormatIn.Logfmt => textVar.set(SampleData.logfmtSample)
        case Config.FormatIn.Csv    => textVar.set(SampleData.csvSample)
    }
  )

  def loadingIndicator(loadingBus: EventBus[Boolean]): HtmlElement =
    div(
      child <-- loadingBus.events.map {
        case true =>
          div(
            styleAttr := "color: var(--term-accent); padding: 4px 0;",
            "Processing..."
          )
        case false =>
          emptyNode
      }
    )
}
