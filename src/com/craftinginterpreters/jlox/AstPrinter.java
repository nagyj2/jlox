package com.craftinginterpreters.jlox;

import java.util.List;

// Implements the Visitor interface, so it requries accept methods for each type.
public class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {

	String print(Expr expr) {
		return expr.accept(this);
	}

	String print(Stmt stmt) {
		return stmt.accept(this);
	}

	String print(List<Stmt> stmts) {
		StringBuilder builder = new StringBuilder();

		for (Stmt stmt : stmts) {
			builder.append(stmt.accept(this)).append("\n");
		}
		return builder.toString();
	}

	@Override
	public String visitIfStmt(Stmt.If stmt) {
		StringBuilder builder = new StringBuilder();
		builder.append("(").append("if");
		builder.append(print(stmt.thenBranch));
		if (stmt.elseBranch != null) {
			builder.append(" ");
			builder.append(print(stmt.elseBranch));
		}
		builder.append(")");

		return builder.toString();
	}

	@Override
	public String visitWhileStmt(Stmt.While stmt) {
		StringBuilder builder = new StringBuilder();
		builder.append("(").append("while");
		builder.append(" ");
		builder.append(print(stmt.body));
		builder.append(")");

		return builder.toString();
	}

	@Override
	public String visitDoStmt(Stmt.Do stmt) {
		StringBuilder builder = new StringBuilder();
		builder.append("(").append("do");
		builder.append(" ");
		builder.append(print(stmt.body));
		builder.append(" ");
		builder.append(print(stmt.condition));
		builder.append(")");

		return builder.toString();
	}

	@Override
	public String visitBreakStmt(Stmt.Break stmt) {
		return "break";
	}

	//~ Expressions

	@Override
	public String visitBinaryExpr(Expr.Binary expr) {
		return parenthesize(expr.operator.lexeme, expr.left, expr.right);
	}

	@Override
	public String visitUnaryExpr(Expr.Unary expr) {
		return parenthesize(expr.operator.lexeme, expr.right);
	}

	@Override
	public String visitTernaryExpr(Expr.Ternary expr) {
		return parenthesize(expr.operator.lexeme, expr.left, expr.center, expr.right);
	}

	@Override
	public String visitLogicalExpr(Expr.Logical expr) {
		return parenthesize(expr.operator.lexeme, expr.left, expr.right);
	}

	@Override
	public String visitGroupingExpr(Expr.Grouping expr) {
		return parenthesize("grouping", expr.expression);
	}

	@Override
	public String visitLiteralExpr(Expr.Literal expr) {
		// Literals cannot have subexpressions, so we don't need to call parenthesize.
		if (expr.value == null)
			return "nil";
		return expr.value.toString();
	}

	@Override
	public String visitSequenceExpr(Expr.Sequence expr) {
		return parenthesize("sequence", expr.first, expr.second);
	}

	@Override
	public String visitVariableExpr(Expr.Variable expr) {
		return expr.name.lexeme;
	}

	@Override
	public String visitAssignExpr(Expr.Assign expr) {
		return parenthesize("assign", new Expr.Literal(expr.name), expr.value);
	}

	@Override
	public String visitCallExpr(Expr.Call expr) {
		return parenthesize("call " + print(expr.callee), expr.arguments.toArray(new Expr[0]));
	}

	@Override
	public String visitLambdaExpr(Expr.Lambda expr) {
		StringBuilder builder = new StringBuilder();

		builder.append("(").append("lambda");
		for (Token param : expr.params) {
			builder.append(" ");
			builder.append(param.lexeme);
		}
		builder.append(" ").append(print(expr.body));
		builder.append(")");
		return builder.toString();
	}
	
	//~ Statements

	@Override
	public String visitBlockStmt(Stmt.Block stmt) {
		return encase(stmt.statements);
	}

	@Override
	public String visitExpressionStmt(Stmt.Expression stmt) {
		return parenthesize("expr", stmt.expression);
	}

	@Override
	public String visitPrintStmt(Stmt.Print stmt) {
		return parenthesize("print", stmt.expression);
	}

	@Override
	public String visitVarStmt(Stmt.Var stmt) {
		return parenthesize("var", new Expr.Literal(stmt.name), stmt.initializer);
	}

	@Override
	public String visitFunctionStmt(Stmt.Function stmt) {
		StringBuilder builder = new StringBuilder();

		builder.append("(").append(stmt.name.lexeme);
		for (Token param : stmt.lambda.params) {
			builder.append(" ");
			builder.append(param.lexeme);
		}
		builder.append(" ").append(print(stmt.lambda.body));
		builder.append(")");
		return builder.toString();
	}

	@Override
	public String visitClassStmt(Stmt.Class stmt) {
		StringBuilder builder = new StringBuilder();

		builder.append("(class ").append(stmt.name.lexeme);
		for (Stmt method : stmt.classmethods) {
			builder.append(" ");
			builder.append(method.accept(this));
		}
		builder.append(")");
		return builder.toString();
	}

	@Override
	public String visitReturnStmt(Stmt.Return stmt) {
		return parenthesize("return", stmt.value);
	}

	@Override
	public String visitGetExpr(Expr.Get expr) {
		return parenthesize("get " + expr.name.lexeme, expr.object);
	}

	@Override
	public String visitSetExpr(Expr.Set expr) {
		return parenthesize("set " + expr.name.lexeme, expr.object, expr.value);
	}

	@Override
	public String visitThisExpr(Expr.This expr) {
		return "this";
	}

	@Override
	public String visitSuperExpr(Expr.Super expr) {
		return "(super " + expr.method + ")";
	}

	//~ Helper Functions

	private String parenthesize(String name, Expr... exprs) {
		StringBuilder builder = new StringBuilder();

		builder.append("(").append(name);
		for (Expr expr : exprs) {
			builder.append(" ");
			builder.append(expr.accept(this));
		}
		builder.append(")");
		return builder.toString();
	}

	private String encase(List<Stmt> stmts) {
		StringBuilder builder = new StringBuilder();

		builder.append("{");
		for (Stmt stmt : stmts) {
			builder.append(" ");
			builder.append(stmt.accept(this));
		}
		builder.append("}");
		return builder.toString();
	}

}
