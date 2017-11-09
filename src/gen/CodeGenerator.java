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
            Register out = freeRegs.pop();
            if (out.toString().equals("$v0")) {
                System.out.println("\n ALLOCATING $v0");
            }
            return out;
        } catch (EmptyStackException ese) {
            throw new RegisterAllocationError(); // no more free registers, bad luck!
        }
    }

    private void freeRegister(Register reg) {
        if (reg == null) throw new RegisterAllocationError();
        if (freeRegs.contains(reg)) throw new RegisterAllocationError();
        if (reg == Register.v0) return;
        freeRegs.push(reg);
    }
    
    private PrintWriter writer; // use this writer to output the assembly instructions


    // Used so that it is easy to see how much memory a structType will use.
    private HashMap<String, StructTypeDecl> structTypeDecls = new HashMap<String, StructTypeDecl>();
    
    // Track Variables stored on Stack & Heap.
    private ArrayList<String> heapAllocs = new ArrayList<String>();
    private ArrayList<VarDecl> stackAllocs = new ArrayList<VarDecl>();

    // Track String literals.
    private int strNum = 0;

    // To track current function.
    private FunDecl currFunDecl;
    private int fpOffset = -12;

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
            writer.print("\n" + vd.ident + ":\t.space " + vd.num_bytes);
            heapAllocs.add(vd.ident);
        }

        /* Create functions for printing, and a jump to main to start execution. */
        writer.print("\n\n\t\t.text");
        // Push current FP to stack.
        writer.print("\n\n\t# Storing $fp on Stack and updating $fp for [main()]");
        Register temp = getRegister();
        writer.print("\n\tMOVE " + temp + ", $sp\t\t# Store addr[param0] i.e. next $fp");
        writer.print("\n\tLI $fp, 1337\t\t# Fake $fp");
        writer.print("\n\tADDI $sp, $sp, -4\t# Move down Stack.");
        writer.print("\n\tSW $fp ($sp)\t\t#   -> Push curr $fp.");
        writer.print("\n\tMOVE $fp, " + temp + "\t\t#   -> Curr $fp -> [param0]");
        freeRegister(temp);
        // Jump to main()
        writer.print("\n\tJAL main");
         // Write out the exit execution code.
         writer.print("\n\tli\t$v0, 10\t\t\t# Exit cmd code.\n\tsyscall\t\t\t\t# Exit program.\n");

        // Print Stack
        writer.print("\nstackdump:");
        writer.print("\n\tLI $a0, '-'\n\tLI $v0, 11\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall");
        writer.print("\n\tLW $a0, ($fp)\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, -4($fp)");
        writer.print("\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, -8($fp)\n\tLI $v0, 1\n\tsyscall");
        writer.print("\n\tLI $a0, '\\n'\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, -12($fp)\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'");
        writer.print("\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, -16($fp)\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall");
        writer.print("\n\tLW $a0, -20($fp)\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, -24($fp)\n\tLI $v0, 1");
        writer.print("\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, -28($fp)\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11");
        writer.print("\n\tsyscall\n\tLW $a0, -32($fp)\n\t\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, -36($fp)");
        writer.print("\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, -40($fp)\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'");
        writer.print("\n\tLI $v0, 11\n\tsyscall\n\tLI $a0, '\\n'");
        writer.print("\n\tLI $v0, 11\n\tsyscall\n\tJR $ra");
        writer.print("\n\tLI $a0, '-'\n\tLI $v0, 11\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall");

        // read_i()
        writer.print("\n\nread_i:");
        writer.print("\n\tLI $v0, 5");
        writer.print("\n\tsyscall");
        writer.print("\n\tJR $ra");

        // read_c()
        writer.print("\n\nread_c:");
        writer.print("\n\tLI $v0, 12");
        writer.print("\n\tsyscall");
        writer.print("\n\tJR $ra");

        // print_i()
        writer.print("\n\nprint_i:");
        writer.print("\n\tLW $a0, ($fp)");
        writer.print("\n\tLI $v0, 1\t# Print int cmd code.");
        writer.print("\n\tsyscall\t\t# Print int now.");
        writer.print("\n\tJR $ra\t\t# Return to caller.");

        // print_c()
        writer.print("\n\nprint_c:");
        writer.print("\n\tLW $a0, ($fp)");
        writer.print("\n\tLI $v0, 11\t# Print char cmd code.");
        writer.print("\n\tsyscall\t\t# Print char now.");
        writer.print("\n\tJR $ra\t\t# Return to caller.");

        // print_s()
        writer.print("\n\nprint_s:");
        writer.print("\n\tLW $a0, ($fp)");
        writer.print("\n\tLI $v0, 4\t# Print str cmd code.");
        writer.print("\n\tsyscall\t\t# Print str now.");
        // writer.print("\n\tADDI $sp, $sp, 4");
        writer.print("\n\tJR $ra\t\t# Return to caller.");
        
        
        // Declare the functions.
        for (FunDecl funDecl: p.funDecls) {
            fpOffset = -12;
            funDecl.accept(this);
        }
        return null;
    }

    @Override
    public Register visitFunDecl(FunDecl fd) {
        // Mark what FunDecl we are inside.
        currFunDecl = fd;
        // Add the parameters of this function to the StackAllocs.
        int currFPoffset = 0;
        for (VarDecl vd: fd.params) {
            vd.parentFunc = fd;                                 // Tie this VarDecl to its FunDecl.
            vd.fpOffset = currFPoffset; currFPoffset+=4;        // Increment the $fp offset for this arg/param.
            fd.stackArgsUsage += vd.num_bytes;                  // Increment the number of Bytes this FunDecl uses.
            stackAllocs.add(vd);                                // Add this VarDecl to our list of stackAlloc'd variables.
        }
        // Declare this function.
        writer.print("\n\n" + fd.name + ":");
        // Store the return address on the stack.
        writer.print("\n\tADDI $sp, $sp, -4\t# Move down Stack.");
        writer.print("\n\tSW $ra, ($sp)\t\t#   -> Push RET-ADDR.");
        // Generate this functions code.
        fd.block.accept(this);
        
        // Clear the stack, and retrieve the return address.
        writer.print("\n\tADDI $sp, $sp, " + fd.stackVarsUsage + "\t# Move up Stack -> Past all {" + fd.stackVarsUsage/4 + "} allocated vars for [" + fd.name + "]");
        writer.print("\n\tLW $ra, ($sp)\t\t# Load the RET-ADDR off the Stack.");
        writer.print("\n\tADDI $sp, $sp, 4\t#   -> Move up Stack.");
        writer.print("\n\tJR $ra\t\t\t\t#   -> Return to caller.");
        // Reset the current FunDecl and $sp Offset for variables.
        currFunDecl = null;
        return null;
    }

    /* Stmt Methods */

    @Override
	public Register visitBlock(Block b) {
        // Allocate space on stack for the local variables.
        for (VarDecl vd: b.varDecls) {
            // Move down the stack by specified number of bytes.
            writer.print("\n\tADDI $sp, $sp, -" + vd.num_bytes + "\t# Allocating: " + vd.ident + " " + vd.num_bytes + " Bytes.");
            // Set the offset of this Var on stack, and decrement for the next.
            vd.fpOffset = fpOffset;
            fpOffset -= 4;
            // Push this VarDecl onto our CallStack tracker, and increment this func's stack usage.
            currFunDecl.stackVarsUsage+= vd.num_bytes;
            stackAllocs.add(vd);
        }
        writer.print("\n");
        // Generate code for all of this block.
        for (Stmt s: b.stmts) {
            s.accept(this);
        }
        return null;
    }

    @Override
	public Register visitAssign(Assign a) {
        if (a.expr1 instanceof VarExpr) {
            // Get the VarDecl for this var.
            VarDecl lhsVd    = ((VarExpr)a.expr1).vd;

            /* Determine if this Var exists in stack or heap. */
            VarDecl stackVar = null;
            // Look through our stack allocations.
            for (VarDecl vd: stackAllocs) {
                // If this VarDecl exists on the stack, under this function name => save it, and stop searching.
                if (vd.ident.equals(lhsVd.ident) && vd.parentFunc.name.equals(currFunDecl.name)) {
                    stackVar = vd;
                    break;
                }
            }
            // If this var exists on the stack.
            if (stackVar != null) {
                Register rhs = a.expr2.accept(this);
                System.out.println(rhs + " assigned to " + a.expr2);
                writer.print("\n\tSW " + rhs + ", " + stackVar.fpOffset + "($fp)\t\t# Storing " + rhs + " to Stack var [" + stackVar.ident + "]");
                freeRegister(rhs);
            }
            // Else this var exists in the heap.
            else {
                Register rhs = a.expr2.accept(this);
                System.out.println(rhs + " assigned to " + a.expr2);
                writer.print("\n\tSW " + rhs + ", " + lhsVd.ident + "\t\t# Store " + rhs + " to [" + lhsVd.ident + "]");
                freeRegister(rhs);
            }
        }
        return null;
    }

    @Override
    public Register visitExprStmt(ExprStmt es) {
		return es.expr.accept(this);
    }

    @Override
	public Register visitIf(If i) {
        Register condition  = i.expr.accept(this);
        String ifName       = currFunDecl.name + "_if";
        int    ifNum        = currFunDecl.currIf;
        currFunDecl.currIf++;
        writer.print("\n\tBNEZ " + condition + ", " + ifName + ifNum + "_t");
        writer.print("\n\tBEQZ " + condition + ", " + ifName + ifNum + "_f");

        // Print (condition == true) case.
        writer.print("\n" + ifName + ifNum + "_t:");        // Label this branch.
        Register stmt1Reg = i.stmt1.accept(this);           // Generate code.
        writer.print("\n\tJ " + ifName + ifNum + "_cont");  // Once done, jump to cont.

        // Print (condition == false) case.
        writer.print("\n" + ifName + ifNum + "_f:");        // Label this branch.
        if (i.stmt2 != null) {                              // If there is an else stmt.
            Register stmt2Reg = i.stmt2.accept(this);       // Generate the code.
            if (stmt2Reg != null) freeRegister(stmt2Reg);
        }
        writer.print("\n\tJ " + ifName + ifNum + "_cont");  // Once done, jump to cont.

        writer.print("\n" + ifName + ifNum + "_cont:");


        // Free up registers.
        freeRegister(condition);
        if (stmt1Reg != null) freeRegister(stmt1Reg);
		return null;
	}

    @Override
	public Register visitReturn(Return r) {
        if (r.expr != null) {
            Register output = r.expr.accept(this);
            writer.print("\n\tMOVE $v0, " + output + "\t\t#  Move " + output + " into output register.");
            freeRegister(output);
            return Register.v0;
        }
		return null;
    }
    
    @Override
	public Register visitWhile(While w) {
        Register condition = w.expr.accept(this);
        String whileName = currFunDecl.name + "_while";
        int    whileNum  = currFunDecl.currWhile;
        writer.print("\n\tBNEZ " + condition + ", " + whileName + whileNum + "_t");
        writer.print("\n\tBEQZ " + condition + ", " + whileName + whileNum + "_f");
        currFunDecl.currWhile++;
        writer.print("\n" + whileName + whileNum + "_t:");
        w.stmt.accept(this);
        condition = w.expr.accept(this);
        writer.print("\n\tBNEZ " + condition + ", " + whileName + whileNum + "_t");
        writer.print("\n\tBEQZ " + condition + ", " + whileName + whileNum + "_f");
        writer.print("\n" + whileName + whileNum + "_f:");
        writer.print("\n\tJ " + whileName + whileNum + "_cont");
        writer.print("\n" + whileName + whileNum + "_cont:");
        freeRegister(condition);
        return null;
    }
    
    /* Expr Methods */

    @Override
    public Register visitBinOp(BinOp bo) {
        Register output   = getRegister();
        // Different operations can handle different operand types.
        if (bo.op == Op.ADD) {
            // If this BinOp is just two IntLiterals, we can do the calculation here.
            if (bo.expr1 instanceof IntLiteral && bo.expr2 instanceof IntLiteral) {
                // Cast both operands into IntLiterals.
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                writer.print("\n\tLI " + output + ", " + (const16_1.val + const16_2.val));
                return output;
            }
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                writer.print("\n\tADD " + output + ", " + operand1 + ", " + operand2);
                freeRegister(operand1);
                freeRegister(operand2);
                return output;
            }
        }
        if (bo.op == Op.SUB) {
            if (bo.expr1 instanceof IntLiteral && bo.expr2 instanceof IntLiteral) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                writer.print("\n\tLI " + output + ", " + (const16_1.val - const16_2.val));
                return output;
            }
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                writer.print("\n\tSUB " + output + ", " + operand1 + ", " + operand2);
                freeRegister(operand1);
                freeRegister(operand2);
                return output;
            }
        }
        if (bo.op == Op.MUL) {
            if (bo.expr1 instanceof IntLiteral && bo.expr2 instanceof IntLiteral) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                writer.print("\n\tLI " + output + ", " + (const16_1.val * const16_2.val));
                return output;
            }
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                writer.print("\n\tMUL " + output + ", " + operand1 + ", " + operand2);
                freeRegister(operand1);
                freeRegister(operand2);
                return output;
            }
        }
        if (bo.op == Op.DIV) {
            if (bo.expr1 instanceof IntLiteral && bo.expr2 instanceof IntLiteral) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                writer.print("\n\tLI " + output + ", " + (const16_1.val / const16_2.val));
                return output;
            }
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                writer.print("\n\tDIV " + operand1 + ", " + operand2);
                freeRegister(operand1);
                freeRegister(operand2);
                writer.print("\n\tMFLO " + output);
                return output;
            }
        }
        if (bo.op == Op.MOD) {
            if (bo.expr1 instanceof IntLiteral && bo.expr2 instanceof IntLiteral) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                writer.print("\n\tLI " + output + ", " + (const16_1.val % const16_2.val));
                return output;
            }
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                writer.print("\n\tDIV " + operand1 + ", " + operand2);
                freeRegister(operand1);
                freeRegister(operand2);
                writer.print("\n\tMFHI " + output);
                return output;
            }
        }
        if (bo.op == Op.GT) {
            if (bo.expr1 instanceof IntLiteral && bo.expr2 instanceof IntLiteral) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                int result;
                if (const16_1.val > const16_2.val) result = 1; else result = 0;
                writer.print("\n\tLI " + output + ", " + result);
                return output;
            }
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                writer.print("\n\tSLT " + output + ", " + operand2 + ", " + operand1);
                freeRegister(operand1);
                freeRegister(operand2);
                return output;
            }
        }          
        if (bo.op == Op.LT) {
            if (bo.expr1 instanceof IntLiteral && bo.expr2 instanceof IntLiteral) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                int result;
                if (const16_1.val < const16_2.val) result = 1; else result = 0;
                writer.print("\n\tLI " + output + ", " + result);
                return output;
            }
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                writer.print("\n\tSLT " + output + ", " + operand1 + ", " + operand2);
                freeRegister(operand1);
                freeRegister(operand2);
                return output;
            }
        } 
        if (bo.op == Op.GE) {
            if (bo.expr1 instanceof IntLiteral && bo.expr2 instanceof IntLiteral) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                int result;
                if (const16_1.val >= const16_2.val) result = 1; else result = 0;  
                writer.print("\n\tLI " + output + ", " + result);
                return output;
            }
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                // want x >= y
                // :>   z = x < y
                // :>   z = z < 1
                writer.print("\n\tSLT " + output + ", " + operand1 + ", " + operand2);
                Register valOne = getRegister();
                writer.print("\n\tLI " + valOne + ", 1");
                writer.print("\n\tSLT " + output + ", " + output + ", " + valOne);
                freeRegister(operand1);
                freeRegister(operand2);
                freeRegister(valOne);
                return output;
            }
        }
        if (bo.op == Op.LE) {
            if (bo.expr1 instanceof IntLiteral && bo.expr2 instanceof IntLiteral) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                int result;
                if (const16_1.val <= const16_2.val) result = 1; else result = 0;
                writer.print("\n\tLI " + output + ", " + result);
                return output;
            }
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                writer.print("\n\tSLT " + output + ", " + operand2 + ", " + operand1);
                Register valOne = getRegister();
                writer.print("\n\tLI " + valOne + ", 1");
                writer.print("\n\tSLT " + output + ", " + output + ", " + valOne);
                freeRegister(operand1);
                freeRegister(operand2);
                freeRegister(valOne);
                return output;
            }
        }
        if (bo.op == Op.NE) {
            if (bo.expr1 instanceof ChrLiteral && bo.expr2 instanceof ChrLiteral) {
                ChrLiteral const16_1 = (ChrLiteral)bo.expr1;
                ChrLiteral const16_2 = (ChrLiteral)bo.expr2;
                int result;
                if (!const16_1.val.equals(const16_2.val)) result = 1; else result = 0;  
                writer.print("\n\tLI " + output + ", " + result);
                return output;
            }
            if (bo.expr1 instanceof IntLiteral && bo.expr2 instanceof IntLiteral) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                int result;
                if (const16_1.val != const16_2.val) result = 1; else result = 0;  
                writer.print("\n\tLI " + output + ", " + result);
                return output;
            }
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                
                Register valOne = getRegister();
                writer.print("\n\tLI " + valOne + ", 1\t\t\t# Register to hold value 1.");

                writer.print("\n\tSUB " + operand1 + ", " + operand1 + ", " + operand2);
                writer.print("\n\tMOVZ " + output + ", $zero " + operand1);
                writer.print("\n\tMOVN " + output + ", " + valOne + ", " + output);
                freeRegister(operand1);
                freeRegister(operand2);
                freeRegister(valOne);
                return output;
            }
        }
        if (bo.op == Op.EQ) {
            if (bo.expr1 instanceof ChrLiteral && bo.expr2 instanceof ChrLiteral) {
                ChrLiteral const16_1 = (ChrLiteral)bo.expr1;
                ChrLiteral const16_2 = (ChrLiteral)bo.expr2;
                int result;
                if (const16_1.val.equals(const16_2.val)) result = 1; else result = 0;  
                writer.print("\n\tLI " + output + ", " + result);
                return output;
            }
            if (bo.expr1 instanceof IntLiteral && bo.expr2 instanceof IntLiteral) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                int result;
                if (const16_1.val == const16_2.val) result = 1; else result = 0;  
                writer.print("\n\tLI " + output + ", " + result);
                return output;
            }
            else {
                writer.print("\n\t# HERE");
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                Register valOne = getRegister();
                System.out.println(operand1 + " == " + operand2);
                writer.print("\n\tLI " + valOne + ", 1\t\t\t# Register to hold value 1.");

                writer.print("\n\tSUB " + operand1 + ", " + operand1 + ", " + operand2);
                writer.print("\n\tMOVN " + output + ",$zero , " + operand1);
                writer.print("\n\tMOVZ " + output + ", " + valOne + ", " + operand1 + "\t# " + output + " now holds if " + operand1 + " == " + operand2);
                
                freeRegister(operand1);
                freeRegister(operand2);
                freeRegister(valOne);
                writer.print("\n\t# HERE");
                return output;
            }
        }
        if (bo.op == Op.OR) {
            if (bo.expr1 instanceof IntLiteral && bo.expr2 instanceof IntLiteral) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                int result;
                if (const16_1.val == 1 || const16_2.val == 1) result = 1; else result = 0;
                writer.print("\n\tLI " + output + ", " + result);
                return output;
            }
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                writer.print("\n\tOR " + output + ", " + operand1 + ", " + operand2);
                freeRegister(operand1);
                freeRegister(operand2);
                return output;
            }
        }
        if (bo.op == Op.AND) {
            if (bo.expr1 instanceof IntLiteral && bo.expr2 instanceof IntLiteral) {
                IntLiteral const16_1 = (IntLiteral)bo.expr1;
                IntLiteral const16_2 = (IntLiteral)bo.expr2;
                int result;
                if (const16_1.val == 1 && const16_2.val == 1) result = 1; else result = 0;
                writer.print("\n\tLI " + output + ", " + result);
                return output;
            }
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                writer.print("\n\tMUL " + output + ", " + operand1 + ", " + operand2);
                freeRegister(operand1);
                freeRegister(operand2);
                return output;    
            }
        }
        return null;
    }

    @Override
    public Register visitChrLiteral(ChrLiteral cl) {
        Register output = getRegister();
        writer.print("\n\tLI " + output + ", '" + cl.val + "'");
        return output;
	}

    // FieldAccessExpr

    @Override
    public Register visitFunCallExpr(FunCallExpr fce) {
        // Swap out the current FunDecl so that the parameters
        // are declared within the callee's scope.
        FunDecl callee = currFunDecl;
        FunDecl caller = fce.fd;
        currFunDecl = caller;
        
        // Push params onto stack in rever order.
        int num_params = fce.fd.params.size();
        //System.out.println("Visiting FunCallExpr[" + fce.ident + "] with {" + num_params + "} params");
        writer.print("\n\n\t# --- About to call function: " + fce.ident + " --- #");
        writer.print("\n\t# Pushing {" + num_params + "} Params on Stack for [" + fce.ident + "()]");
        for (int i = (num_params - 1); i >= 0; i--) {
            currFunDecl = callee;
            Register paramReg = fce.exprs.get(i).accept(this);
            currFunDecl = caller;
            writer.print("\n\tADDI $sp, $sp, -4\t# Move down Stack.");
            writer.print("\n\tSW " + paramReg + ", ($sp)\t\t#   -> Push Param.");
            if (paramReg != null) freeRegister(paramReg);
        }
        
        // Push current FP to stack.
        writer.print("\n\n\t# Storing $fp on Stack and updating $fp for [" + fce.ident + "()]");
        Register temp = getRegister();
        writer.print("\n\tMOVE " + temp + ", $sp\t\t# Store addr[param0] i.e. next $fp");
        writer.print("\n\tADDI $sp, $sp, -4\t# Move down Stack.");
        writer.print("\n\tSW $fp ($sp)\t\t#   -> Push curr $fp.");
        writer.print("\n\tMOVE $fp, " + temp + "\t\t#   -> Curr $fp -> [param0]");
        freeRegister(temp);

        // Jump to function.
        writer.print("\n\n\tJAL " + fce.ident + "\t\t\t#  CALL => " + fce.ident + "()\n");

        // Re-instate $fp & $sp
        writer.print("\n\tLW $fp, ($sp)\t\t# Re-Instate the $fp");
        writer.print("\n\tSW $zero, ($sp)");
        writer.print("\n\tADDI $sp, $sp, 4\t#   -> Move up Stack.");

        // Clean up params on stack.
        for (int i = 0; i < num_params; i++) {
            writer.print("\n\tSW $zero, ($sp)");
            writer.print("\n\tADDI $sp, $sp, 4");
        }
        // writer.print("\n\tADDI $sp, $sp, " + (num_params * 4) + "\t# Clear up {" + num_params + "} params for [" + fce.ident + "()] on Stack.\n\t\t\t\t\t\t# $v0 is the return of [" + fce.ident + "()] if applicable.");
        writer.print("\n\t# --- Stack restored after function call to: " + fce.ident + " --- #\n");
        // Return the current function back to one we are currently in.
        currFunDecl = callee;
        return Register.v0;
	}

    @Override
    public Register visitIntLiteral(IntLiteral il) {
        Register output = getRegister();
        writer.print("\n\tLI " + output + ", " + il.val + "\t\t\t# Load {" + il.val + "} into " + output);
        return output;
    }

    // SizeOfExpr

    @Override
    public Register visitStrLiteral(StrLiteral sl) {
        Register output = getRegister();
        writer.print("\n\t.data");
        writer.print("\n\t\tstr" + strNum + ":\t.asciiz \"" + sl.val + "\"");
        writer.print("\n\t.text");
        writer.print("\n\tLA " + output + ", str" + strNum);
        strNum++;
		return output;
    }
    
    // TypecastExpr

    // ValueAtExpr

    @Override
    public Register visitVarExpr(VarExpr v) {
        // Search our Stack allocated Vars for this var.
        VarDecl stackVar = null;
        for (VarDecl vd: stackAllocs) {
            // If this VarDecl exists on the stack, under this function name.
            if (vd.ident.equals(v.ident) && vd.parentFunc.name.equals(currFunDecl.name)) {
                stackVar = vd;
                break;
            }
        }
        // If this var exists on the stack.
        if (stackVar != null) {
            Register output = getRegister();
            writer.print("\n\tLW " + output + ", " + stackVar.fpOffset + "($fp)\t\t# Loading variable [" + stackVar.ident + "] into " + output);
            return output;
        }
        else {
            Register output = getRegister();
            writer.print("\n\tLW " + output + ", " + v.ident + "\t\t# Loading variable [" + v.ident + "] into " + output);
            return output;
        }
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
    public Register visitSizeOfExpr(SizeOfExpr soe) {
        return null;
    }

    @Override
    public Register visitBaseType(BaseType bt) {
        return null;
    }

	@Override
	public Register visitArrayType(ArrayType at) {
		// To be completed...
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

    /* Not Used */

    @Override
    public Register visitOp(Op o) {
        return null;
	}

}
