import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.api.L

object CliArgsElement {
  def render(mods: Modifier[TextArea]*): HtmlElement =
    div(
      cls := "cli-args",
      textArea(typ := "text", mods)
    )
}
