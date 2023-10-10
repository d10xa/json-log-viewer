
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.api.L
object EditElement {

  def render(mods: Modifier[TextArea]*): HtmlElement =
    div(
      cls := "log-edit",
      textArea(typ := "text", mods)
    )
}
