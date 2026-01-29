package ru.d10xa.jsonlogviewer

import ru.d10xa.jsonlogviewer.config.ResolvedConfig
import ru.d10xa.jsonlogviewer.decline.{Config, FieldNamesConfig}

trait TestResolvedConfig {

  def baseResolvedConfig: ResolvedConfig = ResolvedConfig(
    feedName = None,
    commands = List.empty,
    inlineInput = None,
    filter = None,
    formatIn = Some(Config.FormatIn.Json),
    formatOut = Some(Config.FormatOut.Raw),
    fieldNames = FieldNamesConfig(
      timestampFieldName = "@timestamp",
      levelFieldName = "level",
      messageFieldName = "message",
      stackTraceFieldName = "stack_trace",
      loggerNameFieldName = "logger_name",
      threadNameFieldName = "thread_name"
    ),
    rawInclude = None,
    rawExclude = None,
    fuzzyInclude = None,
    fuzzyExclude = None,
    excludeFields = None,
    timestampAfter = None,
    timestampBefore = None,
    grep = List.empty,
    showEmptyFields = false,
    restart = false,
    restartDelayMs = 1000L,
    maxRestarts = None
  )
}
