package com.craftinginterpreters.jlox;

public class Token {
	final TokenType type; // type of the token
	final String lexeme;  // what appears in the source code
	final Object literal; // value of the token
	final int line; 			// the line the token appears on

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
