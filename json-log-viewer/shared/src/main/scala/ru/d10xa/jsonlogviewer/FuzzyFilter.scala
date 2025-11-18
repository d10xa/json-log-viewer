package ru.d10xa.jsonlogviewer

import ru.d10xa.jsonlogviewer.config.ResolvedConfig

/** Fuzzy filter that searches for patterns across all fields in a parsed log
  * entry using token-based matching.
  *
  * Unlike rawFilter (regex on raw strings) or SQL filters (exact field
  * matching), fuzzy filter:
  *   - Works after JSON parsing
  *   - Searches across all fields (level, message, stackTrace, etc.)
  *   - Uses tokenization to ignore punctuation
  *   - Is case-insensitive
  *   - Supports partial token matching
  *
  * Example: {{{ fuzzyInclude: ["error timeout"] // Will match: {"level":
  * "ERROR", "message": "Connection timeout"} {"message": "Error: request
  * timeout occurred"} {"error_code": "500", "details": "timeout"} }}}
  *
  * @param config
  *   Resolved configuration containing fuzzyInclude and fuzzyExclude patterns
  */
class FuzzyFilter(config: ResolvedConfig) {

  /** Collects all values from the parsed log entry into a single searchable
    * string.
    *
    * Includes standard fields (level, message, etc.) and all custom attributes
    * from otherAttributes.
    *
    * @param parseResult
    *   Parsed log entry
    * @return
    *   Space-separated concatenation of all field values
    */
  private def collectAllValues(parseResult: ParseResult): String =
    parseResult.parsed match {
      case None => parseResult.raw // Fallback to raw string if parsing failed
      case Some(parsed) =>
        val standardFields = List(
          parsed.timestamp,
          parsed.level,
          parsed.message,
          parsed.stackTrace,
          parsed.loggerName,
          parsed.threadName
        ).flatten

        val otherValues = parsed.otherAttributes.values

        (standardFields ++ otherValues).mkString(" ")
    }

  /** Token-based fuzzy matching: checks if all tokens from the pattern exist in
    * the text.
    *
    * Uses partial matching: pattern token "timeout" will match text tokens
    * "timeout", "timeouts", "timeout_ms", etc.
    *
    * @param text
    *   Text to search in (typically all log field values concatenated)
    * @param pattern
    *   Search pattern (e.g., "error timeout")
    * @return
    *   true if all pattern tokens are found in text tokens
    */
  private def tokenBasedMatch(text: String, pattern: String): Boolean = {
    val textTokens = FuzzyTokenizer.tokenize(text)
    val patternTokens = FuzzyTokenizer.tokenize(pattern)

    // All pattern tokens must be present in text tokens (with partial matching)
    patternTokens.forall { patternToken =>
      textTokens.exists(textToken => textToken.contains(patternToken))
    }
  }

  /** Tests whether the parsed log entry matches fuzzyInclude and fuzzyExclude
    * patterns.
    *
    * Logic:
    *   - fuzzyInclude: At least one pattern must match (OR logic)
    *   - fuzzyExclude: No pattern should match (AND NOT logic)
    *   - If fuzzyInclude is empty or None, all entries pass
    *
    * @param parseResult
    *   Parsed log entry to test
    * @return
    *   true if entry should be included in output
    */
  def test(parseResult: ParseResult): Boolean = {
    val allValues = collectAllValues(parseResult)

    val includeMatches = config.fuzzyInclude match {
      case None                               => true
      case Some(patterns) if patterns.isEmpty => true
      case Some(patterns) =>
        patterns.exists(pattern => tokenBasedMatch(allValues, pattern))
    }

    val excludeMatches = config.fuzzyExclude match {
      case None => true
      case Some(patterns) =>
        patterns.forall(pattern => !tokenBasedMatch(allValues, pattern))
    }

    includeMatches && excludeMatches
  }
}
