package silentorb.imp.library.standard.math

import silentorb.imp.core.*

const val mathPath = "$standardLibraryPath.math"

val plusKey = PathKey(mathPath, "+")

fun mathOperatorSignature(type: TypeHash) = Signature(
    parameters = listOf(
        Parameter("first", type),
        Parameter("second", type)
    ),
    output = type
)

val intOperatorSignature = mathOperatorSignature(intType.hash)
val floatOperatorSignature = mathOperatorSignature(floatType.hash)
val doubleOperatorSignature = mathOperatorSignature(doubleType.hash)

fun standardMathOperatorDefinition() =
    listOf(
        intOperatorSignature,
        floatOperatorSignature,
        doubleOperatorSignature
    )

fun mathOperators(): OverloadsMap {
  val definition = standardMathOperatorDefinition()
  return setOf(plusKey)
      .associateWith { definition }
}
