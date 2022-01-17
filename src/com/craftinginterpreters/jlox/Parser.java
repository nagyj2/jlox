package com.craftinginterpreters.jlox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftinginterpreters.jlox.TokenType.*;

public class Parser {
	private static class ParseError extends RuntimeException {}

	private final List<Token> tokens; // The tokens to parse
	private int current = 0; // Current token index in 'tokens'

	private final boolean isREPL; // Whether the parser was started in REPL mode

	public Parser(List<Token> tokens, boolean isREPL) {
		this.tokens = tokens;
		this.isREPL = isREPL;
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
			if (match(LET))
				return letDeclaration();
			if (check(FUNC) && checkNext(IDENTIFIER)) {
				match(FUNC);
				return funDeclaration();
			}

			return statement();

		} catch (ParseError error) {
			synchronize();
			return null;
		}
	}
	
	//* Parse a variable declaration. Note that the 'var' was consumed by the declaration() method.
	private Stmt varDeclaration() {
		return newDeclaration(false);
	}
	//* Parse a constant declaration. Note that the 'let' was consumed by the declaration() method.
	private Stmt letDeclaration() {
		return newDeclaration(true);
	}
	
	//* Parse a new variable-like declaration.
	private Stmt newDeclaration(boolean constant) {
		Stmt stmt = null;
		do {
			Token name = consume(IDENTIFIER, "Expected variable name.");
	
			Expr initializer = null;
			if (match(EQUAL)) {
				initializer = expression();
			}

			stmt = new Stmt.Var(name, initializer, constant, stmt);
		} while (false); // while (match(COMMA));

		REPLSemicolon();
		
		return stmt; 
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
		if (match(DO)) {
			return doStatement();
		}
		if (match(PRINT)) {
			return printStatement();
		}
		if (match(BREAK)) {
			return breakStatement();
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
		} else if (match(LET)) {
			initializer = letDeclaration();
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

	//* Parses a do-while statement.
	private Stmt doStatement() {
		Stmt body = statement();

		Expr condition;

		if (match(WHILE)) {
			consume(LEFT_PAREN, "Expected '(' after 'while'.");
			condition = expression();
			consume(RIGHT_PAREN, "Expected ')' after condition.");
			
			REPLSemicolon();
		} else {
			condition = null;
		}

		return new Stmt.Do(body, condition);

	}

	//* Parses a break statement.
	private Stmt breakStatement() {
		Token _break = previous();
		
		REPLSemicolon();

		return new Stmt.Break(_break);
	}

	//* Parse a print statement. Note that the 'print' was consumed by the statement() method.
	private Stmt printStatement() {
		Expr value = expression();

		REPLSemicolon();

		return new Stmt.Print(value);
	}
	
	//* Parses a return statement.
	private Stmt returnStatement() {
		Token keyword = previous();
		Expr value = null;
		if (!check(SEMICOLON)) {
			value = expression();
		}

		REPLSemicolon();
		return new Stmt.Return(keyword, value);
	}

	//* Parse an expression statement.
	private Stmt expressionStatement() {
		Expr expr = expression();

		// If in REPL mode and at the end, the ';' is optional
		REPLSemicolon();

		return new Stmt.Expression(expr);
	}
	
	//* Parse a full expression. Shortcut for the lowest precidence level.
	private Expr expression() {
		return comma();
	}

	//* Parse an expression without the comma operator. Shortcut for the lowest precidence level after the comma expression.
	private Expr commaless() {
		return assignment();
	}

	//* Parse an expression.
	private Expr comma() {
		Expr expr = assignment();

		while (match(COMMA)) {
			if (!isPrimaryNext()) {
				error(isAtEnd() ? peek() : previous(), "Expected second operand.");
				return expr;
			}

			Expr right = assignment();
			expr = new Expr.Sequence(expr, right);
		}

		return expr;
	}

	//* Parse an assignment (=) expression.
	private Expr assignment() {
		Expr expr = functional();

		if (match(EQUAL)) {
			Token equals = previous();

			if (!isPrimaryNext()) {
				error(isAtEnd() ? peek() : previous(), "Expected r-value.");
				return expr;
			}

			// b/c assignment is right associative, call this level recursively
			Expr value = assignment();

			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable) expr).name;
				return new Expr.Assign(name, value);
			}

			throw error(equals, "Invalid assignment target.");
		}

		return expr;
	}

	//* Parses an anonymous function expression.
	private Expr functional() {
		Expr expr;
		if (match(FUNC)) {
			consume(LEFT_PAREN, "Expected '(' after 'fun'.");
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

			consume(LEFT_BRACE, "Expected '{' before 'fun' body.");
			List<Stmt> body = block();

			return new Expr.Lambda(null, parameters, body);

		// } else if (match(FUNC)) {
		// 	Token name = consume(IDENTIFIER, "Names are not allowed for anonymous functions.");
		//  Do above logic after eating identifier
		} else {
			expr = conditional();
		}
		
		return expr;
	}


	//* Parse an ternary conditional expression.
	private Expr conditional() {
		Expr expr = logical_or();

		while (match(QUESTION)) {
			Token operator = previous();

			if (!isPrimaryNext()) {
				error(isAtEnd() ? peek() : previous(), "Expected second operand.");
				return expr;
			}

			Expr center = logical_or();
			consume(COLON, "Expected ':' after '?' true expression.");

			if (!isPrimaryNext()) {
				error(isAtEnd() ? peek() : previous(), "Expected third operand.");
				return expr;
			}

			Expr right = conditional();
			expr = new Expr.Ternary(operator, expr, center, right);
		}

		return expr;
	}

	//* Parses a logical OR expression.
	private Expr logical_or() {
		Expr expr = logical_and();

		while (match(OR)) {
			Token operator = previous();

			if (!isPrimaryNext()) {
				error(isAtEnd() ? peek() : previous(), "Expected second operand.");
				return expr;
			}

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
			
			if (!isPrimaryNext()) {
				error(isAtEnd() ? peek() : previous(), "Expected second operand.");
				return expr;
			}

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
			
			if (!isPrimaryNext()) {
				error(isAtEnd() ? peek() : previous(), "Expected second operand.");
				return expr;
			}
			
			Expr right = comparison();
			expr = new Expr.Binary(operator, expr, right);
		}

		return expr;
	}

	//* Parse a comparison (<, >, <= or >=) expression.
	private Expr comparison() {
		Expr expr = term();

		while (match(LESSER, GREATER, LESSER_EQUAL, GREATER_EQUAL)) {
			Token operator = previous();

			if (!isPrimaryNext()) {
				error(isAtEnd() ? peek() : previous(), "Expected second operand.");
				return expr;
			}

			Expr right = term();
			expr = new Expr.Binary(operator, expr, right);
		}

		return expr;
	}

	//* Parse a term (+ or -) expression.
	private Expr term() {
		Expr expr = factor();

		while (match(MINUS, PLUS)) {
			Token operator = previous();

			if (!isPrimaryNext()) {
				error(isAtEnd() ? peek() : previous(), "Expected second operand.");
				return expr;
			}

			Expr right = factor();
			expr = new Expr.Binary(operator, expr, right);
		}

		return expr;
	}

	//* Parse a factor (* or /) expression.
	private Expr factor() {
		Expr expr = unary();

		while (match(STAR, SLASH)) {
			Token operator = previous();

			if (!isPrimaryNext()) {
				error(isAtEnd() ? peek() : previous(), "Expected second operand.");
				return expr;
			}

			Expr right = unary();
			expr = new Expr.Binary(operator, expr, right);
		}

		return expr;
	}

	//* Parse a unary (- or !) expression.
	private Expr unary() {
		while (match(MINUS, BANG)) {
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
				arguments.add(commaless());
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

	//* Looks at the current token and returns whether it matches a given token type
	private boolean checks(TokenType... types) {
		// Try to match the current token type against the given types.
		for (TokenType type : types) {
			if (check(type)) { // If there is a match, consume it and return true.
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

 	//* Checks if the token after the current token matches the given token type. Does NOT consume either token.
	private boolean checkNext(TokenType type) {
		if (isAtEnd()) // Never match the end of the token sequence.
			return false;
		if (tokens.get(current + 1).type == EOF) // Never match beyond the end of the token sequence.
			return false;
		return tokens.get(current + 1).type == type;
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

	//* Requires a semicolon if NOT in REPL mode.
	private void REPLSemicolon(){
		if (!isREPL || !isAtEnd())
			consume(SEMICOLON, "Expected ';' after variable declaration.");

	}

	//* Looks at the current token and returns whether it matches a start of a valid primary or unary.
	private boolean isPrimaryNext() {
		return checks(NUMBER, STRING, IDENTIFIER, TRUE, FALSE, NIL, LEFT_PAREN, MINUS, BANG, FUNC);
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
