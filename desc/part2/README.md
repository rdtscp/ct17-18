
**The description below is subject to changes over the following days.**

# Part II : AST builder + Semantic Analyser

The goal of part II is to modify your parser so that it can build the Abstract Syntax Tree (AST) corresponding to your input program and then perform semantic analysis.

In order to achieve this goal, you will have to perform five tasks.
First, you will have to follow the abstract grammar specification and design the Java classes that represent the AST as seen during the course.
Then, you should write an AST printer in order to output the AST into a file.
Thirdly, you will have to modify your parser so that it builds the AST as your are parsing the tokens.
Finally you will be able to perform name and type analysis.

Note that we highly recommend following an iterative approach where you add AST nodes one by one, extend the printer and modify your parser as you go.
We also encourage you to write small test programs that test every AST node as you are building them rather than trying to implement everything at once.
If you encounter any problem, have any questions or find a bug with the newly provided files, please post a note on Piazza.

## 0. Setup

You will have to pull the AST class nodes and abstract grammar from the main repository.
First, open a terminal and navigate to the root of your local repository.
Then type:
```
$ git pull https://bitbucket.org/cdubach/ct-17-18.git
```
This will cause some merge conflict(s) due to the change of the return type of some of the parse functions to return an AST node instead of void.
For instance:
```
From https://bitbucket.org/cdubach/ct-17-18
 * branch            HEAD       -> FETCH_HEAD
Auto-merging src/parser/Parser.java
CONFLICT (content): Merge conflict in src/parser/Parser.java
```
Here, the file Parser.java is causing a merge conflict.
In order to resolve it, you should open the file to fix the conflict.
For the parser, you'd possibly want to remove everything between the equals symbols and the greater than symbols, e.g.
```
=======
    public Program parse() {
>>>>>>> 92a7665c3dde600e1bd2d5681b2fc8fb43e1d37b
```
Thereafter you can continue to extend your solution.

## 1. AST Nodes

As seen in the course, the AST can be defined using an abstract grammar.
You can find the abstract grammar [here](../../grammar/abstract_grammar.txt).
It is important to ensure that the design of your classes follows the abstract grammar;
the automated marking system will rely exclusively on the name of the class to determine the type of AST node and will expect the subtrees to appear in the same order as defined in the grammar file.

Note that a few AST node classes are already given as a starting point.
You should not have to modify these (unless otherwise stated in the file).

## 2. AST Printer

Your next job will consists in extending the AST printer class provided to handle your newly added AST node classes.
As seen during the course, the AST printer uses the visitor design pattern.

It is important to respect the following format when printing the AST to ensure that your output can be validated by our automatic marking system.
Using EBNF syntax, the output should be of the form: `AST_NODE_CLASS_NAME '(' [SUB_TREE (',' SUB_TREE)*] ')'`

### Examples:

* `y = 3*x;` should result in the following output: `Assign(VarExpr(y),BinOp(IntLiteral(3), MUL, VarExpr(x)))`.
* `void foo() { return; }` should result in: `FunDecl(VOID, foo, Block(Return()))`.
* `-x;` should result in: `BinOp(IntLiteral(0),SUB,VarExpr(x))`.
* `-1` should result in `BinOp(IntLiteral(0),SUB,IntLiteral(1))`.
* `2+3+4` should result in `BinOp(BinOp(IntLiteral(2), ADD, IntLiteral(3)), ADD, IntLiteral(4))`  (all binary operators are left associative in our language)
* `2*3+4` should result in `BinOp(BinOp(IntLiteral(2), MUL, IntLiteral(3)), ADD, IntLiteral(4))`  (multiplication has precedence over addition, see precedence table from part I)
* `struct node_t { int field1; char field2; };` should result in `StructTypeDecl(StructType(node_t),VarDecl(INT,field1),VarDecl(CHAR,field2))`
* `struct node_t n;` should result in `VarDecl(StructType(node_t), n)`

Note that you are free to add white spaces in your output format; spaces, newlines and tabulations will be ignore by our comparison tool.

See the file [fibonacci.c-ast-dump](./fibonacci.c-ast-dump) for an example output of `java -cp bin Main -ast tests/fibonacci.c fibonacci.c-ast-dump`.

## 2'. Dot Printer

As seen during the lectures, it might be a good idea to also implement a Dot printer in order to easily visualise your AST.
This task is completely optional and will not be marked, but it might help you find problems more easily. 

## 3. Parser modifications

Your final tasks consists in updating your parser so that it creates the AST nodes as it parses your input program.
For most of your parseXYZ methods, you will have to modify the return type to the type of the node the parsing method should produce as seen during the lecture and implement the functionality that builds the AST nodes.
You may have to modify slightly the design of your parser in order to accommodate the creation of the AST nodes.


## 4. Name Analysis
 
The goal of the name analysis is to ensure that the scoping and visibility rules of the language are respected. This means for instance ensuring identifiers are only declared once or that any use of an identifier is preceded by a declaration in the current or enclosing scope.

Please note that an identifier can either be a variable or a function.

### Global and local scopes

As seen during the lectures, our language only have two scopes: global and local.

The global scope corresponds to the global variables declared outside any procedure and for the procedure declarations. Identifiers declared in the global scope can be accessed anywhere in the program.

The block scope (or local scope) is a set of statements enclosed within left and right braces ({ and } respectively). Blocks may be nested (a block may contain other blocks inside it). A variable declared in a block is accessible in the block and all inner blocks of that block, but not accessible outside the block. Procedure parameter identifiers have block scope, as if they had been declared inside the block forming the body of the procedure.

In both cases (global or local), it is illegal to declare twice the same identifiers in the same current block (note that this means it is illegal to declare a variable with the same name as a procedure at the global level).

Special care must be taken in any struct definition since it is not allowed to declare twice the same field. For instance the following is invalid:
```C
struct foo_t {
  int bar;
  int bar;
}
```

### Shadowing

Shadowing occurs when an identifier declared within a given scope has the same name as an identifier declared in an outer scope. The outer identifier is said to be shadowed and any use of the identifier will refer to the one from the inner scope.

### Built-in functions

As you may have noticed in the previous part, our language supports a set of built-in functions which are defined as parts of our standard library: 

```C
void print_s(char* s);
void print_i(int i);
void print_c(char c);
char read_c();
int read_i();
void* mcmalloc(int size);
```

In order to be able to recognise any call to these functions as valid, we suggest that you simply add dummy declaration for each of these (with an empty body) to the list of declared functions into the Program AST node.
Please note that it is important to do this just before name analysis but after having printed the AST so that our automatic tests do not fail (we are not expecting to see these built-ins function in the AST when checking for the AST correctness). 



### Task

Your task is to implement a visitor that traverses the AST and identifies when the above rules are violated. In addition, you should add (and fill in), for the FunCallExpr and VarExpr AST nodes, a field referencing the declaration (either a FunDecl or VarDecl). This field should be updated to point to the actual declaration of the identifier when traversing the AST with the name analysis visitor. This establishes the link between the use of a variable or function and its declaration as seen during the lectures.


## 5. Type analysis

The goal of type analysis is to verify that the input program is well-typed and assign a type for each expression encountered.
As seen during the course, the typing rule of our miniC language are defined using a formal notation.
You can find all the typing rules [here](./typing-rules/rules.pdf).
As usual, if you notice an error or if something is not clear, please post your question on Piazza.

Please note that when checking for type equivalence for arrays, it is important to ensure that the lenght matches.

Your task consists of extending the `sem.TypeCheckVisitor` class and implement the type checking mechanism following the typing rules.

### Structures

Structure declaration are represented in the AST as StructTypeDecl.
The type analysis pass must ensure that each structure declaration has a unique name.
You can enforce this by creating a simple visitor which checks for this before running the type checker for instance.

Similarly to the function call and variable use, your type analyser needs to check that if any variable is declared with a struct type, the struct type exists.
For instance if you encounter a variable declaration such as `struct node_t var;`, you must ensure that the corresponding `node_t` structure has been declared at the beginning of the program.

Finally, when accessing a structure, you must also check that the field exist in the structure type declaration.

### String literal

String literals are represented in our language as null terminated char arrays.
The string literal `"Hello"` should therefore be of type `char[6]` holding characters `'H'`,`'e'`,`'l'`,`'l'`,`'o'` and `'\0'` where \0 represents the null character.

### Strong typing

Our language is strongly typed. This means that there are no implicit casts between expressions and the cast must be explicit. For instance the following code bit of code is invalid in our language 
```C
int i;
char c;
i=c;
```
To make this valid, an explicit cast operation must be performed. The following is valid:
```C
int i;
char c;
i=(int)c;
```

## 6. Checking assignments

Finally, your last task will consist in checking that the left hand side expression of an assignment statement is one of the following: VarExpr, FieldAccessExpr, ArrayAccessExpr or ValuteAtExpr.
For instance the following code is legal:
```C
int i;
int* p;
i=0;
*p=i;
```
while the following code is invalid:
```C
int i;
i+2=3;
```


## New Files
* grammar/abstract_grammar.txt : This file describes the abstract grammar that defines our AST.
* src/ast/ASTVisitor.java : This is the visitor interface for the AST.
* src/ast/ASTPrinter.java : This the AST printer built as a visitor.
* src/ast/\*.java : These files implements some of the AST nodes.

A new package has been added under `src/sem/`. This package contains template classes to get you started on implementing the semantic analysis.

 * The `sem.SemanticAnalyzer` is the only class which `Main.java` directly interfaces with for semantic analysis. Inside this class you should run all of your semantic visitors.
 * The `sem.NameAnalysisVisitor` is a template for the name analysis.
 * The `sem.TypeCheckVisitor` is a template for typechecker.
 * The `sem.Symbol` is an abstract parent class for other concrete symbols (e.g. variables and procedures).
 * The `sem.Scope` is a partial implementation of the `Scope`-class discussed in the lectures.
 * The `typing-rules.pdf` contains all the typing rules for our language


## Updated Files
* src/Main.java : The main file has been updated to print the AST and perform semantic analysis.
