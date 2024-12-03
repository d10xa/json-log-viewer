package ru.d10xa.jsonlogviewer.decline

import cats.effect.IO

trait ConfigInit {
  def initConfig(c: Config): IO[Config]
}
