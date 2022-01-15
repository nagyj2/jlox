package com.craftinginterpreters.jlox;

import java.util.List;

import static com.craftinginterpreters.jlox.TokenType.*;

public class Parser {
	private static class ParseError extends RuntimeException {}

	private final List<Token> tokens;
	private int current = 0;

	public Parser(List<Token> tokens) {
		this.tokens = tokens;
	}

	public Expr parse() {
		try {
			return expression();
		} catch (ParseError error) {
			return null;
		}
	}

	//~ Productions

	//* Entry function for parsing an expression. Allows for comma expressions.
	private Expr expression() {
		Expr expr = equality();

		while (match(COMMA)) {
			Expr right = equality();
			expr = new Expr.Sequence(expr, right);
		}

		return expr;
	}

	//* Parse an equality (== or !=) expression.
	private Expr equality() {
		Expr expr = comparison();

		while (match(BANG_EQUAL, EQUAL_EQUAL)) {
			Token operator = previous();
			Expr right = comparison();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	//* Parse a comparison (<, >, <= or >=) expression.
	private Expr comparison() {
		Expr expr = term();

		while (match(LESSER, GREATER, LESSER_EQUAL, GREATER_EQUAL)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	//* Parse a term (+ or -) expression.
	private Expr term() {
		Expr expr = factor();

		while (match(MINUS, PLUS)) {
			Token operator = previous();
			Expr right = factor();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	//* Parse a factor (* or /) expression.
	private Expr factor() {
		Expr expr = unary();

		while (match(STAR, SLASH)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	//* Parse a unary (- or !) expression.
	private Expr unary() {
		if (match(MINUS, BANG)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}

		return primary();
	}

	//* Parse a primary/ literal expression.
	private Expr primary() {
		if (match(FALSE))
			return new Expr.Literal(false);
		if (match(TRUE))
			return new Expr.Literal(true);
		if (match(NIL))
			return new Expr.Literal(null);
		if (match(NUMBER, STRING))
			return new Expr.Literal(previous().literal);
		if (match(LEFT_PAREN)) {
			Expr expr = expression();
			consume(RIGHT_PAREN, "Expecte ')' after expression.");
			return new Expr.Grouping(expr);
		}

		throw error(peek(), "Expected expression.");
	}

	//~ Utility Functions

	//* Looks at the current token and advances if it matches the given token type. Returns true if the token matches.
	private boolean match(TokenType... types) {
		// Try to match the current token type against the given types.
		for (TokenType type : types) {
			if (check(type)) { // If there is a match, consume it and return true.
				advance();
				return true;
			}
		}
		return false;
	}

	//* Checks if the current token matches the given token type. Does NOT consume the token.
	private boolean check(TokenType type) {
		if (isAtEnd()) // Never match the end of the token sequence.
			return false;
		return peek().type == type;
	}

	//* Consumes the current token and returns it.
	private Token advance() {
		if (!isAtEnd()) // Never go beyond the end.
			current++;
		return previous();
	}

	//* Returns the current token.
	private Token peek() {
		return tokens.get(current);
	}

	//* Returns the most previously consumed token.
	private Token previous() {
		return tokens.get(current - 1);
	}

	//* Determines if the current token is at the end of the list of tokens (i.e. are there tokens left?).
	private boolean isAtEnd() {
		return peek().type == EOF;
	}

	//* Consumes the current token if it matches the given token type. Otherwise, throws an error.
	private void consume(TokenType type, String message) {
		if (check(type))
			advance();
		else
			error(peek(), message);
	}

	//~ Error Handling

	//* Throws an error for the given token.
	private ParseError error(Token token, String message) {
		Lox.error(token, message);
		return new ParseError();
	}

	//* Synchronizes the parser state and token sequence.
	private void synchronize() {
		advance(); // Consume the erronous token.

		while (!isAtEnd()) {
			if (previous().type == SEMICOLON) // Marks a new statement (our synchronization point)
				return;

			// In addition to semicolons (which always denote a new statement), we can also resume on a keyword.
			switch (peek().type) {
				case CLASS:
				case FUNC:
				case VAR:
				case FOR:
				case IF:
				case WHILE:
				case PRINT:
				case RETURN:
					return;
				default:
					break;
			}

			// No matches, so continue.
			advance();
		}
	}
}
