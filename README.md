# json-log-viewer

[![Maven Central](https://img.shields.io/maven-central/v/ru.d10xa/json-log-viewer_2.12.svg?label=Maven%20Central%20(Scala%202.12))](https://search.maven.org/artifact/ru.d10xa/json-log-viewer_2.12)
[![Maven Central](https://img.shields.io/maven-central/v/ru.d10xa/json-log-viewer_3.svg?label=Maven%20Central%20(Scala%203))](https://search.maven.org/artifact/ru.d10xa/json-log-viewer_3)


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

```~fastLinkJS::webpack```

# k9s plugin

Integrate json-log-viewer with k9s to view formatted JSON logs directly within the k9s interface.

### Prerequisites
For the plugin to work correctly, the following must be installed on your system:
1. **coursier** - Used to download and run `json-log-viewer` if it's not installed.
2. **json-log-viewer** (optional) - If already installed, the plugin will use it directly; otherwise, it will fall back to using `coursier` to launch it.

### Installation
Add the following to your k9s plugin file 
(usually located at ~/.k9s/plugins.yaml or, on macOS, check the plugin path with `k9s info`):

```yaml
plugins:
  json-log-viewer:
    shortCut: Ctrl-L
    description: "json-log-viewer"
    scopes:
      - pod
      - containers
    command: sh
    background: false
    args:
      - -c
      - |
        if command -v json-log-viewer >/dev/null 2>&1; then
          VIEWER_COMMAND="json-log-viewer"
        else
          VIEWER_COMMAND="coursier launch ru.d10xa:json-log-viewer_3:latest.release"
        fi

        if [ -n "$POD" ]; then
          kubectl logs $POD -n $NAMESPACE --context $CONTEXT -c $NAME -f --tail 500 | $VIEWER_COMMAND; read -p "Press [Enter] to close..."
        else
          kubectl logs $NAME -n $NAMESPACE --context $CONTEXT -f --tail 500 | $VIEWER_COMMAND; read -p "Press [Enter] to close..."
        fi
```

##  k9s plugin usage

1. Install json-log-viewer
2. Launch k9s
3. Select a Pod or Container
4. Press Ctrl+L to view logs formatted by json-log-viewer
