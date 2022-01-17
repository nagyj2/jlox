package com.craftinginterpreters.jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	//* Top level environment. Stays fixed for the interpreter.
	final Environment globals = new Environment();
	//* Current environment for the interpreter. Starts with the global environment.
	private Environment environment = globals;
	//* Associates an AST node with the results of the resolver (how many environments to peel back to find the variable)
	private final Map<Expr, Integer> locals = new HashMap<>();

	Interpreter() {
		// Create a native function with a Java anonymous class
		globals.define("clock", true, new LoxCallable() {
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

	//* Tells the interpreter how many environments to skip to get to the desired variable.
	void resolve(Expr expr, int depth) {
		locals.put(expr, depth);
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

		environment.define(stmt.name, stmt.constant, value);
		if (stmt.next != null)
			visitVarStmt((Stmt.Var) stmt.next);
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
		LoxFunction function = new LoxFunction(stmt, environment, false); // Save the environment which declares the function, not calls
		environment.define(stmt.name.lexeme, false, function);
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

	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		environment.define(stmt.name.lexeme, false, null); // Classes ARE NOT constant to allow recursion
		
		Map<String, LoxFunction> classmethods = new HashMap<>();
		Map<String, LoxFunction> staticmethods = new HashMap<>();

		for (Stmt.Function function : stmt.classmethods) {
			LoxFunction classmethod = new LoxFunction(function, environment, function.name.lexeme.equals("init")); // No anonymous functions in classes, so method.name is guarenteed
			classmethods.put(function.name.lexeme, classmethod);
		}

		for (Stmt.Function function : stmt.staticmethods) {
			LoxFunction staticmethod = new LoxFunction(function, environment, false);
			staticmethods.put(function.name.lexeme, staticmethod);
		}

		LoxClass metaklass = new LoxClass(null, stmt.name.lexeme, staticmethods);
		LoxClass klass = new LoxClass(metaklass, stmt.name.lexeme, classmethods);
		environment.assign(stmt.name, klass); // By splitting, methods can refer to eachother

		return null;
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
		// return environment.get(expr.name);
		return lookUpVariable(expr.name, expr);
	}

	@Override
	public Object visitThisExpr(Expr.This expr) {
		// return environment.get(expr.name);
		return lookUpVariable(expr.keyword, expr);
	}

	@Override
	public Object visitAssignExpr(Expr.Assign expr) {
		Object value = evaluate(expr.value);

		Integer distance = locals.get(expr);
		if (distance != null) {
			environment.assignAt(distance, expr.name, value);
		} else {
			globals.assign(expr.name, value);
		}
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
			throw new RuntimeError(expr.paren,
					"Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
		}

		return function.call(this, arguments); // Simply return whatever the call() returns
	}
	
	@Override
	public Object visitLambdaExpr(Expr.Lambda expr) {
		LoxFunction function = new LoxFunction(new Stmt.Function(null, expr.params, expr.body), environment, false); // Save the environment which declares the function, not calls
		return function;
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

	@Override
	public Object visitGetExpr(Expr.Get expr) {
		Object object = evaluate(expr.object); // What object are we getting from?
		if (object instanceof LoxInstance) {
			return ((LoxInstance) object).get(expr.name);
		}

		throw new RuntimeError(expr.name, "Only instances have properties.");
	}

	@Override
	public Object visitSetExpr(Expr.Set expr) {
		Object object = evaluate(expr.object);

		if (!(object instanceof LoxInstance)) {
			throw new RuntimeError(expr.name, "Only instances have fields.");
		}

		Object value = evaluate(expr.value);
		((LoxInstance) object).set(expr.name, value);
		return value;
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

	//* Looks up a variable in the environemnt. Recieves delegations where the number of environments to skip is specified.
	private Object lookUpVariable(Token name, Expr expr) {
		// Get resolver distance information
		Integer distance = locals.get(expr);
		if (distance != null) {
			return environment.getAt(distance, name.lexeme);
		} else { // If no distance info, we assumed global scope, so look there
			return globals.get(name);
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
