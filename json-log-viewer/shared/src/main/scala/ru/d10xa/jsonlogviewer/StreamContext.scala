package ru.d10xa.jsonlogviewer

import cats.effect.IO
import cats.effect.Ref
import ru.d10xa.jsonlogviewer.cache.CachedResolvedState
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.shell.Shell

/** Bundles common streaming context parameters to simplify method signatures.
  */
final case class StreamContext(
  config: Config,
  configYamlRef: Ref[IO, Option[ConfigYaml]],
  cacheRef: Ref[IO, CachedResolvedState],
  stdinStream: StdInLinesStream,
  shell: Shell
)
