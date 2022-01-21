package com.craftinginterpreters.jlox;

enum TokenType {
	// Single-character tokens.
	LEFT_PAREN, // (
	RIGHT_PAREN, // )
	LEFT_BRACE, // [
	RIGHT_BRACE, // ]
	LEFT_CURLY, // {
	RIGHT_CURLY, // }
	COMMA, // ,
	DOT, // .
	MINUS, // -
	PLUS, // +
	SEMICOLON, // ;
	COLON, // :
	SLASH, // /
	STAR, // *
	QUESTION, // ?

	// One or two character tokens.
	BANG, // !
	BANG_EQUAL, // !=
	EQUAL, // =
	EQUAL_EQUAL, // ==
	GREATER, // >
	GREATER_EQUAL, // >=
	LESSER, // <
	LESSER_EQUAL, // <=
	LESSER_MINUS, // <-
	PLUS_PLUS, // ++
	MINUS_MINUS, // --
	MINUS_MINUS_POST, // -- // Operator for postfix removal
	STAR_EQUAL, // *=
	SLASH_EQUAL, // /=
	MINUS_EQUAL, // -=
	PLUS_EQUAL, // +=

	// Literals.
	IDENTIFIER,
	STRING,
	NUMBER,

	// Keywords.
	AND,
	BREAK,
	CATCH,
	CLASS,
	DO,
	ELSE,
	FALSE,
	FOR,
	FUNC,
	IF,
	LET,
	NIL,
	OR,
	PANIC,
	PRINT,
	RETURN,
	STATIC,
	SUPER,
	THIS,
	TRUE,
	TRY,
	VAR,
	WHILE,

	EOF
}
