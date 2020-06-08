package silentorb.imp.parsing.structureOld

import silentorb.imp.parsing.resolution.TokenIndex

data class TokenGroup(
    val depth: Int,
    val children: List<TokenIndex>
)

typealias TokenGroups = Map<TokenIndex, TokenGroup>

fun accumulate(top: List<TokenIndex>, depth: Int, accumulator: TokenGroups): TokenGroups {
  return accumulator.plus(top.first() to TokenGroup(depth, top.drop(1)))
}

fun appendToStack(tokenIndex: TokenIndex, stack: List<List<TokenIndex>>) =
    stack.dropLast(1).plusElement(stack.last().plus(tokenIndex))

//fun updateStack(rune: Rune, tokenIndex: TokenIndex, stack: List<List<TokenIndex>>) =
//    when (rune) {
//      Rune.parenthesesOpen -> stack.plusElement(listOf())
//      Rune.parenthesesClose -> {
//        val lastTwo = stack.takeLast(2)
//        val returnValue = lastTwo.last().first()
//        stack.dropLast(2).plusElement(lastTwo.first().plus(returnValue))
//      }
//      // Skip pipe operators without a preceding expression
//      // It's easier to ignore them now and detect the errors in later integrity checks
//      // than to include them and sort out the side effects later
//      Rune.dot -> if (stack.last().any())
//        appendToStack(tokenIndex, stack)
//      else
//        stack
//      else -> appendToStack(tokenIndex, stack)
//    }
//
//tailrec fun groupTokens(
//    tokenIndex: TokenIndex,
//    tokens: Tokens,
//    accumulator: TokenGroups,
//    stack: List<List<TokenIndex>>
//): TokenGroups {
//  val token = tokens[tokenIndex]
//  val nextStack = updateStack(token.rune, tokenIndex, stack)
//  val isAtEnd = tokenIndex == tokens.size - 1
//  val nextAccumulator = if (token.rune == Rune.parenthesesClose)
//    accumulate(stack.last(), stack.size, accumulator)
//  else
//    accumulator
//
//  return if (isAtEnd)
//    accumulate(nextStack.last(), nextStack.size, nextAccumulator)
//  else
//    groupTokens(tokenIndex + 1, tokens, nextAccumulator, nextStack)
//}
//
//fun groupTokens(tokens: Tokens) =
//    groupTokens(0, tokens, mapOf(), listOf(listOf()))
//
//fun newGroupingGraph(groups: TokenGroups): TokenGraph =
//    TokenGraph(
//        parents = groups.mapValues { it.value.children },
//        stages = groups.entries
//            .groupBy { it.value.depth }
//            .toList()
//            .sortedByDescending { it.first }
//            .map { it.second.map { it.key } }
//    )