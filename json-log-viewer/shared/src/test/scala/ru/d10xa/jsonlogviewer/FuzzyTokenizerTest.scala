package ru.d10xa.jsonlogviewer

import munit.FunSuite

class FuzzyTokenizerTest extends FunSuite {

  test("tokenize should handle basic words") {
    val result = FuzzyTokenizer.tokenize("error timeout user")
    assertEquals(result, Set("error", "timeout", "user"))
  }

  test("tokenize should handle punctuation") {
    val result = FuzzyTokenizer.tokenize("User 'john.doe' timeout")
    assert(result.contains("user"))
    assert(result.contains("john.doe"))
    assert(result.contains("timeout"))
  }

  test("tokenize should preserve dots in words") {
    val result = FuzzyTokenizer.tokenize("database.query() failed")
    assert(result.contains("database.query"))
    assert(result.contains("failed"))
  }

  test("tokenize should preserve underscores") {
    val result = FuzzyTokenizer.tokenize("card_number=1234 user_id=5678")
    assert(result.contains("card_number"))
    assert(result.contains("user_id"))
    assert(result.contains("1234"))
    assert(result.contains("5678"))
  }

  test("tokenize should handle quoted strings") {
    val result =
      FuzzyTokenizer.tokenize("reason=\"insufficient funds\" error")
    assert(result.contains("reason"))
    assert(result.contains("insufficient"))
    assert(result.contains("funds"))
    assert(result.contains("error"))
  }

  test("tokenize should filter short tokens") {
    val result = FuzzyTokenizer.tokenize("a b c error")
    assert(!result.contains("a"))
    assert(!result.contains("b"))
    assert(!result.contains("c"))
    assert(result.contains("error"))
  }

  test("tokenize should be case insensitive") {
    val result1 = FuzzyTokenizer.tokenize("ERROR Timeout USER")
    val result2 = FuzzyTokenizer.tokenize("error timeout user")
    assertEquals(result1, result2)
    assertEquals(result1, Set("error", "timeout", "user"))
  }

  test("tokenize should handle complex punctuation") {
    val result = FuzzyTokenizer.tokenize(
      "ERROR: database.query() failed - timeout=30s (retry: 3)"
    )
    assert(result.contains("error"))
    assert(result.contains("database.query"))
    assert(result.contains("failed"))
    assert(result.contains("timeout"))
    assert(result.contains("30s"))
    assert(result.contains("retry"))
  }

  test("tokenize should handle brackets and parens") {
    val result = FuzzyTokenizer.tokenize("User[admin] login(failed)")
    assert(result.contains("user"))
    assert(result.contains("admin"))
    assert(result.contains("login"))
    assert(result.contains("failed"))
  }

  test("tokenize should handle URLs and paths") {
    val result =
      FuzzyTokenizer.tokenize("Request /api/v1/users?id=123 returned 500")
    assert(result.contains("request"))
    assert(result.contains("api"))
    assert(result.contains("v1"))
    assert(result.contains("users"))
    assert(result.contains("id"))
    assert(result.contains("123"))
    assert(result.contains("returned"))
    assert(result.contains("500"))
  }

  test("tokenize should handle email addresses") {
    val result = FuzzyTokenizer.tokenize("User john.doe@example.com logged in")
    assert(result.contains("user"))
    assert(result.contains("john.doe"))
    assert(result.contains("example.com"))
    assert(result.contains("logged"))
    assert(result.contains("in"))
  }

  test("tokenize should handle empty string") {
    val result = FuzzyTokenizer.tokenize("")
    assertEquals(result, Set.empty[String])
  }

  test("tokenize should handle only punctuation") {
    val result = FuzzyTokenizer.tokenize("!@#$%^&*()")
    assertEquals(result, Set.empty[String])
  }

  test("tokenize should handle mixed case with special chars") {
    val result = FuzzyTokenizer.tokenize(
      "PaymentError: card_declined (code=INSUFFICIENT_FUNDS)"
    )
    assert(result.contains("paymenterror"))
    assert(result.contains("card_declined"))
    assert(result.contains("code"))
    assert(result.contains("insufficient_funds"))
  }
}
