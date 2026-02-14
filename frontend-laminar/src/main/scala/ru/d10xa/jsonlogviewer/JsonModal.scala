package ru.d10xa.jsonlogviewer

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import scala.scalajs.js

object JsonModal {

  private val jsonVar: Var[Option[String]] = Var(None)

  def show(json: String): Unit =
    jsonVar.set(Some(json))

  def hide(): Unit =
    jsonVar.set(None)

  private def prettyPrint(json: String): String =
    try {
      val parsed = js.JSON.parse(json)
      js.JSON.stringify(parsed, null.asInstanceOf[js.Array[js.Any]], 2)
    } catch {
      case _: Throwable => json
    }

  def render: HtmlElement =
    div(
      cls := "json-modal-overlay",
      display <-- jsonVar.signal.map {
        case Some(_) => "flex"
        case None    => "none"
      },
      onClick --> { ev =>
        if (
          ev.target
            .asInstanceOf[dom.Element]
            .classList
            .contains("json-modal-overlay")
        )
          hide()
      },
      documentEvents(_.onKeyDown)
        .filter(_.key == "Escape") --> { _ => hide() },
      div(
        cls := "json-modal-content",
        div(
          cls := "json-modal-header",
          span("JSON"),
          div(
            cls := "json-modal-actions",
            button(
              cls := "term-btn json-modal-copy",
              "Copy",
              onClick --> { _ =>
                jsonVar.now().foreach { json =>
                  dom.window.navigator.clipboard.writeText(prettyPrint(json))
                }
              }
            ),
            button(
              cls := "term-btn json-modal-close",
              "Close",
              onClick --> { _ => hide() }
            )
          )
        ),
        pre(
          cls := "json-modal-body",
          child.text <-- jsonVar.signal.map {
            case Some(json) => prettyPrint(json)
            case None       => ""
          }
        )
      )
    )
}
