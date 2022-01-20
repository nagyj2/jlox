package com.craftinginterpreters.jlox;

abstract class Exception extends RuntimeException {
	Exception() {
		super(null, null, false, false);
	}

	static class Break extends Exception {
		final Token token;

		public Break(Token token) {
			super(); // Used for control flow, so lighten the overhead
			this.token = token;
		}
	}
	
	static class Return extends RuntimeException {
		final Object value;
		
		public Return(Object value) {
			super();
			this.value = value;
		}
	}
	
	static class Runtime extends Exception {
		final Token token;

		public Runtime(Token token, String message) {
			super();
			this.token = token;
		}
	}
}
	
