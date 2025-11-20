package ru.d10xa.jsonlogviewer.decline.yaml

import cats.data.ValidatedNel
import cats.syntax.all.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.yaml.scalayaml.parser
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.FormatInValidator
import ru.d10xa.jsonlogviewer.decline.QueryASTValidator
import ru.d10xa.jsonlogviewer.query.QueryAST

class ConfigYamlLoaderImpl extends ConfigYamlLoader {
  private def trimCommentedLines(str: String): String =
    str.linesIterator
      .filterNot(line =>
        line.trim.startsWith("#") || line.trim.startsWith("//")
      )
      .mkString("\n")
      .replace("\\n", " ")
      .trim

  private given Decoder[QueryAST] = Decoder[String].emap { str =>
    val trimmed = trimCommentedLines(str)
    QueryASTValidator.toValidatedQueryAST(trimmed).toEither.leftMap { errors =>
      errors.toList.mkString(", ")
    }
  }

  // Custom Decoder for FormatIn - converts string to enum
  private given Decoder[FormatIn] = Decoder[String].emap { formatStr =>
    FormatInValidator.toValidatedFormatIn(formatStr).toEither.leftMap { errors =>
      errors.toList.mkString(", ")
    }
  }

  // Automatic derivation for case classes
  private given Decoder[FieldNames] = deriveDecoder[FieldNames]
  private given Decoder[Feed] = deriveDecoder[Feed]
  private given Decoder[ConfigYaml] = deriveDecoder[ConfigYaml]

  def parseYamlFile(content: String): ValidatedNel[String, ConfigYaml] = {
    val uncommentedContent = content.linesIterator
      .filterNot(line => line.trim.startsWith("#"))
      .mkString("\n")
      .trim

    if (uncommentedContent.isEmpty) {
      cats.data.Validated.valid(ConfigYaml.empty)
    } else {
      parser.parse(content) match {
        case Left(error) =>
          cats.data.Validated.invalidNel(
            s"YAML parsing error: ${error.getMessage}"
          )
        case Right(json) =>
          json.as[ConfigYaml] match {
            case Right(config) =>
              cats.data.Validated.valid(config)
            case Left(error) =>
              cats.data.Validated.invalidNel(
                s"YAML validation error: ${error.getMessage}"
              )
          }
      }
    }
  }
}
