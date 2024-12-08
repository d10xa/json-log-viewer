package ru.d10xa.jsonlogviewer.decline

import cats.data.Validated
import cats.data.ValidatedNel
import cats.syntax.all.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.yaml.scalayaml.parser
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.query.QueryAST

object ConfigYamlLoader {
  def parseYamlFile(content: String): ValidatedNel[String, ConfigYaml] = {
    val uncommentedContent = content.linesIterator
      .filterNot(line => line.trim.startsWith("#"))
      .mkString("\n")
      .trim
    if (uncommentedContent.isEmpty) {
      Validated.valid(ConfigYaml.empty)
    } else {
      parser.parse(content) match {
        case Left(error) =>
          Validated.invalidNel(s"YAML parsing error: ${error.getMessage}")
        case Right(json) =>
          json.asObject.map(_.toMap) match {
            case None => Validated.invalidNel("YAML is not a valid JSON object")
            case Some(fields) =>
              val filterValidated: ValidatedNel[String, Option[QueryAST]] =
                fields.get("filter") match {
                  case Some(jsonValue) =>
                    jsonValue.as[String] match {
                      case Left(_) =>
                        Validated.invalidNel("Invalid 'filter' field format")
                      case Right(filterStr) =>
                        val trimmedStr = filterStr.linesIterator
                          .filterNot(line =>
                            line.trim.startsWith("#") || line.trim.startsWith(
                              "//"
                            )
                          )
                          .mkString("\n")
                          .replace("\\n", " ")
                          .trim
                        QueryASTValidator
                          .toValidatedQueryAST(trimmedStr)
                          .map(Some(_))
                    }
                  case None => Validated.valid(None)
                }

              val formatInValidated: ValidatedNel[String, Option[FormatIn]] =
                fields.get("formatIn") match {
                  case Some(jsonValue) =>
                    jsonValue.as[String] match {
                      case Left(_) =>
                        Validated.invalidNel("Invalid 'formatIn' field format")
                      case Right(formatStr) =>
                        FormatInValidator
                          .toValidatedFormatIn(formatStr)
                          .map(Some(_))
                    }
                  case None => Validated.valid(None)
                }
              val commandsValidated: ValidatedNel[String, Option[List[String]]] =
                fields.get("commands") match {
                  case Some(jsonValue) =>
                    jsonValue.as[List[String]] match {
                      case Left(_) =>
                        Validated.invalidNel("Invalid 'commands' field format")
                      case Right(cmds) =>
                        Validated.valid(Some(cmds))
                    }
                  case None => Validated.valid(None)
                }

              (filterValidated, formatInValidated, commandsValidated)
                .mapN(ConfigYaml.apply)
          }
      }
    }
  }
}
