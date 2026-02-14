package ru.d10xa.jsonlogviewer

import com.monovore.decline.Help
import com.raquo.laminar.api.L.*
import com.raquo.waypoint.*
import org.scalajs.dom
import ru.d10xa.jsonlogviewer.decline.DeclineOpts
import ru.d10xa.jsonlogviewer.Router0.*
import ru.d10xa.jsonlogviewer.Router0.navigateTo

object App {

  private val state = AppState.default

  def main(args: Array[String]): Unit = {
    val search = dom.window.location.search
    if (search.nonEmpty) {
      val hash = dom.window.location.hash
      val hashPart = if (hash.isEmpty) "#/" else hash
      dom.window.history.replaceState(
        null,
        "",
        dom.window.location.pathname + hashPart + search
      )
    }
    UrlState.hydrate(state)

    lazy val container = dom.document.getElementById("app-container")

    val selectedAppSignal = SplitRender(Router0.router.currentPageSignal)
      .collectStatic(LivePage)(renderLivePage())
      .collectStatic(EditPage)(renderEditPage())
      .collectStatic(ViewPage)(renderViewPage())
      .collectStatic(HelpPage)(renderHelpPage())
      .signal

    lazy val appElement =
      div(
        cls := "app-container",
        UrlState.persistBinder(state),
        div(
          cls := "nav-bar",
          linkPages.map { page =>
            span(
              cls := "nav-tab",
              a(
                cls <-- Router0.router.currentPageSignal.map {
                  case currentPage if currentPage == page => Seq("active")
                  case _                                  => Seq.empty
                },
                navigateTo(page),
                page.title
              )
            )
          }
        ),
        div(child <-- selectedAppSignal),
        JsonModal.render
      )
    renderOnDomContentLoaded(container, appElement)
  }

  private def controlsBlock: Seq[HtmlElement] = Seq(
    Controls.formatsDiv(state.formatInVar, state.formatOutVar),
    Controls.filterDiv(state.filterVar, state.filterResultSignal),
    Controls.fuzzyIncludeDiv(state.fuzzyIncludeVar),
    Controls.fuzzyExcludeDiv(state.fuzzyExcludeVar),
    Controls.showEmptyFieldsDiv(state.showEmptyFieldsVar),
    Controls.excludeFieldsDiv(state.excludeFieldsVar),
    Controls.fieldNamesDiv(
      state.timestampFieldVar,
      state.levelFieldVar,
      state.messageFieldVar,
      state.stackTraceFieldVar,
      state.loggerNameFieldVar,
      state.threadNameFieldVar
    ),
    Controls.additionalArgsDiv(state.cliVar),
    Controls.buttonGenerateLogs(state.formatInVar, state.textVar),
    Controls.editElementDiv(state.textVar)
  )

  private def renderLivePage(): HtmlElement =
    div(
      controlsBlock,
      Controls.loadingIndicator(state.loadingBus),
      ViewElement.render(
        state.textVar.signal,
        state.configSignal,
        state.uiExtraConfigSignal,
        state.loadingBus
      )
    )

  private def renderEditPage(): HtmlElement =
    div(
      controlsBlock
    )

  private def renderViewPage(): HtmlElement =
    div(
      Controls.loadingIndicator(state.loadingBus),
      ViewElement.render(
        state.textVar.signal,
        state.configSignal,
        state.uiExtraConfigSignal,
        state.loadingBus
      )
    )

  private def renderHelpPage(): HtmlElement =
    pre(
      cls := "help-pre",
      Help.fromCommand(DeclineOpts.command).toString
    )

  val linkPages: List[Page] =
    List(LivePage, EditPage, ViewPage, HelpPage)
}
