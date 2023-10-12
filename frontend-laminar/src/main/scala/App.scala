import Router0.{*, given}
import com.monovore.decline.Help
import com.raquo.laminar.DomApi
import fansi.ErrorMode
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.api.L
import com.raquo.waypoint.*
import org.scalajs.dom
import ru.d10xa.jsonlogviewer.Config
import ru.d10xa.jsonlogviewer.DeclineOpts

object App {
  val textVar: Var[String] = Var("""
      |{"@timestamp":"2023-09-18T19:10:42.132318Z","message":"first line","logger_name":"MakeLogs","thread_name":"main","level":"INFO"}
      |{"@timestamp":"2023-09-18T19:11:42.132318Z","message":"test","logger_name":"MakeLogs","thread_name":"main","level":"INFO"}
      |{"@timestamp":"2023-09-18T19:11:42.132319Z","message":"debug msg","logger_name":"MakeLogs","thread_name":"main","level":"DEBUG"}
      |prefix before json {"@timestamp":"2023-09-18T19:11:42.132320Z","message":"warning msg","logger_name":"MakeLogs","thread_name":"main","level":"WARNING"}
      |{"@timestamp":"2023-09-18T19:11:42.132321Z","message":"warn msg","logger_name":"MakeLogs","thread_name":"main","level":"WARN"}
      |{"@timestamp":"2023-09-18T19:12:42.137207Z","message":"error message","logger_name":"MakeLogs","thread_name":"main","level":"ERROR","stack_trace":"java.lang.RuntimeException: java.lang.IllegalArgumentException: java.lang.ArithmeticException: hello\n\tat ru.d10xa.jsonlogviewer.MakeLogs$.main(MakeLogs.scala:9)\n\tat ru.d10xa.jsonlogviewer.MakeLogs.main(MakeLogs.scala)\nCaused by: java.lang.IllegalArgumentException: java.lang.ArithmeticException: hello\n\t... 2 common frames omitted\nCaused by: java.lang.ArithmeticException: hello\n\t... 2 common frames omitted\n"}
      |{"@timestamp":"2023-09-18T19:14:42.137207Z","message":"last line","logger_name":"MakeLogs","thread_name":"main","level":"INFO"}
      |
      |""".stripMargin)

  val cliVar: Var[String] = Var("""--timestamp-after 2023-09-18T19:10:42.132318Z --grep level:INFO --grep level:.+ING --grep level:ERR.+""")

  val splitPattern = "([^\"]\\S*|\".+?\")\\s*".r
  def splitArgs(s: String): Seq[String] =
    splitPattern
      .findAllMatchIn(s)
      .map(_.group(1).replace("\"", ""))
      .toSeq

  val configSignal: Signal[Either[Help, Config]] =
    cliVar.signal.map(s => DeclineOpts.command.parse(splitArgs(s)))

  def main(args: Array[String]): Unit = {
    lazy val container = dom.document.getElementById("app-container")

    lazy val appElement = {
      div(
        div(
          fontSize := "120%",
          lineHeight := "2em",
          listStyleType.none,
          paddingLeft := "20px",
          linkPages.map { page =>
            a(
              margin.px := 10,
              navigateTo(page),
              page.title
            )
          }
        ),
        div(
          cls := "log-page-container",
          child <-- selectedAppSignal
        )
      )
    }
    renderOnDomContentLoaded(container, appElement)
  }

  private val selectedAppSignal =
    SplitRender(Router0.router.currentPageSignal)
      .collectStatic(LivePage)(renderLivePage())
      .collectStatic(EditPage)(renderEditPage())
      .collectStatic(ViewPage)(renderViewPage())
      .signal

  private def renderLivePage(): HtmlElement = {
    div(
      CliArgsElement.render(
        value <-- cliVar,
        onInput.mapToValue --> cliVar
      ),
      EditElement.render(
        value <-- textVar,
        onInput.mapToValue --> textVar
      ),
      ViewElement.render(textVar.signal, configSignal)
    )
  }

  private def renderViewPage(): HtmlElement = {
    div(
      ViewElement.render(textVar.signal, configSignal)
    )
  }

  private def renderEditPage(): HtmlElement = {
    div(
      CliArgsElement.render(
        value <-- cliVar,
        onInput.mapToValue --> cliVar
      ),
      EditElement.render(
        value <-- textVar,
        onInput.mapToValue --> textVar
      )
    )
  }

  val linkPages: List[Page] = List(
    LivePage,
    EditPage,
    ViewPage
  )
}
