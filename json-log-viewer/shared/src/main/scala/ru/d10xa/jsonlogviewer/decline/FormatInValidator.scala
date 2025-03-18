package ru.d10xa.jsonlogviewer.decline

import cats.data.NonEmptyList
import cats.data.Validated
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn

object FormatInValidator {
  def toValidatedFormatIn(
    str: String
  ): Validated[NonEmptyList[String], FormatIn] = str match
    case "json"   => Validated.valid(FormatIn.Json)
    case "logfmt" => Validated.valid(FormatIn.Logfmt)
    case "csv" => Validated.valid(FormatIn.Csv)
    case other    => Validated.invalidNel(s"Wrong format: $other")
}
