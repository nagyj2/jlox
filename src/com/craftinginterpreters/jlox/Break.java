package com.craftinginterpreters.jlox;

class Break extends RuntimeException {
	final Token token;

	public Break(Token token) {
		super(null, null, false, false); // Used for control flow, so lighten the overhead
		this.token = token;
	}
}
