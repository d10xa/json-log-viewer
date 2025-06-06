# json-log-viewer

[![Maven Central](https://img.shields.io/maven-central/v/ru.d10xa/json-log-viewer_3.svg?label=Maven%20Central%20(Scala%203))](https://search.maven.org/artifact/ru.d10xa/json-log-viewer_3)

## Introduction

`json-log-viewer` is a versatile tool designed to simplify the analysis
of JSON logs for developers and system administrators.
Whether you work in the command line or prefer a browser-based interface,
`json-log-viewer` provides powerful features for filtering, combining, and visualizing log streams.

The primary goal of `json-log-viewer` is to transform raw JSON logs into a human-readable format,
making them significantly easier to interpret and process.
With support for advanced filtering, dynamic configuration updates, multi-source input,
and streamlined configuration management,
this tool offers a comprehensive solution for log analysis across various environments.

You can also try the browser-based version online: [DEMO](https://d10xa.ru/json-log-viewer/).

### Example Output
Below is an example of formatted JSON logs in the command-line interface:

![Example Output](screenshot.png)

## Key Features

- **Human-Readable Log Output**  
  Effortlessly transform raw JSON logs into a structured, easy-to-read format.

- **Flexible Interfaces**  
  Use the command-line utility or a browser-based application for visualization.

- **Multi-Source Input**  
  Combine the outputs of multiple commands into a single unified log stream.

- **Multiple Input Formats**
    - **JSON** (default): Process standard JSON log formats
    - **Logfmt**: Support for key-value pair log formats
    - **CSV**: Parse and analyze CSV-formatted logs with headers

- **Advanced Filtering**
    - Apply regular expressions to extract relevant log entries.
    - Use SQL-like queries to filter and query JSON fields.
    - Filter logs by timestamp ranges.

- **YAML Configuration**  
  Define input streams and configurations using YAML files.

- **Dynamic Configuration Updates**  
  Hot-reload filters and configurations without restarting the tool.

- **stdin Support**  
  Process logs directly from the standard input for real-time analysis.

- **Integration with k9s**  
  Seamlessly integrate with the k9s Kubernetes CLI tool to visualize logs directly within k9s.

- **Custom Field Mapping**  
  Configure custom field names mapping to work with non-standard log formats.

## Installation

### Requirements

- **Coursier** (for installation and dependency management)

### Install with Coursier

Install `json-log-viewer` globally using `coursier`:

```bash
coursier install json-log-viewer --channel https://git.io/JvV0g
```

After installation, ensure the tool is available:

```bash
json-log-viewer --help
```

## Usage

### Quick Start
1. Install `json-log-viewer` using Coursier.
2. Pass logs to the tool via stdin:
   ```bash
   cat log.txt | json-log-viewer
   ```
3. Apply filters directly from the command line:
   ```bash
   cat log.txt | json-log-viewer --filter "level = 'ERROR'"
   ```

### Working with Different Log Formats

#### JSON (Default)
For standard JSON logs:
```bash
cat json-logs.txt | json-log-viewer
```

#### Logfmt
For logs in logfmt format:
```bash
cat logfmt-logs.txt | json-log-viewer --format-in logfmt
```

#### CSV
For CSV-formatted logs (requires header row):
```bash
cat csv-logs.csv | json-log-viewer --format-in csv
```

Note: CSV format requires a header row with column names. The tool will map these column names to standard log fields.

### SQL Filtering

`json-log-viewer` supports SQL-like filtering for JSON fields, allowing precise log analysis.
You can use comparison and logical operations.

#### Supported Syntax
- **Comparison operators**:
    - `=`: Equal
    - `!=`: Not equal
    - `LIKE`: Matches with patterns (`%` for wildcard)
    - `NOT LIKE`: Negates pattern matching
- **Logical operators**:
    - `AND`: Logical AND
    - `OR`: Logical OR
    - `()` for grouping expressions
- **Literals and Identifiers**:
    - Identifiers represent JSON keys (e.g., `level`).
    - Literals are strings enclosed in single quotes (e.g., `'ERROR'`).

#### Examples

1. Filter logs where `level` is `'ERROR'`:
   ```bash
   cat log.txt | json-log-viewer --filter "level = 'ERROR'"
   ```
2. Match logs where message contains the word "timeout":
   ```bash
   cat log.txt | json-log-viewer --filter "message LIKE '%timeout%'"
   ```
3. Combine conditions:
   ```bash
   cat log.txt | json-log-viewer --filter "level = 'ERROR' AND message LIKE '%timeout%'"
   ```
4. Exclude logs with specific patterns:
   ```bash
   cat log.txt | json-log-viewer --filter "message NOT LIKE '%timeout%'"
   ```
5. Group conditions with parentheses:
   ```bash
   cat log.txt | json-log-viewer --filter "(level = 'ERROR' OR level = 'WARN') AND message LIKE '%connection%'"
   ```

### Timestamp Filtering

Filter logs by timestamp range:

```bash
cat log.txt | json-log-viewer --timestamp-after 2024-01-01T00:00:00Z --timestamp-before 2024-01-31T23:59:59Z
```

You can also specify a custom timestamp field:

```bash
cat log.txt | json-log-viewer --timestamp-field time
```

## Configuration

`json-log-viewer` supports defining input streams, filters, and other settings using a YAML configuration file.

### YAML Configuration Structure

A configuration file consists of one or more **feeds**.
Each feed represents a log source and can have the following attributes:
- **name** (optional): A descriptive name for the feed.
- **commands** (required): A list of shell commands that produce log output.
- **inlineInput** (optional): Direct input as a string instead of executing commands.
- **filter** (optional): SQL-like filter expression for processing logs (e.g., `level = 'ERROR'`).
- **formatIn** (optional): Input log format. Supported values:
    - `json` (default).
    - `logfmt`.
    - `csv`.
- **rawInclude** and **rawExclude** (optional): Lists of regular expressions to include or exclude from processing.
- **excludeFields** (optional): List of fields to exclude from output.
- **fieldNames** (optional): Custom mapping for field names, helpful when working with non-standard log formats.

### Custom Field Mapping

You can define custom field name mappings either globally or per feed:

```yaml
# Global field mapping
fieldNames:
  timestamp: "ts"
  level: "severity"
  message: "msg"
  stackTrace: "error"
  loggerName: "logger"
  threadName: "thread"

feeds:
  - name: "application-logs"
    commands:
      - cat log1.txt
    # Feed-specific field mapping (overrides global mapping)
    fieldNames:
      timestamp: "time"
      level: "priority"
```

### Example Configuration File

```yaml
showEmptyFields: true
feeds:
  - name: "application-1-logs"
    commands:
      - cat log1.txt
    filter: |
      level = 'ERROR' AND message LIKE '%timeout%'
    formatIn: json
    rawInclude:
      - "ERROR"
    rawExclude:
      - "DEBUG"
    excludeFields:
      - "thread_name"
    showEmptyFields: false
  - name: "application-2-logs"
    commands:
      - cat log2.txt
    filter: |
      message NOT LIKE '%heartbeat%'
    formatIn: logfmt
  - name: "csv-logs"
    commands:
      - cat logs.csv
    formatIn: csv
    fieldNames:
      timestamp: "time"
      level: "severity"
```

#### Running with a Configuration File

To use a YAML configuration file, pass its path with the --config-file option:

```bash
json-log-viewer --config-file json-log-viewer.yml
```

## Command-Line Arguments

`json-log-viewer` also supports direct configuration via command-line arguments:

### Supported Options

- **--filter**: Apply SQL-like filters directly to logs.
  ```bash
  cat log.txt | json-log-viewer --filter "level = 'ERROR'"
  ```

- **--config-file**: Specify the path to a YAML configuration file.
  ```bash
  cat log.txt | json-log-viewer --config-file json-log-viewer.yml
  ```

- **--format-in**: Specify the input log format (supported formats: json, logfmt, csv).
  ```bash
  cat log.txt | json-log-viewer --format-in logfmt
  ```

- **--format-out**: Specify the output format (supported formats: pretty, raw).
  ```bash
  cat log.txt | json-log-viewer --format-out raw
  ```

- **--timestamp-after** and **--timestamp-before**: Filter logs by a specific time range.
  ```bash
  cat log.txt | json-log-viewer --timestamp-after 2024-01-01T00:00:00Z --timestamp-before 2024-01-31T23:59:59Z
  ```

- **--timestamp-field**: Specify the field name for timestamps (default: @timestamp).
  ```bash
  json-log-viewer --timestamp-field time
  ```

- **--show-empty-fields**: Display fields with empty values (null, empty strings, etc.) in output.
  ```bash
  cat log.txt | json-log-viewer --show-empty-fields
  ```


#### Field Name Options

You can override the default field names to work with non-standard log formats:

- **--level-field**: Override default level field name (default: level).
  ```bash
  json-log-viewer --level-field severity
  ```

- **--message-field**: Override default message field name (default: message).
  ```bash
  json-log-viewer --message-field msg
  ```

- **--stack-trace-field**: Override default stack trace field name (default: stack_trace).
  ```bash
  json-log-viewer --stack-trace-field exception
  ```

- **--logger-name-field**: Override default logger name field name (default: logger_name).
  ```bash
  json-log-viewer --logger-name-field logger
  ```

- **--thread-name-field**: Override default thread name field name (default: thread_name).
  ```bash
  json-log-viewer --thread-name-field thread
  ```

## k9s Plugin

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

###  k9s plugin usage

1. Install json-log-viewer
2. Launch k9s
3. Select a Pod or Container
4. Press Ctrl+L to view logs formatted by json-log-viewer


## Development

This section provides instructions for building and running
both the JVM and JavaScript versions of `json-log-viewer`.
It also includes notes for working on the `frontend-laminar` module.

### Prerequisites
Ensure you have the following installed on your system:
- Java 11+ (for building the JVM version)
- Node.js 23+ (for building the JS version)
- sbt (Scala Build Tool)

### Building the JVM Version
To build the JVM version of the project, use the command:

```bash
sbt stage
```

This compiles the code and prepares the executable under the `jvm/target/universal/stage/bin/` directory.

### Running the JVM Version
Run the application with:
```bash
cat log.txt | ./json-log-viewer/jvm/target/universal/stage/bin/json-log-viewer
```

Replace `log.txt` with the path to your JSON log file. The output will be displayed in a human-readable format.

### Building the JavaScript Version

To build the JavaScript version, you can use one of the following options:

1. **Optimized Build**: Use the command:
   ```bash
   sbt fullLinkJS  
   ```
   This generates a production-ready JavaScript file located at:  
   `frontend-laminar/target/scala-3.6.2/frontend-laminar-opt/main.js`
2. **Fast Development Build**: Use the command:
   ```bash
   sbt fastLinkJS 
   ```
   This generates a faster, less optimized build located at:  
   `frontend-laminar/target/scala-3.6.2/frontend-laminar-fastopt/main.js`

Choose the appropriate option based on your needs:
- Use `fullLinkJS` for production or when you need a fully optimized bundle.
- Use `fastLinkJS` for local development with faster build times.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for full details.