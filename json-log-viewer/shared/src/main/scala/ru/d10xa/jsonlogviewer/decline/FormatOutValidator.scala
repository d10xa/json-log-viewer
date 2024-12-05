package ru.d10xa.jsonlogviewer.decline

import cats.data.NonEmptyList
import cats.data.Validated
import cats.data.ValidatedNel
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.Config.FormatOut

object FormatOutValidator {
  def toValidatedFormatOut(
    str: String
  ): Validated[NonEmptyList[String], FormatOut] = str match
    case "pretty"   => Validated.valid(FormatOut.Pretty)
    case "raw" => Validated.valid(FormatOut.Raw)
    case other    => Validated.invalidNel(s"Wrong format: $other")
}
