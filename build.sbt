import xerial.sbt.Sonatype.sonatypeCentralHost

ThisBuild / scalaVersion := "3.5.0"
ThisBuild / licenses := List(("MIT", url("https://opensource.org/licenses/MIT")))
ThisBuild / homepage := Some(url("https://github.com/d10xa/json-log-viewer"))
ThisBuild / organization := "ru.d10xa"
ThisBuild / developers := List(
  Developer(
    "d10xa",
    "Andrey Stolyarov",
    "d10xa@mail.ru",
    url("https://d10xa.ru")
  )
)
ThisBuild / sonatypeCredentialHost := sonatypeCentralHost

val circeVersion = "0.14.10"
val declineVersion = "2.4.1"
val fs2Version = "3.11.0"

lazy val `json-log-viewer` = crossProject(JSPlatform, JVMPlatform)
  .in(file("json-log-viewer"))
//  .jvmEnablePlugins(JavaAppPackaging)
  .settings(
    name := "json-log-viewer",
    organization := "ru.d10xa",
    description := "The json-log-viewer converts JSON logs to a human-readable format",
    pomIncludeRepository := { _ => false },
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % "3.5.4",
      "co.fs2" %%% "fs2-core" % fs2Version,
      "co.fs2" %%% "fs2-io" % fs2Version,
      "com.monovore" %%% "decline" % declineVersion,
      "com.monovore" %%% "decline-effect" % declineVersion,
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-literal" % circeVersion % Test,
      "io.circe" %%% "circe-parser" % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion,
      "io.circe" %%% "circe-yaml-scalayaml" % "0.16.0",
      "com.lihaoyi" %%% "fansi" % "0.5.0",
      "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.4.0",
      "org.scalameta" %% "munit" % "0.7.29" % Test
    ),
    fork := true,
    run / connectInput := true,
  )
  .jvmSettings(
    publish / skip := false
  )
  .jsSettings(
    publish / skip := true,
    scalaJSUseMainModuleInitializer := true,
    fork := false,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )

lazy val `make-logs` = project
  .in(file("make-logs"))
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
