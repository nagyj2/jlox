## jlox

jlox (jay-locks) is a small language defined in the book 'Crafting Interpreters'. This project implements the language as it appears in the book.

### Grammar

Below is the grammar for xlox up to this point:
```
expression      -> equality ;
equality        -> comparison ( ( "==" | "!=" ) comparison )* ;
comparison      -> term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term            -> factor ( ( "+" | "-" ) factor )* ;
factor          -> unary ( ( "*" | "/" ) unary )* ;
unary           -> ( "!" | "-" ) unary 
                 | unary ;
primary         -> NUMBER
                 | STRING
                 | "true" | "false" | "nil" ;
                 | "(" expression ")"
```

### Types

jlox is a dynamically typed language which supports the following types:
- Booleans
- Numbers (integers and floating point)
- Strings
- Nil

### Operations

jlox supports the following operations:
- Arithmetic
  - Addition
  - Subtraction
  - Multiplication
  - Division
  - Negation
- String
  - Concatenate
- Comparisons
  - Less than
  - Greater than
  - Less than or equal to
  - Greater than or equal to
  - Equality
  - Inequality
- Logical
  - Conjunction
  - Disjunction
  - Negation
- Precidence
  - Grouping

### Statements

All statements in jlox end with a semicolon. A semicolon can also be added to a value expression to promote it to an expression statement. jlox uses C-like syntax for creating code blocks. Variables can be created with the 'var' keyword and have the default value of nil unless an initializer is provided. Printing is also supported as a statement instead of a library function. For control flow, if, while, and for statements are supported. Functions can be called using the expression syntax, are user creatable and can return a value. Functions are first class and can be nested. Classes can also be declared and subclassed. Classes contain state and methods.

In short, the language features:
- Variable assignment
- Expression statements
- Functions
  - Declarations
  - Nesting
- Classes
  - Initialization
  - Subclassing
  - States
  - Methods
- Printing
- Control Flow
  - If
  - While
  - For

### Standard Library

The standard library includes functions for:
- clock: tracking time

### Sample Code

Simulating modulo arithmetic:
```
fun modulo(dividend, divisor) {
  while (dividend >= divisor) {
    dividend = dividend - divisor;
  }

  return dividend;
}

print "5 % 2 = " + modulo(5, 2);
print "2 % 5 = " + modulo(2, 5);
print "34 % 5 = " + modulo(34, 5);
print "35 % 5 = " + modulo(35, 5);

```


<!-- ## Getting Started

Welcome to the VS Code Java world. Here is a guideline to help you get started to write Java code in Visual Studio Code.

## Folder Structure

The workspace contains two folders by default, where:

- `src`: the folder to maintain sources
- `lib`: the folder to maintain dependencies

Meanwhile, the compiled output files will be generated in the `bin` folder by default.

> If you want to customize the folder structure, open `.vscode/settings.json` and update the related settings there.

## Dependency Management

The `JAVA PROJECTS` view allows you to manage your dependencies. More details can be found [here](https://github.com/microsoft/vscode-java-dependency#manage-dependencies). -->
