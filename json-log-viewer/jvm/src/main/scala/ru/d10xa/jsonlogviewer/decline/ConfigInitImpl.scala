package ru.d10xa.jsonlogviewer.decline

import cats.effect.std.Supervisor
import cats.effect.IO
import cats.effect.Ref
import cats.effect.Resource
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.file.Watcher
import ru.d10xa.jsonlogviewer.cache.CachedResolvedState
import ru.d10xa.jsonlogviewer.cache.FilterCacheManager
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYamlReader

class ConfigInitImpl extends ConfigInit {

  private def printError(message: String): IO[Unit] =
    fs2.Stream
      .emit(s"\n${fansi.Color.Red(message).render}\n")
      .through(fs2.text.utf8.encode)
      .through(fs2.io.stderr[IO])
      .compile
      .drain

  override def initConfigRefs(
    c: Config,
    supervisor: Supervisor[IO]
  ): Resource[IO, ConfigRefs] = {
    val configFileOpt: Option[Path] =
      c.configFile
        .map(file => Path(file.file))
    loadConfigRefs(c, configFileOpt, supervisor)
  }

  private def loadConfigRefs(
    config: Config,
    configFileOpt: Option[Path],
    supervisor: Supervisor[IO]
  ): Resource[IO, ConfigRefs] =
    for {
      initialConfigYaml <- Resource.eval {
        configFileOpt match {
          case Some(filePath) => readConfig(filePath)
          case None           => IO.pure(None)
        }
      }
      configYamlRef <- Resource.eval(
        Ref.of[IO, Option[ConfigYaml]](initialConfigYaml)
      )
      initialCache <- Resource.eval {
        FilterCacheManager.buildCache(config, initialConfigYaml) match {
          case Right(cache) => IO.pure(cache)
          case Left(errorMessage) =>
            printError(s"[CONFIG ERROR] $errorMessage").as(
              FilterCacheManager
                .buildCache(config, None)
                .getOrElse(CachedResolvedState.noFilters(config, None))
            )
        }
      }
      cacheRef <- Resource.eval(Ref.of[IO, CachedResolvedState](initialCache))
      _ <- configFileOpt match {
        case Some(filePath) =>
          watchFileForChangesWithCache(
            absolutePath = filePath.absolute,
            config = config,
            configYamlRef = configYamlRef,
            cacheRef = cacheRef,
            supervisor = supervisor
          )
        case None => Resource.unit[IO]
      }
    } yield ConfigRefs(configYamlRef, cacheRef)

  private def watchFileForChangesWithCache(
    absolutePath: Path,
    config: Config,
    configYamlRef: Ref[IO, Option[ConfigYaml]],
    cacheRef: Ref[IO, CachedResolvedState],
    supervisor: Supervisor[IO]
  ): Resource[IO, Unit] =
    val watch: IO[Unit] = Files[IO]
      .watch(absolutePath)
      .handleErrorWith { e =>
        fs2.Stream.eval(IO.println(s"Watcher error: ${e.getMessage}"))
      }
      .evalMap {
        case Watcher.Event.Modified(_, _) | Watcher.Event.Created(_, _) =>
          for {
            updatedConfigYaml <- readConfig(absolutePath)
            _ <- updatedConfigYaml match {
              case Some(yaml) =>
                FilterCacheManager.buildCache(config, Some(yaml)) match {
                  case Right(newCache) =>
                    configYamlRef.set(Some(yaml)) >> cacheRef.set(newCache)
                  case Left(errorMessage) =>
                    printError(
                      s"[CONFIG ERROR] $errorMessage. Keeping previous configuration."
                    )
                }
              case None =>
                IO.unit
            }
          } yield ()
        case _ => IO.unit
      }
      .compile
      .drain
    Resource.eval(supervisor.supervise(watch).void)

  private def readConfig(filePath: Path): IO[Option[ConfigYaml]] =
    ConfigYamlReader.fromYamlFile(filePath.toString).flatMap {
      case cats.data.Validated.Valid(configYaml) =>
        IO.pure(Some(configYaml))
      case cats.data.Validated.Invalid(errors) =>
        printError(
          s"[CONFIG ERROR] Failed to parse config: ${errors.toList.mkString(", ")}. Keeping previous configuration."
        ).as(None)
    }

}
