package ru.d10xa.jsonlogviewer.decline

import cats.effect.IO
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn

class ConfigInitImpl extends ConfigInit {

  override def initConfig(c: Config): IO[Config] = {
    IO.pure(
      c.copy(
        formatIn = c.formatIn.orElse(Some(FormatIn.Json)),
        filter = c.filter
      )
    )
  }
}
