package ru.d10xa.jsonlogviewer.decline

import cats.data.Validated
import cats.data.ValidatedNel
import com.monovore.decline.Argument
import ru.d10xa.jsonlogviewer.decline.Config.ConfigGrep

object ConfigGrepValidator {

  def toValidatedConfigGrep(
    string: String
  ): ValidatedNel[String, ConfigGrep] =
    string.split(":", 2) match {
      case Array(key, value) =>
        Validated.valid(ConfigGrep(key, value.r))
      case _ => Validated.invalidNel(s"Invalid key:value pair: $string")
    }

  given Argument[ConfigGrep] with
    def read(string: String): ValidatedNel[String, ConfigGrep] =
      toValidatedConfigGrep(string)
    def defaultMetavar: String = "key:value"

}
