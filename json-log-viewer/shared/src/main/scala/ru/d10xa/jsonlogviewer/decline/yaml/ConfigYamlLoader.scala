package ru.d10xa.jsonlogviewer.decline.yaml

import cats.data.NonEmptyList
import cats.data.Validated
import cats.data.ValidatedNel
import cats.syntax.all.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.yaml.scalayaml.parser
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.FormatInValidator
import ru.d10xa.jsonlogviewer.decline.QueryASTValidator
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.query.QueryAST

object ConfigYamlLoader {
  private def trimCommentedLines(str: String): String =
    str.linesIterator
      .filterNot(line =>
        line.trim.startsWith("#") || line.trim.startsWith("//")
      )
      .mkString("\n")
      .replace("\\n", " ")
      .trim

  private def parseOptionalQueryAST(
    fields: Map[String, Json],
    fieldName: String
  ): ValidatedNel[String, Option[QueryAST]] =
    parseOptionalStringField(
      fields,
      fieldName,
      s"Invalid '$fieldName' field format"
    ).andThen {
      case Some(str) =>
        val trimmed = trimCommentedLines(str)
        QueryASTValidator.toValidatedQueryAST(trimmed).map(Some(_))
      case None => Validated.valid(None)
    }

  private def parseOptionalFormatIn(
    fields: Map[String, Json],
    fieldName: String
  ): ValidatedNel[String, Option[FormatIn]] =
    parseOptionalStringField(
      fields,
      fieldName,
      s"Invalid '$fieldName' field format"
    ).andThen {
      case Some(formatStr) =>
        FormatInValidator.toValidatedFormatIn(formatStr).map(Some(_))
      case None => Validated.valid(None)
    }

  private def parseOptionalListString(
    fields: Map[String, Json],
    fieldName: String
  ): ValidatedNel[String, Option[List[String]]] =
    fields.get(fieldName) match {
      case Some(jsonValue) =>
        jsonValue
          .as[List[String]]
          .leftMap(_ => s"Invalid '$fieldName' field format")
          .toValidatedNel
          .map(Some(_))
      case None => Validated.valid(None)
    }

  private def parseOptionalFeeds(
    fields: Map[String, Json],
    fieldName: String
  ): ValidatedNel[String, Option[List[Feed]]] =
    fields.get(fieldName) match {
      case Some(jsonValue) =>
        jsonValue
          .as[List[Json]]
          .leftMap(_ => s"Invalid '$fieldName' field format, should be a list")
          .toValidatedNel
          .andThen(_.traverse(parseFeed))
          .map(Some(_))
      case None => Validated.valid(None)
    }

  private def parseOptionalStringField(
    fields: Map[String, Json],
    fieldName: String,
    errorMsg: String
  ): ValidatedNel[String, Option[String]] =
    fields.get(fieldName) match {
      case Some(jsonValue) =>
        jsonValue.as[String].leftMap(_ => errorMsg).toValidatedNel.map(Some(_))
      case None => Validated.valid(None)
    }

  private def parseMandatoryStringField(
    fields: Map[String, Json],
    fieldName: String,
    errorMsg: String
  ): ValidatedNel[String, String] =
    fields.get(fieldName) match {
      case Some(j) =>
        j.as[String].leftMap(_ => errorMsg).toValidatedNel
      case None =>
        Validated.invalidNel(s"Missing '$fieldName' field in feed")
    }

  private def parseMandatoryCommands(
    fields: Map[String, Json]
  ): ValidatedNel[String, List[String]] =
    fields.get("commands") match {
      case Some(c) =>
        c.as[List[String]]
          .leftMap(_ => "Invalid 'commands' field in feed")
          .toValidatedNel
      case None =>
        Validated.invalidNel("Missing 'commands' field in feed")
    }

  private def parseFeed(feedJson: Json): ValidatedNel[String, Feed] =
    feedJson.asObject.map(_.toMap) match {
      case None => Validated.invalidNel("Feed entry is not a valid JSON object")
      case Some(feedFields) =>
        val nameValidated = parseMandatoryStringField(
          feedFields,
          "name",
          "Invalid 'name' field in feed"
        )
        val commandsValidated = parseMandatoryCommands(feedFields)
        val filterValidated = parseOptionalQueryAST(feedFields, "filter")
        val formatInValidated
          : Validated[NonEmptyList[String], Option[FormatIn]] =
          parseOptionalFormatIn(feedFields, "formatIn")

        (nameValidated, commandsValidated, filterValidated, formatInValidated)
          .mapN(Feed.apply)
    }

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
                parseOptionalQueryAST(fields, "filter")

              val formatInValidated: ValidatedNel[String, Option[FormatIn]] =
                parseOptionalFormatIn(fields, "formatIn")
              val commandsValidated
                : ValidatedNel[String, Option[List[String]]] =
                parseOptionalListString(fields, "commands")
              val feedsValidated: ValidatedNel[String, Option[List[Feed]]] =
                parseOptionalFeeds(fields, "feeds")

              (
                filterValidated,
                formatInValidated,
                commandsValidated,
                feedsValidated
              ).mapN(ConfigYaml.apply)
          }
      }
    }
  }
}
