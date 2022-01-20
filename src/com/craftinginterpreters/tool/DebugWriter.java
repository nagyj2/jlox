package com.craftinginterpreters.tool;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;

public class DebugWriter {
	
	private final String resolverPath = "src/com/craftinginterpreters/gen/resolverScopes.txt";
	private final String environmentPath = "src/com/craftinginterpreters/gen/environmentScopes.txt";
	
	private PrintWriter writer;
	private int depth; // Current depth

	public DebugWriter() {

		depth = 0;
		setPath(true);
	}

	public void setPath(boolean isResolver) {
		
		if (writer != null) {
			writer.close();
		}
		
		String path = isResolver ? resolverPath : environmentPath;
		try {
			writer = new PrintWriter(path);
		} catch (FileNotFoundException e) {
			System.err.println("Could not open file: " + path);
			e.printStackTrace();
		}
	}

	public void newScope(String name) {
		writeLine("new scope " + name);
		depth++;
	}

	public void endScope(Map<String, Boolean> scope) {
		for (String name : scope.keySet()) {
			writeLine("deleted " + name);
		}
		depth--;
		writeLine("end scope");
	}

	public void endScope() {
		// for (String name : scope.keySet()) {
		// 	writeLine("deleted " + name);
		// }
		depth--;
	}

	public void declareVar(String name) {
		writeLine("declared " + name);
	}

	public void resolveVar(String name, int resolution) {
		writeLine("resolved " + name + " @" + resolution);
	}

	public void close() {
		writer.close();
	}

	private void writeLine(String item) {
		writer.println(genString(depth, item));
	}

	private String genString(int depth, String item) {
		return "-|".repeat(depth) + " " + item;
	}

}
