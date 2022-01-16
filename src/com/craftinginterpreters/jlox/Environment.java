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

	Environment() {
		this.enclosing = null;
	}

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

	Object get(Token name) {
		// note: disallows accessing nil variables. Uninitialized variables and nil SHOULD not be the same.
		boolean errOnNil = false; // Should accessing a nil variable throw an error?

		if (values.containsKey(name.lexeme)) {
			Entry value = values.get(name.lexeme);

			if (errOnNil && value == null)
				throw new RuntimeError(name, "Uninitialized variable '" + name.lexeme + "'.");
			
			return value.content;
		}

		if (enclosing != null)
			return enclosing.get(name);

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

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
}
