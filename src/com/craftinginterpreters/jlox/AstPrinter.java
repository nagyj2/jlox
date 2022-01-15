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
