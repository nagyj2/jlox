package com.craftinginterpreters.jlox;

import java.util.HashMap;
import java.util.Map;

//* Runtime version of a Lox class instance
public class LoxInstance {
	private LoxClass klass; // nullable
	private final Map<String, Object> fields = new HashMap<>();

	LoxInstance(LoxClass klass) {
		this.klass = klass; // Can be null
	}

	Object get(Token name) {
		if (fields.containsKey(name.lexeme)) {
			return fields.get(name.lexeme);
		}
		if (klass != null) {
			LoxFunction method = klass.findMethod(name.lexeme);
			if (method != null)
				return method.bind(this);
		}

		throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
	}

	void set(Token name, Object value) {
		// Lox allows arbitrary fields, so no need to check if the field exists.
		fields.put(name.lexeme, value);
	}

	public String toString() {
		return "<instance " + klass + ">";
	}
}
