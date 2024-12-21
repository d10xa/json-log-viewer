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
    getPageTitle = _.title, // displayed in the browser tab next to favicon
    serializePage = page =>
      writeToString(
        page
      ), // serialize page data for storage in History API log
    deserializePage = pageStr =>
      readFromString(pageStr) // deserialize the above
  )(
    popStateEvents = windowEvents(
      _.onPopState
    ), // this is how Waypoint avoids an explicit dependency on Laminar
    owner = unsafeWindowOwner // this router will live as long as the window
  )

  // Note: for fragment ('#') URLs this isn't actually needed.
  // See https://github.com/raquo/Waypoint docs for why this modifier is useful in general.
  def navigateTo(page: Page): Binder[HtmlElement] = Binder { el =>

    val isLinkElement = el.ref.isInstanceOf[dom.html.Anchor]

    if (isLinkElement) {
      el.amend(href(router.absoluteUrlForPage(page)))
    }

    // If element is a link and user is holding a modifier while clicking:
    //  - Do nothing, browser will open the URL in new tab / window / etc. depending on the modifier key
    // Otherwise:
    //  - Perform regular pushState transition
    (onClick
      .filter(ev =>
        !(isLinkElement && (ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey))
      )
      .preventDefault
      --> (_ => router.pushState(page))).bind(el)
  }
}
