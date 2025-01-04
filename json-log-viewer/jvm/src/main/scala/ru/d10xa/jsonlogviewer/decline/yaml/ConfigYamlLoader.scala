package ru.d10xa.jsonlogviewer.decline.yaml

import cats.data.ValidatedNel

trait ConfigYamlLoader {
  def parseYamlFile(content: String): ValidatedNel[String, ConfigYaml]
}
