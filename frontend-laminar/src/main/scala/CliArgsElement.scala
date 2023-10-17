import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.api.L

object CliArgsElement {
  def render(mods: Modifier[TextArea]*): HtmlElement =
    div(
      textArea(cls := "col-12", typ := "text", mods)
    )
}
