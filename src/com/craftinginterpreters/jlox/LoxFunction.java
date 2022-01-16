package com.craftinginterpreters.jlox;

import java.util.List;

public class LoxFunction implements LoxCallable {
	private final Stmt.Function declaration; // Contains name, list of parameters and list of stmts
	private final Environment closure; // Contains the environment preceding the function

	LoxFunction(Stmt.Function declaration, Environment closure) {
		this.declaration = declaration;
		this.closure = closure;
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		// Create new environment based off the global environment. Will place arguments into here
		Environment environment = new Environment(closure);

		// Add each argument to the environment under the name of the parameter
		for (int i = 0; i < declaration.params.size(); i++) {
			environment.define(declaration.params.get(i).lexeme, arguments.get(i));
		}

		// Execute the function body in the new environment
		try {
			interpreter.executeBlock(declaration.body, environment);
		} catch (Return returnValue) {
			return returnValue.value;
		}
		// note: environment was made and used here. When the function returns, it is discarded. However, if a reference is kept to it from an outside environment, it will be kept.
		return null;
	}

	@Override
	public int arity() {
		return declaration.params.size();
	}

	@Override
	// B/c This LoxFunction is passed around internally as an Object, if we print it in our lang, sysout will call toString() on it. Might as well implement it...
	public String toString() {
		if (declaration == null)
			return "<fn>";
		return "<fn " + declaration.name.lexeme +">";
	}
}
