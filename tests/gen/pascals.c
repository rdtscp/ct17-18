/**
 * Prints n rows of Pascal's triangle
 * Written by Daniel Hillerström
 */
#include "io.h"
// Computes the factorial of n in bottom-up fashion
int factorial(int n) {
  int i;
  int product;
  
  // Initialise product to 1 (base-case)
  product = 1;
  // Initialise iterator i to 1
  i = 1;

  while (i <= n) {
    product = product * i;
    i = i + 1;
  }
  return product;
}

void main() {
    int i; // Outer iterator
    int j; // Inner iterator
    int n; // Number of rows to compute

    print_s("Enter the number of rows> ");
    n = read_i();

    // Initialise outer iterator to 0
    i = 0;

    // Iterate over the number of rows to display
    while (i < n) {
        // Initialise inner iterator to 0
        j = 0;

        // Display spaces for row i
        while ((n - i) - 2 >= j) {
            print_s(" ");
            j = j + 1; // Increment inner iterator
        }

        // Reset inner iterator to 0
        j = 0;

        // Compute and display the numbers in row i
        while (j <= i) {
            {
              int n; // A single number in the triangle
              int k; // Difference between i and j

            //   int a;
            //   int b;
            //   int c;

              k = i - j;
              

            //   a = factorial(i);
            //   b = factorial(j);
            //   c = factorial(k);

              n = factorial(i) / (factorial(j) * factorial(k));
              print_i(n);print_s(" ");
            }
            j = j + 1; // Increment inner iterator
        }
        print_s("\n");
        i = i + 1; // Increment outer iterator
    }
}
/*

Program
(FunDecl(INT,factorial,VarDecl(INT,n),Block(VarDecl(INT,i),VarDecl(INT,product),Assign(VarExpr(product),IntLiteral(1)),Assign(VarExpr(i),IntLiteral(1)),While(BinOp(VarExpr(i),LE,VarExpr(n)),Block(Assign(VarExpr(product),BinOp(VarExpr(product),MUL,VarExpr(i))),Assign(VarExpr(i),BinOp(VarExpr(i),ADD,IntLiteral(1))))),Return(VarExpr(product)))),
FunDecl(VOID,main,
    Block(
        VarDecl(INT,i),
        VarDecl(INT,j),
        VarDecl(INT,n),
        ExprStmt(
            FunCallExpr(print_s,StrLiteral(Enter the number of rows> ))
        ),
        Assign(
            VarExpr(n),
            FunCallExpr(read_i)
        ),
        Assign(VarExpr(i),IntLiteral(0)),
        While(
            BinOp(VarExpr(i),LT,VarExpr(n)),
            Block(
                Assign(VarExpr(j),IntLiteral(0)),
                While(BinOp(BinOp(BinOp(VarExpr(n),SUB,VarExpr(i)),SUB,IntLiteral(2)),GE,VarExpr(j)),Block(ExprStmt(FunCallExpr(print_s,StrLiteral( ))),Assign(VarExpr(j),BinOp(VarExpr(j),ADD,IntLiteral(1))))),
                Assign(VarExpr(j),IntLiteral(0)),
                While(
                    BinOp(VarExpr(j),LE,VarExpr(i)),
                    Block(
                        Block(
                            VarDecl(INT,n),
                            VarDecl(INT,k),
                            Assign(
                                VarExpr(k),
                                BinOp(
                                    VarExpr(i),
                                    SUB,
                                    VarExpr(j)
                                )
                            ),
                            Assign(
                                VarExpr(n),
                                BinOp(
                                    FunCallExpr(factorial,VarExpr(i)),
                                    DIV,
                                    BinOp(
                                        FunCallExpr(factorial,VarExpr(j)),
                                        MUL,
                                        FunCallExpr(factorial,VarExpr(k))
                                    )
                                )
                            ),ExprStmt(FunCallExpr(print_i,VarExpr(n))),ExprStmt(FunCallExpr(print_s,StrLiteral( )))),Assign(VarExpr(j),BinOp(VarExpr(j),ADD,IntLiteral(1))))),ExprStmt(FunCallExpr(print_s,StrLiteral(\n))),Assign(VarExpr(i),BinOp(VarExpr(i),ADD,IntLiteral(1))))))))