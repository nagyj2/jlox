package com.craftinginterpreters.jlox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
	final Environment enclosing;
	private final Map<String, Object> values = new HashMap<>();

	Environment() {
		this.enclosing = null;
	}

	Environment(Environment env) {
		this.enclosing = env;
	}

	void define(String name, Object value) {
		values.put(name, value);
	}

	Object get(Token name) {
		// note: disallows accessing nil variables. Uninitialized variables and nil SHOULD not be the same.
		boolean errOnNil = false; // Should accessing a nil variable throw an error?

		if (values.containsKey(name.lexeme)) {
			Object value = values.get(name.lexeme);

			if (errOnNil && value == null)
				throw new RuntimeError(name, "Uninitialized variable '" + name.lexeme + "'.");
			
			return value;
		}

		if (enclosing != null)
			return enclosing.get(name);

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	void assign(Token name, Object value) {
		if (values.containsKey(name.lexeme)) {
			values.put(name.lexeme, value);
			return;
		}

		if (enclosing != null) {
			enclosing.assign(name, value);
			return;
		}

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}
}
