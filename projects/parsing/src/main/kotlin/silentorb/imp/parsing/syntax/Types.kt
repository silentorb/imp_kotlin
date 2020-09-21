package silentorb.imp.parsing.syntax

import silentorb.imp.core.FileRange
import silentorb.imp.core.Range
import silentorb.imp.core.TokenFile
import silentorb.imp.parsing.general.Token

data class Burg(
    val type: BurgType,
    val range: Range,
    val children: List<Burg>,
    val value: Any? = null
) {
  val file: TokenFile get() = range.start.file
  val fileRange: FileRange get() = FileRange(file, range)

  init {
    if (file != range.start.file) {
      val k = 0
    }
  }
}

typealias BurgId = Int

typealias Roads = Map<BurgId, List<BurgId>>

data class Realm(
    val root: Burg,
    val burgs: Set<Burg>
) {
//  val roads: Roads get() = burgs.mapValues { it.value.children }
}

data class PendingParsingError(
    val message: Any,
    val range: Range
)

//data class ParsingStep(
//    val transition: ParsingStateTransition,
//    val mode: ParsingMode? = null,
//    val consume: Boolean = true
//)
typealias ValueTranslator = (String) -> Any?

typealias NewBurg = (BurgType, ValueTranslator) -> Burg
typealias ParsingStateTransition = (NewBurg, ParsingState) -> ParsingState
typealias ParsingStep = ParsingStateTransition

typealias TokenToParsingTransition = (Token) -> ParsingStep
typealias ContextualTokenToParsingTransition = (Token, ContextMode) -> ParsingStep
typealias NullableTokenToParsingTransition = (Token) -> ParsingStep?
typealias NullableContextualTokenToParsingTransition = (Token, ContextMode) -> ParsingStep?

typealias Stack<T> = List<List<T>>

data class BurgLayer(
    val burgs: List<Burg> = listOf(),
    val type: Any? = null
)

typealias BurgStack = List<BurgLayer>

typealias TokenPattern = (Token) -> Boolean
