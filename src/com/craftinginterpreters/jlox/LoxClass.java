package com.craftinginterpreters.jlox;

import java.util.List;
import java.util.Map;

public class LoxClass extends LoxInstance implements LoxCallable {
	final String name;
	private LoxClass metaclass; // Allows for static class methods
	private LoxClass superclass; // Allows for inheritance
	private final Map<String, LoxFunction> methods; // Methods for all instances of this class
		
	LoxClass(String name, LoxClass metaclass, LoxClass superclass, Map<String, LoxFunction> methods) {
		super(metaclass); // Can be null
		this.name = name;
		this.superclass = superclass;
		this.methods = methods;
	}

	public LoxFunction findMethod(String name) {
		if (methods.containsKey(name)) {
			return methods.get(name);
		}else if (metaclass != null){
			return metaclass.findMethod(name);
		}
		
		if (superclass != null)
			return superclass.findMethod(name);

		return null;
	}

	// LoxCallable -> When called, a new instance of this class is created and returned.
	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		LoxInstance instance = new LoxInstance(this);
		LoxFunction initializer = findMethod("init"); // Find initializer method and if present, call it with the arguments passed to it
		if (initializer != null) {
			initializer.bind(instance).call(interpreter, arguments);
		}
		return instance;
	}

	@Override
	public int arity() {
		LoxFunction initializer = findMethod("init");
		if (initializer == null)
			return 0;
		return initializer.arity();
	}

	@Override
	public String toString() {
		return "<class " + name + ">";
	}
}
