package com.craftinginterpreters.jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.List;

import com.craftinginterpreters.tool.DebugWriter;

public class Lox {

	private static final Interpreter interpreter = new Interpreter();

	// Whether an error occured during execution.
	static boolean hadError = false;
	static boolean hadRuntimeError = false;

	static DebugWriter writer;

	public static void main(String[] args) throws IOException {
		if (args.length > 1) {
			// Too many source files are supplied. jlox takes one at a time.
			System.out.println("Usage: jlox [script]");
		} else if (args.length == 1) {
			// A file is supplied, so run it
			runFile(args[0]);
		} else {
			// No file is supplied, so open up a REPL session
			runPrompt();
		}
	}

	//~ Execution

	//* Executes a Lox file.
	private static void runFile(String path) throws IOException {
		// Read the file
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		// Execute the file
		run(new String(bytes, Charset.defaultCharset()), false);

		// Error codes
		if (hadError)
			System.exit(65);
		if (hadRuntimeError) // We care about runtime errors on files, but not REPL
			System.exit(70);
	}

	//* Opens a Lox interpreter in REPL mode.
	private static void runPrompt() throws IOException {
		// Create object to capture input
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);
		
		for (;;) {
			System.out.print("> ");
			String line = reader.readLine();
			if (line == null)
				break;
			run(line, true);
			// Each prompt is separate.
			hadError = false;
		}
	}

	//* Runs a Lox program.
	private static void run(String source, boolean isREPL) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();

		Parser parser = new Parser(tokens, isREPL);
		List<Stmt> statements = parser.parse();

		if (hadError)
			return;

		writer = new DebugWriter();
		Resolver resolver = new Resolver(interpreter);
		resolver.resolve(statements);
		writer.close();

		if (hadError)
			return;

		// System.out.println(new AstPrinter().print(expression));
		interpreter.interpret(statements);

	}
	
	//~ Error Handling
	
	//* Marks an error at a line with a message.
	static void error(int line, String message) {
		report(line, "", message);
	}

	//* Marks an error at a token with a message.
	static void error(Token token, String message) {
		if (token.type == TokenType.EOF)
			report(token.line, " at end", message);
		else
			report(token.line, " at '" + token.lexeme + "'", message);
	}

	static void runtimeError(Exception.Runtime error) {
		System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
		hadRuntimeError = true;
	}

	//* Reports an error to stderr with the line number, message and a position.
	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error" + where + ": " + message);
		hadError = true;
	}
}
