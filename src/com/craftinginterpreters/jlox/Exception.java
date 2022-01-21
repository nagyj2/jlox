package com.craftinginterpreters.jlox;

abstract class Exception extends RuntimeException {
	Exception(String message) {
		super(message, null, false, false);
	}

	static class Break extends Exception {
		final Token token;

		public Break(Token token) {
			super("Unexpected '" + token.lexeme + "' outside loop or 'do'."); // Used for control flow, so lighten the overhead
			this.token = token;
		}
	}
	
	static class Return extends RuntimeException {
		final Object value;
		
		public Return(Object value) {
			super("Unexpected return outside function.");
			this.value = value;
		}
	}
	
	static class Runtime extends Exception {
		final Token token;

		public Runtime(Token token, String message) {
			super(message);
			this.token = token;
		}
	}

	static class Generic extends Exception {

		public Generic(String message) {
			super(message);
		}
	}

	static class Panic extends Exception {
		final Double code;

		public Panic(Double code) {
			super("Uncaught panic: Code " + code);
			this.code = code;
		}
	}

	static class FailedAssertion extends Exception {
		final Object left;
		final Object right;

		public FailedAssertion(Object left, Object right, String op) {
			super("Failed assertion. Expected " + LoxSemantics.stringify(left) + " " + op + " " + LoxSemantics.stringify(right) + ".");
			this.left = left;
			this.right = right;
		}
	}
}
	
