package sem;

import ast.*;
import java.util.HashMap;
import java.util.List;

public class NameAnalysisVisitor extends BaseSemanticVisitor<Void> {

	Scope currScope;

	@Override
	public Void visitProgram(Program p) {
		currScope = new Scope();
		for (StructTypeDecl structTypeDecl : p.structTypeDecls) structTypeDecl.accept(this);
		for (VarDecl varDecl: p.varDecls) varDecl.accept(this);
		for (FunDecl funDecl: p.funDecls) funDecl.accept(this);
		return null;
	}

	@Override
	public Void visitStructTypeDecl(StructTypeDecl std) {

		// String type = std.structType.identifier;
		
		// // Check if anything under this IDENTIFIER exists within the scope.
		// if (currScope.lookupCurrent(std.structType.identifier) == null) {
		// 	currScope.put(new StructTypeDeclSem(type, std.varDecls));
		// 	return null;
		// }
		// else {
		// 	error("Tried to declare a StructType using an identifier already in use: " + std.structType.identifier);
		// 	return null;
		// }
		return null;
	}

	@Override
	public Void visitVarDecl(VarDecl vd) {
		String varIdent = vd.ident;

		// Create the VarDecl's Symbol.
		Symbol varDecl = null;
		// TYPE IDENT;
		if (vd.type instanceof BaseType) {
			varDecl = new Variable(vd, varIdent);
		}
			// struct IDENT IDENT;
		else if (vd.type instanceof StructType) {
			String structTypeIdent = ((StructType)vd.type).identifier;
			varDecl = new Struct(vd, structTypeIdent, varIdent);
		}
		// TYPE IDENT[INT_LITERAL];
		else if (vd.type instanceof ArrayType) {
			int arraySize = ((ArrayType)vd.type).size;
			varDecl = new Array(vd, varIdent, arraySize);
		}
		// TYPE * IDENT;
		else if (vd.type instanceof PointerType) {
			varDecl = new Variable(vd, varIdent);
		}

		// Check if anything else exists under this identifier exists in current scope.
		if (currScope.lookupCurrent(vd.ident) == null) {
			currScope.put(varDecl);
		}
		else {
			error("Attempted to declare a variable with identifier that is already in use: " + varIdent);
		}
		return null;
	}

	@Override
	public Void visitFunDecl(FunDecl fd) {
		// Check if anything else exists under this identifier in the current scope.
		if (currScope.lookupCurrent(fd.name) == null) {
			// Add this identifier to our current scope.
			currScope.put(new Procedure(fd.name, fd.type));
			
			// Create a Scope for this FunDecl, and check the scope of all its items.
			currScope = new Scope(currScope);
			// Check Params.
			for (VarDecl varDecl: fd.params) varDecl.accept(this);
			// Check block.
			visitBlock(fd.block);

			// Return to previous scope.
			currScope = currScope.outer;
		}
		else {
			error("Attempe to declare a function with identifier that is already in use: " + fd.name);
		}
		return null;
	}

	@Override
	public Void visitBlock(Block b) {
		// Go through all VarDecl's and Stmt's checking their scope.
		for (VarDecl varDecl: b.varDecls) varDecl.accept(this);
		for (Stmt stmt: b.stmts) stmt.accept(this);
		return null;
	}	

	@Override
	public Void visitWhile(While w) {
		// Check the while condition's scope validity.
		w.expr.accept(this);

		// Create a new Scope for the while block and check all the Stmt's.
		currScope = new Scope(currScope);
		w.stmt.accept(this);

		// Return to previous Scope.
		currScope = currScope.outer;
		return null;
	}

	@Override
	public Void visitIf(If i) {
		// Check if the if condition's scope validity.
		i.expr.accept(this);

		// Create a new Scope for the IF block and check all the Stmt's.
		currScope = new Scope(currScope);
		i.stmt1.accept(this);

		// Return to the previous Scope.
		currScope = currScope.outer;

		// If an ELSE block exists, create a new Scope for it, and check it.
		if (i.stmt2 != null) {
			currScope = new Scope(currScope);
			i.stmt2.accept(this);
			currScope = currScope.outer;
		}
		return null;
	}

	@Override
	public Void visitReturn(Return r) {
		// If this return's an expression, check the expression's scope validity.
		if (r.expr != null) {
			r.expr.accept(this);
		}
		return null;
	}

	@Override
	public Void visitVarExpr(VarExpr v) {
		// Get the Symbol associated with this identifier.
		Symbol varDecl = currScope.lookup(v.ident);
		// If no Symbol exists, program is referencing something undefined.
		if (varDecl == null) {
			error("Reference to variable that does not exist: " + v.ident);
		}
		// Check the Symbol is a Variable.
		else if (!(varDecl instanceof Variable)) {
			error("Variable referenced that does not exist: " + v.ident);
		}
		else {
			v.vd = (VarDecl)varDecl.decl;
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
        return null;
    }

    @Override
    public Void visitFunCallExpr(FunCallExpr fce) {
		// Get the Symbol associated with this identifier.
		Symbol funDecl = currScope.lookup(fce.ident);
		// If no Symbol exists, program is referencing something undefined.
		if (funDecl == null) {
			error("Reference to function that does not exist: " + fce.ident);
		}
		else if (!(funDecl instanceof Procedure)) {
			error("Function referenced that does not exist: " + fce.ident);
		}
		else {
			fce.fd = (FunDecl)funDecl.decl;
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
