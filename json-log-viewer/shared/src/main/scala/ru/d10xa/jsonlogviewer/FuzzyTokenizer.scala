package ru.d10xa.jsonlogviewer

/** Tokenizer for fuzzy search that handles punctuation, quotes, and special
  * characters in log messages.
  *
  * Rules:
  *   - Splits text into words while preserving meaningful characters
  *   - Keeps dots and underscores inside words (e.g., john.doe, user_id)
  *   - Removes standalone punctuation
  *   - Converts to lowercase for case-insensitive matching
  *   - Filters tokens shorter than 2 characters
  *
  * Examples: {{{ tokenize("User 'john.doe' timeout") → Set("user", "john.doe",
  * "timeout") tokenize("ERROR: database.query() failed") → Set("error",
  * "database.query", "failed") tokenize("card_number=1234") →
  * Set("card_number", "1234") }}}
  */
object FuzzyTokenizer {

  /** Tokenizes text into a set of searchable words.
    *
    * @param text
    *   Text to tokenize
    * @return
    *   Set of normalized tokens (lowercase, minimum 2 characters)
    */
  def tokenize(text: String): Set[String] = {
    // Pattern matches alphanumeric characters, dots, and underscores
    // This preserves: user_id, john.doe, 192.168.1.1, etc.
    val tokenPattern = """[\w._]+""".r

    tokenPattern
      .findAllIn(text.toLowerCase)
      .toSet
      .filter(_.length >= 2)
      .filterNot(isOnlyPunctuation)
  }

  /** Checks if a token consists only of non-alphanumeric characters.
    *
    * @param token
    *   Token to check
    * @return
    *   true if token contains only punctuation
    */
  private def isOnlyPunctuation(token: String): Boolean =
    token.forall(c => !c.isLetterOrDigit)
}
