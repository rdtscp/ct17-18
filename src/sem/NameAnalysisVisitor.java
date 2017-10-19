package sem;

import ast.*;

public class NameAnalysisVisitor extends BaseSemanticVisitor<Void> {

	@Override
	public Void visitBaseType(BaseType bt) {
		// To be completed...
		return null;
	}

	@Override
	public Void visitStructTypeDecl(StructTypeDecl sts) {
		// To be completed...
		return null;
	}

	@Override
	public Void visitBlock(Block b) {
		// To be completed...
		return null;
	}

	@Override
	public Void visitFunDecl(FunDecl p) {
		// To be completed...
		return null;
	}


	@Override
	public Void visitProgram(Program p) {
		// To be completed...
		return null;
	}

	@Override
	public Void visitVarDecl(VarDecl vd) {
		// To be completed...
		return null;
	}

	@Override
	public Void visitVarExpr(VarExpr v) {
		// To be completed...
		return null;
	}

	@Override
	public Void visitStructType(StructType st) {
		// To be completed...
		return null;
	}

	@Override
	public Void visitPointerType(PointerType pt) {
		// To be completed...
		return null;
	}

	@Override
	public Void visitArrayType(ArrayType at) {
		// To be completed...
		return null;
	}

	@Override
	public Void visitWhile(While w) {
		// To be completed...
		return null;
	}

	@Override
	public Void visitIf(If i) {
		// To be completed...
		return null;
	}

	@Override
	public Void visitAssign(Assign a) {
		// To be completed...
		return null;
	}

	@Override
	public Void visitReturn(Return r) {
		// To be completed...
		return null;
	}

	@Override
	public Void visitExprStmt(ExprStmt es) {
		// To be completed...
		return null;
	}

	@Override
    public Void visitIntLiteral(IntLiteral il) {
        return null;
    }

    @Override
    public Void visitStrLiteral(StrLiteral sl) {
        return null;
    }

    @Override
    public Void visitChrLiteral(ChrLiteral cl) {
        return null;
    }

    @Override
    public Void visitArrayAccessExpr(ArrayAccessExpr aae) {
        return null;
    }

    @Override
    public Void visitBinOp(BinOp bo) {
        return null;
    }

    @Override
    public Void visitFieldAccessExpr(FieldAccessExpr fae) {
        return null;
    }

    @Override
    public Void visitFunCallExpr(FunCallExpr fce) {
        return null;
    }

    @Override
    public Void visitSizeOfExpr(SizeOfExpr soe) {
        return null;
    }

    @Override
    public Void visitTypecastExpr(TypecastExpr te) {
        return null;
    }

    @Override
    public Void visitValueAtExpr(ValueAtExpr vae) {
        return null;
    }

    @Override
    public Void visitOp(Op o) {
        return null;
    }

	// To be completed...


}
