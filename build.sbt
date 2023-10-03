val scala3Version = "3.3.1"

val commonSettings = Seq(
  version := "0.1.0-SNAPSHOT",
  scalaVersion := scala3Version,
)

lazy val `json-log-viewer` = crossProject(JSPlatform, JVMPlatform)
  .in(file("json-log-viewer"))
  .settings(commonSettings)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "json-log-viewer",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % "3.5.1",
      "co.fs2" %%% "fs2-core" % "3.9.2",
      "co.fs2" %%% "fs2-io" % "3.9.2",
      "com.monovore" %%% "decline" % "2.4.1",
      "com.monovore" %%% "decline-effect" % "2.4.1",
      "io.circe" %%% "circe-core" % "0.14.6",
      "io.circe" %%% "circe-parser" % "0.14.6",
      "com.lihaoyi" %%% "fansi" % "0.4.0"
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
      "ch.qos.logback" % "logback-classic" % "1.4.11",
      "net.logstash.logback" % "logstash-logback-encoder" % "7.4"
    ),
    libraryDependencies ++= Seq("org.scalameta" %% "munit" % "0.7.29" % Test)
  )
