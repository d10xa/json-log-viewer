import xerial.sbt.Sonatype._
sonatypeProfileName := "ru.d10xa"
publishMavenStyle := true
licenses := List(("MIT", url("https://opensource.org/licenses/MIT")))
sonatypeProjectHosting := Some(GitHubHosting(
  user = "ru.d10xa",
  repository = "json-log-viewer",
  email = "d10xa@mail.ru"
))
developers := List(
  Developer(
    "d10xa",
    "Andrey Stolyarov",
    "d10xa@mail.ru",
    url("https://d10xa.ru")
  )
)
ThisBuild / publishTo := sonatypePublishToBundle.value
import ReleaseTransformations._
releaseCrossBuild := true // true if you cross-build the project for multiple Scala versions
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  // For non cross-build projects, use releaseStepCommand("publishSigned")
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)