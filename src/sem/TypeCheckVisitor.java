package sem;

import ast.*;

public class TypeCheckVisitor extends BaseSemanticVisitor<Type> {

	Scope globalScope;
	Scope currScope = null;

	@Override
	public Type visitProgram(Program p) {
		globalScope = new Scope();
		currScope = globalScope;
		System.out.println("Testing Types of Program");
		for (StructTypeDecl structTypeDecl : p.structTypeDecls) structTypeDecl.accept(this);
		for (VarDecl varDecl: p.varDecls) varDecl.accept(this);
		for (FunDecl funDecl: p.funDecls) funDecl.accept(this);
		return null;
	}

	@Override
	public Type visitStructTypeDecl(StructTypeDecl st) {
		// To be completed...
		return null;
	}

	@Override
	public Type visitVarDecl(VarDecl vd) {
		// To be completed...
		return null;
	}

	@Override
	public Type visitVarExpr(VarExpr v) {
		// To be completed...
		return null;
	}

	@Override
	public Type visitStructType(StructType st) {
		// To be completed...
		return null;
	}

	@Override
	public Type visitPointerType(PointerType pt) {
		// To be completed...
		return null;
	}

	@Override
	public Type visitArrayType(ArrayType at) {
		// To be completed...
		return null;
	}

	@Override
	public Type visitWhile(While w) {
		// To be completed...
		return null;
	}

	@Override
	public Type visitIf(If i) {
		// To be completed...
		return null;
	}

	@Override
	public Type visitAssign(Assign a) {
		// To be completed...
		return null;
	}

	@Override
	public Type visitReturn(Return r) {
		// To be completed...
		return null;
	}

	@Override
	public Type visitExprStmt(ExprStmt es) {
		// To be completed...
		return null;
	}

	@Override
    public Type visitIntLiteral(IntLiteral il) {
        return null;
    }

    @Override
    public Type visitStrLiteral(StrLiteral sl) {
        return null;
    }

    @Override
    public Type visitChrLiteral(ChrLiteral cl) {
        return null;
    }

    @Override
    public Type visitArrayAccessExpr(ArrayAccessExpr aae) {
        return null;
    }

    @Override
    public Type visitBinOp(BinOp bo) {
        return null;
    }

    @Override
    public Type visitFieldAccessExpr(FieldAccessExpr fae) {
        return null;
    }

    @Override
    public Type visitFunCallExpr(FunCallExpr fce) {
        return null;
    }

    @Override
    public Type visitSizeOfExpr(SizeOfExpr soe) {
        return null;
    }

    @Override
    public Type visitTypecastExpr(TypecastExpr te) {
        return null;
    }

    @Override
    public Type visitValueAtExpr(ValueAtExpr vae) {
        return null;
    }

    @Override
    public Type visitOp(Op o) {
        return null;
	}
	
	@Override
	public Type visitBaseType(BaseType bt) {
		// To be completed...
		return null;
	}

	@Override
	public Type visitBlock(Block b) {
		// To be completed...
		return null;
	}

	@Override
	public Type visitFunDecl(FunDecl p) {
		// To be completed...
		return null;
	}


	

	// To be completed...


}
