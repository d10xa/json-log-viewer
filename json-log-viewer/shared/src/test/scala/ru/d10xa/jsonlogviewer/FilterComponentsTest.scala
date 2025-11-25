package ru.d10xa.jsonlogviewer

import munit.FunSuite
import ru.d10xa.jsonlogviewer.decline.{Config, FieldNamesConfig}
import ru.d10xa.jsonlogviewer.formatout.{ColorLineFormatter, RawFormatter}

class FilterComponentsTest extends FunSuite with TestResolvedConfig {

  override def baseResolvedConfig = super.baseResolvedConfig.copy(
    feedName = Some("test-feed")
  )

  test("fromConfig should create all filter components") {
    val components = FilterComponents.fromConfig(baseResolvedConfig)

    assertNotEquals(components.timestampFilter, null, "TimestampFilter should be created")
    assertNotEquals(components.parseResultKeys, null, "ParseResultKeys should be created")
    assertNotEquals(components.logLineFilter, null, "LogLineFilter should be created")
    assertNotEquals(components.fuzzyFilter, null, "FuzzyFilter should be created")
    assertNotEquals(components.outputLineFormatter, null, "OutputLineFormatter should be created")
  }

  test("fromConfig should create RawFormatter when formatOut is Raw") {
    val config = baseResolvedConfig.copy(formatOut = Some(Config.FormatOut.Raw))
    val components = FilterComponents.fromConfig(config)

    assert(
      components.outputLineFormatter.isInstanceOf[RawFormatter],
      "Should create RawFormatter for Raw format"
    )
  }

  test("fromConfig should create ColorLineFormatter when formatOut is Pretty") {
    val config = baseResolvedConfig.copy(formatOut = Some(Config.FormatOut.Pretty))
    val components = FilterComponents.fromConfig(config)

    assert(
      components.outputLineFormatter.isInstanceOf[ColorLineFormatter],
      "Should create ColorLineFormatter for Pretty format"
    )
  }

  test("fromConfig should create ColorLineFormatter when formatOut is None") {
    val config = baseResolvedConfig.copy(formatOut = None)
    val components = FilterComponents.fromConfig(config)

    assert(
      components.outputLineFormatter.isInstanceOf[ColorLineFormatter],
      "Should create ColorLineFormatter when formatOut is None (default)"
    )
  }

  test("fromConfig should initialize ParseResultKeys with correct config") {
    val config = baseResolvedConfig.copy(
      fieldNames = FieldNamesConfig(
        timestampFieldName = "custom_ts",
        levelFieldName = "custom_level",
        messageFieldName = "custom_msg",
        stackTraceFieldName = "custom_stack",
        loggerNameFieldName = "custom_logger",
        threadNameFieldName = "custom_thread"
      )
    )
    val components = FilterComponents.fromConfig(config)

    // Verify that ParseResultKeys was created (opaque object, can't test internals)
    assertNotEquals(
      components.parseResultKeys,
      null,
      "ParseResultKeys should be created with custom config"
    )
  }

  test("fromConfig should create components multiple times with same config") {
    val components1 = FilterComponents.fromConfig(baseResolvedConfig)
    val components2 = FilterComponents.fromConfig(baseResolvedConfig)

    // Components should be created each time (not cached)
    assert(
      components1 ne components2,
      "Each call should create new FilterComponents"
    )
  }
}
