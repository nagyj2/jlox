package com.craftinginterpreters.jlox;

import java.util.List;

public interface LoxCallable {
	//* How many parameters the callable expects.
	int arity();

	Object call(Interpreter interpreter, List<Object> arguments);
}
