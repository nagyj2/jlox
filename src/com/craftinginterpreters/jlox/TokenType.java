package com.craftinginterpreters.jlox;

enum TokenType {
	// Single-character tokens.
	LEFT_PAREN, // (
	RIGHT_PAREN, // )
	LEFT_BRACE, // {
	RIGHT_BRACE, // }
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
	LESSER_DASH, // <-

	// Literals.
	IDENTIFIER,
	STRING,
	NUMBER,

	// Keywords.
	AND,
	CLASS,
	ELSE,
	FALSE,
	FOR,
	FUNC,
	IF,
	NIL,
	OR,
	PRINT,
	RETURN,
	SUPER,
	THIS,
	TRUE,
	VAR,
	WHILE,

	EOF
}
