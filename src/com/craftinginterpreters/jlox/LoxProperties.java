package com.craftinginterpreters.jlox;

//* Collection of functions used to determine Lox language properties.
public final class LoxProperties {
	private LoxProperties() {}
	
	//* Implicitly converts any object to a boolean.
	public static boolean isTruthy(Object obj) {
		if (obj == null)
			return false;
		if (obj instanceof Boolean)
			return (boolean) obj;
		return true;
	}

	//* Compares two objects for equality.
	public static boolean isEqual(Object a, Object b) {
		if (a == null && b == null)
			return true;
		if (a == null)
			return false;
		return a.equals(b);
	}

	//* Checks if the operand is a number.
	public static void checkNumberOperand(Token operator, Object operand) {
		if (operand instanceof Double)
			return;
		throw new RuntimeError(operator, "Operand must be a number.");
	}

	//* Checks if both operands are a number.
	public static void checkNumberOperands(Token operator, Object left, Object right) {
		if (left instanceof Double && right instanceof Double)
			return;
		throw new RuntimeError(operator, "Operands must be a numbers.");
	}

	public static String stringify(Object object) {
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
