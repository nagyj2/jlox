package com.craftinginterpreters.jlox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftinginterpreters.jlox.TokenType.*;

public class Parser {
	private static class ParseError extends RuntimeException {}

	private final List<Token> tokens;
	private int current = 0;

	public Parser(List<Token> tokens) {
		this.tokens = tokens;
	}

	public List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<Stmt>();
		while (!isAtEnd()) { // Checks for EOF
			statements.add(declaration());
		}

		return statements;
	}

	//~ Productions

	private Stmt declaration() {
		try {
			if (match(VAR))
				return varDeclaration();
			if (match(FUNC))
				return funDeclaration();
			if (match(CLASS))
				return classDeclaration();

			return statement();

		} catch (ParseError error) {
			synchronize();
			return null;
		}
	}
	
	//* Parse a variable declaration. Note that the 'var' was consumed by the declaration() method.
	private Stmt varDeclaration() {
		Token name = consume(IDENTIFIER, "Expected variable name.");

		Expr initializer = null;
		if (match(EQUAL)) {
			initializer = expression();
		}

		consume(SEMICOLON, "Expected ';' after variable declaration.");
		return new Stmt.Var(name, initializer);
	}

	//* Parse a function declaration.
	private Stmt funDeclaration() {
		return function("function"); // Want a function to be made, so specify
	}

	//* Parse a function declaration. Takes a string so that the code can be reused and the errors still make sense.
	private Stmt function(String kind) {
		Token name = consume(IDENTIFIER, "Expected " + kind + " name.");

		consume(LEFT_PAREN, "Expected '(' after " + kind + " name.");
		List<Token> parameters = new ArrayList<>();
		if (!check(RIGHT_PAREN)) {
			do {
				if (parameters.size() >= 255) {
					error(peek(), "Cannot have more than 255 parameters.");
				}

				parameters.add(consume(IDENTIFIER, "Expected parameter name."));
			} while (match(COMMA));
		}
		consume(RIGHT_PAREN, "Expected ')' after parameters.");

		consume(LEFT_BRACE, "Expected '{' before " + kind + " body.");
		List<Stmt> body = block();

		return new Stmt.Function(name, parameters, body);
	}

	//* Parse a class declaration
	private Stmt classDeclaration() {
		Token name = consume(IDENTIFIER, "Expected class name.");
		consume(LEFT_BRACE, "Expected '{' before class body.");

		List<Stmt.Function> methods = new ArrayList<>();
		while (!check(RIGHT_BRACE) && !isAtEnd()) {
			methods.add((Stmt.Function) function("method"));
		}

		consume(RIGHT_BRACE, "Expected '}' after class body.");

		return new Stmt.Class(name, methods);
	}

	//* Parse a statement.
	private Stmt statement() {
		if (match(LEFT_BRACE)) {
			return new Stmt.Block(block());
		}
		if (match(IF)) {
			return ifStatement();
		}
		if (match(WHILE)) {
			return whileStatement();
		}
		if (match(FOR)) {
			return forStatement();
		}
		if (match(PRINT)) {
			return printStatement();
		}
		if (match(RETURN)) {
			return returnStatement();
		}

		return expressionStatement();
	}

	//* Parses a group of statements. Return is a list of statements, as opposed to a single statement.
	private List<Stmt> block() {
		List<Stmt> statements = new ArrayList<>();

		while (!check(RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}

		consume(RIGHT_BRACE, "Expected '}' after block.");
		// return new Stmt.Block(statements);
		return statements;
	}

	//* Parse an if statement.
	private Stmt ifStatement() {
		consume(LEFT_PAREN, "Expected '(' after 'if'.");
		Expr condition = expression();
		consume(RIGHT_PAREN, "Expected ')' after if condition.");

		Stmt thenBranch = statement();
		Stmt elseBranch = null;

		if (match(ELSE)) {
			elseBranch = statement();
		}

		return new Stmt.If(condition, thenBranch, elseBranch);
	}

	//* Parses a while statement.
	private Stmt whileStatement() {
		consume(LEFT_PAREN, "Expected '(' after 'while'.");
		Expr condition = expression();
		consume(RIGHT_PAREN, "Expected ')' after condition.");
		Stmt body = statement();

		return new Stmt.While(condition, body);
	}

	//* Parses a for loop and 'desugars' it (transforms to more basic primitives)
	private Stmt forStatement() {
		consume(LEFT_PAREN, "Expected '(' after 'for'.");

		Stmt initializer;
		if (match(SEMICOLON)) {
			initializer = null;
		} else if (match(VAR)) {
			initializer = varDeclaration();
		} else {
			initializer = expressionStatement();
		}

		Expr condition = null;
		if (!check(SEMICOLON)) {
			condition = expression();
		}

		consume(SEMICOLON, "Expected ';' after 'for' condition.");

		Expr increment = null;
		if (!check(RIGHT_PAREN)) {
			increment = expression();
		}

		consume(RIGHT_PAREN, "Expected ')' after 'for' clauses.");

		Stmt body = statement();

		// Conduct desugaring!
		// Append increment to the end of the body
		if (increment != null) {
			body = new Stmt.Block(
					Arrays.asList(
							body,
							new Stmt.Expression(increment)));
		}
		
		// If no condition was given, make it an infinite loop
		if (condition == null) {
			condition = new Expr.Literal(true);
		}
		body = new Stmt.While(condition, body);

		// Prepend the initializer
		if (initializer != null) {
			body = new Stmt.Block(Arrays.asList(initializer, body));
		}

		return body;

	}

	//* Parse a print statement. Note that the 'print' was consumed by the statement() method.
	private Stmt printStatement() {
		Expr value = expression();
		consume(SEMICOLON, "Expected ';' after expression.");
		return new Stmt.Print(value);
	}

	//* Parse an expression statement.
	private Stmt expressionStatement() {
		Expr expr = expression();
		consume(SEMICOLON, "Expected ';' after expression.");
		return new Stmt.Expression(expr);
	}

	//* Parse an expression.
	private Expr expression() {
		return assignment();
	}

	//* Parse an assignment (=) expression.
	private Expr assignment() {
		Expr expr = logical_or();

		if (match(EQUAL)) {
			Token equals = previous();
			// b/c assignment is right associative, call this level recursively
			Expr value = assignment();

			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable) expr).name;
				return new Expr.Assign(name, value);
			} else if (expr instanceof Expr.Get) {
				Expr.Get get = (Expr.Get) expr; // Cast so we can easily reconstruct the expr in the form we want (Expr.Set)
				return new Expr.Set(get.object, get.name, value);
			}

			throw error(equals, "Invalid assignment target.");
		}

		return expr;
	}

	//* Parses a return statement.
	private Stmt returnStatement() {
		Token keyword = previous();
		Expr value = null;
		if (!check(SEMICOLON)) {
			value = expression();
		}

		consume(SEMICOLON, "Expected ';' after return value.");
		return new Stmt.Return(keyword, value);
	}

	//* Parses a logical OR expression.
	private Expr logical_or() {
		Expr expr = logical_and();

		while (match(OR)) {
			Token operator = previous();
			Expr right = logical_and();
			expr = new Expr.Logical(operator, expr, right);
		}

		return expr;
	}

	//* Parses a logical AND expression.
	private Expr logical_and() {
		Expr expr = equality();

		while (match(AND)) {
			Token operator = previous();
			Expr right = equality();
			expr = new Expr.Logical(operator, expr, right);
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

		return call();
	}

	//* Parse a call expression.
	private Expr call() {
		Expr expr = primary();

		while (true) {
			if (match(LEFT_PAREN)) {
				expr = finishCall(expr); // The newly created call expression (expr) becomes the next callee. Allows for 'func()()' shapes
			} else if (match(DOT)) {
				Token name = consume(IDENTIFIER, "Expected property name after '.'");
				expr = new Expr.Get(expr, name);
			} else {
				break;
			}
		}

		return expr;
	}

	private Expr finishCall(Expr callee) {
		List<Expr> arguments = new ArrayList<>();
		if (!check(RIGHT_PAREN)) { // Handle no argument case
			do {
				if (arguments.size() >= 255) {
					// _reports_, not _throws_, an error. The parser state isn't invalid, but a warning should be shown.'
					error(peek(), "Cannot have more than 255 arguments.");
				}
				arguments.add(expression());
			} while (match(COMMA));
		}

		Token paren = consume(RIGHT_PAREN, "Expected ')' after arguments.");
		return new Expr.Call(callee, paren, arguments);
	}

	//* Parse a primary/ literal expression.
	private Expr primary() {
		if (match(FALSE))
			return new Expr.Literal(false);
		if (match(TRUE))
			return new Expr.Literal(true);
		if (match(NIL))
			return new Expr.Literal(null);
		if (match(THIS))
			return new Expr.This(previous());
		if (match(NUMBER, STRING))
			return new Expr.Literal(previous().literal);
		if (match(IDENTIFIER))
			return new Expr.Variable(previous());
		if (match(LEFT_PAREN)) {
			Expr expr = expression();
			consume(RIGHT_PAREN, "Expected ')' after expression.");
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
	private Token consume(TokenType type, String message) {
		if (check(type))
			return advance();
		else
			throw error(peek(), message);
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
