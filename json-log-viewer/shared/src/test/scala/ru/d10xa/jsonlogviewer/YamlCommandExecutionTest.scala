package ru.d10xa.jsonlogviewer

import cats.effect.IO
import cats.effect.Ref
import fs2.Stream
import munit.CatsEffectSuite
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.Feed
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig
import ru.d10xa.jsonlogviewer.decline.TimestampConfig
import ru.d10xa.jsonlogviewer.shell.Shell

/** Tests to verify the proper command execution behavior based on YAML
  * configuration. Ensures that commands from YAML are executed when present and
  * stdin is used when no commands are available.
  */
class YamlCommandExecutionTest extends CatsEffectSuite {

  private val basicConfig = Config(
    configFile = None,
    fieldNames = FieldNamesConfig(
      timestampFieldName = "@timestamp",
      levelFieldName = "level",
      messageFieldName = "message",
      stackTraceFieldName = "stack_trace",
      loggerNameFieldName = "logger_name",
      threadNameFieldName = "thread_name"
    ),
    timestamp = TimestampConfig(None, None),
    grep = List.empty,
    filter = None,
    formatIn = None,
    formatOut = None,
    showEmptyFields = false
  )

  test("should use commands from YAML when inlineInput is absent") {
    val testStdinStream = new StdInLinesStream {
      override def stdinLinesStream: Stream[IO, String] =
        Stream.emit("FROM_STDIN")
    }

    val testShell = new Shell {
      override def mergeCommandsAndInlineInput(
        commands: List[String],
        inlineInput: Option[String]
      ): Stream[IO, String] =
        Stream.emit(s"FROM_COMMAND:${commands.mkString(",")}")
    }

    val configYaml = ConfigYaml(
      fieldNames = None,
      feeds = Some(
        List(
          Feed(
            name = Some("test-feed"),
            commands = List("cat test.log"),
            inlineInput = None, // No inline input
            filter = None,
            formatIn = None,
            fieldNames = None,
            rawInclude = None,
            rawExclude = None,
            excludeFields = None,
            showEmptyFields = None
          )
        )
      ),
      showEmptyFields = None
    )

    for {
      yamlRef <- Ref.of[IO, Option[ConfigYaml]](Some(configYaml))
      output <- LogViewerStream
        .stream(
          basicConfig,
          yamlRef,
          testStdinStream,
          testShell
        )
        .compile
        .toList
      _ <- IO {
        assert(
          output.exists(_.contains("FROM_COMMAND")),
          "Should use output from commands in YAML"
        )
        assert(
          !output.exists(_.contains("FROM_STDIN")),
          "Should not use stdin"
        )
      }
    } yield ()
  }

  test("should use stdin when no commands or inlineInput are present") {
    val testStdinStream = new StdInLinesStream {
      override def stdinLinesStream: Stream[IO, String] =
        Stream.emit("FROM_STDIN")
    }

    val testShell = new Shell {
      override def mergeCommandsAndInlineInput(
        commands: List[String],
        inlineInput: Option[String]
      ): Stream[IO, String] =
        Stream.emit("FROM_COMMAND")
    }

    val configYaml = ConfigYaml(
      fieldNames = None,
      feeds = Some(
        List(
          Feed(
            name = Some("test-feed"),
            commands = List.empty, // Empty commands list
            inlineInput = None, // No inline input
            filter = None,
            formatIn = None,
            fieldNames = None,
            rawInclude = None,
            rawExclude = None,
            excludeFields = None,
            showEmptyFields = None
          )
        )
      ),
      showEmptyFields = None
    )

    for {
      yamlRef <- Ref.of[IO, Option[ConfigYaml]](Some(configYaml))
      output <- LogViewerStream
        .stream(
          basicConfig,
          yamlRef,
          testStdinStream,
          testShell
        )
        .compile
        .toList
      _ <- IO {
        assert(
          output.exists(_.contains("FROM_STDIN")),
          "Should use stdin"
        )
        assert(
          !output.exists(_.contains("FROM_COMMAND")),
          "Should not use command output when no commands are present"
        )
      }
    } yield ()
  }
}
