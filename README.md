## xlox

xlox (ecks-locks) is a small language based off the Lox language in the book 'Crafting Interpreters'. This project implements some of the proposed challenges in the book and some personal modifications.

### Grammar

Below is the grammar for xlox up to this point:
```
program         -> declarations* EOF ;
declarations    -> varDecl
                 | letDecl
                 | funDecl
                 | classDecl
                 | statement ;
varDecl         -> "var" IDENTIFIER ( "=" expression )? ";" ;
letDecl         -> "let" IDENTIFIER "=" expression ";" ;
funDecl         -> "fun" function;
classDecl       -> "class" IDENTIFIER ( "<" IDENTIFIER )? "{" method* "}" ;
function        -> IDENTIFIER "(" parameters? ")" block ;
method          -> "class"? IDENTIFIER ( "(" parameters? ")" )? block ;
statement       -> block
                 | ifStmt
                 | whileStmt
                 | doStmt
                 | forStmt
                 | breakStmt
                 | printStmt
                 | returnStmt
                 | exprStmt ;
block           -> "{" declarations* "}" ;
ifStmt          -> "if" "(" expression ")" statement ( "else" statement )? ;
whileStmt       -> "while" "(" expression ")" statement ;
doStmt          -> "do" statement ( "while" expression ";" )? ;
forStmt         -> "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement ;
breakStmt       -> "break" ";" ;
printStmt       -> "print" expr ";" ;
returnStmt      -> "return expression? ";" ;
exprStmt        -> expression ";" ;
expression      -> assignment ( "," assignment )*
                 | assignment "," ;
assignment      -> ( call "." )? IDENTIFIER ( "[" assignment "]" ) "=" assignment
                 | ( call "." )? IDENTIFIER ( "[" assignment "]" ) "="
                 | functional ;
functional      -> "fun" ( "(" parameters? ")" )? block
                 | conditional ;
conditional     -> logic_or ( "?" logic_or ":" conditional )* 
                 | logic_or "?" ":"
                 | logic_or "?" logic_or ":" ;
logic_or        -> logic_and ( "or" logic_and )*
                 | logic_and "or ;
logic_and       -> equality ( "and" equality )* 
                 | equality "and" ;
equality        -> comparison ( ( "==" | "!=" ) comparison )* 
                 | comparison ( "==" | "!=" ) ;
comparison      -> term ( ( ">" | ">=" | "<" | "<=" ) term )* 
                 | term ( ">" | ">=" | "<" | "<=" );
term            -> factor ( ( "+" | "-" ) factor )* 
                 | factor ( "+" | "-" ) ;
factor          -> unary ( ( "*" | "/" ) unary )* 
                 | unary ( "*" | "/" ) ;
unary           -> ( "!" | "-" ) unary 
                 | call ;
call            -> primary ( "(" arguments? ")" | "." IDENTIFIER | "[" assignment "]" )* ;   
primary         -> NUMBER
                 | STRING
                 | IDENTIFIER
                 | "true" | "false" | "nil" | "this"
                 | "super" "." IDENTIFIER
                 | "(" expression ")" 
                 | "[" assignment ( "," assignment )* "]" ;
parameters      -> IDENTIFIER ( "," IDENTIFIER )* ;
arguments       -> assignment ( "," assignment )* ;
```

### Modifications

- Comma expressions
- Ternary operator
- More capable error checking
  - Missing operands
  - Division by zero
- Auto conversion for string concatenation
- `break` statement
- `do` statement and `do-while` loops
  - `do` statements allow for `break` statements
- Lambda functions
- **BROKEN** Multiple variable declaration within `var` using `,`
- Constant declarations
  - func statements **ARE NOT** constants
  - parameters **ARE NOT** constants 
  - classes **ARE NOT** constants
- Static methods
- Getters
- Constant folding
- Nestable block comments
- Lists!
  - Indexable and assignable
  - Nestable
  - Can hold arbitrary length and type

#### Desired Modifications

- Module system
- Standard Library
  - xlox Implemented
  - Native Functions to extend xlox capability (Java implemented)
  - Foreign Function Interface?
- Everything is a class
- Classes can integrated into syntax
  - Customizable syntax?
- Modify Lists
  - Add native operations
  - Modify runtime to use 'LoxObject' style structure
    - Implement an indexable interface so others can support
- Migrate native Java runtime objects to 'LoxObject's

### Types

xlox is a dynamically typed language which supports the following types:
- Booleans
- Numbers (integers and floating point)
- Strings
- Nil

### Operations

xlox supports the following operations:
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

All statements in xlox end with a semicolon. A semicolon can also be added to a value expression to promote it to an expression statement. xlox uses C-like syntax for creating code blocks. Variables can be created with the 'var' keyword and have the default value of nil unless an initializer is provided. Printing is also supported as a statement instead of a library function. For control flow, if, while, and for statements are supported. Functions can be called using the expression syntax, are user creatable and can return a value. Functions are first class and can be nested. Classes can also be declared and subclassed. Classes contain state and methods.

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
