package silentorb.imp.parsing.parser

import silentorb.imp.core.*
import silentorb.imp.parsing.general.ParsingResponse
import silentorb.imp.parsing.parser.expressions.parseExpression

fun newParameterNamespace(context: Context, pathKey: PathKey, parameters: List<Parameter>): Namespace {
  val pathString = pathKeyToString(pathKey)
  val nodeTypes = parameters.associate { parameter ->
    Pair(PathKey(pathString, parameter.name), parameter.type)
  }
  return newNamespace()
      .copy(
          returnTypes = nodeTypes,
          typings = newTypings()
              .copy(
                  typeNames = nodeTypes.values
                      .associateWith { getTypeNameOrUnknown(context, it) }
              )
      )
}

fun prepareDefinitionFunction(
    graph: Graph,
    parameters: List<Parameter>,
    output: PathKey,
    outputType: TypeHash,
    key: PathKey,
    dungeon: Dungeon
): Dungeon {
  val signature = Signature(
      parameters = parameters,
      output = outputType
  )
  val definitionType = signature.hashCode()
  val typings = graph.typings.copy(
      signatures = graph.typings.signatures + (signature.hashCode() to signature)
  )
  val implementation = graph.copy(
      connections = graph.connections + (Input(
          destination = key,
          parameter = defaultParameter
      ) to output),
      returnTypes = graph.returnTypes + (key to outputType),
      typings = typings
  )
  return dungeon.copy(
      graph = newNamespace().copy(
          returnTypes = mapOf(key to definitionType),
          typings = typings
      ),
      implementationGraphs = mapOf(
          FunctionKey(key, definitionType) to implementation
      )
  )
}

fun parseDefinitionSecondPass(namespaceContext: Context, largerContext: Context, definition: DefinitionFirstPass): ParsingResponse<Dungeon> {
  val parameters = definition.tokenized.parameters.map { parameter ->
    val type = getImplementationType(namespaceContext, parameter.type.value as String)
        ?: unknownType.hash
    Parameter(parameter.name.value as String, type)
  }
  val parameterNamespace = if (parameters.any()) {
    newParameterNamespace(namespaceContext, definition.key, parameters)
  } else
    null

  val localContext = namespaceContext + listOfNotNull(parameterNamespace)

  val (dungeon, expressionErrors) = parseExpression(localContext, largerContext, definition.intermediate)

  val output = getGraphOutputNode(dungeon.graph)

  val nextDungeon = if (output != null) {
    val outputType = dungeon.graph.returnTypes[output]!!
    if (parameters.any()) {
      prepareDefinitionFunction(
          graph = parameterNamespace!! + dungeon.graph,
          parameters = parameters,
          output = output,
          outputType = outputType,
          key = definition.key,
          dungeon = dungeon
      )
    } else {
      val graph = dungeon.graph
      dungeon.copy(
          graph = graph.copy(
              connections = graph.connections + (Input(
                  destination = definition.key,
                  parameter = defaultParameter
              ) to output),
              returnTypes = graph.returnTypes + (definition.key to outputType)
          )
      )
    }
  } else
    dungeon

  return ParsingResponse(
      nextDungeon,
      expressionErrors
  )
}
