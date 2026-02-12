package ru.d10xa.jsonlogviewer

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
object EditElement {

  def render(mods: Modifier[TextArea]*): HtmlElement =
    div(
      textArea(
        minHeight := "320px",
        typ := "text",
        mods
      )
    )
}
