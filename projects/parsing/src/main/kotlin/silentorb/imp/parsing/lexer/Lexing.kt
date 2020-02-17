package silentorb.imp.parsing.lexer

import silentorb.imp.parsing.general.Response
import silentorb.imp.parsing.general.failure
import silentorb.imp.parsing.general.success
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

fun singleCharacterTokenMatch(position: Position, character: Char): Response<TokenStep>? {
  val rune = singleCharacterTokens(character)
  return if (rune != null)
    success(TokenStep(
        token = Token(rune, Range(position, position), character.toString()),
        position = nextPosition(character, position)
    ))
  else
    null
}

typealias BundleToToken = (Bundle) -> Response<TokenStep>

fun branchTokenStart(character: Char): BundleToToken? =
    when {
      literalZero(character) -> ::consumeLiteralZero
      newLineStart(character) -> ::consumeNewline
      identifierStart(character) -> ::consumeIdentifier
      integerStart(character) -> ::consumeInteger
      operatorStart(character) -> ::consumeOperator
      commentStartOrDivisionOperator(character) -> ::consumeCommentOrDivisionOperator
      else -> null
    }

fun tokenStart(code: CodeBuffer, position: Position, character: Char): Response<TokenStep> {
  val branch = branchTokenStart(character)
  return if (branch == null)
    failure(listOf(ParsingError(TextId.unexpectedCharacter, Range(position))))
  else
    branch(Bundle(
        code = code,
        start = position,
        end = nextPosition(character, position),
        buffer = newLexicalBuffer(character)
    ))
}

fun tokenStart(code: CodeBuffer): (Position) -> Response<TokenStep> = { position ->
  val character = nextCharacter(code, position.index)
  if (character != null) {
    singleCharacterTokenMatch(position, character)
        ?: tokenStart(code, position, character)
  } else
    success(TokenStep(position))
}

fun nextToken(code: CodeBuffer, position: Position): Response<TokenStep> {
  return tokenStart(code)(consumeSingleLineWhitespace(Bundle(code, position, position)))
}

tailrec fun tokenize(code: CodeBuffer, position: Position, tokens: Tokens): Response<Tokens> {
  val result = nextToken(code, position)
  return when (result) {
    is Response.Failure -> failure(result.errors)
    is Response.Success -> {
      val (newPosition, token) = result.value
      if (token == null)
        success(tokens)
      else
        tokenize(code, newPosition, tokens.plus(token))
    }
  }
}

fun tokenize(code: CodeBuffer): Response<Tokens> =
    tokenize(code, position = newPosition(), tokens = listOf())
