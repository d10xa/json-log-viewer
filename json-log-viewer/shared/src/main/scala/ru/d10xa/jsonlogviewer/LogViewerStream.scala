package ru.d10xa.jsonlogviewer

import cats.effect.IO
import cats.effect.Ref
import fs2.*
import ru.d10xa.jsonlogviewer.cache.CachedFilterSet
import ru.d10xa.jsonlogviewer.cache.CachedResolvedState
import ru.d10xa.jsonlogviewer.cache.FilterCacheManager
import ru.d10xa.jsonlogviewer.config.ConfigResolver
import ru.d10xa.jsonlogviewer.config.ResolvedConfig
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.restart.RestartState
import ru.d10xa.jsonlogviewer.restart.RestartableStreamWrapper
import ru.d10xa.jsonlogviewer.shell.RestartConfig
import ru.d10xa.jsonlogviewer.shell.Shell

object LogViewerStream {

  private def getOrUpdateCache(
    cacheRef: Ref[IO, CachedResolvedState],
    config: Config,
    currentConfigYaml: Option[ConfigYaml]
  ): IO[CachedResolvedState] =
    cacheRef.get.flatMap { currentCache =>
      val (newCache, wasUpdated) = FilterCacheManager.updateCacheIfNeeded(
        Some(currentCache),
        config,
        currentConfigYaml
      )
      if (wasUpdated) cacheRef.set(newCache).as(newCache)
      else IO.pure(currentCache)
    }

  private def findCachedFilterSet(
    cache: CachedResolvedState,
    feedName: Option[String]
  ): Option[CachedFilterSet] =
    cache.filterSets.find(_.resolvedConfig.feedName == feedName)

  private def getInputStream(
    resolvedConfig: ResolvedConfig,
    stdinStream: StdInLinesStream,
    shell: Shell,
    restartState: Option[RestartState]
  ): Stream[IO, String] =
    if (
      resolvedConfig.inlineInput.isDefined || resolvedConfig.commands.nonEmpty
    ) {
      restartState.filter(_ => resolvedConfig.restart) match {
        case Some(state) =>
          val restartConfig = RestartConfig(
            enabled = true,
            delayMs = resolvedConfig.restartDelayMs,
            maxRestarts = resolvedConfig.maxRestarts
          )
          shell.mergeCommandsAndInlineInputWithRestart(
            resolvedConfig.commands,
            resolvedConfig.inlineInput,
            restartConfig,
            RestartableStreamWrapper.createOnRestartCallback(state.isRestartRef)
          )
        case None =>
          shell.mergeCommandsAndInlineInput(
            resolvedConfig.commands,
            resolvedConfig.inlineInput
          )
      }
    } else {
      stdinStream.stdinLinesStream
    }

  private def getCachedFilterSet(
    configYamlRef: Ref[IO, Option[ConfigYaml]],
    cacheRef: Ref[IO, CachedResolvedState],
    config: Config,
    feedName: Option[String],
    fallback: CachedFilterSet
  ): IO[CachedFilterSet] =
    for {
      currentConfigYaml <- configYamlRef.get
      cache <- getOrUpdateCache(cacheRef, config, currentConfigYaml)
    } yield findCachedFilterSet(cache, feedName).getOrElse(fallback)

  def stream(ctx: StreamContext): Stream[IO, String] = {
    import ctx.*

    def processStreamWithCache(
      inputStream: Stream[IO, String],
      initialCachedFilterSet: CachedFilterSet,
      restartState: Option[RestartState]
    ): Stream[IO, String] = {
      val feedName = initialCachedFilterSet.resolvedConfig.feedName

      if (
        initialCachedFilterSet.resolvedConfig.formatIn.contains(FormatIn.Csv)
      ) {
        val csvContext = CsvProcessingContext(
          initialFilterSet = initialCachedFilterSet,
          feedName = feedName,
          getCachedFilterSet = getCachedFilterSet(
            configYamlRef,
            cacheRef,
            config,
            feedName,
            initialCachedFilterSet
          ),
          restartState = restartState
        )
        CsvStreamProcessor.process(inputStream, csvContext)
      } else {
        inputStream.flatMap { line =>
          Stream
            .eval(
              getCachedFilterSet(
                configYamlRef,
                cacheRef,
                config,
                feedName,
                initialCachedFilterSet
              )
            )
            .flatMap { filterSet =>
              processLineWithCachedFilterSet(
                line,
                filterSet,
                restartState
              )
            }
        }
      }
    }

    def createFeedStream(filterSet: CachedFilterSet): Stream[IO, String] = {
      val resolvedConfig = filterSet.resolvedConfig
      if (resolvedConfig.restart) {
        Stream.eval(RestartState.create).flatMap { restartState =>
          val feedStream = getInputStream(
            resolvedConfig,
            stdinStream,
            shell,
            Some(restartState)
          )
          processStreamWithCache(feedStream, filterSet, Some(restartState))
        }
      } else {
        val feedStream =
          getInputStream(resolvedConfig, stdinStream, shell, None)
        processStreamWithCache(feedStream, filterSet, None)
      }
    }

    Stream.eval(cacheRef.get).flatMap { initialCache =>
      val filterSets = initialCache.filterSets

      val finalStream = if (filterSets.isEmpty) {
        Stream.empty
      } else if (filterSets.length > 1) {
        val feedStreams = filterSets.map(createFeedStream)
        Stream.emits(feedStreams).parJoin(feedStreams.size)
      } else {
        createFeedStream(filterSets.head)
      }

      finalStream.intersperse("\n").append(Stream.emit("\n"))
    }
  }

  def processLineWithRef(
    line: String,
    configYamlRef: Ref[IO, Option[ConfigYaml]],
    config: Config
  ): Stream[IO, String] =
    Stream.eval(configYamlRef.get).flatMap { configYaml =>
      processLineWithConfig(line, configYaml, config)
    }

  def processLineWithConfig(
    line: String,
    configYaml: Option[ConfigYaml],
    config: Config
  ): Stream[IO, String] = {
    val resolvedConfigs = ConfigResolver.resolve(config, configYaml)

    if (resolvedConfigs.isEmpty) {
      Stream.empty
    } else if (resolvedConfigs.length > 1) {
      val results = resolvedConfigs.map { resolvedConfig =>
        processLineWithResolvedConfig(line, resolvedConfig)
      }
      Stream.emits(results).parJoinUnbounded
    } else {
      processLineWithResolvedConfig(line, resolvedConfigs.head)
    }
  }

  def processLineWithResolvedConfig(
    line: String,
    resolvedConfig: ResolvedConfig
  ): Stream[IO, String] = {
    val getParser: IO[LogLineParser] =
      if (resolvedConfig.formatIn.contains(FormatIn.Csv)) {
        IO.raiseError(
          new IllegalStateException(
            "CSV format requires header line, cannot process a single line"
          )
        )
      } else {
        IO.pure(LogLineParserFactory.createNonCsvParser(resolvedConfig))
      }

    Stream.eval(getParser).flatMap { parser =>
      val components = FilterComponents.fromConfig(resolvedConfig)

      FilterPipeline.applyFilters(
        Stream.emit(line),
        parser,
        components,
        resolvedConfig
      )
    }
  }

  def processLineWithCachedFilterSet(
    line: String,
    filterSet: CachedFilterSet,
    restartState: Option[RestartState] = None
  ): Stream[IO, String] =
    filterSet.parser match {
      case Some(parser) =>
        FilterPipeline.applyFiltersWithRestartState(
          Stream.emit(line),
          parser,
          filterSet.components,
          filterSet.resolvedConfig,
          restartState
        )
      case None =>
        // CSV: parser=None, will throw error (CSV needs header for parser creation)
        processLineWithResolvedConfig(line, filterSet.resolvedConfig)
    }
}
