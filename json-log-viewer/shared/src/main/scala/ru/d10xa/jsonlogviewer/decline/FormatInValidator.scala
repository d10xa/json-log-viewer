package ru.d10xa.jsonlogviewer.decline

import cats.data.NonEmptyList
import cats.data.Validated
import cats.data.ValidatedNel
import com.monovore.decline.Argument
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn

object FormatInValidator {
  def toValidatedFormatIn(
    str: String
  ): Validated[NonEmptyList[String], FormatIn] = str match
    case "json"   => Validated.valid(FormatIn.Json)
    case "logfmt" => Validated.valid(FormatIn.Logfmt)
    case "csv"    => Validated.valid(FormatIn.Csv)
    case other    => Validated.invalidNel(s"Wrong format: $other")

  given Argument[FormatIn] with
    def read(string: String): ValidatedNel[String, FormatIn] =
      toValidatedFormatIn(string)
    def defaultMetavar: String = "json|logfmt|csv"
}
