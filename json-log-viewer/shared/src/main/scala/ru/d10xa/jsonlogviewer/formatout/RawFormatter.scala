package ru.d10xa.jsonlogviewer.formatout

import fansi.Str
import ru.d10xa.jsonlogviewer.OutputLineFormatter
import ru.d10xa.jsonlogviewer.ParseResult

class RawFormatter extends OutputLineFormatter:
  override def formatLine(p: ParseResult): Str =
    p.raw
