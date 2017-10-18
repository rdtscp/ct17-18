package ast;

import java.io.PrintWriter;

public class ASTPrinter implements ASTVisitor<Void> {

    private PrintWriter writer;

    public ASTPrinter(PrintWriter writer) {
            this.writer = writer;
    }

    @Override
    public Void visitFunDecl(FunDecl fd) {
        writer.print("FunDecl(");
        fd.type.accept(this);
        writer.print(","+fd.name+",");
        for (VarDecl vd : fd.params) {
            vd.accept(this);
            writer.print(",");
        }
        // fd.block.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitProgram(Program p) {
        writer.print("Program(");
        String delimiter = "";
        for (StructTypeDecl std : p.structTypeDecls) {
            writer.print(delimiter);
            delimiter = ",";
            std.accept(this);
        }
        for (VarDecl vd : p.varDecls) {
            writer.print(delimiter);
            delimiter = ",";
            vd.accept(this);
        }
        for (FunDecl fd : p.funDecls) {
            writer.print(delimiter);
            delimiter = ",";
            fd.accept(this);
        }
        writer.print(")");
        writer.print("\n");
        writer.flush();
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl vd){
        writer.print("VarDecl(");
        vd.type.accept(this);
        writer.print(","+vd.varName);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitVarExpr(VarExpr v) {
        writer.print("Var(");
        writer.print(v.name);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitBaseType(BaseType bt) {
        writer.print(bt);
        return null;
    }

    @Override
    public Void visitStructTypeDecl(StructTypeDecl st) {
        writer.print("StructTypeDecl(");
        writer.print(st.structName + ",");
        String delimiter = "";
        for (VarDecl vd : st.varDecls) {
            writer.print(delimiter);
            delimiter = ",";
            vd.accept(this);
        }     
        writer.print(")");
        return null;
    }

    @Override
    public Void visitStructType(StructType st) {
        writer.print("StructType(");
        writer.print(st.structType + ")");
        return null;
    }

    @Override
    public Void visitPointerType(PointerType pt) {
        writer.print("PointerType(");
        pt.type.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitArrayType(ArrayType at) {
        writer.print("ArrayType(");
        at.arrayType.accept(this);
        writer.print("," + at.size + ")");
        return null;
    }

    @Override
    public Void visitBlock(Block b) {
        writer.print("Block(");
        // to complete
        writer.print(")");
        return null;
    }

	@Override
	public Void visitWhile(While w) {
        writer.print("While(");
        w.expr.accept(this);
        writer.print(",");
        w.stmt.accept(this);
        writer.print(")");
		return null;
	}

	@Override
	public Void visitIf(If i) {
        writer.print("If(");
        i.expr.accept(this);
        writer.print(",");
        i.stmt1.accept(this);
        if (i.stmt2 != null) {
            writer.print(",");
            i.stmt2.accept(this);
        }
        writer.print(")");
		return null;
	}

	@Override
	public Void visitAssign(Assign a) {
        writer.print("Assign(");
        a.expr1.accept(this);
        writer.print(",");
        a.expr2.accept(this);
        writer.print(")");
        return null;
	}

	@Override
	public Void visitReturn(Return r) {
        writer.print("Return(");
        if (r.expr != null) {
            r.expr.accept(this);
        }
        writer.print(")");
		return null;
	}

	@Override
	public Void visitExprStmt(ExprStmt es) {
        writer.print("ExprStmt(");
        es.expr.accept(this);
        writer.print(")");
		return null;
	}

    // to complete ...
    
}
