package ru.d10xa.jsonlogviewer.logfmt

object Parser {
  
  def parse(line: String): Map[String, Any] = {
    var key = ""
    var value: Any = ""
    var inKey = false
    var inValue = false
    var inQuote = false
    var hadQuote = false
    var objectMap = Map.empty[String, Any]

    val trimmedLine = if (line.endsWith("\n")) line.dropRight(1) else line
    var i = 0

    while (i < trimmedLine.length) {
      if ((trimmedLine(i) == ' ' && !inQuote) || i == trimmedLine.length) {
        if (inKey && key.nonEmpty) {
          objectMap += (key -> true)
        } else if (inValue) {
          value = value match {
            case s: String =>
              s match {
                case "true" => true
                case "false" => false
                case "" if !hadQuote => null
                case _ => s
              }
            case _ => value
          }
          objectMap += (key -> value)
          value = ""
        }

        inKey = false
        inValue = false
        inQuote = false
        hadQuote = false
      }

      if (i < trimmedLine.length) {
        trimmedLine(i) match {
          case '=' if !inQuote =>
            inKey = false
            inValue = true
          case '\\' =>
            i += 1
            if (i < trimmedLine.length) value = value match {
              case s: String => s + trimmedLine(i)
              case _ => trimmedLine(i).toString
            }
          case '"' =>
            hadQuote = true
            inQuote = !inQuote
          case ' ' if !inValue && !inKey => // No action required
          case _ if !inValue && !inKey =>
            inKey = true
            key = trimmedLine(i).toString
          case _ if inKey =>
            key += trimmedLine(i)
          case _ if inValue =>
            value = value match {
              case s: String => s + trimmedLine(i)
              case _ => trimmedLine(i).toString
            }
        }
      }
      i += 1
    }
    objectMap
  }
}

