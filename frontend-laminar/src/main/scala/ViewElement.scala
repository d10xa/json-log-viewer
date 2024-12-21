import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.monovore.decline.Help
import com.raquo.airstream.core.Signal
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.ownership.Owner
import com.raquo.laminar.DomApi
import com.raquo.laminar.api.L.*
import ru.d10xa.jsonlogviewer.LogViewerStream
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.Feed

import scala.concurrent.ExecutionContext.Implicits.global

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
          c.copy(configYaml =
            Some(c.configYaml.getOrElse(
              ConfigYaml(
                filter = None,
                formatIn = None,
                commands = None,
                feeds = Some(
                  List(
                    Feed(
                      name = "inlineInput",
                      commands = List.empty,
                      inlineInput = Some(string),
                      filter = c.filter,
                      formatIn = c.formatIn
                    )
                  )
                )
              )
            ))
          )
          val stream: fs2.Stream[IO, HtmlElement] = LogViewerStream
            .stream(c)
            .map(Ansi2HtmlWithClasses.apply)
            .map(_.mkString("<div>", "", "</div>"))
            .map(DomApi.unsafeParseHtmlString)
            .map(foreignHtmlElement)

          stream.compile.lastOrError
            .unsafeToFuture()
            .foreach(eventBus.writer.onNext)

        case (_, Left(help)) =>
          eventBus.writer.onNext(pre(cls := "text-light", help.toString))
      }(owner)

    pre(
      cls := "bg-dark font-monospace text-white",
      child <-- eventBus.events
    )
  }
}
