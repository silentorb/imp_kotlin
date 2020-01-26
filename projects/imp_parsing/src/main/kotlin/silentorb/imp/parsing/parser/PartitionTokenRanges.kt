package silentorb.imp.parsing.parser

import silentorb.imp.core.Context
import silentorb.imp.parsing.general.*
import silentorb.imp.parsing.lexer.Rune

fun peek(tokens: Tokens, position: Int): (Int) -> Token? = { offset ->
  tokens.getOrNull(position + offset)
}

data class ImportRange(
    val start: Int,
    val end: Int
)

data class DefinitionRange(
    val symbol: Int,
    val expressionStart: Int,
    val expressionEnd: Int
)

fun extractImportTokens(tokens: Tokens): (ImportRange) -> TokenizedImport = { range ->
  TokenizedImport(
      path = tokens.subList(range.start + 1, range.end) // Skip the "import" token
  )
}

fun extractDefinitionTokens(tokens: Tokens): (DefinitionRange) -> TokenizedDefinition = { range ->
  TokenizedDefinition(
      symbol = tokens[range.symbol],
      expression = tokens.subList(range.expressionStart, range.expressionEnd)
  )
}

typealias TokenFilter = (Token) -> Boolean

val isImportToken: TokenFilter = { token ->
  token.rune == Rune.identifier && token.value == "import"
}

val isAssignmentToken: TokenFilter = { token ->
  token.rune == Rune.assignment
}

val isImportTerminator: TokenFilter = { token ->
  token.rune == Rune.newline
}

val isDefinitionTerminator: TokenFilter = { token ->
  token.rune == Rune.newline
}

fun partitionImports(tokens: Tokens): Response<List<ImportRange>> {
  val importTokenIndices = filterIndicies(tokens, isImportToken)
  val imports = importTokenIndices.map { importTokenIndex ->
    val end = nextIndexOf(tokens, importTokenIndex, isImportTerminator) ?: tokens.size - 1
    ImportRange(
        start = importTokenIndex,
        end = end
    )
  }
  return success(imports)
}

fun partitionDefinitions(tokens: Tokens): Response<List<DefinitionRange>> {
  val assignmentTokenIndices = filterIndicies(tokens, isAssignmentToken)
  val entries = assignmentTokenIndices.map { step ->
    val token = tokens[step]
    val peek = peek(tokens, step)
    val neighbor = peek(-2)
    val symbol = peek(-1)
    val firstExpressionToken = peek(1)
    fun formatError(condition: Boolean, textId: TextId, errorToken: Token?) =
        if (condition) null else newParsingError(textId, errorToken ?: token)

    val newErrors = listOfNotNull(
        formatError(symbol?.rune == Rune.identifier, TextId.expectedIdentifier, symbol),
        formatError(neighbor?.rune == Rune.newline || neighbor?.rune == null, TextId.expectedNewline, neighbor),
        formatError(firstExpressionToken?.rune != Rune.newline && firstExpressionToken?.rune != null, TextId.expectedExpression, firstExpressionToken)
    )

    val expressionStart = step + 1
    val terminatorMatchIndex = nextIndexOf(tokens, step + 1, isImportTerminator)
    val expressionEnd = if (terminatorMatchIndex != null)
      terminatorMatchIndex - 1
    else
      tokens.size

    val newDefinition = DefinitionRange(
        symbol = step,
        expressionStart = expressionStart,
        expressionEnd = expressionEnd
    )

    Pair(newErrors, newDefinition)
  }

  val errors = entries.flatMap { it.first }

  return if (errors.any())
    failure(errors)
  else
    success(entries.map { it.second })
}

fun toTokenizedGraph(
    tokens: Tokens,
    importRanges: List<ImportRange>,
    definitionRanges: List<DefinitionRange>
): Response<TokenizedGraph> {
  val imports = importRanges.map(extractImportTokens(tokens))
  val definitions = definitionRanges.map(extractDefinitionTokens(tokens))
  return checkImportTokens(imports)
      .then { checkDefinitionTokens(definitions) }
      .map {
        TokenizedGraph(
            imports = imports,
            definitions = definitions
        )
      }
}

fun getDefinitionsRangeStart(tokens: Tokens, definitionRanges: List<DefinitionRange>): Int =
    definitionRanges.minBy { it.symbol }?.symbol ?: tokens.size - 1

fun parseTokens(context: Context, tokens: Tokens): Response<Dungeon> =
    partitionDefinitions(tokens)
        .then { definitionRanges ->
          val importRangeMax = getDefinitionsRangeStart(tokens, definitionRanges)
          partitionImports(tokens.take(importRangeMax))
              .then { importRanges ->
                toTokenizedGraph(tokens, importRanges, definitionRanges)
              }
        }
        .then(parseDungeon(context))

fun parseTokens(context: Context): (Tokens) -> Response<Dungeon> = { tokens ->
  if (tokens.none())
    success(emptyDungeon())
  else
    parseTokens(context, tokens)
}
