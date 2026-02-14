package ru.d10xa.jsonlogviewer

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.Config.FormatOut
import scalajs.js

object UrlState {

  private def getSearchParams: Map[String, String] = {
    val search = dom.window.location.search
    val queryString =
      if search.length > 1 then search
      else
        val hash = dom.window.location.hash
        val qIdx = hash.indexOf('?')
        if qIdx >= 0 then hash.substring(qIdx) else ""
    if queryString.length <= 1 then Map.empty
    else
      val params = new dom.URLSearchParams(queryString)
      var result = Map.empty[String, String]
      params.forEach { (value, key) =>
        result = result + (key -> value)
      }
      result
  }

  def hydrate(state: AppState): Unit = {
    val params = getSearchParams
    params.get("filter").foreach(state.filterVar.set)
    params.get("formatIn").foreach {
      case "json"   => state.formatInVar.set(FormatIn.Json)
      case "logfmt" => state.formatInVar.set(FormatIn.Logfmt)
      case "csv"    => state.formatInVar.set(FormatIn.Csv)
      case _        => ()
    }
    params.get("formatOut").foreach {
      case "raw"    => state.formatOutVar.set(FormatOut.Raw)
      case "pretty" => state.formatOutVar.set(FormatOut.Pretty)
      case _        => ()
    }
    params.get("fuzzyInclude").foreach(state.fuzzyIncludeVar.set)
    params.get("fuzzyExclude").foreach(state.fuzzyExcludeVar.set)
    params.get("excludeFields").foreach(state.excludeFieldsVar.set)
    params
      .get("showEmptyFields")
      .foreach(v => state.showEmptyFieldsVar.set(v == "true"))
    params.get("timestampField").foreach(state.timestampFieldVar.set)
    params.get("levelField").foreach(state.levelFieldVar.set)
    params.get("messageField").foreach(state.messageFieldVar.set)
    params.get("stackTraceField").foreach(state.stackTraceFieldVar.set)
    params.get("loggerNameField").foreach(state.loggerNameFieldVar.set)
    params.get("threadNameField").foreach(state.threadNameFieldVar.set)
  }

  def persistBinder(state: AppState): Modifier[HtmlElement] = {
    val combinedSignal = for {
      filter <- state.filterVar.signal
      formatIn <- state.formatInVar.signal
      formatOut <- state.formatOutVar.signal
      fuzzyInclude <- state.fuzzyIncludeVar.signal
      fuzzyExclude <- state.fuzzyExcludeVar.signal
      excludeFields <- state.excludeFieldsVar.signal
      showEmptyFields <- state.showEmptyFieldsVar.signal
      timestampField <- state.timestampFieldVar.signal
      levelField <- state.levelFieldVar.signal
      messageField <- state.messageFieldVar.signal
      stackTraceField <- state.stackTraceFieldVar.signal
      loggerNameField <- state.loggerNameFieldVar.signal
      threadNameField <- state.threadNameFieldVar.signal
    } yield (
      filter,
      formatIn,
      formatOut,
      fuzzyInclude,
      fuzzyExclude,
      excludeFields,
      showEmptyFields,
      timestampField,
      levelField,
      messageField,
      stackTraceField,
      loggerNameField,
      threadNameField
    )

    combinedSignal.changes
      .debounce(500) --> Observer[
      (
        String,
        FormatIn,
        FormatOut,
        String,
        String,
        String,
        Boolean,
        String,
        String,
        String,
        String,
        String,
        String
      )
    ] {
      case (
            filter,
            formatIn,
            formatOut,
            fuzzyInclude,
            fuzzyExclude,
            excludeFields,
            showEmptyFields,
            timestampField,
            levelField,
            messageField,
            stackTraceField,
            loggerNameField,
            threadNameField
          ) =>
        val formatInStr = formatIn match
          case FormatIn.Json   => "json"
          case FormatIn.Logfmt => "logfmt"
          case FormatIn.Csv    => "csv"
        val formatOutStr = formatOut match
          case FormatOut.Raw    => "raw"
          case FormatOut.Pretty => "pretty"
        val params = new dom.URLSearchParams()
        if filter.nonEmpty then params.set("filter", filter)
        if formatInStr != "json" then params.set("formatIn", formatInStr)
        if formatOutStr != "pretty" then params.set("formatOut", formatOutStr)
        if fuzzyInclude.nonEmpty then params.set("fuzzyInclude", fuzzyInclude)
        if fuzzyExclude.nonEmpty then params.set("fuzzyExclude", fuzzyExclude)
        if excludeFields.nonEmpty then
          params.set("excludeFields", excludeFields)
        if showEmptyFields then params.set("showEmptyFields", "true")
        if timestampField.nonEmpty then
          params.set("timestampField", timestampField)
        if levelField.nonEmpty then params.set("levelField", levelField)
        if messageField.nonEmpty then params.set("messageField", messageField)
        if stackTraceField.nonEmpty then
          params.set("stackTraceField", stackTraceField)
        if loggerNameField.nonEmpty then
          params.set("loggerNameField", loggerNameField)
        if threadNameField.nonEmpty then
          params.set("threadNameField", threadNameField)
        val qs = params.toString()
        val hash = dom.window.location.hash
        val hashPath =
          val qIdx = hash.indexOf('?')
          if qIdx >= 0 then hash.substring(0, qIdx)
          else if hash.isEmpty then "#/"
          else hash
        val newUrl =
          if qs.nonEmpty then
            s"${dom.window.location.pathname}$hashPath?$qs"
          else s"${dom.window.location.pathname}$hashPath"
        dom.window.history.replaceState(null, "", newUrl)
    }
  }
}
