package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

//* Class to automate the generation of AST node classes.
public class GenerateAST {
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("Usage: generate_ast <output directory>");
			System.exit(64);
		}

		String outputDir = args[0];

		//Create expression nodes
		defineAst(outputDir, "Expr", Arrays.asList(
				"Assign   : Token name, Expr value",
				"Binary   : Expr left, Token operator, Expr right",
				"Call     : Expr callee, Token paren, List<Expr> arguments",
				"Get      : Expr object, Token name",
				"Grouping : Expr expression",
				"Literal  : Object value",
				"Logical  : Token operator, Expr left, Expr right",
				"Set			: Expr object, Token name, Expr value",
				"Super    : Token keyword, Token method",
				"This     : Token keyword",
				"Unary    : Token operator, Expr right",
				"Variable : Token name"
		));

		// Create statement nodes
		defineAst(outputDir, "Stmt", Arrays.asList(
				"Block      : List<Stmt> statements",
				"Class      : Token name, Expr.Variable superclass, List<Stmt.Function> methods",
				"Expression : Expr expression",
				"Function   : Token name, List<Token> params, List<Stmt> body",
				"If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
				"Print      : Expr expression",
				"Return     : Token keyword, Expr value",
				"Var        : Token name, Expr initializer",
				"While      : Expr condition, Stmt body"
		));
	}
	
	//* Create an AST file from a definition.
	private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
		String path = outputDir + "/" + baseName + ".java";
		PrintWriter writer = new PrintWriter(path, "UTF-8");

		writer.println("package com.craftinginterpreters.jlox;");
		writer.println();
		writer.println("import java.util.List;");
		writer.println();

		// The base class
		writer.println("abstract class " + baseName + " {");

		// The visitor interface for dispatching method calls.
		defineVisitor(writer, baseName, types);

		// The AST classes
		for (String type : types) {
			String className = type.split(":")[0].trim(); // Name for the AST class
			String fields = type.split(":")[1].trim(); // Fields to put into the AST class
			defineType(writer, baseName, className, fields);
		}

		// The base accept() method
		writer.println();
		writer.println("	abstract <R> R accept(Visitor<R> visitor);");

		writer.println("}");
		writer.close();
	}

	//* Defines the visitor interface.
	private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
		writer.println("	interface Visitor<R> {");
		for (String type : types) {
			String typeName = type.split(":")[0].trim();
			writer.println("		R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
		}
		writer.println("	}");
		writer.println();
	}

	//* Define a subtype of the base class. Creates fields, constructor and visitor methods.
	private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
		writer.println("	static class " + className + " extends " + baseName + " {");
		String[] fields = fieldList.split(", ");

		// Fields
		for (String field : fields) {
			writer.println("		final " + field + ";");
		}
		writer.println();

		// Constructor
		writer.println("		" + className + "(" + fieldList + ") {");
		for (String field : fields) { // Store parameters into the fields
			String name = field.split(" ")[1];
			writer.println("			this." + name + " = " + name + ";");
		}
		writer.println("		}");

		// Visitor pattern functions
		writer.println();
		writer.println("		@Override");
		writer.println("		<R> R accept(Visitor<R> visitor) {");
		writer.println("			return visitor.visit" + className + baseName + "(this);");
		writer.println("		}");

		writer.println("	}");
		writer.println();

	}
}
