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


    // Used so that it is easy to see how much memory a structType will use.
    private HashMap<String, StructTypeDecl> structTypeDecls = new HashMap<String, StructTypeDecl>();
    private int strNum = 0;             // Tracks StrLiteral's so they can be declared unique.
    
    // To track current function.
    private FunDecl currFunDecl;
    private int currWhile = 0;
    private int currIf    = 0;
    
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
            writer.print("\n" + vd.ident + ":\t.space " + vd.num_bytes);
        }

        /* Create functions for printing, and a jump to main to start execution. */
        writer.print("\n\n\t\t.text");
        writer.print("\nj main");
        writer.print("\n\nprint_i:\n\tADDI $sp, $sp, 4\n\tli\t$v0, 1\t# Print int cmd code.\n\tsyscall\t\t# Print int now.\n\tADDI $sp, $sp, 4\n\tjr $ra\t\t# Return to caller.");
        writer.print("\n\nprint_s:\n\tli\t$v0, 4\n\tsyscall\n\tADD $sp, $sp, 4\n\tjr $ra");
        writer.print("\n\nprint_c:\n\tli\t$v0, 11\n\tsyscall\n\tjr $ra");
        
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
        fd.block.accept(this);
        currFunDecl = null;
        return null;
    }

    @Override
	public Register visitBlock(Block b) {
        // Generate code for all of this block.
        for (Stmt s: b.stmts) {
            s.accept(this);
        }
        return null;
    }

    @Override
	public Register visitAssign(Assign a) {
        Register lhs = a.expr1.accept(this);
        Register rhs = a.expr1.accept(this);
        writer.print("MOVE " + lhs + ", " + rhs);
        return null;
    }

    @Override
    public Register visitExprStmt(ExprStmt es) {
		return es.expr.accept(this);
    }

    @Override
	public Register visitIf(If i) {
        Register condition = i.expr.accept(this);
        String ifName = currFunDecl.name + "_if" + currIf;
        currIf++;
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
    public Register visitFunCallExpr(FunCallExpr fce) {
        // @TEMP Will handle only the print functions.
        writer.print("\n\tMOVE $a0, " + fce.exprs.get(0).accept(this));
        writer.print("\n\tjal " + fce.ident);
        return Register.v0;
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
            // @TODO
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                writer.print("\n\tSLT " + output + ", " + operand2 + ", " + operand1);
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
            // @TODO
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                writer.print("\n\tSLT " + output + ", " + operand1 + ", " + operand2);
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
                if (const16_1.val != const16_2.val) result = 1; else result = 0;  
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
            // @TODO
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                writer.print("\n\tSLT " + output + ", " + operand2 + ", " + operand1);
                freeRegister(operand1);
                freeRegister(operand2);
                return output;
            }
        }
        if (bo.op == Op.EQ) {
            if (bo.expr1 instanceof ChrLiteral && bo.expr2 instanceof ChrLiteral) {
                ChrLiteral const16_1 = (ChrLiteral)bo.expr1;
                ChrLiteral const16_2 = (ChrLiteral)bo.expr2;
                int result;
                if (const16_1.val == const16_2.val) result = 1; else result = 0;  
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
            // @TODO
            else {
                Register operand1 = bo.expr1.accept(this);
                Register operand2 = bo.expr2.accept(this);
                writer.print("\n\tSLT " + output + ", " + operand2 + ", " + operand1);
                freeRegister(operand1);
                freeRegister(operand2);
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
            // @TODO
            else {
                return null;
            }
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
        return null;
    }

    @Override
    public Register visitVarExpr(VarExpr v) {
        return null;
    }

    /* ******************* */

    

	@Override
	public Register visitWhile(While w) {
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
    public Register visitIntLiteral(IntLiteral il) {
        Register output = getRegister();
        writer.print("\n\tLI " + output + ", " + il.val);
        return output;
    }

    @Override
    public Register visitChrLiteral(ChrLiteral cl) {
        Register output = getRegister();
        writer.print("\n\tLI " + output + ", '" + cl.val + "'");
        return output;
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
