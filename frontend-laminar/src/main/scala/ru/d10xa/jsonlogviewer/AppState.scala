package ru.d10xa.jsonlogviewer

import com.monovore.decline.Help
import com.raquo.airstream.core.Signal
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.L.*
import ru.d10xa.jsonlogviewer.decline.yaml.FieldNames
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.Config.FormatOut
import ru.d10xa.jsonlogviewer.decline.DeclineOpts
import ru.d10xa.jsonlogviewer.query.QueryAST
import ru.d10xa.jsonlogviewer.query.QueryCompilationError
import ru.d10xa.jsonlogviewer.query.QueryCompiler
import scala.util.matching.Regex

case class AppState(
  textVar: Var[String],
  cliVar: Var[String],
  filterVar: Var[String],
  formatInVar: Var[FormatIn],
  formatOutVar: Var[FormatOut],
  fuzzyIncludeVar: Var[String],
  fuzzyExcludeVar: Var[String],
  showEmptyFieldsVar: Var[Boolean],
  excludeFieldsVar: Var[String],
  timestampFieldVar: Var[String],
  levelFieldVar: Var[String],
  messageFieldVar: Var[String],
  stackTraceFieldVar: Var[String],
  loggerNameFieldVar: Var[String],
  threadNameFieldVar: Var[String],
  loadingBus: EventBus[Boolean],
  filterResultSignal: Signal[Either[QueryCompilationError, Option[QueryAST]]],
  configSignal: Signal[Either[Help, Config]],
  uiExtraConfigSignal: Signal[UiExtraConfig]
)

object AppState {

  private val splitPattern: Regex = "([^\"]\\S*|\".+?\")\\s*".r

  private def splitArgs(s: String): Seq[String] =
    splitPattern
      .findAllMatchIn(s)
      .map(_.group(1).replace("\"", ""))
      .toSeq

  private def nonEmptyToOption(s: String): Option[String] =
    Option(s.trim).filter(_.nonEmpty)

  private def commaSeparatedToOption(s: String): Option[List[String]] =
    Option(s.trim)
      .filter(_.nonEmpty)
      .map(_.split(",").map(_.trim).filter(_.nonEmpty).toList)
      .filter(_.nonEmpty)

  def default: AppState = {
    val textVar = Var("")
    val cliVar = Var("")
    val filterVar = Var("")
    val formatInVar = Var(FormatIn.Json)
    val formatOutVar = Var(FormatOut.Pretty)
    val fuzzyIncludeVar = Var("")
    val fuzzyExcludeVar = Var("")
    val showEmptyFieldsVar = Var(false)
    val excludeFieldsVar = Var("")
    val timestampFieldVar = Var("")
    val levelFieldVar = Var("")
    val messageFieldVar = Var("")
    val stackTraceFieldVar = Var("")
    val loggerNameFieldVar = Var("")
    val threadNameFieldVar = Var("")
    val loadingBus = new EventBus[Boolean]

    val filterResultSignal
      : Signal[Either[QueryCompilationError, Option[QueryAST]]] =
      filterVar.signal.map { filterString =>
        if filterString.trim.isEmpty then Right(None)
        else QueryCompiler(filterString).map(Some(_))
      }

    val configSignal: Signal[Either[Help, Config]] = for {
      cli <- cliVar.signal
      filterResult <- filterResultSignal
      formatIn <- formatInVar.signal
      formatOut <- formatOutVar.signal
      filter = filterResult.toOption.flatten
    } yield DeclineOpts.command
      .parse(splitArgs(cli))
      .map(cfg =>
        cfg.copy(
          filter = filter,
          formatIn = Some(formatIn),
          formatOut = Some(formatOut)
        )
      )

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

    AppState(
      textVar = textVar,
      cliVar = cliVar,
      filterVar = filterVar,
      formatInVar = formatInVar,
      formatOutVar = formatOutVar,
      fuzzyIncludeVar = fuzzyIncludeVar,
      fuzzyExcludeVar = fuzzyExcludeVar,
      showEmptyFieldsVar = showEmptyFieldsVar,
      excludeFieldsVar = excludeFieldsVar,
      timestampFieldVar = timestampFieldVar,
      levelFieldVar = levelFieldVar,
      messageFieldVar = messageFieldVar,
      stackTraceFieldVar = stackTraceFieldVar,
      loggerNameFieldVar = loggerNameFieldVar,
      threadNameFieldVar = threadNameFieldVar,
      loadingBus = loadingBus,
      filterResultSignal = filterResultSignal,
      configSignal = configSignal,
      uiExtraConfigSignal = uiExtraConfigSignal
    )
  }
}
