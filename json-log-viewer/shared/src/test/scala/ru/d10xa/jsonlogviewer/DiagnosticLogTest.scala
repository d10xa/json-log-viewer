package ru.d10xa.jsonlogviewer

import cats.effect.IO
import cats.effect.Ref
import fs2.Stream
import munit.CatsEffectSuite
import ru.d10xa.jsonlogviewer.cache.CachedResolvedState
import ru.d10xa.jsonlogviewer.cache.FilterCacheManager
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.Feed
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig
import ru.d10xa.jsonlogviewer.decline.TimestampConfig
import ru.d10xa.jsonlogviewer.query.QueryCompiler
import ru.d10xa.jsonlogviewer.shell.RestartConfig
import ru.d10xa.jsonlogviewer.shell.Shell

class DiagnosticLogTest extends CatsEffectSuite {

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
    formatIn = Some(Config.FormatIn.Json),
    formatOut = Some(Config.FormatOut.Raw),
    showEmptyFields = false,
    commands = List.empty,
    restart = false,
    restartDelayMs = None,
    maxRestarts = None,
    debug = true
  )

  private class CollectingDiagnosticLog(ref: Ref[IO, List[String]])
      extends DiagnosticLog:
    override def error(message: String): IO[Unit] =
      ref.update(_ :+ s"ERROR: $message")
    override def debug(message: String): IO[Unit] =
      ref.update(_ :+ s"DEBUG: $message")

  private val infoLog =
    """{"@timestamp":"2023-01-01T10:00:00Z","level":"INFO","message":"Hello world","logger_name":"Test","thread_name":"main"}"""

  private val errorLog =
    """{"@timestamp":"2023-01-01T12:00:00Z","level":"ERROR","message":"Error occurred","logger_name":"Test","thread_name":"main"}"""

  private val noopShell = new Shell {
    override def mergeCommandsAndInlineInput(
      commands: List[String],
      inlineInput: Option[String]
    ): Stream[IO, String] =
      Stream.emits(inlineInput.toList.flatMap(_.split("\n").toList))

    override def mergeCommandsAndInlineInputWithRestart(
      commands: List[String],
      inlineInput: Option[String],
      restartConfig: RestartConfig,
      onRestart: IO[Unit]
    ): Stream[IO, String] =
      Stream.emits(inlineInput.toList.flatMap(_.split("\n").toList))
  }

  private def makeFeed(
    name: String,
    inlineInput: String,
    filter: Option[ru.d10xa.jsonlogviewer.query.QueryAST] = None
  ): Feed =
    Feed(
      name = Some(name),
      commands = List.empty,
      inlineInput = Some(inlineInput),
      filter = filter,
      formatIn = None,
      fieldNames = None,
      rawInclude = None,
      rawExclude = None,
      fuzzyInclude = None,
      fuzzyExclude = None,
      excludeFields = None,
      showEmptyFields = None,
      restart = None,
      restartDelayMs = None,
      maxRestarts = None
    )

  private def runWithFeed(
    feed: Feed,
    config: Config = basicConfig
  ): IO[(List[String], List[String])] = {
    val yamlConfig = ConfigYaml(
      showEmptyFields = None,
      fieldNames = None,
      feeds = Some(List(feed))
    )
    val initialConfigYaml = Some(yamlConfig)
    val initialCache = FilterCacheManager
      .buildCache(config, initialConfigYaml)
      .fold(err => throw new RuntimeException(s"buildCache failed: $err"), identity)

    for {
      messagesRef <- Ref.of[IO, List[String]](List.empty)
      log = new CollectingDiagnosticLog(messagesRef)
      configRef <- Ref.of[IO, Option[ConfigYaml]](initialConfigYaml)
      cacheRef <- Ref.of[IO, CachedResolvedState](initialCache)

      testStdin = new StdInLinesStream {
        override def stdinLinesStream: Stream[IO, String] = Stream.empty
      }

      ctx = StreamContext(
        config = config,
        configYamlRef = configRef,
        cacheRef = cacheRef,
        stdinStream = testStdin,
        shell = noopShell,
        diagnosticLog = log
      )

      output <- LogViewerStream
        .stream(ctx)
        .compile
        .toList

      messages <- messagesRef.get
    } yield (messages, output)
  }

  test("debug output includes starting, input source, and first line diagnostics") {
    val feed = makeFeed("my-feed", infoLog)

    runWithFeed(feed).map { (messages, _) =>
      assert(
        messages.exists(_.contains("Feed 'my-feed': starting")),
        s"Expected 'starting' diagnostic, got: $messages"
      )
      assert(
        messages.exists(
          _.contains("Feed 'my-feed': input source: inline input")
        ),
        s"Expected 'input source' diagnostic, got: $messages"
      )
      assert(
        messages.exists(
          _.contains("Feed 'my-feed': first line received from input")
        ),
        s"Expected 'first line received from input' diagnostic, got: $messages"
      )
      assert(
        messages.exists(
          _.contains("Feed 'my-feed': first line received after filtering")
        ),
        s"Expected 'first line received after filtering' diagnostic, got: $messages"
      )
    }
  }

  test("first line diagnostics fire only once with multiple input lines") {
    val multiLineInput = s"$infoLog\n$errorLog\n$infoLog"
    val feed = makeFeed("multi-feed", multiLineInput)

    runWithFeed(feed).map { (messages, _) =>
      val inputCount = messages.count(
        _.contains("first line received from input")
      )
      val filterCount = messages.count(
        _.contains("first line received after filtering")
      )
      assertEquals(
        inputCount,
        1,
        s"'first line received from input' should fire once, got $inputCount: $messages"
      )
      assertEquals(
        filterCount,
        1,
        s"'first line received after filtering' should fire once, got $filterCount: $messages"
      )
    }
  }

  test("no 'after filtering' diagnostic when all lines are filtered out") {
    val filter = QueryCompiler("level = 'FATAL'").toOption
    val feed = makeFeed("filtered-feed", s"$infoLog\n$errorLog", filter)

    runWithFeed(feed).map { (messages, _) =>
      assert(
        messages.exists(
          _.contains("Feed 'filtered-feed': first line received from input")
        ),
        s"Expected 'first line received from input' even when filtered, got: $messages"
      )
      assert(
        !messages.exists(
          _.contains("first line received after filtering")
        ),
        s"Should NOT have 'first line received after filtering' when all filtered out, got: $messages"
      )
    }
  }

  test("'from input' fires but 'after filtering' does not when filter is set on YAML feed") {
    val filter = QueryCompiler("level = 'ERROR'").toOption
    val feed = makeFeed("selective-feed", infoLog, filter)

    runWithFeed(feed).map { (messages, _) =>
      assert(
        messages.exists(
          _.contains("Feed 'selective-feed': first line received from input")
        ),
        s"Expected 'first line received from input', got: $messages"
      )
      assert(
        !messages.exists(
          _.contains(
            "Feed 'selective-feed': first line received after filtering"
          )
        ),
        s"Should NOT have 'after filtering' when single INFO line filtered by ERROR filter, got: $messages"
      )
    }
  }
}
