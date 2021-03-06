package silentorb.imp.parsing.general

enum class TextId {

  // Error messages
  ambiguousOverload,
  badArgument,
  circularDependency,
  duplicateSymbol,
  expectedAssignment,
  expectedColon,
  expectedCommaOrAssignment,
  expectedExpression,
  expectedIdentifier,
  expectedIdentifierOrWildcard,
  expectedImportOrLetKeywords,
  expectedLetKeyword,
  expectedNewline,
  expectedParameterName,
  expectedParameterNameOrAssignment,
  expectedPeriodOrNewline,
  importNotFound,
  incompleteParameter,
  invalidToken,
  missingClosingParenthesis,
  missingOpeningParenthesis,
  missingArgumentName,
  missingArgumentExpression,
  missingExpression,
  missingImportPath,
  missingLefthandExpression,
  missingRighthandExpression,
  multipleGraphOutputs,
  noGraphOutput,
  noMatchingSignature,
  outsideTypeRange,
  unexpectedCharacter,
  unknownFunction,
}
