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
publishTo := Some(
  "Sonatype Central Publisher" at "https://central.sonatype.com/api/v1/publish"
)
