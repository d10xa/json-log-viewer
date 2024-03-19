# json-log-viewer

The `json-log-viewer` converts JSON logs to a human-readable
format via stdin and offers a Scala.js browser version,
streamlining log analysis for developers and system administrators.

[DEMO](https://d10xa.ru/json-log-viewer/)

![screenshot.png](screenshot.png)

## install

```
coursier install json-log-viewer --channel https://git.io/JvV0g
```

## build jvm version

```
sbt stage
```

## run jvm version

```
cat log.txt | ./json-log-viewer/jvm/target/universal/stage/bin/json-log-viewer
```

## build js version

```
sbt fullLinkJS
```

## run js version

```
cat log.txt | node ./json-log-viewer/js/target/scala-3.3.1/json-log-viewer-opt/main.js
```

# frontend-laminar

```~fastOptJS::webpack```
