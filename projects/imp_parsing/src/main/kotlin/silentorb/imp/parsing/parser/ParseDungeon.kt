package silentorb.imp.parsing.parser

import silentorb.imp.core.*
import silentorb.imp.parsing.general.*
import silentorb.imp.parsing.lexer.Rune

data class TokenizedDefinition(
    val symbol: Token,
    val expression: Tokens
)

fun parseDungeon(context: Context): (List<TokenizedDefinition>) -> Response<Dungeon> = { definitions ->
  val passThroughNodes = definitions.mapIndexed { index, definition ->
    Pair((index + 1).toLong(), definition)
  }
      .associate { it }

  data class ExpressionInfo(
      val expressionId: Int,
      val owner: String,
      val token: Token
  )

  val expressionResults = definitions.mapIndexed { index, definition ->
    ExpressionInfo(index, definition.symbol.value, parseExpression(context, definition.expression))
  }

  val definitionSymbols = passThroughNodes.map { (id, definition) ->
    Pair(definition.symbol.value, id)
  }
      .associate { it }

  val nodeReferences = expressionResults
      .filter { it.token.rune == Rune.identifier }
      .mapNotNull { expressionInfo ->
        val sourceNode = definitionSymbols[expressionInfo.token.value]
        if (sourceNode != null)
          Pair(sourceNode, expressionInfo.owner)
        else
          null
      }

  val valueSources = expressionResults
      .filter { parseTokenValue(it.token) != null }
      .mapIndexed { index, expressionInfo ->
        val expressionId = (index + passThroughNodes.size + 1).toLong()
        Pair(expressionId, expressionInfo)
      }
      .associate { it }

  val values = valueSources
      .mapValues { (_, value) ->
        parseTokenValue(value.token)!!
      }

  val nodes: Set<Id> = passThroughNodes.keys
      .plus(values.keys)

  val nodeMap = passThroughNodes
      .mapValues { (_, definition) -> definition.symbol.range }
      .plus(valueSources.mapValues { (_, valueInfo) -> valueInfo.token.range })

  val connections = valueSources
      .map { (expressionId, value) ->
        val targetNode = definitionSymbols[value.owner]!!
        Connection(
            source = expressionId,
            destination = targetNode,
            parameter = defaultParameter
        )
      }
      .plus(nodeReferences.map { (source, owner) ->
        val destination = definitionSymbols[owner]!!
        Connection(
            source = source,
            destination = destination,
            parameter = defaultParameter
        )
      })
      .toSet()

  val graph = Graph(
      nodes = nodes,
      connections = connections,
      functions = mapOf(),
      values = values
  )

  checkForGraphErrors(nodeMap)(graph)
      .map { finalGraph ->
        Dungeon(
            graph = finalGraph,
            nodeMap = nodeMap
        )
      }
}
