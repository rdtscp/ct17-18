package sem;

import ast.*;
import java.util.HashMap;

public class TypeCheckVisitor extends BaseSemanticVisitor<Type> {

	Scope currScope = null;
	FunDecl currFunDecl = null;
	HashMap<String, StructIdent> structTypes;
	
	@Override
	public Type visitProgram(Program p) {
		structTypes = new HashMap<String, StructIdent>();
		currScope = new Scope();
		for (StructTypeDecl structTypeDecl : p.structTypeDecls) structTypeDecl.accept(this);
		for (VarDecl varDecl: p.varDecls) varDecl.accept(this);
		for (FunDecl funDecl: p.funDecls) funDecl.accept(this);
		return null;
	}

	@Override
	public Type visitStructTypeDecl(StructTypeDecl std) {
		String structTypeIdent = std.structType.identifier;
		// Check if we already have a StructIdent under this ident.
		StructIdent newStruct = new StructIdent(structTypeIdent, std.varDecls);
		structTypes.put(std.structType.identifier, newStruct);
		return null;
	}

	@Override
	public Type visitVarDecl(VarDecl vd) {
		String varIdent = vd.ident;

		// Create the VarDecl's Symbol.
		Symbol varDecl = null;
		// TYPE IDENT;
		if (vd.type instanceof BaseType) {
			if (vd.type == BaseType.VOID) {
				error("Variable cannot have type void.");
				return null;
			}
			varDecl = new Variable(vd, varIdent);
		}
		// // struct IDENT IDENT;
		else if (vd.type instanceof StructType) {
			// Get the ident of the type.
			String structTypeIdent = ((StructType)vd.type).identifier;
			// Get the Type>Fields mapping object.
			StructIdent type = structTypes.get(structTypeIdent);
			varDecl = new Struct(vd, type, varIdent);
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

		// We have done NameAnalysis, so can just safely add this variable to scope.
		currScope.put(varDecl);

		return null;
	}

	@Override
	public Type visitFunDecl(FunDecl fd) {
		currFunDecl = fd;
		// If we are at this stage, we know that there are no name conflicts.
		currScope.put(new Procedure(fd, fd.name, fd.type));

		// Create a Scope for this FunDecl, and check the scope of all its items.
		currScope = new Scope(currScope);
		// Check Params.
		for (VarDecl varDecl: fd.params) varDecl.accept(this);
		// Check block.
		visitBlock(fd.block);

		// Return to the previous scope.
		currScope = currScope.outer;

		currFunDecl = null;
		return null;
	}

	@Override
	public Type visitBlock(Block b) {
		for (VarDecl varDecl: b.varDecls) varDecl.accept(this);
		for (Stmt stmt: b.stmts) stmt.accept(this);
		return null;
	}

	@Override
	public Type visitWhile(While w) {
		Type whileCondition = w.expr.accept(this);
		if (whileCondition != BaseType.INT) {
			error("While Condition is not of Type BaseType.INT");
		}
		currScope = new Scope(currScope);
		w.stmt.accept(this);
		currScope = currScope.outer;
		return null;
	}

	@Override
	public Type visitIf(If i) {
		Type ifCondition = i.expr.accept(this);
		if (ifCondition != BaseType.INT) {
			error("If Condition is not of Type BaseType.INT");
		}
		currScope = new Scope(currScope);
		i.stmt1.accept(this);
		currScope = currScope.outer;

		if (i.stmt2 != null) {
			currScope = new Scope(currScope);
			i.stmt2.accept(this);
			currScope = currScope.outer;
		}
		return null;
	}

	@Override
	public Type visitReturn(Return r) {
		Type funRetType = currFunDecl.type;
		Type returnType = BaseType.VOID;
		if (r.expr != null) {
			returnType = r.expr.accept(this);
			if (funRetType != returnType) {
				error("Return statement in FunDecl[" + currFunDecl.name + "] should return [" + funRetType + "], but returns: " + returnType);
			}
		}
		else {
			if (funRetType != returnType) {
				error("Return statement in FunDecl[" + currFunDecl.name + "] should return [VOID], but returns: " + funRetType);
			}
		}
		return null;
	}

	@Override
	public Type visitVarExpr(VarExpr v) {
		Symbol varDeclSym = currScope.lookup(v.ident);
		ASTNode varDeclNode = varDeclSym.decl;
		VarDecl varDecl = (VarDecl) varDeclNode;
		return varDecl.type;
	}

	@Override
	public Type visitAssign(Assign a) {
		// a.expr1 should be one of the following: VarExpr, FieldAccessExpr, ArrayAccessExpr or ValuteAtExpr
		if (!(a.expr1 instanceof VarExpr) && !(a.expr1 instanceof FieldAccessExpr) && !(a.expr1 instanceof ArrayAccessExpr) && !(a.expr1 instanceof ValueAtExpr)) {
			error("LHS of Assign is not one of the following: VarExpr, FieldAccessExpr, ArrayAccessExpr or ValuteAtExpr");
		}
		Type lhs = a.expr1.accept(this);
		Type rhs = a.expr2.accept(this);
		if (lhs != rhs) {
			error("Assignment LHS has different Type expectation to RHS Type.");
		}
		return null;
	}

	@Override
	public Type visitExprStmt(ExprStmt es) {
		return es.expr.accept(this);
	}

	@Override
    public Type visitArrayAccessExpr(ArrayAccessExpr aae) {
		Type arrayType = aae.array.accept(this);
		Type indexExpType = aae.index.accept(this);
		if (indexExpType != BaseType.INT) {
			error("Attempted to reference an array index with expression that is not of Type BaseType.INT");
		}
		return arrayType;
    }

	@Override
    public Type visitFieldAccessExpr(FieldAccessExpr fae) {
		VarExpr var = (VarExpr)fae.struct;

		Symbol structDecl = currScope.lookup(var.ident);
		// System.out.println(" --- Struct Field Access ---");
		// System.out.println("   Accessing: " + var.ident + "." + fae.field);
		// System.out.println("   StructType: " + ((Struct)structDecl).type.typeIdent);
		// System.out.println("   Num Fields: " + ((Struct)structDecl).type.fields.size());
		
		for (VarDecl field: ((Struct)structDecl).type.fields) {
			// System.out.println("      Looking @ field: " + field.ident + " with type: " + field.type);
			if (field.ident.equals(fae.field)) {
				return field.type;
			}
		}

		error("FATAL Error, should never reach here! Error occured because Struct Field access attempted to access a field that did not exist.");
        return null;
    }

	@Override
    public Type visitFunCallExpr(FunCallExpr fce) {
		if (fce.ident.equals("print_s") || fce.ident.equals("print_c") || fce.ident.equals("print_i")) {
			return BaseType.VOID;
		}
		if (fce.ident.equals("read_c") || fce.ident.equals("read_i")) {
			return BaseType.CHAR;
		}
		Symbol funDeclSym = currScope.lookup(fce.ident);
		FunDecl funDecl = (FunDecl)funDeclSym.decl;
		// Check if we are calling this function with incorrect number of params.
		if (fce.exprs.size() != funDecl.params.size()) {
			error("Function [" + funDecl.name + "] requires " + funDecl.params.size() + " parameters, but " + fce.exprs.size() + " were provided.");
			return BaseType.VOID;
		}
		for (int i=0; i < fce.exprs.size(); i++) {
			Type argType   = fce.exprs.get(i).accept(this);
			Type paramType = funDecl.params.get(i).type;
			if (!(argType.getClass().equals(paramType.getClass()))) {
				error("Parameter " + i + " of FunCall[" + fce.ident + " is not the correct type");
			}
		}
		return funDecl.type;
	}
	
	@Override
    public Type visitTypecastExpr(TypecastExpr te) {
		return te.type;
	}
	
	@Override
    public Type visitValueAtExpr(ValueAtExpr vae) {
		return vae.expr.accept(this);
    }


	@Override
    public Type visitIntLiteral(IntLiteral il) {
        return BaseType.INT;
    }

    @Override
    public Type visitStrLiteral(StrLiteral sl) {
        return BaseType.CHAR;
    }

    @Override
    public Type visitChrLiteral(ChrLiteral cl) {
        return BaseType.CHAR;
	}

	@Override
    public Type visitBinOp(BinOp bo) {
		Type lhs = bo.expr1.accept(this);
		Type rhs = bo.expr2.accept(this);
		if (lhs == BaseType.VOID) {
			error("LHS of Binary Operation has type VOID");
		}
		if (rhs == BaseType.VOID) {
			error("RHS of Binary Operation has type VOID");
		}
		if (lhs != rhs) {
			error("Binary Operation where LHS and RHS are not of same type.");
		}
		switch (bo.op) {
			case SUB:
				if (lhs != BaseType.INT) error("LHS of - Operation is not of Type INT");
				if (rhs != BaseType.INT) error("RHS of - Operation is not of Type INT");
				break;
			case MUL:
				if (lhs != BaseType.INT) error("LHS of * Operation is not of Type INT");
				if (rhs != BaseType.INT) error("RHS of * Operation is not of Type INT");
				break;
			case DIV:
				if (lhs != BaseType.INT) error("LHS of / Operation is not of Type INT");
				if (rhs != BaseType.INT) error("RHS of / Operation is not of Type INT");
				break;
			case MOD:
				if (lhs != BaseType.INT) error("LHS of % Operation is not of Type INT");
				if (rhs != BaseType.INT) error("RHS of % Operation is not of Type INT");
				break;
			case GT:
				if (lhs != BaseType.INT) error("LHS of > Operation is not of Type INT");
				if (rhs != BaseType.INT) error("RHS of > Operation is not of Type INT");
				break;
			case LT:
				if (lhs != BaseType.INT) error("LHS of < Operation is not of Type INT");
				if (rhs != BaseType.INT) error("RHS of < Operation is not of Type INT");
				break;
			case GE:
				if (lhs != BaseType.INT) error("LHS of >= Operation is not of Type INT");
				if (rhs != BaseType.INT) error("RHS of >= Operation is not of Type INT");
				break;
			case LE:
				if (lhs != BaseType.INT) error("LHS of <= Operation is not of Type INT");
				if (rhs != BaseType.INT) error("RHS of <= Operation is not of Type INT");
				break;
			case OR:
				if (lhs != BaseType.INT) error("LHS of || Operation is not of Type INT");
				if (rhs != BaseType.INT) error("RHS of || Operation is not of Type INT");
				break;
			case AND:
				if (lhs != BaseType.INT) error("LHS of && Operation is not of Type INT");
				if (rhs != BaseType.INT) error("RHS of && Operation is not of Type INT");
				break;
		}
        return lhs;
    }

    @Override
    public Type visitSizeOfExpr(SizeOfExpr soe) {
        return BaseType.INT;
    }    



	



	/**************************\
			   Not Used
	\**************************/

	
	@Override
	public Type visitArrayType(ArrayType at) {
		// To be completed...
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
	public Type visitStructType(StructType st) {
		// To be completed...
		return null;
	}

	@Override
	public Type visitPointerType(PointerType pt) {
		// To be completed...
		return null;
	}

}
