package ru.d10xa.jsonlogviewer

import cats.effect.unsafe.implicits.global
import cats.effect.FiberIO
import cats.effect.IO
import cats.effect.Ref
import com.monovore.decline.Help
import com.raquo.airstream.core.Signal
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.L.*
import com.raquo.laminar.DomApi
import org.scalajs.dom
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
    uiExtraSignal: Signal[UiExtraConfig],
    loadingBus: EventBus[Boolean]
  ): HtmlElement = {
    val contentBus = new EventBus[HtmlElement]
    val plainTextVar = Var("")
    val fiberRef = Ref.unsafe[IO, Option[FiberIO[Unit]]](None)

    val debouncedSignal =
      logLinesSignal
        .combineWith(configSignal, uiExtraSignal)
        .composeChanges(_.debounce(300))

    div(
      styleAttr := "position: relative;",
      div(
        styleAttr := "position: absolute; top: 4px; right: 4px; z-index: 10;",
        child <-- plainTextVar.signal.map { text =>
          if text.nonEmpty then
            button(
              cls := "term-btn",
              "Copy",
              onClick --> { _ =>
                dom.window.navigator.clipboard.writeText(text)
              }
            )
          else emptyNode
        }
      ),
      pre(
        cls := "output-pre",
        child <-- contentBus.events,
        debouncedSignal --> Observer[
          (String, Either[Help, Config], UiExtraConfig)
        ] {
          case (string, Right(c), extra) =>
            val task = for {
              _ <- IO(loadingBus.writer.onNext(true))
              configYaml = Some(
                makeConfigYamlForInlineInput(string, c, extra)
              )
              initialCache = FilterCacheManager.buildCache(c, configYaml)
              result <- (for {
                configYamlRef <- Ref.of[IO, Option[ConfigYaml]](configYaml)
                cacheRef <- Ref.of[IO, CachedResolvedState](initialCache)
              } yield (configYamlRef, cacheRef)).flatMap {
                case (configYamlRef, cacheRef) =>
                  val ctx = StreamContext(
                    config = c,
                    configYamlRef = configYamlRef,
                    cacheRef = cacheRef,
                    stdinStream = new StdInLinesStreamImpl,
                    shell = new ShellImpl
                  )
                  LogViewerStream.stream(ctx).compile.toList
              }
              _ <- IO {
                contentBus.writer.onNext(stringsToHtmlElement(result))
                plainTextVar.set(result.mkString("\n"))
              }
              _ <- IO(loadingBus.writer.onNext(false))
            } yield ()

            val managed = for {
              prev <- fiberRef.get
              _ <- prev.fold(IO.unit)(_.cancel)
              fiber <- task.start
              _ <- fiberRef.set(Some(fiber))
            } yield ()

            managed.unsafeRunAndForget()

          case (_, Left(help), _) =>
            loadingBus.writer.onNext(false)
            plainTextVar.set("")
            contentBus.writer.onNext(pre(cls := "help-text", help.toString))
        }
      )
    )
  }
}
