import xerial.sbt.Sonatype.sonatypeCentralHost
import xerial.sbt.Sonatype.sonatypePublishToBundle
import xerial.sbt.Sonatype.GitHubHosting

val scala3Version = "3.5.0"

val commonSettings = Seq(
  scalaVersion := scala3Version
)

inThisBuild(
  List(
    organization := "ru.d10xa",
    homepage := Some(url("https://github.com/d10xa/json-log-viewer")),
    licenses := List(("MIT", url("https://opensource.org/licenses/MIT"))),
    developers := List(
      Developer(
        "d10xa",
        "Andrey Stolyarov",
        "d10xa@mail.ru",
        url("https://d10xa.ru")
      )
    ),
    sonatypeProfileName := "ru.d10xa",
    sonatypeProjectHosting := Some(
      GitHubHosting(
        user = "ru.d10xa",
        repository = "json-log-viewer",
        email = "d10xa@mail.ru"
      )
    ),
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
    publishTo := sonatypePublishToBundle.value
//    credentials += Credentials(
//      "Sonatype Nexus Repository Manager",
//      "oss.sonatype.org",
//      sys.env.getOrElse("SONATYPE_USERNAME", ""),
//      sys.env.getOrElse("SONATYPE_PASSWORD", "")
//    )
  )
)

val circeVersion = "0.14.10"
val declineVersion = "2.4.1"
val fs2Version = "3.11.0"

lazy val `json-log-viewer` = crossProject(JSPlatform, JVMPlatform)
  .in(file("json-log-viewer"))
  .settings(
    organization := "ru.d10xa",
    description := "The json-log-viewer converts JSON logs to a human-readable format",
    pomIncludeRepository := { _ => false }
  )
  .settings(commonSettings)
  .jvmEnablePlugins(JavaAppPackaging)
  .settings(
    name := "json-log-viewer",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % "3.5.4",
      "co.fs2" %%% "fs2-core" % fs2Version,
      "co.fs2" %%% "fs2-io" % fs2Version,
      "com.monovore" %%% "decline" % declineVersion,
      "com.monovore" %%% "decline-effect" % declineVersion,
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-literal" % circeVersion % Test,
      "io.circe" %%% "circe-parser" % circeVersion,
      "com.lihaoyi" %%% "fansi" % "0.5.0",
      "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.4.0"
    ),
    libraryDependencies ++= Seq("org.scalameta" %% "munit" % "0.7.29" % Test),
    fork := true,
    run / connectInput := true,
    sonatypeCredentialHost := sonatypeCentralHost
  )
  .jsSettings(
    publish / skip := true,
    scalaJSUseMainModuleInitializer := true,
    fork := false,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )

lazy val `make-logs` = project
  .in(file("make-logs"))
  .settings(commonSettings)
  .settings(
    publish / skip := true,
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.5.3",
      "net.logstash.logback" % "logstash-logback-encoder" % "7.4"
    ),
    libraryDependencies ++= Seq("org.scalameta" %% "munit" % "1.0.2" % Test)
  )

lazy val `frontend-laminar` = project
  .in(file("frontend-laminar"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(`json-log-viewer`.js)
  .settings(commonSettings)
  .settings(
    publish / skip := true,
    (installJsdom / version) := "20.0.3",
    (webpack / version) := "5.75.0",
    (startWebpackDevServer / version) := "4.11.1",
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % "16.0.0",
      "com.raquo" %%% "airstream" % "16.0.0",
      "com.raquo" %%% "waypoint" % "7.0.0",
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-core" % "2.28.4",
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.28.4" % "provided"
    ),
    Compile / fastOptJS / scalaJSLinkerConfig ~= { _.withSourceMap(false) },
    Compile / fullOptJS / scalaJSLinkerConfig ~= { _.withSourceMap(true) },
    scalaJSUseMainModuleInitializer := true,
    (Test / requireJsDomEnv) := true,
    useYarn := true
  )
