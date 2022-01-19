package com.craftinginterpreters.jlox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
	private class Entry {
		public Object content;
		public final boolean constant;
	
		public Entry(Object content, boolean constant) {
			this.content = content;
			this.constant = constant;
		}

		public void change(Object content) {
			if (!constant)
				this.content = content;
		}
	}

	final Environment enclosing;
	private final Map<String, Entry > values = new HashMap<>();

	//* Used for the global environment. There is no enclosing environment.
	Environment() {
		this.enclosing = null;
	}

	//* Used for the local environments.
	Environment(Environment env) {
		this.enclosing = env;
	}

	void define(Token token, boolean constant, Object value) {
		if (values.containsKey(token.lexeme) && values.get(token.lexeme).constant) {
			throw new RuntimeError(token, "Cannot redefine constant.");
		}

		Entry entry = new Entry(value, constant);
		values.put(token.lexeme, entry);
	}

	void define(String name, boolean constant, Object value) {

		Entry entry = new Entry(value, constant);
		values.put(name, entry);
	}

	void updateConstant(Token token, boolean constant) {
		if (values.containsKey(token.lexeme)) {
			values.put(token.lexeme, new Entry(get(token), constant));
			return;
		}

		Lox.error(token, "Constant change of non-existant variable.");
	}


	//* Looks up a variable in the current environment. If it does not exist, a runtime error is thrown. Used for global env.
	Object get(Token name) {
		return retrieve(name).content;
	}

	//* Returns a variable at a specific distance from the current environment.
	Object getAt(int distance, String name) {
		return ancestor(distance).values.get(name).content;
	}

	//* Assigns a value to a variable in the current environment. If it doesn't exist, a runtime error is thrown. Used for global env.
	void assign(Token name, Object value) {
		if (values.containsKey(name.lexeme)) {
			Entry entry = values.get(name.lexeme);

			if (entry.constant){
				throw new RuntimeError(name, "Cannot redefine constant");
			}

			entry.change(value);
			return;
		}

		if (enclosing != null) {
			enclosing.assign(name, value);
			return;
		}

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	//* Assigns a value to a specific variable identified by a name and number of environments to skip.
	void assignAt(int distance, Token token, Object value) {
		Entry entry = ancestor(distance).retrieve(token);
		if (entry.constant) {
			throw new RuntimeError(token, "Cannot redefine constant");
		}

		entry.change(value);
	}

	//* Returns an environment that is a specific distance away from the current environment.
	Environment ancestor(int distance) {
		Environment env = this;
		for (int i = 0; i < distance; i++)
			env = env.enclosing;

		return env;
	}

	private Entry retrieve(Token token) {
		// note: disallows accessing nil variables. Uninitialized variables and nil SHOULD not be the same.
		boolean errOnNil = false; // Should accessing a nil variable throw an error?

		if (values.containsKey(token.lexeme)) {
			Entry value = values.get(token.lexeme);

			if (errOnNil && value == null)
				throw new RuntimeError(token, "Uninitialized variable '" + token.lexeme + "'.");
			
			return value;
		}

		if (enclosing != null)
			return enclosing.retrieve(token);

		throw new RuntimeError(token, "Undefined variable '" + token.lexeme + "'.");
	}
}
