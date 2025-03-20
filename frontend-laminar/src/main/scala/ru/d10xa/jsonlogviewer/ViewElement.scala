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
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.Feed
import ru.d10xa.jsonlogviewer.decline.Config

import scala.util.chaining.*

object ViewElement {

  def stringsToHtmlElement(strings: List[String]): HtmlElement =
    strings
      .map(Ansi2HtmlWithClasses.apply)
      .mkString("<div>", "", "</div>")
      .pipe(DomApi.unsafeParseHtmlString)
      .pipe(foreignHtmlElement)

  def makeConfigYamlForInlineInput(string: String, config: Config): ConfigYaml =
    ConfigYaml(
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
            excludeFields = None,
            fieldNames = None
          )
        )
      )
    )

  def render(
    logLinesSignal: Signal[String],
    configSignal: Signal[Either[Help, Config]]
  )(implicit owner: Owner): HtmlElement = {
    val eventBus = new EventBus[HtmlElement]
    logLinesSignal
      .combineWith(configSignal)
      .foreach {
        case (string, Right(c)) =>
          val configYamlRefIO =
            Ref.of[IO, Option[ConfigYaml]](
              Some(makeConfigYamlForInlineInput(string, c))
            )
          fs2.Stream
            .eval(configYamlRefIO)
            .flatMap(configYamlRef => LogViewerStream.stream(c, configYamlRef))
            .compile
            .toList
            .map(stringsToHtmlElement)
            .flatMap(e => IO(eventBus.writer.onNext(e)))
            .unsafeRunAndForget()

        case (_, Left(help)) =>
          eventBus.writer.onNext(pre(cls := "text-light", help.toString))
      }(owner)

    pre(
      cls := "bg-dark font-monospace text-white",
      child <-- eventBus.events
    )
  }
}
