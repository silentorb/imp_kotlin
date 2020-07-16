package silentorb.imp.parsing.resolution

import silentorb.imp.core.*
import silentorb.imp.core.Response
import silentorb.imp.parsing.parser.validateFunctionTypes
import silentorb.imp.parsing.parser.validateSignatures

fun resolveExpression(
    context: Context,
    largerContext: Context,
    intermediate: IntermediateExpression
): Response<Dungeon> {
  val (
      applications,
      literalTypes,
      namedArguments,
      nodeMap,
      parents,
      references,
      stages,
      values
  ) = intermediate

//  val referencePairs = references
//      .flatMap { (typeName, referenceNodes) ->
//        val types = getSymbolTypes(context, typeName)
//            .entries
//            .associate { it.value to it.key }
//        val type = typesToTypeHash(types.keys) ?: unknownType.hash
//        referenceNodes.map { Pair(it, Pair(type, types)) }
//      }
//      .associate { it }

//  val referenceTypes = referencePairs
//      .mapValues { it.value.first }

//  val unionTypes = referencePairs
//      .filter { it.value.second.size > 1 }.entries
//      .associate { (_, value) ->
//        value.first to value.second.keys
//      }

//  val appendedContext = if (unionTypes.any())
//    largerContext + newNamespace().copy(typings = newTypings().copy(unions = unionTypes))
//  else
//    largerContext

  val initialTypes = literalTypes
  val (signatureOptions, reducedTypes, typings) =
      resolveFunctionSignatures(
          context,
          largerContext,
          stages,
          applications,
          initialTypes,
          namedArguments
      )
  val signatures = signatureOptions
      .filter { it.value.size == 1 }
      .mapValues { it.value.first() }
  val connections = arrangeConnections(parents, signatures)
      .plus(
          applications.map { (key, application) ->
            Input(
                destination = key,
                parameter = defaultParameter
            ) to application.target
          }
      )

//  val referenceConnections = referencePairs.entries
//      .mapNotNull { (key, reference) ->
//        val target = if (reference.second.size < 2)
//          reference.second.values.firstOrNull()
//        else {
//          val options = reference.second
//          val application = applications.entries.firstOrNull { it.value.target == key }
//          val signature = signatures[application?.key]?.signature
//          options[signature.hashCode()]
//        }
//        if (target != null)
//          Input(key, defaultParameter) to target
//        else
//          null
//      }
//      .associate { it }

  val nodeTypes = initialTypes + reducedTypes
  val referenceValues = connections
      .filter { it.key.parameter == defaultParameter }
      .mapNotNull { (destination, source) ->
        val nodeType = nodeTypes[destination.destination]
        val referenceValue = getValue(largerContext, source.copy(type = nodeType)) ?: getValue(largerContext, source)
        if (referenceValue != null)
          destination.destination to referenceValue
        else
          null
      }
      .associate { it }

  val nonNullaryFunctions = parents.filter { it.value.any() }
  val typeResolutionErrors = validateFunctionTypes(
      setOf(),
      mapOf(),
      nodeMap)
  val signatureErrors = validateSignatures(largerContext, nodeTypes, nonNullaryFunctions, signatureOptions, nodeMap)// +
  val errors = signatureErrors + typeResolutionErrors

//  if (referenceConnections.size == referencePairs.size || errors.any()) {
//    val k = 0
//  }
//  val temp = nodeTypes.values
//      .filter { type -> getTypeSignature(largerContext, type) ?: typings.signatures[type] != null }
//  assert(temp.size == nodeTypes.size || errors.any())

  val dungeon = emptyDungeon.copy(
      namespace = newNamespace().copy(
          connections = connections,
          nodeTypes = nodeTypes,
          typings = typings,
          values = values + referenceValues
      ),
      nodeMap = nodeMap
  )
  return Response(dungeon, errors)
}
