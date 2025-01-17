package gen;

import ast.*;
import sem.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.EmptyStackException;
import java.util.Stack;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.*;

/*

    Notes:
    Need to create hierarchy of Scope, or pop stack otherwise reading from wrong scope.



*/

public class CodeGenerator implements ASTVisitor<Register> {

    /*
     * Simple register allocator.
     */

    // contains all the free temporary registers
    private Stack<Register> freeRegs = new Stack<Register>();

    public CodeGenerator() {
        freeAllRegs();
    }

    private void freeAllRegs() {
        freeRegs.addAll(Register.tmpRegs);
    }

    private class RegisterAllocationError extends Error {}

    private Register getRegister() {
        try {
            Register out = freeRegs.pop();
            return out;
        } catch (EmptyStackException ese) {
            throw new RegisterAllocationError(); // no more free registers, bad luck!
        }
    }

    private void freeRegister(Register reg) {
        if (reg == null) return;
        if (reg == Register.v0) return;
        freeRegs.push(reg);
    }
    
    private PrintWriter writer; // use this writer to output the assembly instructions


    // Used so that it is easy to see how much memory a structType will use.
    private HashMap<String, StructTypeDecl> structTypeDecls = new HashMap<String, StructTypeDecl>();
    
    // Track Variables stored on Stack & Heap.
    private ArrayList<String> heapAllocs = new ArrayList<String>();

    private Stack<ArrayList<VarDecl>> scopesStack= new Stack<ArrayList<VarDecl>>();;     // Stack of all stackAllocs Scopes.
    private Scope currScope;
    
    // @DEBUG
    String stackState = "";

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
        currScope = new Scope();
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

        // Print Frame
        writer.print("\nframedump:");
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

        // Print Frame
        writer.print("\nstackdump:");
        writer.print("\n\tLI $a0, '-'\n\tLI $v0, 11\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall");
        writer.print("\n\tLW $a0, ($sp)\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, 4($sp)");
        writer.print("\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, 8($sp)\n\tLI $v0, 1\n\tsyscall");
        writer.print("\n\tLI $a0, '\\n'\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, 12($sp)\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'");
        writer.print("\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, 16($sp)\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall");
        writer.print("\n\tLW $a0, 20($sp)\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, 24($sp)\n\tLI $v0, 1");
        writer.print("\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, 28($sp)\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11");
        writer.print("\n\tsyscall\n\tLW $a0, 32($sp)\n\t\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, 36($sp)");
        writer.print("\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall\n\tLW $a0, 40($sp)\n\tLI $v0, 1\n\tsyscall\n\tLI $a0, '\\n'");
        writer.print("\n\tLI $v0, 11\n\tsyscall\n\tLI $a0, '\\n'");
        writer.print("\n\tLI $v0, 11\n\tsyscall\n\tJR $ra");
        writer.print("\n\tLI $a0, '-'\n\tLI $v0, 11\n\tsyscall\n\tLI $a0, '\\n'\n\tLI $v0, 11\n\tsyscall");

        // mcmalloc()
        writer.print("\n\nmcmalloc:");
        writer.print("\n\tLW $a0, ($fp)");
        writer.print("\n\tLI $v0, 9");
        writer.print("\n\tsyscall");
        writer.print("\n\tJR $ra");


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
        currScope = new Scope(currScope);

        // Mark what FunDecl we are inside.
        currFunDecl = fd;
        // Add the parameters of this function to the StackAllocs.
        int currFPoffset = 0;
        for (VarDecl vd: fd.params) {
            vd.parentFunc = fd;                                 // Tie this VarDecl to its FunDecl.
            vd.fpOffset = currFPoffset; currFPoffset+=4;        // Increment the $fp offset for this arg/param.
            fd.stackArgsUsage += vd.num_bytes;                  // Increment the number of Bytes this FunDecl uses.
            currScope.put(new Variable(vd, vd.ident));          // Add this VarDecl to current scopes list of stackAlloc'd variables.
        }
        // Declare this function.
         writer.print("\n\n" + fd.name + ":");
        // Store the return address on the stack.
         writer.print("\n\tADDI $sp, $sp, -4\t# Move down Stack.");
         writer.print("\n\tSW $ra, ($sp)\t\t#   -> Push RET-ADDR.");
        // Generate this functions code.
        fd.block.accept(this);
        
        writer.print("\n\tJ " + fd.name + "_ret");
        // Clear the stack, and retrieve the return address.
        writer.print("\n" + fd.name + "_ret:");
        //writer.print("\n\tADDI $sp, $sp, " + fd.stackVarsUsage + "\t# Move up Stack -> Past all {" + fd.stackVarsUsage/4 + "} allocated vars for [" + fd.name + "]");
        writer.print("\n\tLW $ra, ($sp)\t\t# Load the RET-ADDR off the Stack.");
        writer.print("\n\tADDI $sp, $sp, 4\t#   -> Move up Stack.");
        writer.print("\n\tJR $ra\t\t\t\t#   -> Return to caller.");

        // Reset the current FunDecl and the hierarchy of scopes.
        currScope = currScope.outer;
        currFunDecl = null;
        return null;
    }

    /* Stmt Methods */

    @Override
	public Register visitBlock(Block b) {
        currScope = new Scope(currScope);

        writer.print("\n\t# --- NEW BLOCK --- #");
        int stackUsage = 0;
        int fpOffsetUsed = 0;
        
        // Allocate space on stack for the local variables.
        for (VarDecl vd: b.varDecls) {
            // Move down the stack by specified number of bytes.
            writer.print("\n\tADDI $sp, $sp, -" + vd.num_bytes + "\t# Allocating: " + vd.ident + " " + vd.num_bytes + " Bytes.");
            // Set the offset of this Var on stack, and decrement for the next.
            vd.fpOffset = fpOffset;
            fpOffset -= vd.num_bytes; fpOffsetUsed += vd.num_bytes;
            // Push this VarDecl onto our CallStack tracker, and increment this func's stack usage.
            currFunDecl.stackVarsUsage+= vd.num_bytes;
            stackUsage += vd.num_bytes;
            currScope.put(new Variable(vd, vd.ident));
        }
        writer.print("\n");
        // Generate code for all of this block.
        for (Stmt s: b.stmts) {
            Register stmtReg = s.accept(this);
            freeRegister(stmtReg);
        }
        writer.print("\n\tADDI $sp, $sp, " + stackUsage + "\t# Clean up variables declared within this block.");
        
        writer.print("\n\t# --- END BLOCK --- #");
        fpOffset += fpOffsetUsed;
        currScope = currScope.outer;
        return null;
    }

    @Override
	public Register visitAssign(Assign a) {
        if (a.expr1 instanceof VarExpr) {
            // Get the VarDecl for this var.
            VarExpr v = (VarExpr)a.expr1;

            Symbol varSymbol = currScope.lookup(v.ident);

            // If this var exists on the stack.
            if (varSymbol != null) {
                VarDecl stackVar = ((Variable)varSymbol).decl;
                Register rhs = a.expr2.accept(this);
                writer.print("\n\tSW " + rhs + ", " + stackVar.fpOffset + "($fp)\t# Storing " + rhs + " to Stack var [" + stackVar.ident + "]");
                freeRegister(rhs);
            }
            // Else this var exists in the heap.
            else {
                Register rhs = a.expr2.accept(this);
                writer.print("\n\tSW " + rhs + ", " + v.ident + "\t\t# Store " + rhs + " to [" + v.ident + "]");
                freeRegister(rhs);
            }
        }
        else if (a.expr1 instanceof FieldAccessExpr) {
            FieldAccessExpr fae = (FieldAccessExpr)a.expr1;
            VarExpr       faeVE = (VarExpr)fae.struct;
            VarDecl       faeVD = faeVE.vd;
            StructType       st = (StructType)faeVD.type;
            StructTypeDecl  std = structTypeDecls.get(st.identifier);
            int    structOffset = 0;
            for (VarDecl field: std.varDecls) {
                if (field.ident.equals(fae.field)) break;
                structOffset += 4;
            }

            Symbol varSymbol = currScope.lookup(faeVE.ident);
            
            // If this var exists on the stack.
            if (varSymbol != null) {
                VarDecl stackVar = ((Variable)varSymbol).decl;
                Register rhs = a.expr2.accept(this);
                writer.print("\n\tSW " + rhs + ", " + (stackVar.fpOffset - structOffset) + "($fp)\t# Storing " + rhs + " to Stack var [" + stackVar.ident + "." + fae.field + "]");
                freeRegister(rhs);
            }
            // Else this var exists in the heap.
            else {
                Register rhs = a.expr2.accept(this);
                Register heapAddr = getRegister();
                writer.print("\n\tLA " + heapAddr + ", " + faeVD.ident);
                writer.print("\n\tSW " + rhs + ", " + (0 - structOffset) + "("+ heapAddr +")\t# Storing " + rhs + " to Heap var [" + faeVD.ident + "." + fae.field + "]");
                freeRegister(rhs);
                freeRegister(heapAddr);
            }
            

        }
        else if (a.expr1 instanceof ArrayAccessExpr) {
            ArrayAccessExpr aae = (ArrayAccessExpr)a.expr1;    System.out.println("aae array: " + aae.array);

            // Get the value to assign, and the index to assign to.
            Register rhs = a.expr2.accept(this);
            Register index = aae.index.accept(this);
            
            if (aae.array instanceof VarExpr) {
                VarExpr arrVE = (VarExpr)aae.array;
                VarDecl arrVD = arrVE.vd;
                
                // Get registers for storing the offset into this variable.
                Register offsetReg = getRegister();
                Register valFour = getRegister();
                // Multiply the index by 4 to get the correct word in memory.
                writer.print("\n\tLI " + valFour + ", 4\t\t# Holds static value 4.");
                writer.print("\n\tMUL " + index + ", " + index + ", " + valFour + "\t# Calculate how far into this variable to find desired index(" + index + ").");

                // Set the variable offset based on the $fp, then decrement using the arrays offset.
                writer.print("\n\tADDI " + offsetReg + ", $fp, " + arrVD.fpOffset + "\t# Point " + offsetReg + " at the start of this array.");
                writer.print("\n\tADD " + offsetReg + ", " + offsetReg + ", " + index + "\t# Point " + offsetReg + " at the desired index (" + index + ") of this array");
                
                // Store the value.
                writer.print("\n\tSW " + rhs + ", (" + offsetReg + ")");
                freeRegister(valFour);
                freeRegister(offsetReg);
            }
            freeRegister(index);
            freeRegister(rhs);
        }
        else if (a.expr1 instanceof ValueAtExpr) {
            ValueAtExpr vae = (ValueAtExpr)a.expr1;
            Register location = vae.expr.accept(this);
            Register value    = a.expr2.accept(this);
            writer.print("\n\tSW " + value + ", (" + location + ")\t\t# Storing " + value + " at address " + location);
            freeRegister(location);
            freeRegister(value);
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
            freeRegister(stmt2Reg);
        }
        writer.print("\n\tJ " + ifName + ifNum + "_cont");  // Once done, jump to cont.

        writer.print("\n" + ifName + ifNum + "_cont:");


        // Free up registers.
        freeRegister(stmt1Reg);
        freeRegister(condition);
		return null;
	}

    @Override
	public Register visitReturn(Return r) {
        writer.print("\n\t# --- Return Statement --- #");
        if (r.expr != null) {
            Register output = r.expr.accept(this);
            writer.print("\n\tMOVE $v0, " + output + "\t\t#  Move " + output + " into output register.");
            writer.print("\n\tLW $ra, -8($fp)\t\t# Load ret address.");
            writer.print("\n\tADDI $sp, $fp, -4");
            writer.print("\n\tJR $ra");
            writer.print("\n\t# ------------------------ #");        
            freeRegister(output);
            return Register.v0;
        }
        writer.print("\n\tLW $ra, -8($fp)\t\t# Load ret address.");
        writer.print("\n\tADDI $sp, $fp, -4");
        writer.print("\n\tJR $ra");
        writer.print("\n\t# ------------------------ #");
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
        Register temp = w.stmt.accept(this);
        condition = w.expr.accept(this);
        writer.print("\n\tBNEZ " + condition + ", " + whileName + whileNum + "_t");
        writer.print("\n\tBEQZ " + condition + ", " + whileName + whileNum + "_f");
        writer.print("\n" + whileName + whileNum + "_f:");
        writer.print("\n\tJ " + whileName + whileNum + "_cont");
        writer.print("\n" + whileName + whileNum + "_cont:");
        freeRegister(condition);
        freeRegister(temp);
        return null;
    }
    
    /* Expr Methods */

    @Override
    public Register visitArrayAccessExpr(ArrayAccessExpr aae) {
        
        Register output = getRegister();
        Register index = aae.index.accept(this);

        if (aae.array instanceof VarExpr) {
            VarDecl arrVD = ((VarExpr)aae.array).vd;
                
            // Get registers for storing the offset into this variable.
            Register offsetReg = getRegister();
            Register valFour = getRegister();
            // Multiply the index by 4 to get the correct word in memory.
            writer.print("\n\tLI " + valFour + ", 4\t\t# Holds static value 4.");
            writer.print("\n\tMUL " + index + ", " + index + ", " + valFour + "\t# Calculate how far into this variable to find desired index(" + index + ").");

            // Set the variable offset based on the $fp, then decrement using the arrays offset.
            writer.print("\n\tADDI " + offsetReg + ", $fp, " + arrVD.fpOffset + "\t# Point " + offsetReg + " at the start of this array.");
            writer.print("\n\tADD " + offsetReg + ", " + offsetReg + ", " + index + "\t# Point " + offsetReg + " at the desired index (" + index + ") of this array");
            
            // Store the value.
            writer.print("\n\tLW " + output + ", (" + offsetReg + ")");
            freeRegister(valFour);
            freeRegister(offsetReg);
        }
        freeRegister(index);
        return output;
    }

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

    @Override
    public Register visitFieldAccessExpr(FieldAccessExpr fae) {
        VarExpr       faeVE = (VarExpr)fae.struct;
        VarDecl       faeVD = faeVE.vd;
        StructType       st = (StructType)faeVD.type;
        StructTypeDecl  std = structTypeDecls.get(st.identifier);
        int    structOffset = 0;
        for (VarDecl field: std.varDecls) {
            if (field.ident.equals(fae.field)) break;
            structOffset += 4;
        }

        Symbol varSymbol = currScope.lookup(faeVE.ident);
        
        // If this var exists on the stack.
        if (varSymbol != null) {
            VarDecl stackVar = ((Variable)varSymbol).decl;
            Register output = getRegister();
            writer.print("\n\tLW " + output + ", " + (stackVar.fpOffset - structOffset) + "($fp)\t# Loading  Stack var [" + stackVar.ident + "." + fae.field + "] to " + output);
            return output;
        }
        // Else this var exists in the heap.
        else {
            Register output = getRegister();
            Register heapAddr = getRegister();
            writer.print("\n\tLA " + heapAddr + ", " + faeVD.ident);
            writer.print("\n\tLW " + output + ", " + (0 - structOffset) + "(" + heapAddr + ")\t# Loading  Heap var ["  + faeVD.ident +  "." + fae.field + "] to " + output);
            freeRegister(heapAddr);
            return output;
        }
    }

    @Override
    public Register visitFunCallExpr(FunCallExpr fce) {
        // Swap out the current FunDecl so that the parameters
        // are declared within the callee's scope.
        FunDecl callee = currFunDecl;
        FunDecl caller = fce.fd;
        currFunDecl = caller;

        // Push Register state to stack.
        Stack<Register> reinstate = (Stack<Register>)freeRegs.clone();
        freeAllRegs();
        Stack<Register> allRegs   = (Stack<Register>)freeRegs.clone();
        
        writer.print("\n\t# ~~~ Saving Reg State to Stack ~~~ #");
        for (Register reg: allRegs) {
            writer.print("\n\t ADDI $sp, $sp, -4");
            writer.print("\n\t SW " + reg + "($sp)");
        }
        writer.print("\n\t# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #");

        int paramBytes = 0;
        
        // Push params onto stack in rever order.
        int num_params = fce.fd.params.size();
        writer.print("\n\n\t# --- About to call function: " + fce.ident + " --- #");
        writer.print("\n\t# Pushing {" + num_params + "} Params on Stack for [" + fce.ident + "()]");
        for (int i = (num_params - 1); i >= 0; i--) {
            currFunDecl = callee;
            Register paramReg = fce.exprs.get(i).accept(this);
            currFunDecl = caller;
            writer.print("\n\tADDI $sp, $sp, -4\t# Move down Stack.");
            writer.print("\n\tSW " + paramReg + ", ($sp)\t\t#   -> Push Param.");
            freeRegister(paramReg);
            paramBytes += 4;
        }
        
        // Push current FP to stack.
         Register temp = getRegister();
         writer.print("\n\n\t# Pushing $fp on Stack and updating $fp for [" + fce.ident + "()]");
         writer.print("\n\tMOVE " + temp + ", $sp\t\t# Store addr[param0] i.e. next $fp");
         writer.print("\n\tADDI $sp, $sp, -4\t# Move down Stack.");
         writer.print("\n\tSW $fp ($sp)\t\t#   -> Push curr $fp.");
         writer.print("\n\tMOVE $fp, " + temp + "\t\t#   -> Curr $fp -> [param0]");
         freeRegister(temp);

        // Jump to function.
        writer.print("\n\n\tJAL " + fce.ident + "\t\t\t#  CALL => " + fce.ident + "()\n");

        // Re-instate $fp & $sp
         writer.print("\n\n\t# Popping $fp off Stack and re-instating $fp after [" + fce.ident + "()]");
         writer.print("\n\tLW $fp, ($sp)\t\t# Re-Instate the $fp");
         writer.print("\n\tADDI $sp, $sp, 4\t#   -> Move up Stack.");

        // Clean up params on stack.
         writer.print("\n\n\t# Popping {" + num_params + "} Params off Stack after [" + fce.ident + "()]");
         writer.print("\n\tADDI $sp, $sp, " + paramBytes + "\t# Clean up args on stack.");
         writer.print("\n\t# --- Stack restored after function call to: " + fce.ident + " --- #");


        // Push Register state to stack.
        freeAllRegs();
        writer.print("\n\t# ~~~ Restoring Reg State from Stack ~~~ #");
        Collections.reverse(allRegs);
        for (Register reg: allRegs) {
            writer.print("\n\t LW " + reg + "($sp)");
            writer.print("\n\t ADDI $sp, $sp, 4");
        }
        writer.print("\n\t# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #");
        freeRegs = reinstate;
        

        // Return the current function back to one we are currently in.
        currFunDecl = callee;
        Register output = getRegister();
        writer.print("\n\n\tMOVE " + output + ", $v0\t\t# Move output of this function into a clean Register.\n");
        return output;
	}

    @Override
    public Register visitIntLiteral(IntLiteral il) {
        Register output = getRegister();
        writer.print("\n\tLI " + output + ", " + il.val + "\t\t# Load {" + il.val + "} into " + output);
        return output;
    }

    @Override
    public Register visitSizeOfExpr(SizeOfExpr soe) {
        if (soe.type == BaseType.INT) {
            Register output = getRegister();
            writer.print("\n\tLI " + output + ", 4\t\t\t# sizeof(int)");
            return output;
        }
        if (soe.type == BaseType.CHAR) {
            Register output = getRegister();
            writer.print("\n\tLI " + output + ", 1\t\t\t# sizeof(char)");
            return output;
        }
        if (soe.type == BaseType.VOID) {
            Register output = getRegister();
            writer.print("\n\tLI " + output + ", 0\t\t\t# sizeof(void)");
            return output;
        }
        if (soe.type instanceof PointerType) {
            Register output = getRegister();
            writer.print("\n\tLI " + output + ", 4\t\t\t# sizeof(PointerType)");
            return output;
        }
        if (soe.type instanceof ArrayType) {
            ArrayType at = (ArrayType)soe.type;
            Register output = getRegister();
            writer.print("\n\tLI " + output + ", " + at.size + "\t\t\t# sizeof(ArrayType)");
            return output;
        }
        if (soe.type instanceof StructType) {
            StructType st = (StructType)soe.type;
            StructTypeDecl std = structTypeDecls.get(st.identifier);
            Register output = getRegister();
            System.out.println("StructType " + st.identifier + " has size: " + std.compactSize + " and alloc size: " + std.allocSize);
            writer.print("\n\tLI " + output + ", " + std.compactSize + "\t\t# sizeof(StructType: " + st.identifier + ")");
            return output;
        }
        return null;
    }

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
    
    @Override
    public Register visitTypecastExpr(TypecastExpr te) {
        return te.expr.accept(this);
	}

    @Override
    public Register visitValueAtExpr(ValueAtExpr vae) {
        Register output = getRegister();
        System.out.println("VAE: " + output);
        Register addr = vae.expr.accept(this);
        writer.print("\n\tLW " + output + ", (" + addr + ")\t\t# Loading value at addr(" + addr + ")");
        freeRegister(addr);
		return output;
    }

    @Override
    public Register visitVarExpr(VarExpr v) {
        Symbol varSymbol = currScope.lookup(v.ident);
        
        // If this var exists on the stack.
        if (varSymbol != null) {
            VarDecl stackVar = ((Variable)varSymbol).decl;
            Register output = getRegister();
            writer.print("\n\tLW " + output + ", " + stackVar.fpOffset + "($fp)\t\t# Loading stack variable [" + stackVar.ident + "] into " + output);
            return output;
        }
        else {
            Register output = getRegister();
            writer.print("\n\tLW " + output + ", " + v.ident + "\t\t# Loading heap variable [" + v.ident + "] into " + output);
            return output;
        }
    }
    
    
    /* Not Used */

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
    public Register visitStructTypeDecl(StructTypeDecl st) {
        return null;
    }

    @Override
    public Register visitVarDecl(VarDecl vd) {
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
    public Register visitOp(Op o) {
        return null;
	}

}
