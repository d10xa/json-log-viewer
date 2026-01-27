package ru.d10xa.jsonlogviewer

import cats.effect.IO
import cats.effect.Ref
import fs2.Stream
import munit.CatsEffectSuite
import ru.d10xa.jsonlogviewer.cache.CachedResolvedState
import ru.d10xa.jsonlogviewer.cache.FilterCacheManager
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig
import ru.d10xa.jsonlogviewer.decline.TimestampConfig
import ru.d10xa.jsonlogviewer.shell.ShellImpl

class LogViewerStreamPerformanceTest extends CatsEffectSuite {

  val baseConfig: Config = Config(
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
    showEmptyFields = false
  )

  def generateLogLine(i: Int): String =
    s"""{"@timestamp":"2023-01-01T10:00:${i % 60}Z","level":"INFO","message":"Message $i","logger_name":"TestLogger","thread_name":"main"}"""

  test("cache should not be rebuilt when processing many lines without config changes") {
    val lineCount = 10000
    val logLines = (1 to lineCount).map(generateLogLine).toList

    val initialConfigYaml: Option[ConfigYaml] = None
    val initialCache = FilterCacheManager.buildCache(baseConfig, initialConfigYaml)

    for {
      configRef <- Ref.of[IO, Option[ConfigYaml]](initialConfigYaml)
      cacheRef <- Ref.of[IO, CachedResolvedState](initialCache)
      rebuildCounter <- Ref.of[IO, Int](0)

      // Wrap cacheRef to count rebuilds
      countingCacheRef = new Ref[IO, CachedResolvedState] {
        def get: IO[CachedResolvedState] = cacheRef.get
        def set(a: CachedResolvedState): IO[Unit] =
          rebuildCounter.update(_ + 1) *> cacheRef.set(a)
        def access: IO[(CachedResolvedState, CachedResolvedState => IO[Boolean])] =
          cacheRef.access
        def tryUpdate(f: CachedResolvedState => CachedResolvedState): IO[Boolean] =
          cacheRef.tryUpdate(f)
        def tryModify[B](f: CachedResolvedState => (CachedResolvedState, B)): IO[Option[B]] =
          cacheRef.tryModify(f)
        def update(f: CachedResolvedState => CachedResolvedState): IO[Unit] =
          cacheRef.update(f)
        def modify[B](f: CachedResolvedState => (CachedResolvedState, B)): IO[B] =
          cacheRef.modify(f)
        def tryModifyState[B](state: cats.data.State[CachedResolvedState, B]): IO[Option[B]] =
          cacheRef.tryModifyState(state)
        def modifyState[B](state: cats.data.State[CachedResolvedState, B]): IO[B] =
          cacheRef.modifyState(state)
      }

      testStreamImpl = new StdInLinesStream {
        override def stdinLinesStream: Stream[IO, String] =
          Stream.emits(logLines)
      }

      startTime <- IO.realTime
      ctx = StreamContext(
        config = baseConfig,
        configYamlRef = configRef,
        cacheRef = countingCacheRef,
        stdinStream = testStreamImpl,
        shell = new ShellImpl
      )
      results <- LogViewerStream
        .stream(ctx)
        .compile
        .toList
      endTime <- IO.realTime

      rebuilds <- rebuildCounter.get
      duration = (endTime - startTime).toMillis

    } yield {
      // Verify all lines were processed (filter out interspersed newlines)
      val processedLines = results.count(r => r.nonEmpty && r != "\n")
      assertEquals(
        processedLines,
        lineCount,
        s"Expected $lineCount processed lines"
      )

      // Cache should NOT be rebuilt during processing (0 rebuilds expected)
      assertEquals(
        rebuilds,
        0,
        s"Cache was rebuilt $rebuilds times, expected 0 (config didn't change)"
      )

      // Log performance info (not a hard assertion, just for visibility)
      println(s"Performance: processed $lineCount lines in ${duration}ms")
      println(s"Throughput: ${lineCount * 1000 / math.max(duration, 1)} lines/sec")
    }
  }

  test("cache should be rebuilt only once per config change") {
    val linesPerPhase = 1000
    val logLines = (1 to linesPerPhase).map(generateLogLine).toList

    val initialConfigYaml: Option[ConfigYaml] = None
    val initialCache = FilterCacheManager.buildCache(baseConfig, initialConfigYaml)

    for {
      configRef <- Ref.of[IO, Option[ConfigYaml]](initialConfigYaml)
      cacheRef <- Ref.of[IO, CachedResolvedState](initialCache)
      rebuildCounter <- Ref.of[IO, Int](0)

      countingCacheRef = new Ref[IO, CachedResolvedState] {
        def get: IO[CachedResolvedState] = cacheRef.get
        def set(a: CachedResolvedState): IO[Unit] =
          rebuildCounter.update(_ + 1) *> cacheRef.set(a)
        def access: IO[(CachedResolvedState, CachedResolvedState => IO[Boolean])] =
          cacheRef.access
        def tryUpdate(f: CachedResolvedState => CachedResolvedState): IO[Boolean] =
          cacheRef.tryUpdate(f)
        def tryModify[B](f: CachedResolvedState => (CachedResolvedState, B)): IO[Option[B]] =
          cacheRef.tryModify(f)
        def update(f: CachedResolvedState => CachedResolvedState): IO[Unit] =
          cacheRef.update(f)
        def modify[B](f: CachedResolvedState => (CachedResolvedState, B)): IO[B] =
          cacheRef.modify(f)
        def tryModifyState[B](state: cats.data.State[CachedResolvedState, B]): IO[Option[B]] =
          cacheRef.tryModifyState(state)
        def modifyState[B](state: cats.data.State[CachedResolvedState, B]): IO[B] =
          cacheRef.modifyState(state)
      }

      // Process lines, then change config mid-stream
      testStreamImpl = new StdInLinesStream {
        override def stdinLinesStream: Stream[IO, String] =
          Stream.emits(logLines) ++
            Stream
              .eval(
                configRef.set(
                  Some(
                    ConfigYaml(
                      showEmptyFields = Some(true),
                      fieldNames = None,
                      feeds = None
                    )
                  )
                )
              )
              .drain ++
            Stream.emits(logLines)
      }

      ctx = StreamContext(
        config = baseConfig,
        configYamlRef = configRef,
        cacheRef = countingCacheRef,
        stdinStream = testStreamImpl,
        shell = new ShellImpl
      )
      results <- LogViewerStream
        .stream(ctx)
        .compile
        .toList

      rebuilds <- rebuildCounter.get

    } yield {
      // Verify lines were processed from both phases (filter out interspersed newlines)
      val processedLines = results.count(r => r.nonEmpty && r != "\n")
      assert(
        processedLines >= linesPerPhase,
        s"Expected at least $linesPerPhase processed lines, got $processedLines"
      )

      // Cache should be rebuilt exactly once (when config changed)
      assertEquals(
        rebuilds,
        1,
        s"Cache was rebuilt $rebuilds times, expected 1 (one config change)"
      )
    }
  }
}
