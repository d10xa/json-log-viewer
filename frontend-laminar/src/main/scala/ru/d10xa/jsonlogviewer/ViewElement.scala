package ru.d10xa.jsonlogviewer

import cats.effect.unsafe.implicits.global
import cats.effect.IO
import cats.effect.Ref
import com.monovore.decline.Help
import com.raquo.airstream.core.Signal
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.ownership.Owner
import com.raquo.laminar.api.L.*
import com.raquo.laminar.DomApi
import ru.d10xa.jsonlogviewer.cache.CachedResolvedState
import ru.d10xa.jsonlogviewer.cache.FilterCacheManager
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.Feed
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.shell.ShellImpl
import scala.util.chaining.*

object ViewElement {

  def stringsToHtmlElement(strings: List[String]): HtmlElement =
    strings
      .map(Ansi2HtmlWithClasses.apply)
      .mkString("<div>", "", "</div>")
      .pipe(DomApi.unsafeParseHtmlString)
      .pipe(foreignHtmlElement)

  def makeConfigYamlForInlineInput(
    string: String,
    config: Config,
    extra: UiExtraConfig
  ): ConfigYaml =
    ConfigYaml(
      showEmptyFields = Some(extra.showEmptyFields),
      fieldNames = extra.fieldNames,
      feeds = Some(
        List(
          Feed(
            name = None,
            commands = List.empty,
            inlineInput = Some(string),
            filter = config.filter,
            formatIn = config.formatIn,
            rawInclude = None,
            rawExclude = None,
            fuzzyInclude = extra.fuzzyInclude,
            fuzzyExclude = extra.fuzzyExclude,
            excludeFields = extra.excludeFields,
            fieldNames = extra.fieldNames,
            showEmptyFields = Some(extra.showEmptyFields),
            restart = None,
            restartDelayMs = None,
            maxRestarts = None
          )
        )
      )
    )

  def render(
    logLinesSignal: Signal[String],
    configSignal: Signal[Either[Help, Config]],
    uiExtraSignal: Signal[UiExtraConfig]
  )(implicit owner: Owner): HtmlElement = {
    val eventBus = new EventBus[HtmlElement]
    logLinesSignal
      .combineWith(configSignal, uiExtraSignal)
      .foreach {
        case (string, Right(c), extra) =>
          val configYaml =
            Some(makeConfigYamlForInlineInput(string, c, extra))
          val initialCache = FilterCacheManager.buildCache(c, configYaml)
          val refsIO = for {
            configYamlRef <- Ref.of[IO, Option[ConfigYaml]](configYaml)
            cacheRef <- Ref.of[IO, CachedResolvedState](initialCache)
          } yield (configYamlRef, cacheRef)

          fs2.Stream
            .eval(refsIO)
            .flatMap { case (configYamlRef, cacheRef) =>
              val ctx = StreamContext(
                config = c,
                configYamlRef = configYamlRef,
                cacheRef = cacheRef,
                stdinStream = new StdInLinesStreamImpl,
                shell = new ShellImpl
              )
              LogViewerStream.stream(ctx)
            }
            .compile
            .toList
            .map(stringsToHtmlElement)
            .flatMap(e => IO(eventBus.writer.onNext(e)))
            .unsafeRunAndForget()

        case (_, Left(help), _) =>
          eventBus.writer.onNext(pre(cls := "text-light", help.toString))
      }(owner)

    pre(
      cls := "bg-dark font-monospace text-white",
      child <-- eventBus.events
    )
  }
}
