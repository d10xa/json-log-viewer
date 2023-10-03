package ru.d10xa.jsonlogviewer

trait OutputLineFormatter {
  def formatLine(p: ParseResult): fansi.Str
}
