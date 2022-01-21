package com.craftinginterpreters.jlox;

abstract class Exception extends RuntimeException {
	Exception(String message) {
		super(message, null, false, false);
	}

	static class Break extends Exception {
		final Token token;

		public Break(Token token) {
			super("Uncaught break."); // Used for control flow, so lighten the overhead
			this.token = token;
		}
	}
	
	static class Return extends RuntimeException {
		final Object value;
		
		public Return(Object value) {
			super("Uncaught return.");
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
}
	
