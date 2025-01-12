package ru.d10xa.jsonlogviewer.decline

import cats.effect.std.Supervisor
import cats.effect.IO
import cats.effect.Ref
import cats.effect.Resource
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.file.Watcher
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYamlReader

import java.io.File

class ConfigInitImpl extends ConfigInit {

  override def initConfigYaml(
    c: Config
  ): Resource[IO, Ref[IO, Option[ConfigYaml]]] = {
    val configFileOpt: Option[Path] =
      c.configFile.map(file => Path(file.file)).orElse {
        findConfigFile("json-log-viewer", List("yml", "yaml", "YML", "YAML"))
          .map(Path.apply)
      }
    loadConfigRef(configFileOpt)
  }

  private def loadConfigRef(
    configFileOpt: Option[Path]
  ): Resource[IO, Ref[IO, Option[ConfigYaml]]] = {
    for {
      updatedConfig <- Resource.eval {
        configFileOpt match {
          case Some(filePath) => readConfig(filePath)
          case None           => IO.pure(None)
        }
      }
      configRef <- Resource.eval(Ref.of[IO, Option[ConfigYaml]](updatedConfig))
      _ <- configFileOpt match {
        case Some(filePath) => watchFileForChanges(filePath, configRef)
        case None           => Resource.unit[IO]
      }
    } yield configRef
  }

  private def watchFileForChanges(
    filePath: Path,
    configRef: Ref[IO, Option[ConfigYaml]]
  ): Resource[IO, Unit] = {
    Resource.eval {
      Supervisor[IO].use { supervisor =>
        supervisor.supervise {
          Files[IO]
            .watch(filePath)
            .evalMap {
              case Watcher.Event.Modified(_, _) =>
                for {
                  updatedConfig <- readConfig(filePath)
                  _ <- configRef.set(updatedConfig)
                } yield ()
              case _ => IO.unit
            }
            .compile
            .drain
        }.void
      }
    }
  }

  private def readConfig(filePath: Path): IO[Option[ConfigYaml]] = {
    ConfigYamlReader.fromYamlFile(filePath.toString).flatMap {
      case cats.data.Validated.Valid(configYaml) =>
        IO.pure(Some(configYaml))
      case cats.data.Validated.Invalid(errors) =>
        IO.println(s"Failed to parse config: ${errors.toList.mkString(", ")}")
          .as(None)
    }
  }

  private def findConfigFile(
    baseName: String,
    extensions: List[String]
  ): Option[String] = {
    extensions.collectFirst {
      case ext if new File(s"$baseName.$ext").exists() =>
        s"$baseName.$ext"
    }
  }

}
