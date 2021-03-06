package silentorb.imp.parsing.parser

import silentorb.imp.core.*
import silentorb.imp.parsing.general.*
import silentorb.imp.parsing.resolution.FunctionApplication
import java.nio.file.Path

fun validateFunctionTypes(referenceOptions: Map<PathKey, Map<PathKey, TypeHash>>, nodeMap: NodeMap): ImpErrors {
  return referenceOptions
      .filter { it.value.none() }
      .map { (node, _) ->
        val fileRange = nodeMap[node]!!
        newParsingError(TextId.unknownFunction, fileRange)
      }
}

fun getArgumentTypeNames(
    context: Context,
    applications: Map<PathKey, FunctionApplication>,
    types: Map<PathKey, TypeHash>,
    parents: Map<PathKey, List<PathKey>>,
    node: PathKey
): List<String> {
  val application = applications.entries.firstOrNull { it.value.target == node }?.key
  val arguments = parents[application]
  return if (arguments == null)
    listOf()
  else {
    arguments.map { argumentKey ->
      val argumentType = types[argumentKey]
      if (argumentType != null)
        getTypeNameOrUnknown(context, argumentType)
      else
        unknownSymbol
    }
  }
}

fun validateSignatures(
    context: Context,
    types: Map<PathKey, TypeHash>,
    referenceOptions: Set<PathKey>,
    parents: Map<PathKey, List<PathKey>>,
    signatureOptions: Map<PathKey, List<SignatureMatch>>,
    applications: Map<PathKey, FunctionApplication>,
    nodeMap: NodeMap
): ImpErrors {
  return referenceOptions
      .mapNotNull { node ->
        val options = signatureOptions[node] ?: listOf()
        if (options.size == 1)
          null
        else if (options.none()) {
          val argumentTypeNames = getArgumentTypeNames(context, applications, types, parents, node)

          val argumentClause = if (argumentTypeNames.any())
            argumentTypeNames.joinToString(", ")
          else
            "Unit"

          ImpError(TextId.noMatchingSignature, fileRange = nodeMap[node]!!, arguments = listOf(argumentClause))
        } else
          ImpError(TextId.ambiguousOverload, fileRange = nodeMap[node]!!)
      }
}

fun isValueWithinConstraint(constraint: NumericTypeConstraint, value: Any): Boolean {
  val doubleValue = when (value) {
    is Double -> value
    is Int -> value.toDouble()
    is Float -> value.toDouble()
    else -> null
  }
  return if (doubleValue == null)
    false
  else
    doubleValue >= constraint.minimum && doubleValue <= constraint.maximum
}

fun validateTypeConstraints(values: Map<PathKey, Any>, context: Context, constraints: ConstrainedLiteralMap, nodeMap: NodeMap): ImpErrors {
  return values.mapNotNull { (node, value) ->
    val constraintType = constraints[node]
    if (constraintType != null) {
      val constraint = resolveNumericTypeConstraint(constraintType)(context)
      if (constraint == null || isValueWithinConstraint(constraint, value))
        null
      else
        newParsingError(TextId.outsideTypeRange, nodeMap[node]!!)
    } else null
  }
}

fun validateUnusedImports(context: Context, importMap: Map<Path, List<TokenizedImport>>, definitions: Map<PathKey, TokenizedDefinition>): ImpErrors {
  val unusedImports = importMap
      .filter { (key, _) ->
        definitions.none { it.value.file == key }
      }

  return unusedImports.flatMap { (_, imports) ->
    imports
        .flatMap { tokenizedImport ->
          parseImport(context)(tokenizedImport).errors
        }
  }
}
