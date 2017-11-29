# Part IV : Introduction to LLVM
The goal of part IV is to write a simple LLVM pass.
If you have no previous experience with C++, we suggest that you take a look at these [C++ for Java programmers](https://www.cs.cmu.edu/afs/cs/academic/class/15494-s12/lectures/c++forjava.pdf) slides.

The project counts for 30% of your grade: 10% for writing a simple dead code elimination pass using the skeleton code provided, and 20% for implementing liveness analysis and writing your own method to determine dead code.

## 0. Setup

You will use Git to clone the LLVM sources and Cmake to generate the Makefiles to build LLVM on Linux. You need a minimum of Cmake 3.4 or later. The version of Cmake installed in /usr/bin on DICE is sufficient for LLVM.

To get started, you will clone the LLVM and Clang sources into your home directory (or location of your choice). You will need about 1.6GB of  free disk space to clone the sources. We will use 'ug3-ct' as the name of the directory to clone into.

```
cd ~
mkdir ug3-ct
cd ug3-ct
git clone https://github.com/llvm-mirror/llvm
git clone https://github.com/llvm-mirror/clang llvm/tools/clang
```

You have been given an extra 30GB of space for this course. The Debug build of LLVM requires around 30GB of disk space! Be careful not to fill up your home directory. If you are using DICE use the 'RelWithDebInfo' cmake build type, which uses less space.

Create a directory called 'build' where you will build LLVM. This directory can be located anywhere EXCEPT under your LLVM source directory. We will place it under 'ug3-ct' in this document.

If you are using DICE, the correct version of Cmake is installed as 'cmake3'. If you are using your own machine you may need to install Cmake from http://cmake.org. After installation the name of the binary will be 'cmake'.

```
mkdir build
cd build
cmake3 -DLLVM_TARGETS_TO_BUILD=X86 -DCMAKE_BUILD_TYPE=RelWithDebInfo ../llvm
```

After Cmake finishes creating the Makefiles the next step is to actually build LLVM. This can take anywhere from 15-45 minutes depending on your machine.

```
make -j4
```

After make finishes you will have a bin directory with the LLVM tools (clang, clang++, llc, etc). Try to compile a simple C example with Clang to LLVM bitcode to make sure it works.

```
echo "int main() { return 1; }" > test.c
~/ug3-ct/build/bin/clang -c -S -emit-llvm -Xclang -disable-O0-optnone test.c -o test.ll
cat test.ll
```

Your test.ll should look SOMETHING like this (this output is from Mac OS X, Linux and Windows will be slightly different).

```
; ModuleID = 'test.c'
source_filename = "test.c"
target datalayout = "e-m:o-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-apple-macosx10.12.0"

; Function Attrs: nounwind ssp uwtable
define i32 @main() #0 {
  %1 = alloca i32, align 4
  store i32 0, i32* %1, align 4
  ret i32 1
}

attributes #0 = { nounwind ssp uwtable "disable-tail-calls"="false" "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="penryn" "target-features"="+cx16,+fxsr,+mmx,+sse,+sse2,+sse3,+sse4.1,+ssse3" "unsafe-fp-math"="false" "use-soft-float"="false" }

!llvm.module.flags = !{!0}
!llvm.ident = !{!1}

!0 = !{i32 1, !"PIC Level", i32 2}
!1 = !{!"Apple LLVM version 8.0.0 (clang-800.0.42.1)"}
```

Congratulations you have just built LLVM!


## 1. Writing a Skeleton LLVM Pass

You have LLVM and are able to compile C programs. The next step is to create a pass of your own. Adrian Sampson at Cornell has put a simple skeleton pass online that you can use as a starting point. Change to your home directory and clone Adrian's git repository with the code.

```
cd ~
git clone https://github.com/sampsyo/llvm-pass-skeleton.git
```

Change to the directory for the skeleton pass and take a look at the source. It does nothing except print the name of whatever function it encounters.

```
cd llvm-pass-skeleton
less skeleton/Skeleton.cpp
```

Let's build the pass now. Create a build directory and change to the directory.

```
mkdir build
cd build
```

Before running Cmake and building the pass, you need to set LLVM_DIR to your LLVM build directory, i.e. ~/ug3-ct/build. Otherwise when you build the skeleton pass it will try to build with the version of LLVM that
is already installed on DICE and fail.

```
export LLVM_DIR=~/ug3-ct/build
```

You're ready to create the Makefiles and build the pass.

```
cmake3 ..
make
```

There should be a shared library for your new pass in skeleton/libSkeletonPass.so. When you compile a program with LLVM it will load your pass and automatically call it. You'll need a C file to use as a test. You can use the test.c you created in Step 0 or create a new file.

```
~/ug3-ct/build/bin/clang -Xclang -load -Xclang skeleton/libSkeletonPass.so ~/ug3-ct/build/test.c
I saw a function called main!
```

Congratulations you've just created an LLVM pass and successfully executed it with your own LLVM!


## 2. Write a Pass to Count Instructions

Use the skeleton pass above and the [lecture notes from 
class](http://www.inf.ed.ac.uk/teaching/courses/ct/slides-16-17/llvm/4-lab3_intro.pdf) to write a simple pass to print the number of instructions in a function.

NOTE!! LLVM will run your pass on EACH function. You do not need to use any module iterators. Only function and basic block iterators.

The lecture notes are for an older version of LLVM and you need to make the following changes:

Slide 5: Include the C++ header for vector with the other includes and add namespace std so the compiler can find vector.

```
#include "llvm/Pass.h"
#include "llvm/IR/Function.h"
#include "llvm/Support/raw_ostream.h"
#include <vector>
using namespace llvm;
using namespace std;
```

For your project you only need the code from Slide 5. If you would like to try out the loop analysis code on Slide 17 you need to make the following change. This is completely optional for your project.

Slide 17: The getAnalysis() method to access the LoopInfo structure in runOnFunction() has changed. Here is the correct definition.

```
LoopInfo &LI = getAnalysis<LoopInfoWrapperPass>().getLoopInfo();
```

## 3. Implement a Simple Dead Code Elimination Pass

Add a new method to your instruction counting pass to eliminate dead code. In the C program below, 'd' is dead because it is not used after it's assignment in the program. The assignment to 'c' is dead 
because it's only use is in the assignment to 'd' which is dead.

```
int foo() {
  int a = 7;
  int b = a * 2;
  int c = b - a;   // dead 
  int d = c / a;   // dead
  return b;
}
```

LLVM has a method to detect dead code and a method to remove instructions that you can use in your pass.

```
isInstructionTriviallyDead()
eraseFromParent()
```

You will use the LLVM iterators we discussed in class to find the dead instructions. It is illegal to remove an instruction while you are iterating over them. You need to first identify the instructions that are dead and then in a second loop remove them. Use the LLVM SmallVector data structure to store the dead instructions you find while iterating and a second loop to remove them.

```
SmallVector<Instruction*, 64> Worklist;
```

You need to run LLVM's 'mem2reg' pass before your DCE pass to convert the bitcode into a form that will work with your optimization. Without running 'mem2reg' all instructions will store their destinations operands to the stack and load their source operands from the stack. The memory instructions will block the ability for you to discover dead code. When you run 'mem2reg', you are converting the stack allocated code in non-SSA form, into SSA form with virtual registers.

Use the 'opt' tool to run 'mem2reg' before your DCE pass. Give your pass a command line option called 'skeletonpass'.

```
~/ug3-ct/build/bin/clang -S -emit-llvm -Xclang -disable-O0-optnone dead.c
~/ug3-ct/build/bin/opt -load skeleton/libSkeletonPass.so -mem2reg -skeletonpass dead.ll
``` 

Your pass should print the instruction counts before and after your dead code elimination routines. In order that we can automatically mark it, it should be of the form: 

```
DCE BEFORE: <instruction count>
DCE AFTER: <instruction count>
```


## 4. Implement Iterative Liveness Analysis

For the last part of your project you will replace the isInstructionTriviallyDead() method from LLVM with your own method to identify dead code. This relies on computing liveness which you learned about in [Lecture 15](http://www.inf.ed.ac.uk/teaching/courses/ct/slides-16-17/15-regalloc.pdf).

## 5. Submitting Your Project

As with parts 1-3, part 4 will be marked with a set of automated scripts that will run at 4am and 4pm every day. In order for the scripts to run correctly, your passes will have to be placed and named in a way that they expect. 

Passes should be stored in a folder `part-4/passes`. The folder should *only* contain passes, and nothing else, otherwise the scripts will fail. The first pass that is being marked (simple dead code elimination) should be in a folder called `llvm-pass-simple-dce`. The sources for each pass should be in a folder named `skeleton`, and the pass (when compiled) should be a shared object called `libSkeletonPass.so`. If you cloned the skeleton pass repository (i.e. https://github.com/sampsyo/llvm-pass-skeleton.git), and did not change the CMakelists or the name of the source (`Skeleton.cpp`) then the pass should be configured correctly. 

When registering the pass with LLVM, it should be called `skeletonpass`. Otherwise, our scripts will not be able to call it. 