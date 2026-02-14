package ru.d10xa.jsonlogviewer

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import org.scalajs.dom

object EditElement {

  def render(mods: Modifier[TextArea]*): HtmlElement =
    div(
      textArea(
        typ := "text",
        mods
      )
    )

  def renderWithDrop(
    textVar: Var[String],
    mods: Modifier[TextArea]*
  ): HtmlElement =
    div(
      textArea(
        typ := "text",
        value <-- textVar,
        onInput.mapToValue --> textVar,
        onDragOver.preventDefault --> Observer.empty,
        onDrop.preventDefault --> { ev =>
          val dt = ev.dataTransfer
          if dt.files.length > 0 then
            val file = dt.files(0)
            val reader = new dom.FileReader()
            reader.onload = { _ =>
              val content = reader.result.asInstanceOf[String]
              textVar.set(content)
            }
            reader.readAsText(file)
        },
        mods
      )
    )
}
