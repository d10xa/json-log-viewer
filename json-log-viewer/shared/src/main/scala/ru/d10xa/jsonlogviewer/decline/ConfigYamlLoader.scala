package ru.d10xa.jsonlogviewer.decline

import io.circe._
import io.circe.yaml.scalayaml.parser
import io.circe.generic.auto._
import cats.data.ValidatedNel
import cats.syntax.all._
import cats.data.Validated
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.query.QueryAST

object ConfigYamlLoader {

  def parseYamlFile(content: String): ValidatedNel[String, ConfigYaml] = {
    // Парсим YAML в Json
    parser.parse(content) match {
      case Left(error) =>
        Validated.invalidNel(s"YAML parsing error: ${error.getMessage}")
      case Right(json) =>
        json.asObject.map(_.toMap) match {
          case None => Validated.invalidNel("YAML is not a valid JSON object")
          case Some(fields) =>
            // Обрабатываем и валидируем отдельные поля
            val filterValidated: ValidatedNel[String, Option[QueryAST]] =
              fields.get("filter") match {
                case Some(jsonValue) =>
                  jsonValue.as[String] match {
                    case Left(_) => Validated.invalidNel("Invalid 'filter' field format")
                    case Right(filterStr) =>
                      val trimmedStr = filterStr
                        .linesIterator
                        .filterNot(line => line.trim.startsWith("#") || line.trim.startsWith("//"))
                        .mkString("\n")
                        .replace("\\n", " ")
                        .trim
                      QueryASTValidator.toValidatedQueryAST(trimmedStr).map(Some(_))
                  }
                case None => Validated.valid(None)
              }

            val formatInValidated: ValidatedNel[String, Option[FormatIn]] =
              fields.get("formatIn") match {
                case Some(jsonValue) =>
                  jsonValue.as[String] match {
                    case Left(_) => Validated.invalidNel("Invalid 'formatIn' field format")
                    case Right(formatStr) =>
                      FormatInValidator.toValidatedFormatIn(formatStr).map(Some(_))
                  }
                case None => Validated.invalidNel("'formatIn' field is missing")
              }

            (filterValidated, formatInValidated).mapN(ConfigYaml.apply)
        }
    }
  }
  
}
