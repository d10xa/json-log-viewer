package ru.d10xa.jsonlogviewer

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import com.raquo.laminar.api.L.*
import com.raquo.waypoint.*
import org.scalajs.dom

object Router0 {

  sealed abstract class Page(val title: String)

  case object LivePage extends Page("Live")
  case object EditPage extends Page("Edit")
  case object ViewPage extends Page("View")
  case object HelpPage extends Page("Help")

  given codec: JsonValueCodec[Page] = JsonCodecMaker.make

  private val routes = List(
    Route.static(
      LivePage,
      root / endOfSegments,
      dom.window.location.pathname + "#"
    ),
    Route.static(
      EditPage,
      root / "edit" / endOfSegments,
      dom.window.location.pathname + "#"
    ),
    Route.static(
      ViewPage,
      root / "view" / endOfSegments,
      dom.window.location.pathname + "#"
    ),
    Route.static(
      HelpPage,
      root / "help" / endOfSegments,
      dom.window.location.pathname + "#"
    )
  )

  val router = new Router[Page](
    routes = routes,
    getPageTitle = _.title,
    serializePage = page => writeToString(page),
    deserializePage = pageStr => readFromString(pageStr)
  )(
    popStateEvents = windowEvents(_.onPopState),
    owner = unsafeWindowOwner
  )

  def navigateTo(page: Page): Binder[HtmlElement] = Binder { el =>

    val isLinkElement = el.ref.isInstanceOf[dom.html.Anchor]

    if (isLinkElement) {
      el.amend(href(router.absoluteUrlForPage(page)))
    }

    (onClick
      .filter(ev =>
        !(isLinkElement && (ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey))
      )
      .preventDefault
      --> (_ => router.pushState(page))).bind(el)
  }
}
