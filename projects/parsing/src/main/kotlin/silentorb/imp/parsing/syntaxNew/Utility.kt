package silentorb.imp.parsing.syntaxNew

import silentorb.imp.core.ImpError
import silentorb.imp.core.ImpErrors
import silentorb.imp.parsing.general.TextId
import silentorb.imp.parsing.general.Token
import silentorb.imp.parsing.general.Tokens
import silentorb.imp.parsing.syntax.BurgType
import silentorb.imp.parsing.syntax.ValueTranslator
import silentorb.imp.parsing.syntax.asString

operator fun ParsingFunction.plus(other: ParsingFunction): ParsingFunction = { tokens ->
  val first = this(tokens)
  val second = other(first.tokens)
  first + second
}

tailrec fun reduceParsers(
    functions: Collection<ParsingFunction>,
    tokens: Tokens,
    burgs: List<NestedBurg> = listOf(),
    errors: ImpErrors = listOf()
): ParsingResponse =
    if (functions.none())
      ParsingResponse(tokens, burgs, errors)
    else {
      val next = functions.first()
      val step = next(tokens)
      val (nextTokens, newBurgs, newErrors) = step
      reduceParsers(functions.drop(1), nextTokens, burgs + newBurgs, errors + newErrors)
    }

fun wrapResponseList(burgType: BurgType, tokens: Tokens, response: ParsingResponse): ParsingResponse {
  val (nextTokens, children, errors) = response
  val token = tokens.first()
  val end = children.maxByOrNull { it.range.end.index }?.range?.end ?: token.range.end
  return ParsingResponse(
      nextTokens,
      listOf(newNestedBurg(burgType, children)),
      errors
  )
}

fun wrap(burgType: BurgType, vararg functions: ParsingFunction): ParsingFunction = { tokens ->
  val response = reduceParsers(functions.toList(), tokens)
  wrapResponseList(burgType, tokens, response)
}

val consume: ParsingFunction = { tokens ->
  ParsingResponse(tokens.drop(1))
}

val exitLoop: ParsingFunction = { tokens ->
  ParsingResponse(tokens, exitLoop = true)
}

fun addError(message: TextId): ParsingFunction {
  return { tokens ->
    ParsingResponse(tokens.drop(1), errors = listOf(ImpError(message, tokens.firstOrNull()?.fileRange)))
  }
}

fun route(tokens: Tokens, router: Router): ParsingResponse =
    if (tokens.none())
      ParsingResponse(tokens, listOf(), listOf(ImpError(TextId.expectedExpression)))
    else
      router(tokens.first())(tokens)

fun route(router: (Token) -> ParsingFunction): ParsingFunction = { tokens ->
  route(tokens, router)
}

fun consumeToken(burgType: BurgType, translator: ValueTranslator = asString): ParsingFunction = { tokens ->
  val token = tokens.first()
  ParsingResponse(tokens.drop(1), listOf(newNestedBurg(burgType, token, value = translator(token.value))))
}

fun consumeExpected(condition: (Token) -> Boolean, errorMessage: TextId, function: ParsingFunction = consume): ParsingFunction = { tokens ->
  val token = tokens.first()
  if (condition(token))
    function(tokens)
  else
    ParsingResponse(tokens, listOf(), listOf(ImpError(errorMessage, tokens.firstOrNull()?.fileRange)))
}

fun parsingLoop(
    function: ParsingFunction,
    tokens: Tokens,
    burgs: List<NestedBurg> = listOf(),
    errors: ImpErrors = listOf()
): ParsingResponse {
  val (nextTokens, newBurgs, newErrors, endLoop) = function(tokens)
  val nextBurgs = burgs + newBurgs
  val nextErrors = errors + newErrors
  return if (endLoop || nextTokens.none())
    ParsingResponse(nextTokens, nextBurgs, nextErrors)
  else
    parsingLoop(function, nextTokens, nextBurgs, nextErrors)
}

fun parsingLoop(function: ParsingFunction): ParsingFunction = { tokens -> parsingLoop(function, tokens) }
