val scala3Version = "3.4.0"

val commonSettings = Seq(
  version := "0.2.4",
  scalaVersion := scala3Version
)

lazy val `json-log-viewer` = crossProject(JSPlatform, JVMPlatform)
  .jsSettings(test / fork := false)
  .in(file("json-log-viewer"))
  .settings(
    organization := "ru.d10xa",
    description := "The json-log-viewer converts JSON logs to a human-readable format",
    licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT"))),
    homepage := Some(url("https://github.com/d10xa/json-log-viewer")),
    pomIncludeRepository := { _ => false }
  )
  .jvmSettings(
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true
  )
  .settings(commonSettings)
  .jvmEnablePlugins(JavaAppPackaging)
  .settings(
    name := "json-log-viewer",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % "3.5.4",
      "co.fs2" %%% "fs2-core" % "3.9.2",
      "co.fs2" %%% "fs2-io" % "3.9.2",
      "com.monovore" %%% "decline" % "2.4.1",
      "com.monovore" %%% "decline-effect" % "2.4.1",
      "io.circe" %%% "circe-core" % "0.14.6",
      "io.circe" %%% "circe-literal" % "0.14.6" % Test,
      "io.circe" %%% "circe-parser" % "0.14.6",
      "com.lihaoyi" %%% "fansi" % "0.4.0",
      "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.3.0"
    ),
    libraryDependencies ++= Seq("org.scalameta" %% "munit" % "0.7.29" % Test),
    fork := true,
    run / connectInput := true
  )
  .jsSettings(
    scalaJSUseMainModuleInitializer := true,
//    scalaJSLinkerConfig ~= {
//      _.withModuleKind(ModuleKind.ESModule)
//    },
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
    libraryDependencies ++= Seq("org.scalameta" %% "munit" % "0.7.29" % Test)
  )

lazy val `frontend-laminar` = project
  .in(file("frontend-laminar"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(`json-log-viewer`.js)
  .settings(commonSettings)
  .settings(
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
