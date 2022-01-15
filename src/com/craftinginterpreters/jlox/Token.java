package com.craftinginterpreters.jlox;

public class Token {
	//* The type of the token.
	final TokenType type;
	//* The token as it appears in the source code.
	final String lexeme;
	//* The Java literal which corresponds to the token. Only filled if the token is a number, string, or boolean.
	final Object literal;
	//* The line number of the token in the source code.
	final int line;

	Token(TokenType type, String lexeme, Object literal, int line) {
		this.type = type;
		this.lexeme = lexeme;
		this.literal = literal;
		this.line = line;
	}

	public String toString() {
		return type + " " + lexeme + " " + literal;
	}
}
