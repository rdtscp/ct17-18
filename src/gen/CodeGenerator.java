package gen;

import ast.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.*;


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

    private int currNumParams = 0;
    private int strNum = 0;
    private boolean isFunDecl = false;

    private HashMap<String, Register> varMappings = new HashMap<String, Register>();    // Tracks which register represents which variable.
    private HashMap<String, Register> registersUsed = new HashMap<String, Register>();  // Tracks which registers are in use by current scope.
    private Stack<Stack <Register>> progStates = new Stack<Stack <Register>>();         // Tracks all states on the stack.

    private HashMap<String, StructTypeDecl> structTypeDecls = new HashMap<String, StructTypeDecl>();

    private PrintWriter writer; // use this writer to output the assembly instructions


    public void emitProgram(Program program, File outputFile) throws FileNotFoundException {
        writer = new PrintWriter(outputFile);

        visitProgram(program);
        writer.close();
    }

    @Override
    public Register visitProgram(Program p) {
        writer.print("\t\t.data");
        
        // Create the HashMap of StructTypeDecls.
        for (StructTypeDecl std: p.structTypeDecls) {
            StructType structType = (StructType)std.structType;
            structTypeDecls.put(structType.identifier, std);
        }

        // Allocate memory on the heap for global variables.
        for (VarDecl vd: p.varDecls) {
            writer.print("\n" + vd.ident + "\t.space " + vd.num_bytes);
        }

        /* Create functions for printing, and a jump to main to start execution. */
        writer.print("\n\n\t\t.text\n");
        writer.print("j main\n\n");
        writer.print("print_i:\n\tlw $a0, ($sp)\n\tli\t$v0, 1\t# Print int cmd code.\n\tsyscall\t\t# Print int now.\n\tjr $ra\t\t# Return to caller.");
        writer.print("\n\nprint_s:\n\tlw $a0, ($sp)\n\tli\t$v0, 4\n\tsyscall\n\tjr $ra");
        writer.print("\n\nprint_c:\n\tlw $a0, ($sp)\n\tli\t$v0, 11\n\tsyscall\n\tjr $ra");
        
        // Find the main function first, and declare it.
        for (FunDecl funDecl: p.funDecls) {
            // Generate MIPS for the main function declaration.
            if (funDecl.name.equals("main")) {
                funDecl.accept(this);
                // Write out the exit execution code.
                writer.print("\n\tli\t$v0, 10\t# Exit cmd code.\n\tsyscall\t\t# Exit program.");
            }
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
    public Register visitFunDecl(FunDecl fd) {
        Stack<Register> storedRegs = new Stack<Register>();
        // Declare this function.
        writer.print("\n\n" + fd.name + ":");

        // Store return address.
        writer.print("\n\t# SAVING RET ADDR TO STACK");
        writer.print("\n\tADDI $sp, $sp, -4");
        writer.print("\n\tSW $ra, ($sp)");

        // Store all Register states to the stack.
        writer.print("\n\t# SAVING STATE TO STACK");
        writer.print("\n\tADDI $sp, $sp, -" + (Register.tmpRegs.size() * 4));
        int offset = Register.tmpRegs.size() * 4 - 4;
        for (Register currReg: Register.tmpRegs) {
            writer.print("\n\tSW " + currReg + ", " + offset + "($sp)");
            offset += -4;
            storedRegs.push(currReg);
        }
        writer.print("\n\t# SAVED STATE TO STACK");
        
        // Generate code for this function.
        fd.block.accept(this);

        offset = 0;
        // Re-instate Register states from stack.
        writer.print("\n\t# RE-INSTATING STATE FROM STACK");
        Register currReg;
        while (storedRegs.size() > 0) {
            currReg = storedRegs.pop();
            writer.print("\n\tLW " + currReg + ", " + offset + "($sp)");
            offset += 4;
        }
        writer.print("\n\t# RE-INSTATED STATE");
        assert storedRegs.size() == 0;
        return null;
    }

    @Override
	public Register visitBlock(Block b) {
        // Turn each statement into a code block.
        // b.varDecls
        for (Stmt stmt: b.stmts) {
            stmt.accept(this);
        }
        return null;
    }

    @Override
	public Register visitAssign(Assign a) {
        Register thisVar = null;
        if (a.expr2 instanceof IntLiteral) {
            IntLiteral num = (IntLiteral)a.expr2;
            thisVar = getRegister();
            writer.print("\n\tLI " + thisVar.toString() + ", " + num.val);
        }
        else if (a.expr2 instanceof ChrLiteral) {
            ChrLiteral ltr = (ChrLiteral)a.expr2;
            thisVar = getRegister();
            writer.print("\n\tLI " + thisVar.toString() + ", '" + ltr.val + "'");
        }
        else {
            thisVar = a.expr2.accept(this);
        }
        
        if (a.expr1 instanceof VarExpr) {
            VarExpr var = (VarExpr)a.expr1;
            System.out.println("Saving: " + var.ident + " -> " + thisVar);
            varMappings.put(var.ident, thisVar);
        }
        return thisVar;
    }
    
    @Override
    public Register visitExprStmt(ExprStmt es) {
		return es.expr.accept(this);
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
                writer.print("\n\tLI " + operand1.toString() + ", " + (const16_1.val + const16_2.val));
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
                writer.print("\n\tADD " + operand1.toString() + ", " + operand1.toString() + ", " + operand2.toString());
                varMappings.values().remove(operand1);
                return operand1;
            }
        }
        if (bo.op == Op.SUB) {
            if (op1Null && op2Null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;

                operand1 = getRegister();
                writer.print("\n\tLI " + operand1.toString() + ", " + (const16_1.val - const16_2.val));
                return operand1;
            }
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
            if (op1Null & op2Null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;

                operand1 = getRegister();
                writer.print("\n\tLI " + operand1.toString() + ", " + (const16_1.val * const16_2.val));
                return operand1;
            }
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
            if (op1Null & op2Null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;

                operand1 = getRegister();
                writer.print("\n\tLI " + operand1.toString() + ", " + (const16_1.val / const16_2.val));
                return operand1;
            }
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
            writer.print("\n\tDIV " + operand1.toString() + ", " + operand2.toString());
            writer.print("\n\tMFLO " + operand1.toString());
            if (op2Null) freeRegister(operand2);
            return operand1;
        }
        if (bo.op == Op.MOD) {
            if (op1Null & op2Null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;

                operand1 = getRegister();
                writer.print("\n\tLI " + operand1.toString() + ", " + (const16_1.val % const16_2.val));
                return operand1;
            }
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
            writer.print("\n\tDIV " + operand1.toString() + ", " + operand2.toString());
            writer.print("\n\tMFHI " + operand1.toString());
            if (op2Null) freeRegister(operand2);
            return operand1;
        }
        if (bo.op == Op.GT) {
            if (op1Null && op2Null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;

                operand1 = getRegister();
                if (const16_1.val > const16_2.val) writer.print("\n\tLI " + operand1.toString() + ", 1\t# Value 1 Register");
                else writer.print("\n\tLI " + operand1.toString() + ", 0");
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
                writer.print("\n\tSLT " + operand1.toString() + ", " + operand2.toString() + ", " + operand1.toString());
                freeRegister(operand2);
                return operand1;
            }
        }          
        if (bo.op == Op.LT) {
            if (op1Null && op2Null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;

                operand1 = getRegister();
                if (const16_1.val < const16_2.val) writer.print("\n\tLI " + operand1.toString() + ", 1\t# Value 1 Register");
                else writer.print("\n\tLI " + operand1.toString() + ", 0");
                return operand1;
            }
            else if (op1Null) {
                // We have:     1 < y
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                // Create Register to hold the lesser value.
                operand1 = getRegister();
                writer.print("\n\tLI " + operand1.toString() + ", " + const16_1.val);
                // Print calculation.
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
            if (op1Null && op2Null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;

                operand1 = getRegister();
                if (const16_1.val >= const16_2.val) writer.print("\n\tLI " + operand1.toString() + ", 1\t# Value 1 Register");
                else writer.print("\n\tLI " + operand1.toString() + ", 0");
                return operand1;
            }
        }
        if (bo.op == Op.LE) {
            if (op1Null && op2Null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;

                operand1 = getRegister();
                if (const16_1.val <= const16_2.val) writer.print("\n\tLI " + operand1.toString() + ", 1\t# Value 1 Register");
                else writer.print("\n\tLI " + operand1.toString() + ", 0");
                return operand1;
            }
        }
        if (bo.op == Op.NE) {
            // Must be careful, can see characters here.
            // Type checking ensures that both operands of != are the same type.
            if (bo.expr1 instanceof ChrLiteral) {
                if (op1Null && op2Null) {
                    ChrLiteral const16_1 = (ChrLiteral)bo.expr1;
                    ChrLiteral const16_2 = (ChrLiteral)bo.expr2;
    
                    operand1 = getRegister();
                    if (const16_1.val != const16_2.val) writer.print("\n\tLI " + operand1.toString() + ", 1\t# Value 1 Register");
                    else writer.print("\n\tLI " + operand1.toString() + ", 0");
                    return operand1;
                }
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
                if (op1Null && op2Null) {
                    IntLiteral const16_1 = (IntLiteral)bo.expr1;
                    IntLiteral const16_2 = (IntLiteral)bo.expr2;
    
                    operand1 = getRegister();
                    if (const16_1.val != const16_2.val) writer.print("\n\tLI " + operand1.toString() + ", 1\t# Value 1 Register");
                    else writer.print("\n\tLI " + operand1.toString() + ", 0");
                    return operand1;
                }
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
                if (op1Null && op2Null) {
                    ChrLiteral const16_1 = (ChrLiteral)bo.expr1;
                    ChrLiteral const16_2 = (ChrLiteral)bo.expr2;
    
                    operand1 = getRegister();
                    if (const16_1.val == const16_2.val) writer.print("\n\tLI " + operand1.toString() + ", 1\t# Value 1 Register");
                    else writer.print("\n\tLI " + operand1.toString() + ", 0");
                    return operand1;
                }
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
                writer.print("\n\tLI " + valOne.toString() + ", 1\t# Value 1 Register");

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
                if (op1Null && op2Null) {
                    IntLiteral const16_1 = (IntLiteral)bo.expr1;
                    IntLiteral const16_2 = (IntLiteral)bo.expr2;
    
                    operand1 = getRegister();
                    if (const16_1.val == const16_2.val) writer.print("\n\tLI " + operand1.toString() + ", 1\t# Value 1 Register");
                    else writer.print("\n\tLI " + operand1.toString() + ", 0");
                    return operand1;
                }
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
                writer.print("\n\tLI " + valOne.toString() + ", 1\t# Value 1 Register");

                // If the difference between the 2 numbers is zero, they are equal, so return a register with 0.
                writer.print("\n\tMOVZ " + operand2.toString() + ", " + valOne.toString() + ", " + operand1.toString());
                // If the difference between the 2 numbers is non-zero, they are not equal, so return a register with 1.
                writer.print("\n\tMOVN " + operand2.toString() + ", $zero, " + operand1.toString());
                freeRegister(valOne);
                freeRegister(operand1);
                return operand2;
            }
        }
        if (bo.op == Op.OR) {
            // We have a literal boolean statement, we can just simplify it.
            if (op1Null && op2Null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                Register output = getRegister();
                if (const16_1.val != 0 || const16_2.val != 0) writer.print("LI " + output.toString() + ", 1");
                else  writer.print("LI " + output.toString() + ", 0");
                return output;
            }
            else if (op1Null) {
                // We have: 1 || y
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                writer.print("ORI " + operand2.toString() + ", " + operand2.toString() + ", " + const16_1.val);
                return operand2;
            }
            else if (op2Null) {
                // We have: x || 1
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                writer.print("ORI " + operand1.toString() + ", " + operand1.toString() + ", " + const16_2.val);
                return operand1;
            }
            else {
                // We have: x || y
                writer.print("OR " + operand1.toString() + ", " + operand1.toString() + ", " + operand2.toString());
                freeRegister(operand2);
                return operand1;
            }
        }
        if (bo.op == Op.AND) {
            // We have a literal boolean statement, we can just simplify it.
            if (op1Null && op2Null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                Register output = getRegister();
                if (const16_1.val != 0 && const16_2.val != 0) writer.print("LI " + output.toString() + ", 1");
                else  writer.print("LI " + output.toString() + ", 0");
                return output;
            }
            else if (op1Null) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                operand1 = getRegister();
                writer.print("LI " + operand1.toString() + ", " + const16_1.val);
            }
            else if (op2Null) {

            }
            else {

            }
        }


		return null;
    }
    
    
    @Override
    public Register visitFunCallExpr(FunCallExpr fce) { 
        /* --- Push all arguments onto the stack. --- */
        
        // Point the FramePointer at the top of the new stack frame.
        writer.print("\n\tMOVE $fp, $sp");
        
        // Allocate memory on stack for each of the arguments.
        for (int i = 0; i < fce.exprs.size(); i++) {
            // Get the current parameter being passed, and the argument expected.
            VarDecl currArg = fce.fd.params.get(i);
            Expr  currParam = fce.exprs.get(i);

            /* --- Deal with this parameter differently based on its type. --- */
            
            // Dealing with an int literal.
            if (currParam instanceof IntLiteral) {
                // Make space on stack for this parameter.
                writer.print("\n\tADDI $sp, $sp, -4");
                // Get a register to store this parameter in.
                Register paramReg = getRegister();
                // Load this int literal into a register > stack.
                writer.print("\n\tADDI " + paramReg + ", $zero, " + ((IntLiteral)currParam).val);
                writer.print("\n\tSW " + paramReg + ", ($sp)");
                // Free this register.
                freeRegister(paramReg);
            }
            // Dealing with a String literal.
            else if (currParam instanceof StrLiteral) {
                // Make space on the stack for this parameter.
                writer.print("\n\tADDI $sp, $sp, -4");
                // Get a register to store this parameter in.
                Register paramReg = getRegister();
                // Since this is a string literal, declare this string.
                writer.print("\n\t\t.data\n\tstr" + strNum + ":\t.asciiz \"" + ((StrLiteral)currParam).val + "\"\n\t\t.text");
                // Load this string literals address into a register > stack, and increment the strNum counter.
                writer.print("\n\tLA " + paramReg + ", str"+strNum); strNum++;
                writer.print("\n\tSW " + paramReg + ", ($sp)");
                // Free this register.
                freeRegister(paramReg);
            }
            // Dealing with a char literal.
            else if (currParam instanceof ChrLiteral) {
                // Make space on the stack for this parameter.
                writer.print("\n\tADDI $sp, $sp, -4");
                // Get a register to store this parameter in.
                Register paramReg = getRegister();
                // Load this char literal into a register > stack.
                writer.print("\n\tLI " + paramReg + ", '" + ((ChrLiteral)currParam).val + "'");
                writer.print("\n\tSW " + paramReg + ", ($sp)");
                // Free this register.
                freeRegister(paramReg);
            }
            // Dealing with a variable.
            else if (currParam instanceof VarExpr) {
                Register paramReg;
                VarExpr var = (VarExpr)currParam;
                if (var.type instanceof BaseType) {
                    // Make space on the stack for this parameter.
                    writer.print("\n\tADDI $sp, $sp, -4");
                    paramReg = varMappings.get(var.ident);
                    writer.print("\n\tSW " + paramReg + ", ($sp)");
                }
                if (var.type instanceof ArrayType) {

                }
                if (var.type instanceof StructType) {

                }
                if (var.type instanceof PointerType) {

                }
            }
            else {
                Register paramReg = currParam.accept(this);
                // Make space on the stack for this parameter.
                writer.print("\n\tADDI $sp, $sp, -4");
                // Store this register on the stack.
                writer.print("\n\tSW " + paramReg + ", ($sp)");
                // Free this register.
                freeRegister(paramReg);
            }
            // // Dealing with a variable.
            // else if (currParam instanceof VarExpr) {
            //     Register paramReg = currParam.accept(this);
            //     // Make space on the stack for this parameter.
            //     writer.print("\n\tADDI $sp, $sp, -4");
            //     // Store this register on the stack.
            //     writer.print("\n\tSW " + paramReg + ", ($sp)");
            //     // Free this register.
            //     freeRegister(paramReg);
            // }
            // // Dealing with a field access.
            // else if (currParam instanceof FieldAccessExpr) {

            // }
            // // Dealing with an array access.
            // else if (currParam instanceof ArrayAccessExpr) {

            // }
            // // @TODO: ValueAtExpr, TypecastExpr, FunCallExpr
            // // Else dealing with: BinOp, SizeOfExpr - which are both ints.
            // else {
            //     writer.print("\n\tMOVE $a" + i + ", " + currParam.accept(this));
            // }
        }



        writer.print("\n");
        writer.print("\tjal " + fce.ident);

        return Register.v0;
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
        return null;
    }

    @Override
    public Register visitVarExpr(VarExpr v) {
        Register output = varMappings.get(v.ident);
        return output;
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
    
    @Override
    public Register visitIntLiteral(IntLiteral il) {
        return null;
    }

}
