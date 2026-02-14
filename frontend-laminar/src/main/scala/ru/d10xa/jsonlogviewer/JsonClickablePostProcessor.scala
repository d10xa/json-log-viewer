package ru.d10xa.jsonlogviewer

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.NodeFilter

object JsonClickablePostProcessor {

  def process(element: dom.Element, onClick: String => Unit): Unit = {
    val walker = document.createTreeWalker(
      element,
      NodeFilter.SHOW_TEXT,
      null,
      entityReferenceExpansion = false
    )

    val textNodes = scala.collection.mutable.ArrayBuffer[dom.Text]()
    var node = walker.nextNode()
    while (node != null) {
      textNodes += node.asInstanceOf[dom.Text]
      node = walker.nextNode()
    }

    textNodes.foreach { textNode =>
      val text = textNode.textContent
      val fragments = EmbeddedJsonDetector.detect(text)
      if (fragments.nonEmpty) {
        replaceTextNode(textNode, text, fragments, onClick)
      }
    }
  }

  private def replaceTextNode(
    textNode: dom.Text,
    text: String,
    fragments: List[JsonFragment],
    onClick: String => Unit
  ): Unit = {
    val frag = document.createDocumentFragment()
    var pos = 0

    fragments.foreach { jf =>
      if (jf.startIndex > pos) {
        frag.appendChild(
          document.createTextNode(text.substring(pos, jf.startIndex))
        )
      }

      val span = document.createElement("span")
      span.classList.add("json-clickable")
      span.textContent = text.substring(jf.startIndex, jf.endIndex)
      span.addEventListener(
        "click",
        (_: dom.MouseEvent) => onClick(jf.normalizedJson)
      )
      frag.appendChild(span)

      pos = jf.endIndex
    }

    if (pos < text.length) {
      frag.appendChild(document.createTextNode(text.substring(pos)))
    }

    textNode.parentNode.replaceChild(frag, textNode)
  }
}
