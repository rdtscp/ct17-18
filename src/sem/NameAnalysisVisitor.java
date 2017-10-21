package sem;

import ast.*;
import java.util.HashMap;

public class NameAnalysisVisitor extends BaseSemanticVisitor<Void> {

	Scope globalScope;
	Scope currScope = null;

	@Override
	public Void visitProgram(Program p) {
		globalScope = new Scope();
		currScope  = globalScope;
		for (StructTypeDecl structTypeDecl : p.structTypeDecls) structTypeDecl.accept(this);
		for (VarDecl varDecl: p.varDecls) varDecl.accept(this);
		for (FunDecl funDecl: p.funDecls) funDecl.accept(this);
		return null;
	}

	@Override
	public Void visitStructTypeDecl(StructTypeDecl sts) {
		String structTypeIdent = sts.structName.structType;
		// Check if this IDENT exists within the current scope.
		if (currScope.lookupCurrent(structTypeIdent) == null) {
			currScope.put(new StructIdent(structTypeIdent));
			// Create a new Scope for this StructTypeDecl's fields.
			Scope structScope = new Scope(currScope);
			currScope = structScope;
			for (VarDecl varDecl: sts.varDecls) varDecl.accept(this);
			currScope = currScope.outer;
		}
		else {
			error("Identifier already exists: " + structTypeIdent);
		}
		return null;
	}

	@Override
	public Void visitVarDecl(VarDecl vd) {
		if (currScope.lookupCurrent(vd.varName) == null) {
			currScope.put(new Variable(vd.varName));
		}
		else {
			error("Identifier already exists: " + vd.varName);
		}
		return null;
	}

	@Override
	public Void visitFunDecl(FunDecl fd) {
		if (currScope.lookup(fd.name) == null) {
			currScope.put(new Procedure(fd.name));
			currScope = new Scope(currScope);
			for (VarDecl varDecl: fd.params) varDecl.accept(this);
			visitBlock(fd.block);
			currScope = currScope.outer;
		}
		else {
			error("Identifier already exists: " + fd.name);
		}
		return null;
	}

	@Override
	public Void visitBlock(Block b) {
		for (VarDecl varDecl: b.varDecls) varDecl.accept(this);
		for (Stmt stmt: b.stmts) stmt.accept(this);
		return null;
	}	

	@Override
	public Void visitWhile(While w) {
		w.expr.accept(this);
		currScope = new Scope(currScope);
		w.stmt.accept(this);
		currScope = currScope.outer;
		return null;
	}

	@Override
	public Void visitIf(If i) {
		i.expr.accept(this);
		Scope ifScope = new Scope(currScope);
		currScope = ifScope;
		i.stmt1.accept(this);
		currScope = currScope.outer;
		if (i.stmt2 != null) {
			Scope elseScope = new Scope(currScope);
			currScope = elseScope;
			i.stmt2.accept(this);
			currScope = currScope.outer;
		}
		// To be completed...
		return null;
	}

	@Override
	public Void visitReturn(Return r) {
		if (r.expr != null) {
			r.expr.accept(this);
		}
		return null;
	}

	@Override
	public Void visitVarExpr(VarExpr v) {
		if (currScope.lookup(v.name) == null) {
			error("Variable referenced that does not exist: " + v.name);
		}
		return null;
	}

	@Override
	public Void visitAssign(Assign a) {
		a.expr1.accept(this);
		a.expr2.accept(this);
		return null;
	}

	@Override
	public Void visitExprStmt(ExprStmt es) {
		es.expr.accept(this);
		return null;
	}

	@Override
    public Void visitArrayAccessExpr(ArrayAccessExpr aae) {
		aae.array.accept(this);
		aae.index.accept(this);
        return null;
	}
	
	@Override
    public Void visitBinOp(BinOp bo) {
		bo.expr1.accept(this);
		bo.expr2.accept(this);
        return null;
    }

    @Override
    public Void visitFieldAccessExpr(FieldAccessExpr fae) {
		// @TODO Check Field is valid.
		fae.struct.accept(this);
        return null;
    }

    @Override
    public Void visitFunCallExpr(FunCallExpr fce) {
		if (currScope.lookup(fce.name) == null) {
			error("Called function that does not exits: " + fce.name);
		}
		else {
			for (Expr arg: fce.exprs) arg.accept(this);
		}
        return null;
    }

    @Override
    public Void visitTypecastExpr(TypecastExpr te) {
		te.expr.accept(this);
        return null;
    }

    @Override
    public Void visitValueAtExpr(ValueAtExpr vae) {
		vae.expr.accept(this);
        return null;
    }

	
	/**************************\
			   Not Used
	\**************************/

	@Override
    public Void visitOp(Op o) {
        return null;
    }
	
	@Override
    public Void visitSizeOfExpr(SizeOfExpr soe) {
        return null;
    }

	@Override
	public Void visitBaseType(BaseType bt) {
		return null;
	}
	
	@Override
	public Void visitStructType(StructType st) {
		return null;
	}

	@Override
	public Void visitPointerType(PointerType pt) {
		return null;
	}

	@Override
	public Void visitArrayType(ArrayType at) {
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

}
