package silentorb.imp.core

data class Namespace(
    val connections: Connections,
    val implementationTypes: Map<PathKey, TypeHash>,
    val returnTypes: Map<PathKey, TypeHash>,
    val values: Map<PathKey, Any>,
    val typings: Typings
) {
  val nodes: Set<PathKey>
    get() =
//      connections
//          .flatMap { listOf(it.value, it.key.destination) }
      implementationTypes.keys
          .plus(returnTypes.keys)
//          .filter { !typings.signatures.containsKey(returnTypes[it]) }
          .toSet()

  operator fun plus(other: Namespace): Namespace =
      mergeNamespaces(this, other)
}

typealias Graph = Namespace

fun newNamespace(): Namespace =
    Namespace(
        connections = mapOf(),
        implementationTypes = mapOf(),
        returnTypes = mapOf(),
        values = mapOf(),
        typings = newTypings()
    )

fun mergeNamespaces(first: Namespace, second: Namespace): Namespace =
    Namespace(
        connections = first.connections + second.connections,
        implementationTypes = first.implementationTypes + second.implementationTypes,
        returnTypes = first.returnTypes + second.returnTypes,
        typings = mergeTypings(first.typings, second.typings),
        values = first.values + second.values
    )

fun mergeNamespaces(namespaces: Collection<Namespace>): Namespace =
    namespaces.reduce(::mergeNamespaces)

fun mergeNamespaces(vararg namespaces: Namespace): Namespace =
    mergeNamespaces(namespaces.toList())

typealias Context = List<Namespace>

fun toPathString(list: List<String>) =
    list.joinToString(".")

fun toPathKey(list: List<String>) =
    PathKey(toPathString(list.dropLast(1)), list.takeLast(1).first())

typealias ContextIterator<K, V> = (Context, K) -> V?

tailrec fun <V> resolveContextField(context: Context, index: Int, getter: (Namespace) -> V?): V? =
    if (index < 0)
      null
    else
      getter(context[index])
          ?: resolveContextField(context, index - 1, getter)

fun <V> resolveContextField(getter: (Namespace) -> V?): (Context) -> V? = { context ->
  resolveContextField(context, context.size - 1, getter)
}

tailrec fun <V> resolveContextFieldGreedy(
    context: Context, index: Int, getter: (Namespace) -> List<V>,
    accumulator: List<V>
): List<V> =
    if (index < 0)
      accumulator
    else {
      val next = accumulator + getter(context[index])
      resolveContextFieldGreedy(context, index - 1, getter, next)
    }

fun <V> resolveContextFieldGreedy(context: Context, getter: (Namespace) -> List<V>): List<V> =
    resolveContextFieldGreedy(context, context.size - 1, getter, listOf())

tailrec fun <K, V> resolveContextFieldMap(
    context: Context, index: Int, getter: (Namespace) -> Map<K, V>,
    accumulator: Map<K, V>
): Map<K, V> =
    if (index < 0)
      accumulator
    else {
      val next = accumulator + getter(context[index])
      resolveContextFieldMap(context, index - 1, getter, next)
    }

fun <K, V> resolveContextFieldMap(context: Context, getter: (Namespace) -> Map<K, V>): Map<K, V> =
    resolveContextFieldMap(context, context.size - 1, getter, mapOf())

tailrec fun <V> resolveContextFieldGreedySet(
    context: Context, index: Int, getter: (Namespace) -> Set<V>,
    accumulator: Set<V>
): Set<V> =
    if (index < 0)
      accumulator
    else {
      val next = accumulator + getter(context[index])
      resolveContextFieldGreedySet(context, index - 1, getter, next)
    }

fun <V> resolveContextFieldGreedySet(context: Context, getter: (Namespace) -> Set<V>): Set<V> =
    resolveContextFieldGreedySet(context, context.size - 1, getter, setOf())

fun <V> resolveContextField(context: Context, getter: (Namespace) -> V?): V? =
    resolveContextField(context, context.size - 1, getter)

fun getReturnTypes(context: Context, path: String): Map<PathKey, TypeHash> =
    resolveContextFieldMap(context) { namespace ->
      namespace.returnTypes.filter { it.key.path == path }
    }

fun getReturnType(context: Context, key: PathKey): TypeHash? =
    resolveContextField(context) { namespace ->
      namespace.returnTypes[key]
    }

fun getImplementationTypes(context: Context, path: String): Map<PathKey, TypeHash> =
    resolveContextFieldMap(context) { namespace ->
      namespace.implementationTypes.filter { it.key.path == path }
    }

tailrec fun resolveReference(context: Context, name: String, index: Int): PathKey? =
    if (index < 0)
      null
    else {
      val nodes = context[index].returnTypes.keys.filter { it.name == name }
          .plus(context[index].connections.keys.filter { it.destination.name == name }.map { it.destination })
          .distinct()

      if (nodes.size > 1)
        throw Error("Not yet supported")

      nodes.firstOrNull() ?: resolveReference(context, name, index - 1)
    }

fun resolveReference(context: Context, name: String): PathKey? =
    resolveReference(context, name, context.size - 1)

fun getTypeAlias(context: Context, key: TypeHash): TypeHash? =
    resolveContextField(context) { namespace -> namespace.typings.typeAliases[key] }

fun getTypeSignature(context: Context, key: TypeHash): Signature? =
    resolveContextField(context) { namespace -> namespace.typings.signatures[key] }

fun getTypeUnion(context: Context, key: TypeHash): Union? =
    resolveContextField(context) { namespace -> namespace.typings.unions[key] }

fun getValue(context: Context, key: PathKey): Any? =
    resolveContextField(context) { namespace -> namespace.values[key] }

fun getSymbolType(context: Context, name: String): TypeHash? =
    typesToTypeHash(
        resolveContextFieldGreedy(context) { namespace ->
          namespace.returnTypes.filterKeys { it.name == name }.values.toList()
        }
    )

fun getImplementationType(context: Context, name: String): TypeHash? =
    typesToTypeHash(
        resolveContextFieldGreedy(context) { namespace ->
          namespace.implementationTypes.filterKeys { it.name == name }.values.toList()
        }
    )

fun getPathKeyTypes(context: Context, key: PathKey): List<TypeHash> {
  return resolveContextFieldGreedy(context) { namespace ->
    listOfNotNull(namespace.returnTypes[key])
  }
}

fun getPathKeyImplementationTypes(context: Context, key: PathKey): List<TypeHash> {
  return resolveContextFieldGreedy(context) { namespace ->
    listOfNotNull(namespace.implementationTypes[key])
  }
}

fun getImplementationType(context: Context, key: PathKey): TypeHash? {
  return resolveContextField(context) { namespace ->
    namespace.implementationTypes[key]
  }
}

fun getTypeSignatures(context: Context): (TypeHash) -> List<Signature> = { type ->
  resolveContextFieldGreedy(context) { namespace ->
    val signature = namespace.typings.signatures[type]
    if (signature != null)
      listOf(signature)
    else {
      val union = namespace.typings.unions[type]
      union?.flatMap(getTypeSignatures(context)) ?: listOf()
    }
  }
}

//fun getTypeSignatures(context: Context, pathKey: PathKey): List<Signature> {
//  val types = getPathKeyTypes(context, pathKey)
//  return types
//      .flatMap(getTypeSignatures(context))
//      .distinct()
//}

fun resolveNumericTypeConstraint(key: TypeHash) =
    resolveContextField { namespace -> namespace.typings.numericTypeConstraints[key] }

fun namespaceFromOverloads(functions: OverloadsMap): Namespace {
  return newNamespace().copy(
      returnTypes = functions.mapValues { signaturesToTypeHash(it.value) },
      typings = extractTypings(functions.values)
  )
}

fun namespaceFromCompleteOverloads(signatures: Map<PathKey, List<CompleteSignature>>): Namespace {
  val namespace = namespaceFromOverloads(signatures.mapValues { it.value.map(::convertCompleteSignature) })
  val extractedTypings = signatures.values
      .flatten()
      .fold(mapOf<TypeHash, PathKey>()) { a, signature ->
        a + signature.parameters
            .associate { Pair(it.type.hash, it.type.key) }
            .plus(signature.output.hash to signature.output.key)
      }
  return namespace
      .copy(
          implementationTypes = namespace.implementationTypes + extractedTypings.entries.associate { Pair(it.value, it.key) },
          typings = namespace.typings.copy(
              typeNames = namespace.typings.typeNames + extractedTypings
          )
      )
}

fun getTypeNameOrNull(context: Context, type: TypeHash, step: Int = 0): PathKey? {
  return if (step > 50) {
    PathKey("", "infinite-recursion")
  } else {
    val directName = resolveContextField(context) { namespace ->
      namespace.typings.typeNames[type]
    }
    if (directName != null)
      directName
    else {
      val signature = getTypeSignature(context, type)
      if (signature != null) {
        PathKey("",
            signature.parameters.map { parameter ->
              "${parameter.name}: ${getTypeNameOrUnknown(context, parameter.type, step + 1)}"
            }
                .plus(listOf(getTypeNameOrUnknown(context, signature.output, step + 1)))
                .joinToString(" -> ")
        )
      } else {
        val union = getTypeUnion(context, type)
        if (union != null) {
          PathKey("",
              union
                  .map { option -> getTypeNameOrUnknown(context, option, step + 1) }
                  .joinToString(" -> ")
          )
        } else
          null
      }
    }
  }
}

fun getTypeNameOrUnknown(context: Context, type: TypeHash, step: Int = 0): PathKey =
    getTypeNameOrNull(context, type, step) ?: unknownType.key

fun getInputConnections(context: Context, key: PathKey): Connections {
  return resolveContextFieldMap(context) { namespace ->
    namespace.connections.filter { it.key.destination == key }
  }
}

fun getArgumentConnections(context: Context, key: PathKey): Connections {
  return resolveContextFieldMap(context) { namespace ->
    namespace.connections.filter { it.key.destination == key && it.key.parameter != defaultParameter }
  }
}

fun getConnection(context: Context, input: Input): PathKey? {
  return resolveContextField(context) { namespace ->
    namespace.connections[input]
  }
}
