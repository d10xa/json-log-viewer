# json-log-viewer

Print json logs in human-readable form

![screenshot.png](screenshot.png)

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
