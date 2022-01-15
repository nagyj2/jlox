package com.craftinginterpreters.jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.jlox.TokenType.*;

public class Scanner {
	// The source code to be scanned.
	private final String source;
	// Output token stream.
	private final List<Token> tokens = new ArrayList<>();

	// Scanner location tracking variables
	private int start = 0; 		// start of the current lexeme
	private int current = 0; 	// current scanner position
	private int line = 1; // current line
	
	// Keywords
	private static final Map<String, TokenType> keywords;
	static {
		keywords = new HashMap<>();
		keywords.put("and", 		AND);
		keywords.put("break", 	BREAK);
		keywords.put("class", 	CLASS);
		keywords.put("do", 	DO);
		keywords.put("else", 		ELSE);
		keywords.put("false", 	FALSE);
		keywords.put("for", 		FOR);
		keywords.put("fun", 		FUNC);
		keywords.put("if", 			IF);
		keywords.put("nil", 		NIL);
		keywords.put("or", 			OR);
		keywords.put("print", 	PRINT);
		keywords.put("return",	RETURN);
		keywords.put("super", 	SUPER);
		keywords.put("this", 		THIS);
		keywords.put("true", 		TRUE);
		keywords.put("var", 		VAR);
		keywords.put("while", 	WHILE);
		
	}

	//* Constructs a new Scanner object with some source code to scan.
	Scanner(String source) {
		this.source = source;
	}

	//* Scans the source code and returns a list of tokens.
	List<Token> scanTokens() {
		while (!isAtEnd()) {
			// At the beginning of the next lexeme.
			start = current;
			scanToken();
		}

		// Add EOF at the end.
		tokens.add(new Token(EOF, "", null, line));
		return tokens;
	}

	//* Parse and add the next token
	private void scanToken() {
		char c = advance();
		switch (c) {
			// Single char, literal-less tokens
			case '(':
				addToken(LEFT_PAREN);
				break;
			case ')':
				addToken(RIGHT_PAREN);
				break;
			case '{':
				addToken(LEFT_BRACE);
				break;
			case '}':
				addToken(RIGHT_BRACE);
				break;
			case ',':
				addToken(COMMA);
				break;
			case '.':
				addToken(DOT);
				break;
			case '-':
				addToken(MINUS);
				break;
			case '+':
				addToken(PLUS);
				break;
			case ';':
				addToken(SEMICOLON);
				break;
			case '*':
				addToken(STAR);
				break;
			case ':':
				addToken(COLON);
				break;
			case '?':
				addToken(QUESTION);
				break;

			case ' ':
			case '\t':
			case '\r':
				// Ignore whitespace.
				break;
			case '\n':
				line++;
				break;

			// Single or dual char, literal-less tokens
			case '!':
				addToken(match('=') ? BANG_EQUAL : BANG);
				break;
			case '=':
				addToken(match('=') ? EQUAL_EQUAL : EQUAL);
				break;
			case '>':
				addToken(match('=') ? GREATER_EQUAL : GREATER);
				break;
			case '<':
				if (match('='))
					addToken(LESSER_EQUAL);
				addToken(match('-') ? LESSER_DASH : LESSER);
				break;
			case '/':
				// Special handling b/c single char is token and dual chars mean a comment
				if (match('/')) {
					// A comment. Skip until end of line.
					while (peek() != '\n' && !isAtEnd())
						advance();

				} else if (match('*')) {
					// A block comment. Skip until end of line.
					while (peek() != '*' && peekNext() != '/' && !isAtEnd()) {
						if (peek() == '\n')
							line++;
						advance();
					}

					if (current + 1 >= source.length()) {
						Lox.error(line, "Unterminated block comment.");
					} else {
						advance(); // Skip the '*/'
						advance();
					}

				} else {
					addToken(SLASH);
				}
				break;

			// Delimited, arbitrary-length literal tokens
			case '"':
				string();
				break;
			
			default:
				// Starting digits indicate a number
				if (isDigit(c)) {
					number();

				// Starting letters or underscores indicate an identifier
				} else if (isAlpha(c)) {
					identifier();
					
				// Fallback on an unexpected char error.
				} else {
					Lox.error(line, "Unexpected character, '" + c + "'.");
				}
				
				break;
		}
	}

	//~ Scanning Methods

	//* Consumes the current char and advances to the next one, returning it.
	private char advance() {
		current++;
		return source.charAt(current - 1);
	}

	//* Returns the current char being looked at w/o consuming it.
	private char peek() {
		if (isAtEnd())
			return '\0';
		return source.charAt(current);
	}

	//* Returns the character ahead of current.
	private char peekNext() {
		if (current + 1 >= source.length())
			return '\0';
		return source.charAt(current + 1);
	}

	/**
	 * See if the next character matches the expected character.
	 * Functionally equivalent to peek() followed by advance() if the peek result matches the expected char.
	 */
	private boolean match(char expected) {
		if (isAtEnd())
			return false;
		if (source.charAt(current) != expected)
			return false;

		current++;
		return true;
	}

	//~ Token Creation

	//* Add a token without a value to the list of tokens.
	private void addToken(TokenType type) {
		addToken(type, null);
	}

	/**
	 * Add a token with a value to the list of tokens.
	 * Includes all values required to define a token.
	 */
	private void addToken(TokenType type, Object literal) {
		String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, line));
	}

	//~ Token Mini Parsers

	//* Parses and adds a string token
	private void string() {
		while (peek() != '"' && !isAtEnd()) {
			if (peek() == '\n') // Continually check for newline
				line++;

			advance();
		}

		if (isAtEnd()) {
			Lox.error(line, "Unterminated string.");
			return;
		}

		// Consume the trailing "
		advance();

		String value = source.substring(start + 1, current - 1); // Exclude the "s
		addToken(STRING, value);
	}
	
	//* Parses and adds a number token
	private void number() {
		// Integer part
		while (isDigit(peek()))
			advance();

		// Look for a fractional part.
		if (peek() == '.' && isDigit(peekNext())) {
			// Consume the '.'
			advance();

			// Fractional part
			while (isDigit(peek()))
				advance();
		}

		addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
	}

	//* Parses and adds an identifier token or a keyword
	private void identifier() {
		while (isAlphaNumeric(peek()))
			advance();

		String text = source.substring(start, current);
		TokenType type = keywords.get(text); // Check if it's a keyword
		if (type == null) // If not, it is an identifier
			type = IDENTIFIER;

		addToken(type);
	}

	//~ Character and Position Checkers

	//* Check if the current char is a digit.
	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	//* Check if the current char is a letter.
	private boolean isAlpha(char c) {
		return 	(c >= 'a' && c <= 'z') ||
						(c >= 'A' && c <= 'Z') ||
						(c == '_');
	}

	//* Check if the current char is a letter or digit.
	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}

	//* Check if the current char is at the end of the source code.
	private boolean isAtEnd() {
		return current >= source.length();
	}

}
