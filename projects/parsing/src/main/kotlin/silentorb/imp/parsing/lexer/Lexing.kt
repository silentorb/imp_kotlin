package silentorb.imp.parsing.lexer

import silentorb.imp.core.*
import silentorb.imp.parsing.general.*

fun nextCharacter(code: CodeBuffer, index: CodeInt): Char? {
  val size = getCodeBufferSize(code)
  return if (index > size - 1)
    null
  else
    getCharFromBuffer(code, index)
}

fun nextCharacter(bundle: Bundle): Char? =
    nextCharacter(bundle.code, bundle.end.index)

fun singleCharacterTokenMatch(position: Position, character: Char): TokenStep? {
  val rune = singleCharacterTokens(character)
  return if (rune != null) {
    val end = nextPosition(character, position)
    TokenStep(
        token = Token(rune, FileRange(position.file, Range(position, end)), character.toString()),
        position = end
    )
  } else
    null
}

typealias BundleToToken = (Bundle) -> TokenStep

fun branchTokenStart(character: Char): BundleToToken? =
    when {
      literalZero(character) -> ::consumeLiteralZero
      newLineStart(character) -> ::consumeNewline
      identifierStart(character) -> ::consumeIdentifier
      integerStart(character) -> ::consumeInteger
      operatorStart(character) -> ::consumeOperator
      commentStartOrHyphen(character) -> ::consumeCommentOrHyphenOrNegativeNumber
      isQuoteCharacter(character) -> ::consumeLiteralStringStart
      else -> null
    }

fun tokenStart(code: CodeBuffer, position: Position, character: Char): TokenStep {
  val branch = branchTokenStart(character)
  return if (branch == null)
    badCharacter(FileRange(position.file, Range(position, nextPosition(character, position))))
  else
    branch(Bundle(
        code = code,
        start = position,
        end = nextPosition(character, position),
        buffer = newLexicalBuffer(character)
    ))
}

fun tokenStart(code: CodeBuffer): (Position) -> TokenStep = { position ->
  val character = nextCharacter(code, position.index)
  if (character != null) {
    singleCharacterTokenMatch(position, character)
        ?: tokenStart(code, position, character)
  } else
    TokenStep(position)
}

fun nextToken(code: CodeBuffer, position: Position): TokenStep {
  return consumeSingleLineWhitespace(Bundle(code, position, position)) ?: tokenStart(code)(position)
}

tailrec fun tokenize(code: CodeBuffer, position: Position, tokens: Tokens): Tokens {
  val result = nextToken(code, position)
  val (newPosition, token) = result
  return if (token == null)
    tokens
  else
    tokenize(code, newPosition, tokens.plus(token))
}

fun tokenize(code: CodeBuffer, file: TokenFile = ""): Tokens =
    tokenize(code, position = newPosition(file), tokens = listOf())

fun stripWhitespace(tokens: Tokens): Tokens =
    tokens.filter { it.rune != Rune.whitespace }
