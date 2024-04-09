package ru.d10xa.jsonlogviewer.logfmt

import scala.annotation.tailrec

object LogFmtDecoder {

  def decode(log: String): (Map[String, String], Vector[String]) = {

    @tailrec
    def parse(
      otherValues: Vector[String],
      chars: List[Char],
      acc: Map[String, String],
      currentKey: String = "",
      currentValue: String = "",
      inQuotes: Boolean = false,
      escapeNext: Boolean = false
    ): (Map[String, String], Vector[String]) = chars match {
      case Nil =>
        if (currentKey.nonEmpty)
          (acc + (currentKey -> currentValue), otherValues)
        else
          (
            acc,
            otherValues ++ (if (currentValue.nonEmpty) Vector(currentValue)
                            else Vector.empty)
          )
      case '"' :: tail if !escapeNext && !inQuotes =>
        parse(otherValues, tail, acc, currentKey, currentValue, inQuotes = true)
      case '"' :: tail if !escapeNext && inQuotes =>
        parse(
          otherValues,
          tail,
          acc + (currentKey -> currentValue),
          inQuotes = false
        )
      case '\\' :: tail if inQuotes =>
        parse(
          otherValues,
          tail,
          acc,
          currentKey,
          currentValue,
          inQuotes,
          escapeNext = true
        )
      case char :: tail if escapeNext =>
        parse(
          otherValues,
          tail,
          acc,
          currentKey,
          currentValue + char,
          inQuotes,
          escapeNext = false
        )
      case '=' :: tail if !inQuotes && currentKey.isEmpty =>
        parse(otherValues, tail, acc, currentValue, "")
      case ' ' :: tail if !inQuotes && currentKey.nonEmpty =>
        parse(
          otherValues,
          tail.dropWhile(_ == ' '),
          acc + (currentKey -> currentValue)
        )
      case char :: tail if inQuotes || char != ' ' =>
        parse(otherValues, tail, acc, currentKey, currentValue + char, inQuotes)
      case _ :: tail =>
        val values = currentValue match
          case ""          => otherValues
          case nonEmptyStr => otherValues :+ nonEmptyStr
        parse(values, tail, acc)
    }

    parse(Vector.empty[String], log.toList, Map.empty[String, String])
  }

}
