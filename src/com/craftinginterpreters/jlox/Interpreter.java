package com.craftinginterpreters.jlox;

import java.util.ArrayList;
import java.util.List;

class RuntimeError extends RuntimeException {
	final Token token;

	public RuntimeError(Token token, String message) {
		super(message);
		this.token = token;
	}
}

class Break extends RuntimeException {
	final Token token;

	public Break(Token token) {
		super(null, null, false, false); // Used for control flow, so lighten the overhead
		this.token = token;
	}
}

class Return extends RuntimeException {
	final Object value;

	public Return(Object value) {
		super(null, null, false, false);
		this.value = value;
	}
}

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	//* Top level environment. Stays fixed for the interpreter.
	final Environment globals = new Environment();
	//* Current environment for the interpreter. Starts with the global environment.
	private Environment environment = globals;

	Interpreter() {
		// Create a native function with a Java anonymous class
		globals.define("clock", new LoxCallable() {
			@Override
			public int arity() { return 0; }

			@Override
			public Object call(Interpreter interpreter, List<Object> arguments) {
				return (double) System.currentTimeMillis() / 1000.0; // Retuns the current time in seconds.
			}
		});
	}
	
	//* Start the evaluation of a program.
	public void interpret(List<Stmt> statements) {
		try {
			for (Stmt statement : statements) {
				execute(statement);
			}
		} catch (RuntimeError error) {
			Lox.runtimeError(error);
		} catch (Break error) {
			Lox.runtimeError(new RuntimeError(error.token, "Unexpected '" + error.token.lexeme + "' outside loop or 'do'."));
		}
	}
	
	//* Starts the expression evaluation process.
	private Object evaluate(Expr expr) {
		return expr.accept(this);
	}

	//* Starts the statement evaluation process.
	private void execute(Stmt statement) {
		statement.accept(this);
	}

	//~ Statement Evaluation

	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		evaluate(stmt.expression);
		// if (isREPL)
		// 	System.out.println(stringify(value));
		return null;
	}

	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		Object value = evaluate(stmt.expression);
		System.out.println(stringify(value));
		return null;
	}

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		Object value = null;
		if (stmt.initializer != null) {
			value = evaluate(stmt.initializer);
		}

		environment.define(stmt.name.lexeme, value);
		return null;
	}

	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		executeBlock(stmt.statements, new Environment(environment));
		return null;
	}

	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		if (isTruthy(evaluate(stmt.condition))) {
			execute(stmt.thenBranch);
		} else if (stmt.elseBranch != null) {
			execute(stmt.elseBranch);
		}
		return null;
	}

	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		try {
			while (isTruthy(evaluate(stmt.condition))) {
				execute(stmt.body);
			}
		} catch (Break error) {
			return null;
		}

		return null;
	}
	
	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		LoxFunction function = new LoxFunction(stmt, environment); // Save the environment which declares the function, not calls
		environment.define(stmt.name.lexeme, function);
		return null;
	}

	@Override
	public Void visitDoStmt(Stmt.Do stmt) {
		try {
			if (stmt.condition == null) {
				execute(stmt.body);
			} else {
				do {
					execute(stmt.body);
				} while (isTruthy(evaluate(stmt.condition)));
			}

		} catch (Break error) {
			return null;
		}

		return null;
	}
	
	@Override
	public Void visitBreakStmt(Stmt.Break stmt) {
		throw new Break(stmt.token);
	}

	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		Object value = null;
		if (stmt.value != null) {
			value = evaluate(stmt.value);
		}

		throw new Return(value);
	}

	//~ Expression Evaluation

	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
			case MINUS:
				checkNumberOperands(expr.operator, left, right);
				return (double) left - (double) right;
			case SLASH:
				checkNumberOperands(expr.operator, left, right);
				if ((double) right == 0)
					throw new RuntimeError(expr.operator, "Division by 0.");
				return (double) left / (double) right;
			case STAR:
				checkNumberOperands(expr.operator, left, right);
				return (double) left * (double) right;

			// Mathematical addition and string concatenation
			case PLUS:
				if (left instanceof String) {
					return (String) left + stringify(right);
				}

				if (right instanceof String) {
					return stringify(left) + (String) right;
				}

				if (left instanceof Double && right instanceof Double) {
					return (double) left + (double) right;
				}

				throw new RuntimeError(expr.operator, "Operands must either be 2 numbers or >=1 strings.");

			case LESSER:
				checkNumberOperands(expr.operator, left, right);
				return (double) left < (double) right;
			case GREATER:
				checkNumberOperands(expr.operator, left, right);
				return (double) left > (double) right;
			case LESSER_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double) left <= (double) right;
			case GREATER_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double) left >= (double) right;

			case EQUAL_EQUAL:
				return isEqual(left, right);
			case BANG_EQUAL:
				return !isEqual(left, right);

			default:
				break;
		}

		return null;
	}
	
	@Override
	public Object visitLogicalExpr(Expr.Logical expr) {
		// Implements short circuiting!
		Object left = evaluate(expr.left);

		if (expr.operator.type == TokenType.OR) {
			if (isTruthy(left)) // b/c Lox is dynamically typed, look for truthiness and return that same truthiness.
				return left; // If the entire expression can be determined, simply return it
		} else { // AND
			if (!isTruthy(left))
				return left;
		}

		return evaluate(expr.right);
	}
	
	@Override
	public Object visitGroupingExpr(Expr.Grouping expr) {
		return evaluate(expr.expression);
	}
	
	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		return expr.value;
	}
	
	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
			case MINUS:
				checkNumberOperand(expr.operator, right);
				return -(double) right; // b/c lox is dynamically typed, we don't know if this cast will work or not
			case BANG:
				return !isTruthy(right);
			default:
				break;
		}

		return null;
	}

	@Override
	public Object visitVariableExpr(Expr.Variable expr) {
		return environment.get(expr.name);
	}

	@Override
	public Object visitAssignExpr(Expr.Assign expr) {
		Object value = evaluate(expr.value);

		environment.assign(expr.name, value);
		return value;
	}

	@Override
	public Object visitCallExpr(Expr.Call expr) {
		Object callee = evaluate(expr.callee);

		List<Object> arguments = new ArrayList<>();
		for (Expr argument : expr.arguments) {
			arguments.add(evaluate(argument));
		}

		// Protect against non-callables being called, like 3.14() or "hello"()
		if (!(callee instanceof LoxCallable)) {
			throw new RuntimeError(expr.paren, "Can only call functions and classes");
		}

		LoxCallable function = (LoxCallable) callee; // Cast to a callable and then invoke call. All callables are implement LoxCallable

		// Check function arity. Raise error if not enough/ too many are passed
		if (arguments.size() < function.arity()) {
			throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
		}

		return function.call(this, arguments); // Simply return whatever the call() returns
	}
	
	@Override
	public Object visitSequenceExpr(Expr.Sequence expr) {
		// Object first = evaluate(expr.first); // According to the book, the first expression is discarded. Therefore, it is so here.
		evaluate(expr.first);
	
		Object second = evaluate(expr.second);
	
		return second;
	}
	
	@Override
	public Object visitTernaryExpr(Expr.Ternary expr) {
		Object condition = evaluate(expr.left);

		if (isTruthy(condition)) {
			return evaluate(expr.center);
		} else {
			return evaluate(expr.right);
		}
	}
	
	//~ Helper Functions

	//* Evaluates all statements in a a block.
	public void executeBlock(List<Stmt> statements, Environment environment) {
		Environment previous = this.environment;
		try {
			this.environment = environment;

			for (Stmt statement : statements) {
				execute(statement);
			}
		} finally { // Use 'finally' so that if an exception is thrown, it still gets updated
			this.environment = previous;
		}
	}

	//* Implicitly converts any object to a boolean.
	private boolean isTruthy(Object obj) {
		if (obj == null)
			return false;
		if (obj instanceof Boolean)
			return (boolean) obj;
		return true;
	}

	//* Compares two objects for equality.
	private boolean isEqual(Object a, Object b) {
		if (a == null && b == null)
			return true;
		if (a == null)
			return false;
		return a.equals(b);
	}

	//* Checks if the operand is a number.
	private void checkNumberOperand(Token operator, Object operand) {
		if (operand instanceof Double)
			return;
		throw new RuntimeError(operator, "Operand must be a number.");
	}

	//* Checks if both operands are a number.
	private void checkNumberOperands(Token operator, Object left, Object right) {
		if (left instanceof Double && right instanceof Double)
			return;
		throw new RuntimeError(operator, "Operands must be a numbers.");
	}

	private String stringify(Object object) {
		if (object == null)
			return "nil";

		// Special logic for int vs double
		if (object instanceof Double) {
			String text = object.toString();
			if (text.endsWith(".0"))
				text = text.substring(0, text.length() - 2);
			return text;
		}

		return object.toString();
	}
	
}
