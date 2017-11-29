# Part III : Code Generation

The goal of part IV is to write the code generator, targeting MIPS32 assembly.

**Important**: the marking system will run the simulator from the command line which may change slightly the behaviour of your program (especially when it comes to handling input).
You should always make sure that all your tests execute correctly with the simulator run from the command line.

## 0. Setup and Learning

Your first task consist of setting up the MARS mips simulator.
First download the simulator [here](./Mars4_5.jar) and follow Part 1 of the [tutorial](http://courses.missouristate.edu/KenVollmar/mars/tutorial.htm) to learn how to use the simulator.
We also encourage you to have a look at the documentation provided by MARS which can be found [here](http://courses.missouristate.edu/KenVollmar/mars/Help/MarsHelpIntro.html) as well as the [MIPS 32 Instruction Set Quick Reference](./MD00565-2B-MIPS32-QRC-01.01-1.pdf).
For a more detailed explanation about the MIPS architecture, please have a look at the [MIPS Assembly WikiBooks](http://en.wikibooks.org/wiki/MIPS_Assembly)


## 1. Generating a simple program

Your next task should consists of producing an empty program (e.g. just an empty main function) and see that you can produce an assembly file.
Next, we suggest that you implement the print_i function using the corresponding system calls (check the lecture notes and the link above to the MARS documentation that explain how to do this).
You can then starts implementing simple arithmetic operations operating on literals, following the examples from the lectures.

Please note that we expect your programs to have one main function which should be the assembly entry point for the simulator. 

## 2. Binary Operators

Your next task should be to add support for all the binary operators.
You should make use of the `getRegister` and `freeRegister` helper functions to allocate and free up registers as seen in the class.

Please note that the comparison operators as well as the `||` and `&&` should be implemented with control flow.
For the comparison operations, use the positional encoding as seen during the lecture (value 0 means false, any other value means true).

## 3. Variable allocations and uses

Your next task should be to implement allocations of global and local variables.

As seen during the course, the global variables all go in the static storage area (data section of the assembly file).

The local variables (variables inside a function) go onto the stack.
You should allocate them at a fix offset from the frame pointer ($fp) and store this offset either in a symbol table that you carry around or directly in the VarDecl AST node as a field.
Note that the only thing your compiler has to emit with respect to local variable is code to move the stack pointer ($sp) by an offset corresponding to the size of all the local variables declared on the stack.

Next you should implement the logic to read and write local or global variables.
You can use the `lw` and `sw` instruction to read from or write to a variable respectively.

### sizeof and data alignment

We will follow the following specification for the size of the different types:
`sizeof(char)==1`, `sizeof(int)==4`, `sizeof(int*)==4`

Also arrays should always be represented in a compact form but you may need to pad the end of the array to make sure it is aligned to a 4 byte boundary.
As seen during the lecture, in the case of structures, you should make sure all the field are aligned at a 4 byte boundary.


## 4. Branching (if-then-else, loop, logical operators)

We suggest that you then implement the loop and if-then-else control structures as seen during the course using the branch instructions.
The logical `||` and `&&` should also be implemented with control flow.
Note that in the following example

```C
int foo() {
  print_i(2);
  return 2;
}
if ((1==0) && foo() == 2)
    ...
```

the function foo is never called at runtime since the semantic `&&` imposes that if the left side is false, the right side expression should not be executed. A similar logic applies for `||`. 


## 5. struct and array accesses

Next you should add support for struct and array accesses.
This can be implemented using the `lw` and `sw` instructions for struct and a combination of `add` instruction with the `lw` and `sw` instructions for array accesses.
The idea is to get the address of an array into a register, then add to it the index (keeping in mind that addresses are expressed in byte, so an access to `a[x]` where a is an int array means an offset of x*4 from the base address where the array is stored).

## 6. Function call

You can them move on to implementing function calls.

You can pass up to four arguments (no larger than 4 bytes each) through registers `$a0-$a3`, the rest or other arguments (e.g. struct) being passed on the stack.
Return value can be passed through register `$v0` or through the stack if they are bigger than 4 bytes (e.g. struct).

As seen during the lectures, when entering a function should make sure to save the content of the temporary registers, frame pointers `$fp` and return address `$ra`, and restored them when leaving the function.
You should also initialise the frame pointer to the value of the stack pointer.

## 7. stdLib functions

Finally, you should add support for all the standard library functions found in the file `minic-stdlib.h` provided in the tests folder.
These should all be implemented using [system calls](http://courses.missouristate.edu/KenVollmar/mars/Help/SyscallHelp.html).



## New Files

A new package has been added under `src/gen/`. This package should be used to store your code generator.

 * The `gen.CodeGenerator` is the only class which `Main.java` directly interfaces with.
 * The `gen.Register` class represents registers and contain a definition of most MIPS32 registers.

## Updated Files

* The `Main.java` has been updated to provide a new commandline pass `-gen` which runs your code generator.

