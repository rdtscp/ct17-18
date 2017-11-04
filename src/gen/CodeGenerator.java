package gen;

import ast.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.EmptyStackException;
import java.util.Stack;

public class CodeGenerator implements ASTVisitor<Register> {

    /*
     * Simple register allocator.
     */

    // contains all the free temporary registers
    private Stack<Register> freeRegs = new Stack<Register>();

    public CodeGenerator() {
        freeRegs.addAll(Register.tmpRegs);
    }

    private class RegisterAllocationError extends Error {}

    private Register getRegister() {
        try {
            return freeRegs.pop();
        } catch (EmptyStackException ese) {
            throw new RegisterAllocationError(); // no more free registers, bad luck!
        }
    }

    private void freeRegister(Register reg) {
        freeRegs.push(reg);
    }


    /* lastState marks if we were printing .data or .text last; 0 = .data, 1 = .text */
    private int lastState = 0;
    private int strNum = 0;


    private PrintWriter writer; // use this writer to output the assembly instructions


    public void emitProgram(Program program, File outputFile) throws FileNotFoundException {
        writer = new PrintWriter(outputFile);

        visitProgram(program);
        writer.close();
    }

    @Override
    public Register visitProgram(Program p) {

        writer.print("\t\t.text\n");
        writer.print("j main\n\n");
        // Create functions for printing:
        writer.print("print_i:\n\tli\t$v0, 1\t# Print int cmd code.\n\tsyscall\t\t# Print int now.\n\tjr $ra\t\t# Return to caller.");
        // writer.print("\n\nprint_s:\n\tli\t$v0, 4\n\tsyscall\n\tjr $ra");
        
        // Find the main function first, and declare
        for (FunDecl funDecl: p.funDecls) {
            // Generate MIPS for the main function declaration.
            if (funDecl.name.equals("main")) {
                funDecl.accept(this);
            }
            // Write out the exit execution code.
            writer.print("\n\tli\t$v0, 10\t# Exit cmd code.\n\tsyscall\t\t# Exit program.");
        }
        // Declare the rest of the functions.
        for (FunDecl funDecl: p.funDecls) {
            if (!funDecl.name.equals("main")) {
                funDecl.accept(this);
            }
        }
        return null;
    }

    @Override
    public Register visitBinOp(BinOp bo) {
        Register operand1 = bo.expr1.accept(this);
        Register operand2 = bo.expr2.accept(this);
        // Mark if either of these operands were null Registers.
        boolean op1Null = (operand1 == null);
        boolean op2Null = (operand2 == null);
        // Different operations can handle different operand types.
        if (bo.op == Op.ADD) {
            if (op1Null && op2Null) {
                // If both operands of the operation are literals, then we must assign a register to one of them.
                operand1 = getRegister();
                // Cast both operands into IntLiterals.
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                writer.print("\n\tLI " + operand1.toString() + ", " + const16_1.val);
                writer.print("\n\tADDI " + operand1.toString() + ", " + operand1.toString() + ", " + const16_2.val);
                freeRegister(operand2);
                return operand1;
            }
            // Handle the case where only operand1 is a literal.
            else if (op1Null) {
                // Re-use operand2's register.
                IntLiteral const16 = (IntLiteral)bo.expr1;
                writer.print("\n\tADDI " + operand2.toString() + ", " + operand2.toString() + ", " + const16.val);
                return operand2;
            }
            // Handle the case where only operand2 is a literal.
            else if (op2Null) {
                // Re-use operand1's register.
                IntLiteral const16 = (IntLiteral)bo.expr2;
                writer.print("\n\tADDI " + operand1.toString() + ", " + operand1.toString() + ", " + const16.val);
                return operand1;
            }
            // Handle the case where neither operands are literals.
            else {
                writer.print("ADD " + operand1.toString() + ", " + operand1.toString() + ", " + operand2.toString());
                return operand1;
            }
        }
        if (bo.op == Op.SUB) {
            if (op1Null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                operand1 = getRegister();
                writer.print("\n\tLI " + operand1.toString() + ", " + const16_1.val);
            }
            if (op2Null) {
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                operand2 = getRegister();
                writer.print("\n\tLI " + operand2.toString() + ", " + const16_2.val);
            }
            writer.print("\n\tSUB " + operand1.toString() + ", " + operand1.toString() + ", " + operand2.toString());
            if (op2Null) freeRegister(operand2);
            return operand1;
        }
        if (bo.op == Op.MUL) {
            if (op1Null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                operand1 = getRegister();
                writer.print("\n\tLI " + operand1.toString() + ", " + const16_1.val);
            }
            if (op2Null) {
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                operand2 = getRegister();
                writer.print("\n\tLI " + operand2.toString() + ", " + const16_2.val);
            }
            writer.print("\n\tMUL " + operand1.toString() + ", " + operand1.toString() + ", " + operand2.toString());
            if (op2Null) freeRegister(operand2);
            return operand1;
        }
        if (bo.op == Op.DIV) {
            if (operand1 == null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                operand1 = getRegister();
                writer.print("\n\tLI " + operand1.toString() + ", " + const16_1.val);
            }
            if (operand2 == null) {
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                operand2 = getRegister();
                writer.print("\n\tLI " + operand2.toString() + ", " + const16_2.val);
            }
            writer.print("\n\tDIV " + operand1.toString() + ", " + operand2.toString());
            writer.print("\n\tMFLO " + operand1.toString());
            if (op2Null) freeRegister(operand2);
            return operand1;
        }
        if (bo.op == Op.MOD) {
            if (operand1 == null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                operand1 = getRegister();
                writer.print("\n\tLI " + operand1.toString() + ", " + const16_1.val);
            }
            if (operand2 == null) {
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                operand2 = getRegister();
                writer.print("\n\tLI " + operand2.toString() + ", " + const16_2.val);
            }
            writer.print("\n\tDIV " + operand1.toString() + ", " + operand2.toString());
            writer.print("\n\tMFHI " + operand1.toString());
            if (op2Null) freeRegister(operand2);
            return operand1;
        }
        if (bo.op == Op.GT) {
            if (op1Null && op2Null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;

                /*   If we have:
                 *          2 > 1
                 *   We want to convert to:
                 *          1 < 2
                 *   So const16_1 becomes operand2, and const16_2 becomes operand1.
                 */
                // Use a register to store output and also one operand.
                operand1 = getRegister();
                writer.print("\n\tLI " + operand1.toString() + ", " + const16_2.val);
                writer.print("\n\tSLTI " + operand1.toString() + ", " + operand1.toString() + ", " + const16_1.val);
                return operand1;
            }
            else if (op1Null) {
                /*   If we have:
                 *          1 > y
                 *   We want to convert to:
                 *          y < 1
                 *   So const16_1 becomes operand2, and operand2 becomes operand1.
                 */
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                writer.print("\n\tSLTI " + operand2.toString() + ", " + operand2.toString() + ", " + const16_1.val);
                freeRegister(operand1);
                return operand2;
            }
            else if (op2Null) {
                /*   If we have:
                 *          x > 1
                 *   We want to convert to:
                 *          1 < x
                 *   So const16_2 becomes operand1, and operand2 becomes operand1.
                 */
                operand2 = getRegister();
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                writer.print("\n\tLI " + operand2.toString() + ", " + const16_2.val);
                writer.print("\n\tSLT " + operand1.toString() + ", " + operand2.toString() + ", " + operand1.toString());
                freeRegister(operand2);
                return operand1;
            }
            else {
                // We have: x > y
                writer.print("SLT " + operand1.toString() + ", " + operand2.toString() + ", " + operand1.toString());
                freeRegister(operand2);
                return operand1;
            }
        }          
        if (bo.op == Op.LT) {
            if (op1Null && op2Null) {
                // We have:     1 < 2
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;

                // Use a register to store output and also one operand.
                operand1 = getRegister();
                // Store the 'lesser' value in the register.
                writer.print("\n\tLI " + operand1.toString() + ", " + const16_1.val);
                writer.print("\n\tSLTI " + operand1.toString() + ", " + operand1.toString() + ", " + const16_2.val);
                return operand1;
            }
            else if (op1Null) {
                // We have:     1 < y
                IntLiteral const16_1 = (IntLiteral)bo.expr1;

                operand1 = getRegister();
                writer.print("\n\tSLT " + operand2.toString() + ", " + operand1.toString() + ", " + operand2.toString());
                freeRegister(operand2);
                return operand2;
            }
            else if (op2Null) {
                // We have:     x < 2
                IntLiteral const16_2 = (IntLiteral)bo.expr2;

                writer.print("\n\tSLTI " + operand1.toString() + ", " + operand1.toString() + ", " + const16_2);
                return operand1;
            }
            else {
                // We have:     x < y
                writer.print("\n\tSLT " + operand1.toString() + ", " + operand1.toString() + ", " + operand2.toString());
                freeRegister(operand2);
                return operand1;
            }
        } 
        if (bo.op == Op.GE) {
            
        }
        if (bo.op == Op.LE) {
            
        }
        if (bo.op == Op.NE) {
            // Must be careful, can see characters here.
            // Type checking ensures that both operands of != are the same type.
            if (bo.expr1 instanceof ChrLiteral) {
                if (op1Null) {
                    ChrLiteral const16_1 = (ChrLiteral)bo.expr1;
                    operand1 = getRegister();
                    writer.print("\n\tLI " + operand1.toString() + ", " + "'" + const16_1.val + "'");
                }
                if (op2Null) {
                    ChrLiteral const16_2 = (ChrLiteral)bo.expr2;
                    operand2 = getRegister();
                    writer.print("\n\tLI " + operand2.toString() + ", " + "'" + const16_2.val + "'");                    
                }
                // Get the difference between these two numbers.
                writer.print("\n\tSUB " + operand1.toString() + ", " + operand1.toString() + ", " + operand2.toString());
                
                // Create a register with just the value one.
                Register valOne = getRegister();
                writer.print("\n\tLI " + valOne.toString() + ", " + "1");

                // If the difference between the 2 numbers is zero, they are equal, so return a register with 0.
                writer.print("\n\tMOVZ " + operand2.toString() + ", $zero, " + operand1.toString());
                // If the difference between the 2 numbers is non-zero, they are not equal, so return a register with 1.
                writer.print("\n\tMOVN " + operand2.toString() + ", " + valOne.toString() + ", " + operand1.toString()); 
                freeRegister(valOne);
                freeRegister(operand1);
                return operand2;
            }
            // Else it is an integer.
            else {
                if (op1Null) {
                    IntLiteral const16_1 = (IntLiteral)bo.expr1;
                    operand1 = getRegister();
                    writer.print("\n\tLI " + operand1.toString() + ", " + const16_1.val);
                }
                if (op2Null) {
                    IntLiteral const16_2 = (IntLiteral)bo.expr2;
                    operand2 = getRegister();
                    writer.print("\n\tLI " + operand2.toString() + ", " + const16_2.val);
                }
                // Get the difference between these two numbers.
                writer.print("\n\tSUB " + operand1.toString() + ", " + operand1.toString() + ", " + operand2.toString());
                
                // Create a register with just the value one.
                Register valOne = getRegister();
                writer.print("\n\tLI " + valOne.toString() + ", " + "1");

                // If the difference between the 2 numbers is zero, they are equal, so return a register with 0.
                writer.print("\n\tMOVZ " + operand2.toString() + ", $zero, " + operand1.toString());
                // If the difference between the 2 numbers is non-zero, they are not equal, so return a register with 1.
                writer.print("\n\tMOVN " + operand2.toString() + ", " + valOne.toString() + ", " + operand1.toString()); 
                freeRegister(valOne);
                freeRegister(operand1);
                return operand2;
            }
        }
        if (bo.op == Op.EQ) {
            // Must be careful, can see characters here.
            // Type checking ensures that both operands of != are the same type.
            if (bo.expr1 instanceof ChrLiteral) {
                if (op1Null) {
                    ChrLiteral const16_1 = (ChrLiteral)bo.expr1;
                    operand1 = getRegister();
                    writer.print("\n\tLI " + operand1.toString() + ", " + "'" + const16_1.val + "'");
                }
                if (op2Null) {
                    ChrLiteral const16_2 = (ChrLiteral)bo.expr2;
                    operand2 = getRegister();
                    writer.print("\n\tLI " + operand2.toString() + ", " + "'" + const16_2.val + "'");                    
                }
                // Get the difference between these two numbers.
                writer.print("\n\tSUB " + operand1.toString() + ", " + operand1.toString() + ", " + operand2.toString());
                
                // Create a register with just the value one.
                Register valOne = getRegister();
                writer.print("\n\tLI " + valOne.toString() + ", " + "1");

                // If the difference between the 2 numbers is zero, they are equal, so return a register with 0.
                writer.print("\n\tMOVZ " + operand2.toString() + ", " + valOne.toString() + ", " + operand1.toString());
                // If the difference between the 2 numbers is non-zero, they are not equal, so return a register with 1.
                writer.print("\n\tMOVN " + operand2.toString() + ", $zero, " + operand1.toString());
                freeRegister(valOne);
                freeRegister(operand1);
                return operand2;
            }
            // Else it is an integer.
            else {
                if (op1Null) {
                    IntLiteral const16_1 = (IntLiteral)bo.expr1;
                    operand1 = getRegister();
                    writer.print("\n\tADDI " + operand1.toString() + ",  $zero , " + const16_1.val);
                }
                if (op2Null) {
                    IntLiteral const16_2 = (IntLiteral)bo.expr2;
                    operand2 = getRegister();
                    writer.print("\n\tADDI " + operand2.toString() +  ",  $zero , " + const16_2.val);
                }
                // Now perform AND
                writer.print("\n\tAND " + operand1.toString() + ", " + operand1.toString() + ", " + operand2.toString());
                freeRegister(operand2);
                return operand1;
            }
        }
        if (bo.op == Op.OR) {
            
        }
        if (bo.op == Op.AND) {
            
        }


		return null;
    }

    @Override
    public Register visitIntLiteral(IntLiteral il) {
        return null;
    }


    @Override
    public Register visitFunDecl(FunDecl fd) {
        writer.print("\n\n" + fd.name + ":");
        fd.block.accept(this);
        
        return null;
    }

    

    
    
    @Override
    public Register visitFunCallExpr(FunCallExpr fce) { 
        // Load all expected arguments into the appropriate registers.
        for (int i = 0; i < fce.exprs.size(); i++) {
            // Get the current parameter being passed, and the argument expected.
            VarDecl currArg = fce.fd.params.get(i);
            Expr  currParam = fce.exprs.get(i);
            
            /* Match the type of the current argument of the function being called. */
            if (currArg.type == BaseType.INT) {
                // This argument is an integer, an integer will either exist as a literal, or within a Register.
                // Accept this argument, to retrieve its Register.
                Register paramReg = currParam.accept(this);
                // If this argument does not return a Register, then it must be an IntLiteral.
                if (paramReg == null) writer.print("\n\tADDI $a" + i + ", $zero, "  + ((IntLiteral)currParam).val);
                else writer.print("\n\tADD $a" + i + ", $zero, "  + paramReg.toString());
            }
            else if (currParam instanceof StrLiteral) {
                StrLiteral string = (StrLiteral)currParam;
                writer.print("\n\t\t.data\n\tstr" + strNum + ":\t.asciiz \"" + string.val + "\"\n\t\t.text");
                writer.print("\n\tla $a0, str"+strNum);
            }
        }
        writer.print("\n");
        writer.print("\tjal " + fce.ident);

        return null;
	}

    @Override
    public Register visitExprStmt(ExprStmt es) {
		return es.expr.accept(this);
    }
    @Override
	public Register visitBlock(Block b) {
        for (Stmt stmt: b.stmts) {
            stmt.accept(this);
        }
		return null;
    }



    @Override
    public Register visitBaseType(BaseType bt) {
        return null;
    }

    @Override
    public Register visitStructTypeDecl(StructTypeDecl st) {
        return null;
    }


    @Override
    public Register visitVarDecl(VarDecl vd) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitVarExpr(VarExpr v) {
        // TODO: to complete
        return null;
    }

    /* ******************* */

    

	@Override
	public Register visitWhile(While w) {
		return null;
	}

	@Override
	public Register visitIf(If i) {
		return null;
	}

	@Override
	public Register visitReturn(Return r) {
		return null;
	}

	@Override
	public Register visitAssign(Assign a) {
        Register output = a.expr2.accept(this);
        return output;
	}



	@Override
    public Register visitArrayAccessExpr(ArrayAccessExpr aae) {
		return null;
    }

	@Override
    public Register visitFieldAccessExpr(FieldAccessExpr fae) {
        return null;
    }

	
	
	@Override
    public Register visitTypecastExpr(TypecastExpr te) {
		return null;
	}
	
	@Override
    public Register visitValueAtExpr(ValueAtExpr vae) {
		return null;
    }

    @Override
    public Register visitStrLiteral(StrLiteral sl) {
		return null;
    }

    @Override
    public Register visitChrLiteral(ChrLiteral cl) {
        return null;
	}

    @Override
    public Register visitSizeOfExpr(SizeOfExpr soe) {
        return null;
    }    



	



	/**************************\
			   Not Used
	\**************************/

	
	@Override
	public Register visitArrayType(ArrayType at) {
		// To be completed...
		return null;
	}

    @Override
    public Register visitOp(Op o) {
        return null;
	}

	@Override
	public Register visitStructType(StructType st) {
		// To be completed...
		return null;
	}

	@Override
	public Register visitPointerType(PointerType pt) {
		// To be completed...
		return null;
	}
}
