package com.craftinginterpreters.jlox;

// Implements the Visitor interface, so it requries accept methods for each type.
public class AstPrinter implements Expr.Visitor<String>{

	String print(Expr expr) {
		return expr.accept(this);
	}

	@Override
	public String visitBinaryExpr(Expr.Binary expr) {
		return parenthesize(expr.operator.lexeme, expr.left, expr.right);
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
	public String visitUnaryExpr(Expr.Unary expr) {
		return parenthesize(expr.operator.lexeme, expr.right);
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

}
