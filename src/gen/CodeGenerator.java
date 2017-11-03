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

        writer.print("\t\t.text\n\n");
        writer.print("j main\n\n");
        // Create functions for printing:
        writer.print("print_i:\n\tli\t$v0, 1\n\tsyscall\n\tjr $ra");
        writer.print("\n\nprint_s:\n\tli\t$v0, 4\n\tsyscall\n\tjr $ra");
        
        // Find the main function first, and declare
        for (FunDecl funDecl: p.funDecls) {
            // Generate MIPS for the main function declaration.
            if (funDecl.name.equals("main")) {
                funDecl.accept(this);
            }
            // Write out the exit execution code.
            writer.print("\n\tli\t$v0, 10\n\tsyscall");
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
        System.out.println("Generating MIPS for FunDecl: " + fd.name);
        writer.print("\n\n" + fd.name + ":");
        fd.block.accept(this);
        
        return null;
    }

    @Override
	public Register visitBlock(Block b) {
        for (Stmt stmt: b.stmts) {
            stmt.accept(this);
        }
		return null;
    }

    @Override
    public Register visitExprStmt(ExprStmt es) {
		return es.expr.accept(this);
    }
    
    @Override
    public Register visitFunCallExpr(FunCallExpr fce) {
        // Load all expected arguments into the appropriate registers.
        for (int i = 0; i < fce.exprs.size(); i++) {
            VarDecl currArg = fce.fd.params.get(i);
            Expr  currParam = fce.exprs.get(i);
            System.out.println("Printing funCallExpr " + currParam);
            if (currArg.type == BaseType.INT) {
                writer.print("\n\taddi $a" + i + ", $zero, ");
                currParam.accept(this);
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
    public Register visitIntLiteral(IntLiteral il) {
        writer.print(il.val);
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
    public Register visitBinOp(BinOp bo) {
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
