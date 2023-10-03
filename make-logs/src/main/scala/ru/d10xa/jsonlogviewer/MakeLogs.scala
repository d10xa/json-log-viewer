package ru.d10xa.jsonlogviewer

import org.slf4j.LoggerFactory
import net.logstash.logback.argument.StructuredArguments.*
import scala.jdk.CollectionConverters.*

@main def makeLogs: Unit =
  val logger = LoggerFactory.getLogger("MakeLogs")
  logger.info("test {}", keyValue("customKey", "value"))
  logger.error(
    "error message {}",
    entries(Map("mapKey" -> "mapValue").asJava),
    new RuntimeException(
      new IllegalArgumentException(new ArithmeticException("hello"))
    )
  )
