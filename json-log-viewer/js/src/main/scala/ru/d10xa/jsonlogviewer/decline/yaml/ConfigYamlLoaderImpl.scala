package ru.d10xa.jsonlogviewer.decline.yaml

import cats.data.ValidatedNel

class ConfigYamlLoaderImpl extends ConfigYamlLoader {

  override def parseYamlFile(content: String): ValidatedNel[String, ConfigYaml] = ???
}
