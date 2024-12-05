import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.api.L
object EditElement {

  def render(mods: Modifier[TextArea]*): HtmlElement =
    div(
      textArea(
        minHeight := "400px",
        cls := "col-12",
        typ := "text",
        mods
      )
    )
}
