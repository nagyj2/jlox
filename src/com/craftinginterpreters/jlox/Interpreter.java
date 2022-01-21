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
		} catch (Exception.Runtime error) {
			Lox.runtimeError(error);
		} catch (Exception.Break error) {
			Lox.runtimeError(new Exception.Runtime(error.token, "Unexpected '" + error.token.lexeme + "' outside loop or 'do'."));
		} catch (Exception.Panic error) {
			Lox.runtimeError(error);
		} catch (Exception.Generic error) {
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
		// 	System.out.println(LoxProperties.stringify(value));
		return null;
	}

	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		Object value = evaluate(stmt.expression);
		System.out.println(LoxSemantics.stringify(value));
		return null;
	}

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		Object value = null;
		if (stmt.initializer != null) {
			value = evaluate(stmt.initializer);
		}

		environment.define(stmt.name, stmt.constant, value);
		return null;
	}

	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		executeBlock(stmt.statements, new Environment(environment));
		return null;
	}

	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		if (LoxSemantics.isTruthy(evaluate(stmt.condition))) {
			execute(stmt.thenBranch);
		} else if (stmt.elseBranch != null) {
			execute(stmt.elseBranch);
		}
		return null;
	}

	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		try {
			while (LoxSemantics.isTruthy(evaluate(stmt.condition))) {
				execute(stmt.body);
			}
		} catch (Exception.Break error) {
			return null;
		}

		return null;
	}
	
	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		LoxFunction function = new LoxFunction(stmt.lambda, environment, false); // Save the environment which declares the function, not calls
		environment.define(stmt.name.lexeme, stmt.constant, function);
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
				} while (LoxSemantics.isTruthy(evaluate(stmt.condition)));
			}

		} catch (Exception.Break error) {
			return null;
		}

		return null;
	}
	
	@Override
	public Void visitBreakStmt(Stmt.Break stmt) {
		throw new Exception.Break(stmt.token);
	}

	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		Object value = null;
		if (stmt.value != null) {
			value = evaluate(stmt.value);
		}

		throw new Exception.Return(value);
	}

	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		Object superclass = null; // Setup superclass
		if (stmt.superclass != null) {
			superclass = evaluate(stmt.superclass);
			if (!(superclass instanceof LoxClass))
				throw new Exception.Runtime(stmt.superclass.name, "Superclass must be a class.");
		}

		environment.define(stmt.name.lexeme, false, null); // Classes ARE NOT constant to allow recursion

		if (stmt.superclass != null) {
			environment = new Environment(environment); // b/c we added a scope in the resolver, we need this new scope to insert super
			environment.define("super", true, superclass); // Disallow altering super. May change going forward?
		}

		Map<String, LoxFunction> classmethods = new HashMap<>();
		Map<String, LoxFunction> staticmethods = new HashMap<>();

		for (Stmt.Function function : stmt.classmethods) {
			LoxFunction classmethod = new LoxFunction(function.lambda, environment, function.name.lexeme.equals("init")); // No anonymous functions in classes, so method.name is guarenteed
			classmethods.put(function.name.lexeme, classmethod);
		}

		for (Stmt.Function function : stmt.staticmethods) {
			LoxFunction staticmethod = new LoxFunction(function.lambda, environment, false);
			staticmethods.put(function.name.lexeme, staticmethod);
		}

		LoxClass metaklass = new LoxClass(stmt.name.lexeme, null, (LoxClass) superclass, staticmethods); // No metaclass for generated metaclasses, but it does get the superclass
		LoxClass klass = new LoxClass(stmt.name.lexeme, metaklass, (LoxClass) superclass, classmethods);

		// Don't want to assign into the 'super' scope, so remove it
		if (stmt.superclass != null) {
			environment = environment.enclosing;
		}

		environment.assign(stmt.name, klass); // By splitting, methods can refer to eachothe
		environment.updateConstant(stmt.name, stmt.constant); // After creating class, set its const status

		return null;
	}

	@Override
	public Void visitTryStmt(Stmt.Try stmt) {
		
		try {
			execute(stmt.body);
		} catch (Exception.Panic panic) {
			if (stmt.catches.containsKey(panic.code)) {
				execute(stmt.catches.get(panic.code));
			} else if (stmt.catches.containsKey(-1.0)) {
				execute(stmt.catches.get(-1.0));
			} else {
				throw panic;
			}
		}

		return null;
	}
	
	@Override
	public Void visitPanicStmt(Stmt.Panic stmt) {
		throw new Exception.Panic(stmt.code, "Uncaught panic: Code " + stmt.code);
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
			throw new Exception.Runtime(expr.method, "Undefined property '" + expr.method.lexeme + "'.");

		return method.bind(object); // Bind the superclass' method to the current object
	}

	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
			case MINUS:
				LoxSemantics.checkNumberOperands(expr.operator, left, right);
				return (double) left - (double) right;
			case SLASH:
				LoxSemantics.checkNumberOperands(expr.operator, left, right);
				if ((double) right == 0)
					throw new Exception.Runtime(expr.operator, "Division by 0.");
				return (double) left / (double) right;
			case STAR:
				LoxSemantics.checkNumberOperands(expr.operator, left, right);
				return (double) left * (double) right;

			// Mathematical addition and string concatenation
			case PLUS:
				if (left instanceof String) {
					return (String) left + LoxSemantics.stringify(right);
				}

				if (right instanceof String) {
					return LoxSemantics.stringify(left) + (String) right;
				}

				if (left instanceof Double && right instanceof Double) {
					return (double) left + (double) right;
				}

				throw new Exception.Runtime(expr.operator, "Addition requires 2 numbers or at least one string.");

			case PLUS_PLUS:
				if (left instanceof List) {
					((List<Object>) left).add(right);
					return left;
				}

				if (right instanceof List) {
					((List<Object>) right).add(0, left);
					return right;
				}

				throw new Exception.Runtime(expr.operator, "Appending requires at least one list.");

			case LESSER:
				LoxSemantics.checkNumberOperands(expr.operator, left, right);
				return (double) left < (double) right;
			case GREATER:
				LoxSemantics.checkNumberOperands(expr.operator, left, right);
				return (double) left > (double) right;
			case LESSER_EQUAL:
				LoxSemantics.checkNumberOperands(expr.operator, left, right);
				return (double) left <= (double) right;
			case GREATER_EQUAL:
				LoxSemantics.checkNumberOperands(expr.operator, left, right);
				return (double) left >= (double) right;

			case EQUAL_EQUAL:
				return LoxSemantics.isEqual(left, right);
			case BANG_EQUAL:
				return !LoxSemantics.isEqual(left, right);

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
			if (LoxSemantics.isTruthy(left)) // b/c Lox is dynamically typed, look for truthiness and return that same truthiness.
				return left; // If the entire expression can be determined, simply return it
		} else { // AND
			if (!LoxSemantics.isTruthy(left))
				return left;
		}

		return evaluate(expr.right);
	}
	
	@Override
	public Object visitGroupingExpr(Expr.Grouping expr) {
		return evaluate(expr.expression);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Object visitLiteralExpr(Expr.Literal expr) {
		if (expr.value instanceof List) {
			List<Object> list = new ArrayList<>();
			for (Expr elem : (List<Expr>) (expr.value)) {
				list.add(evaluate(elem));
			}
			return list;
		}
		return expr.value;
	}
	
	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
			case MINUS:
				LoxSemantics.checkNumberOperand(expr.operator, right);
				return -(double) right; // b/c lox is dynamically typed, we don't know if this cast will work or not
			case BANG:
				return !LoxSemantics.isTruthy(right);
			case MINUS_MINUS:
			case MINUS_MINUS_POST:
				if (right instanceof List) {
					List<Object> list = (List<Object>) right;
					if (list.size() == 0)
						throw new Exception.Runtime(expr.operator, "Cannot remove from empty list");
					Object removed;
					if (expr.operator.type == TokenType.MINUS_MINUS)
						removed = list.remove(0);
					else
						removed = list.remove(list.size() - 1);
					return removed; // return popped element
					// return right; // return list
				}

				return new Exception.Runtime(expr.operator, "Remove operator requires a list.");

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
			throw new Exception.Runtime(expr.paren, "Can only call functions and classes");
		}

		LoxCallable function = (LoxCallable) callee; // Cast to a callable and then invoke call. All callables are implement LoxCallable

		// Check function arity. Raise error if not enough/ too many are passed
		if (arguments.size() < function.arity()) {
			throw new Exception.Runtime(expr.paren,
					"Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
		}

		return function.call(this, arguments); // Simply return whatever the call() returns
	}
	
	@Override
	public Object visitLambdaExpr(Expr.Lambda expr) {
		LoxFunction function = new LoxFunction(expr, environment, false); // Save the environment which declares the function, not calls
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

		if (LoxSemantics.isTruthy(condition)) {
			return evaluate(expr.center);
		} else {
			return evaluate(expr.right);
		}
	}

	@Override
	public Object visitGetExpr(Expr.Get expr) {
		Object object = evaluate(expr.object); // What object are we getting from?
		if (object instanceof LoxInstance) {
			Object data = ((LoxInstance) object).get(expr.name);
			if (data instanceof LoxFunction && ((LoxFunction) data).isGetter())
				return ((LoxFunction) data).call(this, null);
			return data;
		}

		throw new Exception.Runtime(expr.name, "Only instances have properties.");
	}

	@Override
	public Object visitSetExpr(Expr.Set expr) {
		Object object = evaluate(expr.object);

		if (!(object instanceof LoxInstance)) {
			throw new Exception.Runtime(expr.name, "Only instances have fields.");
		}

		Object value = evaluate(expr.value);
		((LoxInstance) object).set(expr.name, value);
		return value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object visitIndexExpr(Expr.Index expr) {
		Object index = evaluate(expr.index);

		if (!(index instanceof Double) && (Double) index % 1 == 0) {
			throw new Exception.Runtime(expr.position, "Index must be an integer.");
		}
		int intdex = ((Double) index).intValue();

		Object object = evaluate(expr.object);

		if (!(object instanceof List)) {
			throw new Exception.Runtime(expr.position, "Only lists can be indexed.");
		}

		if (intdex < 0 || intdex >= ((List<Object>) object).size()) {
			throw new Exception.Runtime(expr.position, "Index out of bounds.");
		}

		return ((List<Object>) object).get(intdex);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object visitPlaceExpr(Expr.Place expr) {
		Object index = evaluate(expr.index);

		if (!(index instanceof Double) && (Double) index % 1 == 0) {
			throw new Exception.Runtime(expr.position, "Index must be an integer.");
		}
		int intdex = ((Double) index).intValue();

		Object object = evaluate(expr.object);

		if (!(object instanceof List)) {
			throw new Exception.Runtime(expr.position, "Only lists can be indexed.");
		}

		if (intdex < 0 || intdex >= ((List<Object>) object).size()) {
			throw new Exception.Runtime(expr.position, "Index out of bounds.");
		}

		Object value = evaluate(expr.value);

		((List<Object>) object).set(intdex, value);
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
}
