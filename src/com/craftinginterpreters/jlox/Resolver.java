package com.craftinginterpreters.jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

// Traverses the AST between the Parser and Interpreter steps to 'resolve' (match) all variable references to the exact version of the variable that is in scope.
// This prevents modification done after the initial declaration from affecting the original variable.
public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
	// Determines what type of function we are currently inside of.
	private enum FunctionType {
		NONE,
		FUNCTION,
		INITIALIZER,
		METHOD
	}

	private enum ClassType {
		NONE,
		CLASS,
		SUBCLASS
	}

	//* Interpreter to report variable location results to.
	private final Interpreter interpreter;
	//* Stack of scopes currently in scope. Does NOT include the global scope.
	//* The boolean tracks whether the variable is initialized (ready) or not.
	private final Stack<Map<String, Boolean>> scopes = new Stack<>();

	private FunctionType currentFunction = FunctionType.NONE;
	private ClassType currentClass = ClassType.NONE;

	Resolver(Interpreter interpreter) {
		this.interpreter = interpreter;
	}
	
	//* Resolves a list of statements.
	void resolve(List<Stmt> statements) {
		// Go through each statement and resolve it.
		for (Stmt statement : statements) {
			resolve(statement);
		}
	}

	//* Resolves a single statement.
	private void resolve(Stmt stmt) {
		stmt.accept(this);
	}

	//* Resolves a single expression.
	private void resolve(Expr expr) {
		expr.accept(this);
	}

	//* Opens a new scope
	private void beginScope() {
		scopes.push(new HashMap<String, Boolean>());
	}

	//* Closes the current scope
	private void endScope() {
		scopes.pop();
	}

	//* Declares a variable by adding it to the scopes. This is done before initialization so that it will shadow any other variables with the same name when determining initialization.
	private void declare(Token name) {
		if (scopes.isEmpty()) {
			return;
		}

		Map<String, Boolean> scope = scopes.peek();

		if (scope.containsKey(name.lexeme)) {
			Lox.error(name, "Variable with this name already declared in this scope.");
		}

		scope.put(name.lexeme, false); // Variable exists (is in scopes) but not 'ready' for use
	}

	//* Define a variable and mark it ready for use.
	private void define(Token name) {
		// This function simply updates the map value to true, indicating readiness for use
		if (scopes.isEmpty()) {
			return;
		}

		scopes.peek().put(name.lexeme, true); // Variable is ready for use
	}

	//* Look for a variable in scopes. If found, return the number of environment 'hops' to find it to the interpreter so it can find the right one.
	private void resolveLocal(Expr expr, Token name) {
		// This function counts how far it goes until it finds the variable and if found, it reports it to the interpreter who keeps that info.
		// If it isn't found, it does nothing. When the interpreter looks for the value, it will find null and know to look in the global scope.
		for (int i = scopes.size() - 1; i >= 0; i--) {
			if (scopes.get(i).containsKey(name.lexeme)) {
				interpreter.resolve(expr, scopes.size() - 1 - i);
				return;
			}
		}

		// If unresolved, assume global
	}

	//* Resolve a function's position in the scopes.
	private void resolveFunction(Stmt.Function function, FunctionType type) {
		FunctionType enclosingFunction = currentFunction; // save current enclosing function
		currentFunction = type; // Update current 'within-a-function' state

		beginScope(); // New scope for function body
		for (Token param : function.params) {
			declare(param); // Define and initialize parameters
			define(param);
		}
		resolve(function.body); // Resolve function body
		// In _runtime_, we ignore the function AST's body and only touch it on a function call
		// In _static analysis_, we immediately go into the body and perform work
		endScope();
		currentFunction = enclosingFunction; // Restore 'within-a-function' state
	}

	//~ Statements

	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		beginScope(); // Open a scope
		resolve(stmt.statements); // Traverse AST, adding to current scope
		endScope(); // Discard current scope
		return null;
	}

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		//* Add the variable to the current scope
		declare(stmt.name);
		if (stmt.initializer != null) {
			//* Resolve the initializer expression
			resolve(stmt.initializer);
		}
		// Split definition and initialization. Adds support for var declarations which refer to themselves, like 'var x = x + 1;'
		define(stmt.name);
		return null;
	}

	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		declare(stmt.name);
		define(stmt.name); // Define right after declaration to allow for recursive functions

		resolveFunction(stmt, FunctionType.FUNCTION);
		return null;
	}
	
	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		ClassType enclosingClass = currentClass;
		currentClass = ClassType.CLASS;

		declare(stmt.name); // Allows classes to be local
		define(stmt.name);

		if (stmt.superclass != null) { // Normally a global, but b/c Lox doesnt forbid local classes, we need to check for it
			currentClass = ClassType.SUBCLASS; // Update current class state
			if (stmt.name.lexeme.equals(stmt.superclass.name.lexeme))
				Lox.error(stmt.superclass.name, "A class cannot inherit from itself.");

			resolve(stmt.superclass);
		}

		// Add super to the environment of the class. Create it as a layer between the outer scope and class methods scope so it will catch
		if (stmt.superclass != null) {
			beginScope();
			scopes.peek().put("super", true);
		}

		beginScope(); // Open a scope for the class
		scopes.peek().put("this", true); // Insert 'this' into the scope b/c it isnt declared anywhere

		for (Stmt.Function method : stmt.methods) {
			FunctionType declaration = FunctionType.METHOD;
			if (method.name.lexeme.equals("init")) {
				declaration = FunctionType.INITIALIZER;
			}
			resolveFunction(method, declaration);
		}

		if (stmt.superclass != null)
			endScope();

		currentClass = enclosingClass;
		endScope();
		return null;
	}
	
	@Override
	public Void visitSuperExpr(Expr.Super expr) {
		if (currentClass == ClassType.NONE)
			Lox.error(expr.keyword, "Cannot use 'super' outside of a class.");
		else if (currentClass != ClassType.SUBCLASS)
			Lox.error(expr.keyword, "Cannot use 'super' in a class without a superclass.");

		resolveLocal(expr, expr.keyword); // Resolve as if it were a variable
		// b/c we defined super in the scope immediately before the class' we will always catch it there
		return null;
	}

	//~ Expressions

	@Override
	public Void visitVariableExpr(Expr.Variable expr) {
		//* If variable exists but is not ready, that means we are between the declaration and initialization of the variable. Thrown an error if used.
		// By doing declaration separate, we can shadow any duplicate variables
		if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
			Lox.error(expr.name, "Cannot read local variable in its own initializer.");
		}

		resolveLocal(expr, expr.name);
		return null;
	}

	@Override
	public Void visitAssignExpr(Expr.Assign expr) {
		// Resolve expression first b/c it will be evaluated first and therefore we need the correct resolution for it
		resolve(expr.value); // Resolve any variables in the expression
		resolveLocal(expr, expr.name); // Resolve the variable being assigned
		return null;
	}

	@Override
	public Void visitThisExpr(Expr.This expr) {
		if (currentClass == ClassType.NONE) {
			Lox.error(expr.keyword, "Cannot use 'this' outside of a class.");
			return null;
		}
		// 'this' acts like a variable, t.f. it is resolved in the same way as a variable.
		resolveLocal(expr, expr.keyword);
		return null;
	}

	//~ 'Simple' Traversals

	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		// In _runtime_, we are libral, only evaluating the parts which need to be executed
		// In _static analysis_, we are conservative b/c we dont know which branch will be executed. Therefore, we need to look at both
		resolve(stmt.condition);
		resolve(stmt.thenBranch);
		if (stmt.elseBranch != null) {
			resolve(stmt.elseBranch);
		}
		return null;
	}

	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		if (currentFunction == FunctionType.NONE) { // If we aren't inside anything, then using 'return' is an error
			Lox.error(stmt.keyword, "Cannot return from top-level code.");
		}

		if (stmt.value != null) {
			if (currentFunction == FunctionType.INITIALIZER)
				Lox.error(stmt.keyword, "Cannot return a value from an initializer.");
			resolve(stmt.value);
		}
		return null;
	}

	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		resolve(stmt.condition);
		resolve(stmt.body);
		return null;
	}

	@Override
	public Void visitBinaryExpr(Expr.Binary expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitCallExpr(Expr.Call expr) {
		resolve(expr.callee);

		for (Expr argument : expr.arguments) {
			resolve(argument);
		}

		return null;
	}
	
	@Override
	public Void visitGroupingExpr(Expr.Grouping expr) {
		resolve(expr.expression);
		return null;
	}


	@Override
	public Void visitLiteralExpr(Expr.Literal expr) {
		return null;
	}

	@Override
	public Void visitLogicalExpr(Expr.Logical expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitUnaryExpr(Expr.Unary expr) {
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitGetExpr(Expr.Get expr) {
		resolve(expr.object);
		// Properties are looked up dynamically, so we don't need to do anything here for expr.name
		return null;
	}

	@Override
	public Void visitSetExpr(Expr.Set expr) {
		resolve(expr.value);
		resolve(expr.object);
		// Properties are looked up dynamically, so we don't need to do anything here for expr.name
		return null;
	}

}
