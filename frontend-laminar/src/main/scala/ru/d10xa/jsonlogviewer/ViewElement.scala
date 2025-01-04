package ru.d10xa.jsonlogviewer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.monovore.decline.Help
import com.raquo.airstream.core.Signal
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.ownership.Owner
import com.raquo.laminar.DomApi
import com.raquo.laminar.api.L.*
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.Feed


object ViewElement {

  def render(
    logLinesSignal: Signal[String],
    configSignal: Signal[Either[Help, Config]]
  )(implicit owner: Owner): HtmlElement = {
    val eventBus = new EventBus[HtmlElement]
    logLinesSignal
      .combineWith(configSignal)
      .foreach {
        case (string, Right(c)) =>
          val c2 = c.copy(configYaml =
            Some(
              c.configYaml.getOrElse(
                ConfigYaml(
                  filter = None,
                  formatIn = None,
                  commands = None,
                  feeds = Some(
                    List(
                      Feed(
                        name = None,
                        commands = List.empty,
                        inlineInput = Some(string),
                        filter = c.filter,
                        formatIn = c.formatIn
                      )
                    )
                  )
                )
              )
            )
          )
          LogViewerStream
            .stream(c2)
            .compile
            .toList
            .flatMap { strings =>
              val base = foreignHtmlElement(DomApi.unsafeParseHtmlString(strings
                .map(Ansi2HtmlWithClasses.apply)
                .mkString("<div>", "", "</div>")))
              IO(eventBus.writer.onNext(base))
            }
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
