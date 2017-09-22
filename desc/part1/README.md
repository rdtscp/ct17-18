# Part I : Parsing
The goal of part I is to write a lexical and syntactic analyser - a parser - for a subset of C; mini-C.
As you have learnt in the course, parsing consists of three parts:

1. Scanner: the job of the scanner is to read the input file one character at a time.
2. Lexer: the lexer transforms the stream of characters into a stream of tokens. These tokens represents the lexem (i.e. a word in natural languages)
3. Parser: the parser finally consumes the tokens and determine if the input conforms to the rule of the grammar.

The scanner has already been implemented for you and we provide some partial implementations of the lexer and parser.
You will have to implement the rest.
We strongly encourage you to write a recursive descent parser and as such make your grammar LL(k).
We have provided utility function in the parser class to allow look ahead.


## 0. C language
We strongly encourage you to familiarise yourself with C, before starting implementing the compiler.
Since our target language mini-C is a subset of C, it might be a good idea to learn how to use a C compiler such as gcc.
In case of doubts about the language semantic when implementing your compiler, a good default is to use the same semantic as in C.
This [web tutorial](https://www.tutorialspoint.com/cprogramming/index.htm) is a good starting point and contains most of the information you will need.
Since we are only targeting a subset of C, you do not have to read all of it but only the parts that correspond to our target language (which is described by the EBNF grammar). 


## 1. Lexing
The file `Tokeniser.java` contains a partial implementation of a lexer. Your job is to complete the implementation.
In particular, you have to complete the implementation of the method `next` in the `Tokeniser`-class.
It is strongly recommended that you fill in the missing details, rather than implementing your own `Lexer` from scratch.
Furthermore, do not remove the existing public methods, e.g. `getErrorCount` and `nextToken`.
The tokens that your lexer needs to recognise with the regular expression definition are given in the file `Token.java`.
Note that you are **not** allowed to use [the Java regular expression matcher](https://docs.oracle.com/javase/7/docs/api/java/util/regex/Matcher.html) in your implementation!

Please note that comments should be treated as whitespace. Comments can either be single line:
`// this is a comment`
or multiline
`/* this is a
    multiline comment */`

The "#include" directive should be tokenised into the INCLUDE token. However, you should not do anything with the "include" preprocessor directive (it is completely ignored in the rest of the compiler).
Furthermore, for numbers we consider only integers, and therefore you need not implement support for hexadecimal or octal numbers.

The list of characters to escape are the same as in Java.
Please check this [link](http://docs.oracle.com/javase/tutorial/java/data/characters.html) for the full list.
For instance, `"I am a \"string\""` should return a string token whose data field is `I am a "string"`.

A hint: It is recommended to use the [Character-class methods](https://docs.oracle.com/javase/7/docs/api/java/lang/Character.html) to test whether a character is a digit, whitespace, etc.

## 2. Grammar
Your next job will consists in taking the [grammar](../../grammar/ebnf.txt) expressed in EBNF form and transform it into an equivalent context-free LL(k) grammar.
You should make sure that the resulting grammar is non-ambiguous, eliminate left recursion and ensure that the usual C precedence rules for operators are respected based on this table:
 
| Precedence    |Operator       | Description       |
| :------------ | :------------ | :-----------      | 
| 1             | ()            | Function call     | 
| 1             | \[\]          | Array subscripting |
| 1             | .             | Structure member access |
| 2             | -             | Unary minus |
| 2             | (type)        | Type cast |
| 2             | *             | Pointer indirection |
| 2             | sizeof(type)  | Size of type|
| 3             | * / %         | Multiplication, division, remainder |
| 4             | + -           | Addition, substraction |
| 5             | < <= > >=     | Relational operators |
| 6             | == \!=        | Relational operators |
| 7             | &&            | Logical AND |
| 8             | ⎮⎮            | Logical OR |
  
For instance, the expression 2\*3+2 should be parsed as (2\*3)+2.
Note that although we require you to parse the expression following the precedence, this will only be checked in part 2 of the coursework where you will have to output the Abstract Syntax Tree.
However, we encourage you to directly implement the correct precedence rules to avoid problems in the later stages of your compiler.
This is done by modifying the grammar slightly as seen during the course lectures.


## 3. Parser
After having transformed the grammar into a LL(k)-grammar and implemented the lexer you will have to implement a parser.
The parser determines whether a given source program is syntactically correct or incorrect.
A partial implementation of a recursive-decent parser has already been provided.
The provided `Parser`-class has the following interface:

* `int getErrorCount()` returns the number of parsing errors.
* `void parse()` initiates the parsing of a given source program.

In addition, the `Parser`-class contains various private methods, of which some are ultility methods, e.g.

* `void error(TokenClass... expected)` takes a variable number of expected tokens, and emits an error accordingly.
* `Token lookAhead(int i)` returns the `i`'th token in the token-stream.
* `void nextToken()` advances the token-stream by one, i.e. it consumes one token from the stream.
* `Token expect(TokenClass... expected)` takes a variable number of expected tokens, and consumes them from the token-stream if present, otherwise it generates an error using the `error`-method.
* `boolean accept(TokenClass... expected)` tests whether the next token(s) are identical to the `expected`. However, it *does not* consume any tokens from the token-stream.
* `void parseProgram()` parses a "Program-production" from the LL(k) grammar. Similarly, `void parseIncludes()` parses an "Includes-production". Three additional empty methods have been provided: `parseStructDecls`, `parseVarDecls` and `parseFunDecls` are to be completed by you. Furthermore, you will need to add more parse methods yourself. For each nonterminal you should have a corresponding parse method.

Your parser *should* only determine whether a given source program is syntactically correct.
The `Main`-class relies on the error count provided by the `Parser`-class.
Therefore, make sure you use the `error`-method in the `Parser`-class to report errors correctly!

## Files
* grammar/ebnf.txt : This file describes the grammar of our mini-C language in EBNF format.
* Scanner : This class implements the scanner which returns character strings.
* Token : This class represent the different tokens from the language.
* Tokeniser: This class converts character strings into tokens.
