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
		while (isTruthy(evaluate(stmt.condition))) {
			execute(stmt.body);
		}

		return null;
	}
	
	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		LoxFunction function = new LoxFunction(stmt, environment, false); // Save the environment which declares the function, not calls
		environment.define(stmt.name.lexeme, function);
		return null;
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
		Object superclass = null;
		if (stmt.superclass != null) {
			superclass = evaluate(stmt.superclass);
			if (!(superclass instanceof LoxClass))
				throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
		}

		environment.define(stmt.name.lexeme, null); // Put ourselves into the environment so we can call ourself

		if (stmt.superclass != null) {
			environment = new Environment(environment); // b/c we added a scope in the resolver, we need this new scope to insert super
			environment.define("super", superclass);
		}

		Map<String, LoxFunction> methods = new HashMap<>();
		for (Stmt.Function method : stmt.methods) {
			LoxFunction function = new LoxFunction(method, environment, method.name.lexeme.equals("init")); // No anonymous functions in classes, so method.name is guarenteed
			methods.put(method.name.lexeme, function);
		}

		LoxClass klass = new LoxClass(stmt.name.lexeme, (LoxClass) superclass, methods);

		// Don't want to assign into the 'super' scope, so remove it
		if (stmt.superclass != null) {
			environment = environment.enclosing;
		}

		environment.assign(stmt.name, klass); // By splitting, methods can refer to eachother

		return null;
	}

	//~ Expression Evaluation

	@Override
	public Object visitSuperExpr(Expr.Super expr) {
		int distance = locals.get(expr); // Find where the super is at
		LoxClass superclass = (LoxClass) environment.getAt(distance, "super"); // Get the super
		LoxInstance object = (LoxInstance) environment.getAt(distance - 1, "this"); // Get the object that called the super
		// ^ works b/c we made a 1 env difference between the 'super' and 'this' scope in the resolver
		LoxFunction method = superclass.findMethod(expr.method.lexeme); // Find the method in the super

		if (method == null)
			throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");

		return method.bind(object); // Bind the superclass' method to the current object
	}

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
				return (double) left / (double) right;
			case STAR:
				checkNumberOperands(expr.operator, left, right);
				return (double) left * (double) right;

			// Mathematical addition and string concatenation
			case PLUS:
				if (left instanceof Double && right instanceof Double) {
					return (double) left + (double) right;
				}

				if (left instanceof String && right instanceof String) {
					return (String) left + (String) right;
				}

				throw new RuntimeError(expr.operator, "Operands must either be 2 numbers or 2 strings.");

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
