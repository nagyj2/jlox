package com.craftinginterpreters.jlox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
	final Environment enclosing;
	private final Map<String, Object> values = new HashMap<>();

	//* Used for the global environment. There is no enclosing environment.
	Environment() {
		this.enclosing = null;
	}

	//* Used for the local environments.
	Environment(Environment env) {
		this.enclosing = env;
	}

	//* Defines a variable in the current environment.
	void define(String name, Object value) {
		values.put(name, value);
	}

	//* Looks up a variable in the current environment. If it does not exist, a runtime error is thrown. Used for global env.
	Object get(Token name) {
		if (values.containsKey(name.lexeme)) {
			return values.get(name.lexeme);
		}

		if (enclosing != null)
			return enclosing.get(name);

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	//* Returns a variable at a specific distance from the current environment.
	Object getAt(int distance, String name) {
		return ancestor(distance).values.get(name);
	}

	//* Assigns a value to a variable in the current environment. If it doesn't exist, a runtime error is thrown. Used for global env.
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

	//* Assigns a value to a specific variable identified by a name and number of environments to skip.
	void assignAt(int distance, Token name, Object value) {
		ancestor(distance).values.put(name.lexeme, value);
	}

	//* Returns an environment that is a specific distance away from the current environment.
	Environment ancestor(int distance) {
		Environment env = this;
		for (int i = 0; i < distance; i++)
			env = env.enclosing;

		return env;
	}
}
