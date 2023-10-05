package ru.d10xa.jsonlogviewer

import ru.d10xa.jsonlogviewer.Application.ConfigGrep

final case class Config(timestamp: TimestampConfig, grep: List[ConfigGrep])
