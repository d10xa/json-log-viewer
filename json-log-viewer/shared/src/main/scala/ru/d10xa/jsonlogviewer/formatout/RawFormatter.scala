package ru.d10xa.jsonlogviewer.formatout

import fansi.ErrorMode.Strip
import fansi.EscapeAttr
import fansi.Str
import ru.d10xa.jsonlogviewer.OutputLineFormatter
import ru.d10xa.jsonlogviewer.ParseResult
import ru.d10xa.jsonlogviewer.decline.Config

class RawFormatter extends OutputLineFormatter:
  override def formatLine(p: ParseResult): Str =
    p.raw
