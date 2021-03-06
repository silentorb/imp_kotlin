package silentorb.imp.parsing.lexer

tailrec fun consumeBadIdentifier(bundle: Bundle): TokenStep {
  val character = nextCharacter(bundle)
  return if (isValidCharacterAfterIdentifierOrLiteral(character))
    badCharacter(bundle)
  else if (identifierAfterStart(character!!))
    consumeBadIdentifier(incrementBundle(character, bundle))
  else
    badCharacter(bundle)
}

tailrec fun consumeSingleLineWhitespace(bundle: Bundle): TokenStep? {
  val character = consumeSingle(bundle, singleLineWhitespace)
  return if (character == null) {
    if (bundle.end != bundle.start)
      tokenFromBundle(Rune.whitespace)(bundle)
    else
      null
  } else
    consumeSingleLineWhitespace(incrementBundle(character, bundle))
}

// Multiple newlines in a row are grouped together as a single token
tailrec fun consumeNewline(bundle: Bundle): TokenStep {
  val character = consumeSingle(bundle, newLineAfterStart)
  return if (character == null)
    tokenFromBundle(Rune.newline)(bundle)
  else
    consumeNewline(incrementBundle(character, bundle))
}

tailrec fun consumeIdentifier(bundle: Bundle): TokenStep {
  val character = nextCharacter(bundle)
  return if (isValidCharacterAfterIdentifierOrLiteral(character))
    tokenFromBundle(Rune.identifier)(bundle)
  else if (identifierAfterStart(character!!))
    consumeIdentifier(incrementBundle(character, bundle))
  else
    consumeBadIdentifier(bundle)
}

tailrec fun consumeOperator(bundle: Bundle): TokenStep {
  val character = consumeSingle(bundle, operatorAfterStart)
  return if (character == null)
    tokenFromBundle(Rune.operator)(bundle)
  else
    consumeOperator(incrementBundle(character, bundle))
}

tailrec fun consumeComment(bundle: Bundle): TokenStep {
  val character = nextCharacter(bundle)
  return if (character == null || newLineStart(character))
    tokenFromBundle(Rune.comment)(bundle)
  else
    consumeComment(incrementBundle(character, bundle))
}

fun consumeCommentOrHyphenOrNegativeNumber(bundle: Bundle): TokenStep {
  val character = nextCharacter(bundle)
  return if (character == '-')
    consumeComment(incrementBundle(character, bundle))
  else if (character != null && integerStart(character))
    consumeInteger(bundle)
  else
    consumeOperator(bundle)
}

tailrec fun consumeFloatAfterDot(bundle: Bundle): TokenStep {
  val character = nextCharacter(bundle)
  return if (isValidCharacterAfterIdentifierOrLiteral(character))
    tokenFromBundle(Rune.literalFloat)(bundle)
  else if (floatAfterDot(character!!))
    consumeFloatAfterDot(incrementBundle(character, bundle))
  else
    consumeBadIdentifier(bundle)
}

tailrec fun consumeInteger(bundle: Bundle): TokenStep {
  val character = nextCharacter(bundle)
  return if (character == dot)
    consumeFloatAfterDot(incrementBundle(character, bundle))
  else if (isValidCharacterAfterIdentifierOrLiteral(character))
    tokenFromBundle(Rune.literalInteger)(bundle)
  else if (integerAfterStart(character!!))
    consumeInteger(incrementBundle(character, bundle))
  else
    consumeBadIdentifier(bundle)
}

fun consumeLiteralZero(bundle: Bundle): TokenStep {
  val character = nextCharacter(bundle)
  return if (character == dot)
    consumeFloatAfterDot(incrementBundle(character, bundle))
  else if (isValidCharacterAfterIdentifierOrLiteral(character))
    tokenFromBundle(Rune.literalInteger)(bundle)
  else
    consumeBadIdentifier(bundle)
}

tailrec fun consumeLiteralString(bundle: Bundle): TokenStep {
  val character = nextCharacter(bundle)
  return if (character == quoteCharacter)
    tokenFromBundle(Rune.literalString)(skipCharacter(character, bundle))
  else if (character == null || newLineCharacters.contains(character))
    consumeBadIdentifier(bundle)
  else
    consumeLiteralString(incrementBundle(character, bundle))
}

// Clear the buffer to keep the quotation mark out of the captured string value
fun consumeLiteralStringStart(bundle: Bundle): TokenStep =
    consumeLiteralString(bundle.copy(buffer = ""))
