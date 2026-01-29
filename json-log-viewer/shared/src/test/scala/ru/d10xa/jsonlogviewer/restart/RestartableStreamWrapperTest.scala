package ru.d10xa.jsonlogviewer.restart

import cats.effect.IO
import cats.effect.Ref
import fs2.Stream
import munit.CatsEffectSuite
import ru.d10xa.jsonlogviewer.ParseResult
import ru.d10xa.jsonlogviewer.ParsedLine

import java.time.ZonedDateTime

class RestartableStreamWrapperTest extends CatsEffectSuite {

  private def createParseResult(
    timestamp: Option[String],
    message: String
  ): ParseResult =
    ParseResult(
      raw = s"""{"timestamp":"${timestamp.getOrElse("")}","message":"$message"}""",
      parsed = Some(
        ParsedLine(
          timestamp = timestamp,
          level = Some("INFO"),
          message = Some(message),
          stackTrace = None,
          loggerName = None,
          threadName = None,
          otherAttributes = Map.empty
        )
      ),
      middle = "",
      prefix = None,
      postfix = None
    )

  test("should pass all messages when not restarted") {
    for {
      state <- RestartState.create
      messages = List(
        createParseResult(Some("2024-01-01T10:00:00Z"), "message1"),
        createParseResult(Some("2024-01-01T10:01:00Z"), "message2"),
        createParseResult(Some("2024-01-01T10:02:00Z"), "message3")
      )
      results <- RestartableStreamWrapper
        .wrapWithTimestampTracking(
          Stream.emits(messages),
          state.lastTsRef,
          state.isRestartRef
        )
        .compile
        .toList
    } yield {
      assertEquals(results.size, 3)
      assertEquals(results.flatMap(_.parsed.flatMap(_.message)), List("message1", "message2", "message3"))
    }
  }

  test("should track last timestamp") {
    for {
      state <- RestartState.create
      messages = List(
        createParseResult(Some("2024-01-01T10:00:00Z"), "message1"),
        createParseResult(Some("2024-01-01T10:02:00Z"), "message2"),
        createParseResult(Some("2024-01-01T10:01:00Z"), "message3") // out of order
      )
      _ <- RestartableStreamWrapper
        .wrapWithTimestampTracking(
          Stream.emits(messages),
          state.lastTsRef,
          state.isRestartRef
        )
        .compile
        .drain
      lastTs <- state.lastTsRef.get
    } yield {
      // Should track the max timestamp (10:02:00, not 10:01:00)
      assertEquals(
        lastTs,
        Some(ZonedDateTime.parse("2024-01-01T10:02:00Z"))
      )
    }
  }

  test("should filter duplicates after restart") {
    for {
      state <- RestartState.create

      // First batch - all should pass
      firstBatch = List(
        createParseResult(Some("2024-01-01T10:00:00Z"), "message1"),
        createParseResult(Some("2024-01-01T10:01:00Z"), "message2")
      )
      firstResults <- RestartableStreamWrapper
        .wrapWithTimestampTracking(
          Stream.emits(firstBatch),
          state.lastTsRef,
          state.isRestartRef
        )
        .compile
        .toList

      // Simulate restart
      _ <- RestartableStreamWrapper.createOnRestartCallback(state.isRestartRef)

      // Second batch after restart - duplicates should be filtered
      secondBatch = List(
        createParseResult(Some("2024-01-01T10:00:00Z"), "message1-dup"), // Before last ts
        createParseResult(Some("2024-01-01T10:01:00Z"), "message2-dup"), // Equal to last ts
        createParseResult(Some("2024-01-01T10:02:00Z"), "message3-new")  // After last ts
      )
      secondResults <- RestartableStreamWrapper
        .wrapWithTimestampTracking(
          Stream.emits(secondBatch),
          state.lastTsRef,
          state.isRestartRef
        )
        .compile
        .toList

    } yield {
      assertEquals(firstResults.size, 2)
      assertEquals(secondResults.size, 1)
      assertEquals(
        secondResults.flatMap(_.parsed.flatMap(_.message)),
        List("message3-new")
      )
    }
  }

  test("should pass messages without timestamp after restart") {
    for {
      state <- RestartState.create

      // First batch
      firstBatch = List(
        createParseResult(Some("2024-01-01T10:00:00Z"), "message1")
      )
      _ <- RestartableStreamWrapper
        .wrapWithTimestampTracking(
          Stream.emits(firstBatch),
          state.lastTsRef,
          state.isRestartRef
        )
        .compile
        .drain

      // Simulate restart
      _ <- RestartableStreamWrapper.createOnRestartCallback(state.isRestartRef)

      // Messages without timestamp should pass through
      secondBatch = List(
        createParseResult(None, "no-timestamp-message")
      )
      secondResults <- RestartableStreamWrapper
        .wrapWithTimestampTracking(
          Stream.emits(secondBatch),
          state.lastTsRef,
          state.isRestartRef
        )
        .compile
        .toList

    } yield {
      assertEquals(secondResults.size, 1)
      assertEquals(
        secondResults.flatMap(_.parsed.flatMap(_.message)),
        List("no-timestamp-message")
      )
    }
  }

  test("should keep restart flag after message without timestamp and filter old duplicates") {
    for {
      state <- RestartState.create

      // First batch to set timestamp
      firstBatch = List(
        createParseResult(Some("2024-01-01T10:00:00Z"), "message1"),
        createParseResult(Some("2024-01-01T10:01:00Z"), "message2")
      )
      _ <- RestartableStreamWrapper
        .wrapWithTimestampTracking(
          Stream.emits(firstBatch),
          state.lastTsRef,
          state.isRestartRef
        )
        .compile
        .drain

      // Simulate restart
      _ <- RestartableStreamWrapper.createOnRestartCallback(state.isRestartRef)

      // After restart: message without timestamp passes but flag stays true,
      // then duplicate with old timestamp is still filtered
      secondBatch = List(
        createParseResult(None, "no-ts-message"),
        createParseResult(Some("2024-01-01T10:00:00Z"), "old-duplicate"),
        createParseResult(Some("2024-01-01T10:02:00Z"), "new-message")
      )
      secondResults <- RestartableStreamWrapper
        .wrapWithTimestampTracking(
          Stream.emits(secondBatch),
          state.lastTsRef,
          state.isRestartRef
        )
        .compile
        .toList

      isRestartAfter <- state.isRestartRef.get
    } yield {
      assertEquals(
        secondResults.flatMap(_.parsed.flatMap(_.message)),
        List("no-ts-message", "new-message")
      )
      assertEquals(isRestartAfter, false)
    }
  }

  test("should handle restart with no previous timestamp") {
    for {
      state <- RestartState.create

      // Start with restart flag set (edge case)
      _ <- RestartableStreamWrapper.createOnRestartCallback(state.isRestartRef)

      // Messages should pass since there's no last timestamp to compare
      batch = List(
        createParseResult(Some("2024-01-01T10:00:00Z"), "message1")
      )
      results <- RestartableStreamWrapper
        .wrapWithTimestampTracking(
          Stream.emits(batch),
          state.lastTsRef,
          state.isRestartRef
        )
        .compile
        .toList

    } yield {
      assertEquals(results.size, 1)
    }
  }

  test("should clear restart flag after processing messages") {
    for {
      state <- RestartState.create

      // First batch to set timestamp
      _ <- RestartableStreamWrapper
        .wrapWithTimestampTracking(
          Stream.emit(createParseResult(Some("2024-01-01T10:00:00Z"), "msg1")),
          state.lastTsRef,
          state.isRestartRef
        )
        .compile
        .drain

      // Set restart
      _ <- RestartableStreamWrapper.createOnRestartCallback(state.isRestartRef)
      isRestartBefore <- state.isRestartRef.get

      // Process a message
      _ <- RestartableStreamWrapper
        .wrapWithTimestampTracking(
          Stream.emit(createParseResult(Some("2024-01-01T10:02:00Z"), "msg2")),
          state.lastTsRef,
          state.isRestartRef
        )
        .compile
        .drain

      isRestartAfter <- state.isRestartRef.get
    } yield {
      assertEquals(isRestartBefore, true)
      assertEquals(isRestartAfter, false)
    }
  }
}
