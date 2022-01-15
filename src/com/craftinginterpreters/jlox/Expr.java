package com.craftinginterpreters.jlox;

import java.util.List;

abstract class Expr {
	interface Visitor<R> {
		R visitBinaryExpr(Binary expr);
		R visitUnaryExpr(Unary expr);
		R visitGroupingExpr(Grouping expr);
		R visitLiteralExpr(Literal expr);
		R visitSequenceExpr(Sequence expr);
		R visitTernaryExpr(Ternary expr);
	}

	static class Binary extends Expr {
		final Token operator;
		final Expr left;
		final Expr right;

		Binary(Token operator, Expr left, Expr right) {
			this.operator = operator;
			this.left = left;
			this.right = right;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBinaryExpr(this);
		}
	}

	static class Unary extends Expr {
		final Token operator;
		final Expr right;

		Unary(Token operator, Expr right) {
			this.operator = operator;
			this.right = right;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitUnaryExpr(this);
		}
	}

	static class Grouping extends Expr {
		final Expr expression;

		Grouping(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitGroupingExpr(this);
		}
	}

	static class Literal extends Expr {
		final Object value;

		Literal(Object value) {
			this.value = value;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitLiteralExpr(this);
		}
	}

	static class Sequence extends Expr {
		final Expr first;
		final Expr second;

		Sequence(Expr first, Expr second) {
			this.first = first;
			this.second = second;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitSequenceExpr(this);
		}
	}

	static class Ternary extends Expr {
		final Token operator;
		final Expr left;
		final Expr center;
		final Expr right;

		Ternary(Token operator, Expr left, Expr center, Expr right) {
			this.operator = operator;
			this.left = left;
			this.center = center;
			this.right = right;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitTernaryExpr(this);
		}
	}


	abstract <R> R accept(Visitor<R> visitor);
}
