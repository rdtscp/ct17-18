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
    
    private PrintWriter writer; // use this writer to output the assembly instructions


    // Used so that it is easy to see how much memory a structType will use.
    private HashMap<String, StructTypeDecl> structTypeDecls = new HashMap<String, StructTypeDecl>();
    
    // Track Global variables.
    private ArrayList<String> globalVars = new ArrayList<String>();

    // Track String literals.
    private int strNum = 0;

    // Track the Call Stack
    private Stack<VarDecl> callStack = new Stack<VarDecl>();
    
    // To track current function.
    private FunDecl currFunDecl;


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
            globalVars.add(vd.ident);
        }

        /* Create functions for printing, and a jump to main to start execution. */
        writer.print("\n\n\t\t.text");
        writer.print("\nj main");

        // print_i()
        writer.print("\n\nprint_i:");
        writer.print("\n\tLW $a0, ($sp)");
        writer.print("\n\tli\t$v0, 1\t# Print int cmd code.");
        writer.print("\n\tsyscall\t\t# Print int now.");
        writer.print("\n\tADDI $sp, $sp, 4");
        writer.print("\n\tjr $ra\t\t# Return to caller.");

        // print_c()
        writer.print("\n\nprint_c:");
        writer.print("\n\tLW $a0, ($sp)");
        writer.print("\n\tli\t$v0, 11\t# Print char cmd code.");
        writer.print("\n\tsyscall\t\t# Print char now.");
        writer.print("\n\tADDI $sp, $sp, 4");
        writer.print("\n\tjr $ra\t\t# Return to caller.");

        // print_s()
        writer.print("\n\nprint_s:");
        writer.print("\n\tLW $a0, ($sp)");
        writer.print("\n\tli\t$v0, 4\t# Print str cmd code.");
        writer.print("\n\tsyscall\t\t# Print str now.");
        writer.print("\n\tADDI $sp, $sp, 4");
        writer.print("\n\tjr $ra\t\t# Return to caller.");
        
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
        currFunDecl = fd;
        // Declare this function.
        writer.print("\n\n" + fd.name + ":");
        // Store the return address on the stack.
        if (!fd.name.equals("main")) {
            writer.print("\n\tADDI $sp, $sp, -4");
            writer.print("\n\tSW $ra, ($sp)");
        }
        // Generate this functions code.
        fd.block.accept(this);
        System.out.println("FunDecl: " + fd.name + " used " + fd.stackUsage + "B on Stack.");
        if (!fd.name.equals("main")) {
            // Clear the stack, and retrieve the return address.
            writer.print("\n\tADDI $sp, $sp, " + fd.stackUsage + "\t# Move up the stack past all the params.");
            writer.print("\n\tLW $ra, ($sp)");
            writer.print("\n\tADDI $sp, $sp, 4\t# Consumed the last $ra");
            writer.print("\n\tJR $ra");
        }
        currFunDecl = null;
        return null;
    }

    /* Stmt Methods */

    @Override
	public Register visitBlock(Block b) {
        // Allocate space on stack for the local variables.
        for (VarDecl vd: b.varDecls) {
            // Get the number of bytes to allocate this variable.
            int num_bytes_alloc = vd.num_bytes;
            // Move down the stack by specified number of bytes.
            writer.print("\n\tADDI $sp, $sp, -" + num_bytes_alloc + "\t# Allocating: " + vd.ident + " " + num_bytes_alloc + "Bytes.");     System.out.println("Allocating Var: " + vd.ident + " " + num_bytes_alloc + "B on the stack.");
            // Push this VarDecl onto our CallStack tracker, and increment this func's stack usage.
            currFunDecl.stackUsage += num_bytes_alloc;
            callStack.push(vd);
        }
        // Generate code for all of this block.
        for (Stmt s: b.stmts) {
            s.accept(this);
        }
        return null;
    }

    @Override
	public Register visitAssign(Assign a) {
        if (a.expr1 instanceof VarExpr) {

            VarDecl lhsVd    = ((VarExpr)a.expr1).vd;
            VarDecl stackVar = null;
            /* Get the offset into the stack for this variable. */
            // First reverse the Stack.
            Stack<VarDecl> iterableCallStack = (Stack<VarDecl>)callStack.clone();
            Collections.reverse(iterableCallStack);

            // Traversing from bottom of stack upwards.
            int sp_offset = 0;
            for (VarDecl vd: iterableCallStack) {
                // If this VarDecl exists on the stack, under this function name.
                if (vd.ident.equals(lhsVd.ident) && vd.parentFunc.name.equals(currFunDecl.name)) {
                    stackVar = vd;
                    break;
                }
                else {
                    sp_offset += vd.num_bytes;
                }
            }
            // If this var exists on the stack.
            if (stackVar != null) {
                Register rhs = a.expr2.accept(this);
                writer.print("\n\tSW " + rhs + ", " + sp_offset + "($sp)");
                freeRegister(rhs);
            }
            else {
                Register rhs = a.expr2.accept(this);
                writer.print("\n\tSW " + rhs + ", " + lhsVd.ident);
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
        Register condition = i.expr.accept(this);
        String ifName = currFunDecl.name + "_if" + currFunDecl.currIf;
        currFunDecl.currIf++;
        writer.print("\n\tBNEZ " + condition + ", " + ifName + "_t");
        writer.print("\n\tBEQZ " + condition + ", " + ifName + "_f");
        writer.print("\n" + ifName + "_t:");
        i.stmt1.accept(this);
        writer.print("\n\tJ " + ifName + "_cont");
        writer.print("\n" + ifName + "_f:");
        if (i.stmt2 != null) {
            i.stmt2.accept(this);
            writer.print("\n\tJ " + ifName + "_cont");
        }
        writer.print("\n" + ifName + "_cont:");
        
		return null;
	}

    @Override
	public Register visitReturn(Return r) {
        if (r.expr != null) {
            Register output = r.expr.accept(this);
            writer.print("\n\tMOVE $v0, " + output);
            freeRegister(output);
            return Register.v0;
        }
		return null;
    }
    
    @Override
	public Register visitWhile(While w) {
        Register condition = w.expr.accept(this);
        String whileName = currFunDecl.name + "_while" + currFunDecl.currWhile;
        writer.print("\n\tBNEZ " + condition + ", " + whileName + "_t");
        writer.print("\n\tBEQZ " + condition + ", " + whileName + "_f");
        currFunDecl.currWhile++;
        writer.print("\n" + whileName + "_t:");
        w.stmt.accept(this);
        condition = w.expr.accept(this);
        writer.print("\n\tBNEZ " + condition + ", " + whileName + "_t");
        writer.print("\n\tBEQZ " + condition + ", " + whileName + "_f");
        writer.print("\n" + whileName + "_f:");
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
                // Want to know is op1 >= op2
                writer.print("\n\tSLT " + output + ", " + operand1 + ", " + operand2);
                writer.print("\n\tNOT " + output + ", " + output);
                freeRegister(operand1);
                freeRegister(operand2);
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
                writer.print("\n\tNOT " + output + ", " + output);
                freeRegister(operand1);
                freeRegister(operand2);
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
                writer.print("\n\tLI " + valOne + ", 1");

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
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                Register valOne = getRegister();
                writer.print("\n\tLI " + valOne + ", 1");

                writer.print("\n\tSUB " + operand1 + ", " + operand1 + ", " + operand2);
                writer.print("\n\tMOVN " + output + ",$zero , " + operand1);
                writer.print("\n\tMOVZ " + output + ", " + valOne + ", " + operand1);
                
                freeRegister(operand1);
                freeRegister(operand2);
                freeRegister(valOne);
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
        FunDecl temp = currFunDecl;
        currFunDecl = fce.fd;
        // Get a register for each param and push it onto the stack.
        for (Expr param: fce.exprs) {
            Register paramReg = param.accept(this);
            writer.print("\n\tADDI $sp, $sp, -4");
            writer.print("\n\tSW " + paramReg + ", ($sp)");
        }
        // Jump to function.
        writer.print("\n\tJAL " + fce.ident);

        // Clean up stack usage.
        // if (fce.ident.equals("print_i") || fce.ident.equals("print_c") || fce.ident.equals("print_s")) {
        //     writer.print("\n\tADDI $sp, $sp, 4")
        // }
        if (fce.exprs.size() > 0) {
            int bytes_to_clear = 0;
            for (VarDecl vd: callStack) {
                if (vd.parentFunc.name.equals(fce.fd.name)) {
                    bytes_to_clear += vd.num_bytes;
                }
            }
            writer.print("\n\tADDI $sp, $sp, " + bytes_to_clear + "\t# Clear up params on Stack.");
        }

        // Return the current function back to one we are currently in.
        currFunDecl = temp;
        return Register.v0;
	}

    @Override
    public Register visitIntLiteral(IntLiteral il) {
        Register output = getRegister();
        writer.print("\n\tLI " + output + ", " + il.val);
        return output;
    }

    // SizeOfExpr

    @Override
    public Register visitStrLiteral(StrLiteral sl) {
        Register output = getRegister();
        writer.print("\n\t\t.data");
        writer.print("\nstr" + strNum + ":\t.asciiz \"" + sl.val + "\"");
        writer.print("\n.text");
        writer.print("\n\tLA " + output + ", str" + strNum);
        strNum++;
		return output;
    }
    
    // TypecastExpr

    // ValueAtExpr

    @Override
    public Register visitVarExpr(VarExpr v) {
        Stack<VarDecl> iterableCallStack = (Stack<VarDecl>)callStack.clone();
        Collections.reverse(iterableCallStack);
        VarDecl stackVar = null;

        // Traversing from bottom of stack upwards.
        int sp_offset = 0;
        for (VarDecl vd: iterableCallStack) {
            // If this VarDecl exists on the stack, under this function name.
            if (vd.ident.equals(v.ident) && vd.parentFunc.name.equals(currFunDecl.name)) {
                stackVar = vd;
                break;
            }
            else {
                sp_offset += vd.num_bytes;
            }
        }
        // If this var exists on the stack.
        if (stackVar != null) {
            Register output = getRegister();
            writer.print("\n\tLW " + output + ", " + sp_offset + "($sp)");
            return output;
        }
        else {
            Register output = getRegister();
            writer.print("\n\tLW " + output + ", " + v.ident);
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
