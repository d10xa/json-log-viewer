package ru.d10xa.jsonlogviewer.decline

import cats.effect.std.Supervisor
import cats.effect.IO
import cats.effect.Ref
import cats.effect.Resource
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.file.Watcher

import java.io.File
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYamlReader

class ConfigInitImpl extends ConfigInit {

  override def initConfigYaml(
    c: Config,
    supervisor: Supervisor[IO]
  ): Resource[IO, Ref[IO, Option[ConfigYaml]]] = {
    val configFileOpt: Option[Path] =
      c.configFile
        .map(file => Path(file.file))
        .orElse(findDefaultFile())
    loadConfigRef(configFileOpt, supervisor)
  }

  private def findDefaultFile(): Option[Path] =
    findConfigFile("json-log-viewer", List("yml", "yaml", "YML", "YAML"))
      .map(Path.apply)

  private def loadConfigRef(
    configFileOpt: Option[Path],
    supervisor: Supervisor[IO]
  ): Resource[IO, Ref[IO, Option[ConfigYaml]]] =
    for {
      updatedConfig <- Resource.eval {
        configFileOpt match {
          case Some(filePath) => readConfig(filePath)
          case None           => IO.pure(None)
        }
      }
      configRef <- Resource.eval(Ref.of[IO, Option[ConfigYaml]](updatedConfig))
      _ <- configFileOpt match {
        case Some(filePath) =>
          watchFileForChanges(
            absolutePath = filePath.absolute,
            configRef = configRef,
            supervisor = supervisor
          )
        case None => Resource.unit[IO]
      }
    } yield configRef

  private def watchFileForChanges(
    absolutePath: Path,
    configRef: Ref[IO, Option[ConfigYaml]],
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
            updatedConfig <- readConfig(absolutePath)
            _ <- configRef.set(updatedConfig)
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
        IO.println(s"Failed to parse config: ${errors.toList.mkString(", ")}")
          .as(None)
    }

  private def findConfigFile(
    baseName: String,
    extensions: List[String]
  ): Option[String] =
    extensions.collectFirst {
      case ext if new File(s"$baseName.$ext").exists() =>
        s"$baseName.$ext"
    }

}
