package com.craftinginterpreters.jlox;

import java.util.List;

abstract class Stmt {
	interface Visitor<R> {
		R visitBlockStmt(Block stmt);
		R visitBreakStmt(Break stmt);
		R visitClassStmt(Class stmt);
		R visitDoStmt(Do stmt);
		R visitExpressionStmt(Expression stmt);
		R visitFunctionStmt(Function stmt);
		R visitIfStmt(If stmt);
		R visitPrintStmt(Print stmt);
		R visitReturnStmt(Return stmt);
		R visitVarStmt(Var stmt);
		R visitWhileStmt(While stmt);
	}

	static class Block extends Stmt {
		final List<Stmt> statements;

		Block(List<Stmt> statements) {
			this.statements = statements;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBlockStmt(this);
		}
	}

	static class Break extends Stmt {
		final Token token;

		Break(Token token) {
			this.token = token;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBreakStmt(this);
		}
	}

	static class Class extends Stmt {
		final Token name;
		final List<Stmt.Function> methods;

		Class(Token name, List<Stmt.Function> methods) {
			this.name = name;
			this.methods = methods;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitClassStmt(this);
		}
	}

	static class Do extends Stmt {
		final Stmt body;
		final Expr condition;

		Do(Stmt body, Expr condition) {
			this.body = body;
			this.condition = condition;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitDoStmt(this);
		}
	}

	static class Expression extends Stmt {
		final Expr expression;

		Expression(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitExpressionStmt(this);
		}
	}

	static class Function extends Stmt {
		final Token name;
		final List<Token> params;
		final List<Stmt> body;

		Function(Token name, List<Token> params, List<Stmt> body) {
			this.name = name;
			this.params = params;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitFunctionStmt(this);
		}
	}

	static class If extends Stmt {
		final Expr condition;
		final Stmt thenBranch;
		final Stmt elseBranch;

		If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitIfStmt(this);
		}
	}

	static class Print extends Stmt {
		final Expr expression;

		Print(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitPrintStmt(this);
		}
	}

	static class Return extends Stmt {
		final Token keyword;
		final Expr value;

		Return(Token keyword, Expr value) {
			this.keyword = keyword;
			this.value = value;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitReturnStmt(this);
		}
	}

	static class Var extends Stmt {
		final Token name;
		final Expr initializer;
		final boolean constant;
		final Stmt next;

		Var(Token name, Expr initializer, boolean constant, Stmt next) {
			this.name = name;
			this.initializer = initializer;
			this.constant = constant;
			this.next = next;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVarStmt(this);
		}
	}

	static class While extends Stmt {
		final Expr condition;
		final Stmt body;

		While(Expr condition, Stmt body) {
			this.condition = condition;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitWhileStmt(this);
		}
	}


	abstract <R> R accept(Visitor<R> visitor);
}
