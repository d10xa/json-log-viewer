package ru.d10xa.jsonlogviewer

trait LogLineParser {
  def parse(s: String): ParseResult

}
