package ru.d10xa.jsonlogviewer.config

import java.time.ZonedDateTime
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.Feed
import ru.d10xa.jsonlogviewer.decline.yaml.FieldNames
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.ConfigGrep
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.Config.FormatOut
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig
import ru.d10xa.jsonlogviewer.query.QueryAST

/** Fully resolved configuration that combines global and feed-specific
  * settings. This eliminates the need to check multiple places for
  * configuration values at runtime.
  */
final case class ResolvedConfig(
  // Feed info
  feedName: Option[String],
  commands: List[String],
  inlineInput: Option[String],

  // Config settings
  filter: Option[QueryAST],
  formatIn: Option[FormatIn],
  formatOut: Option[FormatOut],
  fieldNames: FieldNamesConfig, // already merged

  // Feed-specific settings
  rawInclude: Option[List[String]],
  rawExclude: Option[List[String]],
  excludeFields: Option[List[String]],

  // Timestamp settings
  timestampAfter: Option[ZonedDateTime],
  timestampBefore: Option[ZonedDateTime],

  // Other settings
  grep: List[ConfigGrep]
)

/** Resolves configuration by merging global and feed-specific settings into a
  * list of fully resolved configurations.
  */
object ConfigResolver {

  /** Resolves the configuration by combining global and feed-specific settings.
    *
    * @param config
    *   Base CLI configuration
    * @param configYaml
    *   Optional YAML configuration
    * @return
    *   List of fully resolved configurations (one per feed, or a single one
    *   based on global settings)
    */
  def resolve(
    config: Config,
    configYaml: Option[ConfigYaml]
  ): List[ResolvedConfig] =
    configYaml match {
      case Some(yaml) =>
        // Merge global field names from YAML with CLI config
        val globalFieldNames = yaml.fieldNames match {
          case Some(fieldNames) =>
            mergeFieldNames(config.fieldNames, Some(fieldNames))
          case None => config.fieldNames
        }

        // Process feeds if present
        yaml.feeds match {
          case Some(feeds) if feeds.nonEmpty =>
            feeds.map { feed =>
              // For each feed, merge its field names with global field names
              val feedFieldNames =
                mergeFieldNames(globalFieldNames, feed.fieldNames)

              ResolvedConfig(
                feedName = feed.name,
                commands = feed.commands,
                inlineInput = feed.inlineInput,
                filter = feed.filter.orElse(config.filter),
                formatIn = feed.formatIn.orElse(config.formatIn),
                formatOut = config.formatOut,
                fieldNames = feedFieldNames,
                rawInclude = feed.rawInclude,
                rawExclude = feed.rawExclude,
                excludeFields = feed.excludeFields,
                timestampAfter = config.timestamp.after,
                timestampBefore = config.timestamp.before,
                grep = config.grep
              )
            }
          case _ =>
            // If no feeds, create one ResolvedConfig based on global settings
            List(
              ResolvedConfig(
                feedName = None,
                commands = List.empty,
                inlineInput = None,
                filter = config.filter,
                formatIn = config.formatIn,
                formatOut = config.formatOut,
                fieldNames = globalFieldNames,
                rawInclude = None,
                rawExclude = None,
                excludeFields = None,
                timestampAfter = config.timestamp.after,
                timestampBefore = config.timestamp.before,
                grep = config.grep
              )
            )
        }
      case None =>
        // If no ConfigYaml, create one ResolvedConfig based only on CLI config
        List(
          ResolvedConfig(
            feedName = None,
            commands = List.empty,
            inlineInput = None,
            filter = config.filter,
            formatIn = config.formatIn,
            formatOut = config.formatOut,
            fieldNames = config.fieldNames,
            rawInclude = None,
            rawExclude = None,
            excludeFields = None,
            timestampAfter = config.timestamp.after,
            timestampBefore = config.timestamp.before,
            grep = config.grep
          )
        )
    }

  /** Helper method for backwards compatibility with tests. Merges global field
    * names with feed-specific field names.
    */
  def mergeFieldNames(
    globalFieldNames: FieldNamesConfig,
    feedFieldNames: Option[FieldNames]
  ): FieldNamesConfig =
    feedFieldNames match {
      case None => globalFieldNames
      case Some(feedFields) =>
        FieldNamesConfig(
          timestampFieldName =
            feedFields.timestamp.getOrElse(globalFieldNames.timestampFieldName),
          levelFieldName =
            feedFields.level.getOrElse(globalFieldNames.levelFieldName),
          messageFieldName =
            feedFields.message.getOrElse(globalFieldNames.messageFieldName),
          stackTraceFieldName = feedFields.stackTrace.getOrElse(
            globalFieldNames.stackTraceFieldName
          ),
          loggerNameFieldName = feedFields.loggerName.getOrElse(
            globalFieldNames.loggerNameFieldName
          ),
          threadNameFieldName = feedFields.threadName.getOrElse(
            globalFieldNames.threadNameFieldName
          )
        )
    }
}
