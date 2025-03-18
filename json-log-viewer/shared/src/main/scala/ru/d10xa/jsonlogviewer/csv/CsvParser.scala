package ru.d10xa.jsonlogviewer.csv

class CsvParser {

  def parseLine(line: String, delimiter: Char = ','): List[String] = {
    val result = scala.collection.mutable.ListBuffer[String]()
    val currentField = new StringBuilder
    var inQuotes = false
    var i = 0

    while (i < line.length) {
      val c = line(i)
      if (c == '"') {
        if (inQuotes && i + 1 < line.length && line(i + 1) == '"') {
          currentField.append('"')
          i += 1
        } else {
          inQuotes = !inQuotes
        }
      } else if (c == delimiter && !inQuotes) {
        result += currentField.toString
        currentField.clear()
      } else {
        currentField.append(c)
      }
      i += 1
    }

    result += currentField.toString

    result.toList
  }
}
