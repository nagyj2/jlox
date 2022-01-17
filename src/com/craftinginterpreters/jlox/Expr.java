package com.craftinginterpreters.jlox;

import java.util.List;

abstract class Expr {
	interface Visitor<R> {
		R visitAssignExpr(Assign expr);
		R visitBinaryExpr(Binary expr);
		R visitCallExpr(Call expr);
		R visitGetExpr(Get expr);
		R visitGroupingExpr(Grouping expr);
		R visitLambdaExpr(Lambda expr);
		R visitLiteralExpr(Literal expr);
		R visitLogicalExpr(Logical expr);
		R visitSequenceExpr(Sequence expr);
		R visitSetExpr(Set expr);
		R visitTernaryExpr(Ternary expr);
		R visitThisExpr(This expr);
		R visitUnaryExpr(Unary expr);
		R visitVariableExpr(Variable expr);
	}

	static class Assign extends Expr {
		final Token name;
		final Expr value;

		Assign(Token name, Expr value) {
			this.name = name;
			this.value = value;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitAssignExpr(this);
		}
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

	static class Call extends Expr {
		final Expr callee;
		final Token paren;
		final List<Expr> arguments;

		Call(Expr callee, Token paren, List<Expr> arguments) {
			this.callee = callee;
			this.paren = paren;
			this.arguments = arguments;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitCallExpr(this);
		}
	}

	static class Get extends Expr {
		final Expr object;
		final Token name;

		Get(Expr object, Token name) {
			this.object = object;
			this.name = name;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitGetExpr(this);
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

	static class Lambda extends Expr {
		final List<Token> params;
		final List<Stmt> body;

		Lambda(List<Token> params, List<Stmt> body) {
			this.params = params;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitLambdaExpr(this);
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

	static class Logical extends Expr {
		final Token operator;
		final Expr left;
		final Expr right;

		Logical(Token operator, Expr left, Expr right) {
			this.operator = operator;
			this.left = left;
			this.right = right;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitLogicalExpr(this);
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

	static class Set extends Expr {
		final Expr object;
		final Token name;
		final Expr value;

		Set(Expr object, Token name, Expr value) {
			this.object = object;
			this.name = name;
			this.value = value;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitSetExpr(this);
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

	static class This extends Expr {
		final Token keyword;

		This(Token keyword) {
			this.keyword = keyword;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitThisExpr(this);
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

	static class Variable extends Expr {
		final Token name;

		Variable(Token name) {
			this.name = name;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVariableExpr(this);
		}
	}


	abstract <R> R accept(Visitor<R> visitor);
}
