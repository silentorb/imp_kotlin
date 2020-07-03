package silentorb.imp.parsing.lexer

enum class Rune {
  assignment,
  bad,
  braceClose,
  braceOpen,
  colon,
  comma,
  comment,
  dot,
  eof,
  identifier,
  keyword,
  literalFloat,
  literalInteger,
  newline,
  operator,
  parenthesesClose,
  parenthesesOpen,
  whitespace,
  wildcard,
}
