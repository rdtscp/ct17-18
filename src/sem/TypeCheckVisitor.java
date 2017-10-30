package sem;

import ast.*;
import java.util.HashMap;
import java.util.ArrayList;

public class TypeCheckVisitor extends BaseSemanticVisitor<Type> {

	Scope currScope = null;
	HashMap<String, StructTypeDecl> structTypes;
	boolean createBlockScope = true;
	FunDecl currFunDecl = null;
	
	/**********************************************************************\
	  	TypeCheckVisitor Assumes that all variables referenced do exist. 
	\**********************************************************************/

	@Override
	public Type visitProgram(Program p) {
		// Initialise the Scope of the Program, and the declared StructTypes
		structTypes = new HashMap<String, StructTypeDecl>();
		currScope = new Scope();

		// Set Up Imported Functions.
		// --- read_i ---
		currScope.put(new Procedure(new FunDecl(BaseType.INT, "read_i", new ArrayList<VarDecl>(), null), "read_i"));
		
		// --- read_c ---
		currScope.put(new Procedure(new FunDecl(BaseType.CHAR, "read_c", new ArrayList<VarDecl>(), null), "read_c"));
		
		// --- mcmalloc ---
		ArrayList<VarDecl> mcmallocParams = new ArrayList<VarDecl>();
		mcmallocParams.add(new VarDecl(BaseType.INT, "size"));
		currScope.put(new Procedure(new FunDecl(new PointerType(BaseType.VOID), "mcmalloc", mcmallocParams, null), "mcmalloc"));
		
		// --- print_c ---
		ArrayList<VarDecl> print_cParams = new ArrayList<VarDecl>();
		print_cParams.add(new VarDecl(BaseType.CHAR, "c"));
		currScope.put(new Procedure(new FunDecl(BaseType.VOID, "print_c", print_cParams, null), "print_c"));
		
		// --- print_i ---
		ArrayList<VarDecl> print_iParams = new ArrayList<VarDecl>();
		print_iParams.add(new VarDecl(BaseType.INT, "i"));
		currScope.put(new Procedure(new FunDecl(BaseType.VOID, "print_i", print_iParams, null), "print_i"));
		
		// --- print_s ---
		ArrayList<VarDecl> print_sParams = new ArrayList<VarDecl>();
		print_sParams.add(new VarDecl(new PointerType(BaseType.CHAR), "s"));
		currScope.put(new Procedure(new FunDecl(BaseType.VOID, "print_s", print_sParams, null), "print_s"));

		// Check Names of all StructTypeDecls
		for (StructTypeDecl structTypeDecl : p.structTypeDecls) structTypeDecl.accept(this);
		
		// Check Names of all VarDecls
		for (VarDecl varDecl: p.varDecls) { varDecl.accept(this); }
		
		// Check Names of all FunDecls
		for (FunDecl funDecl: p.funDecls) { funDecl.accept(this); }
		return null;
	}

	@Override
	public Type visitStructTypeDecl(StructTypeDecl std) {
		String structTypeIdent = std.structType.identifier;

		// We can create a StructType with ident: structTypeIdent
		structTypes.put(structTypeIdent, std);
		
		// Create a new Scope for the StructTypeDecl, and check its VarDecls names dont clash.
		currScope = new Scope(currScope);
		for (VarDecl varDecl: std.varDecls) { varDecl.accept(this); }
		currScope = currScope.outer;
		
		return null;
	}

	@Override
	public Type visitVarDecl(VarDecl vd) {
		// Declare a standard INT/CHAR/VOID Variable.
		if (vd.type instanceof BaseType) {
			if (vd.type == BaseType.VOID) {
				error("Cannot declare variable of type VOID");
				return null;
			}
			currScope.put(new Variable(vd, vd.ident));
			return null;
		}
		// Declare a Struct Variable.
		if (vd.type instanceof StructType) {
			StructType vdStructType = (StructType)vd.type;
			// Add this Struct Variable to the current Scope.
			currScope.put(new Struct(vd, vd.ident, structTypes.get(vdStructType.identifier)));
			
			return null;
		}
		// Attempting to declare an Array Variable.
		if (vd.type instanceof ArrayType) {
			ArrayType vdArrayType = (ArrayType)vd.type;

			// Add this Array Variable to the Scope.
			currScope.put(new Array(vd, vd.ident));
			return null;
		}
		// Attempting to declare a Pointer Variable.
		if (vd.type instanceof PointerType) {
			PointerType vdPtrType = (PointerType)vd.type;

			// Add this VarDecl to the Scope.
			currScope.put(new Pointer(vd, vd.ident));
			return null;
		}

		// Shouldn't reach here.
		error("FATAL ERROR: VarDecl has unknown Type");
		return null;
	}

	@Override
	public Type visitFunDecl(FunDecl fd) {
		// Store the FunDecl we are currently TypeChecking. [ For use by visitReturn() ]
		currFunDecl = fd;

		// Add this identifier to our current scope.
		currScope.put(new Procedure(fd, fd.name));
		
		currScope = new Scope(currScope);
	
		// Check Params.
		for (VarDecl varDecl: fd.params) varDecl.accept(this);	
		// Check block.
		createBlockScope = false;	// Mark that visitBlock is not to create a new Scope.
		fd.block.accept(this);

		currScope = currScope.outer;
		return null;
	}

	@Override
	public Type visitBlock(Block b) {
		if (createBlockScope) {
			currScope = new Scope(currScope);
			// Go through all VarDecl's and Stmt's checking their scope.
			for (VarDecl varDecl: b.varDecls) varDecl.accept(this);
			for (Stmt stmt: b.stmts) stmt.accept(this);
			currScope = currScope.outer;
		}
		else {
			createBlockScope = true;
			// Go through all VarDecl's and Stmt's checking their scope.
			for (VarDecl varDecl: b.varDecls) varDecl.accept(this);
			for (Stmt stmt: b.stmts) stmt.accept(this);
		}
		return null;
	}

	@Override
	public Type visitWhile(While w) {
		Type conditionType = w.expr.accept(this);
		w.expr.type = conditionType;
		
		if (conditionType != BaseType.INT) error("While Condition is not of Type BaseType.INT");

		w.stmt.accept(this);

		return null;
	}

	@Override
	public Type visitIf(If i) {
		Type conditionType = i.expr.accept(this);
		i.expr.type = conditionType;

		if (conditionType != BaseType.INT) error("If Condition is not of Type BaseType.INT");

		i.stmt1.accept(this);
		if (i.stmt2 != null) i.stmt2.accept(this);

		return null;
	}

	@Override
	public Type visitReturn(Return r) {
		// Get the expected and actual return type.
		Type expectRetType = currFunDecl.type;
		Type actualRetType = BaseType.VOID;
		if (r.expr != null) actualRetType = r.expr.accept(this);

		// Check if return types match.
		if (expectRetType instanceof StructType) {
			if (actualRetType instanceof StructType) {
				StructType expectRetStructType = (StructType) expectRetType;
				StructType actualRetStructType = (StructType) actualRetType;
				if (!expectRetStructType.identifier.equals(actualRetStructType.identifier)) {
					error("Function [" + currFunDecl.name + "] returning incorrect type. Expected: [struct " + expectRetStructType.identifier + "] but returned: [struct " + actualRetStructType.identifier + "]");
				}
			}
			else {
				error("Function [" + currFunDecl.name + "] returning incorrect type. Expected: [" + expectRetType + "] but returned: [" + actualRetType + "]");
			}
		}
		else if (expectRetType != actualRetType) error("Function [" + currFunDecl.name + "] returning incorrect type. Expected: [" + expectRetType + "] but returned: [" + actualRetType + "]");

		return null;
	}

	@Override
	public Type visitVarExpr(VarExpr v) {
		Symbol varSym = currScope.lookup(v.ident);

		
		// Check that this is a Variable and not a Procedure.
		if (varSym instanceof Variable) {
			VarDecl varDecl = (VarDecl)varSym.decl;
			return varDecl.type;
		}

		// Reached here means it is a function identifier and not a Variable identifier.
		error("Incorrect usage of Function identifier: " + v.ident);
		return null;
	}

	@Override
	public Type visitAssign(Assign a) {
		// Check that the LHS is one of the following: VarExpr, FieldAccessExpr, ArrayAccessExpr, ValueAtExpr
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
		// Get the identifier of this array.
		VarExpr array = (VarExpr)aae.array;

		// Get the Symbol for this identifier.
		Symbol arraySym = currScope.lookup(array.ident);

		// Check this identifier is tied to an array.
		if (arraySym instanceof Array) {
			VarDecl arrDecl = (VarDecl)arraySym.decl;
			
			// Check that indexing expression is of type INT.
			Type indexType = aae.index.accept(this);
			if (indexType != BaseType.INT) {
				error("Attempted to index array with non-integer expression.");
				return null;
			}

			// If all is well, return the type of this array element.
			ArrayType arr = (ArrayType) arrDecl.type;
			return arr.arrayType;
		}
		else {
			error("Attempted to treat identifier [" + array.ident + "] as an array.");
			return null;
		}
    }

	@Override
    public Type visitFieldAccessExpr(FieldAccessExpr fae) {
		// Get the identifier of this struct variable.
		VarExpr var = (VarExpr)fae.struct;

		// Get the Symbol for this identifier.
		Symbol varSym = currScope.lookup(var.ident);
		
		// Check this identifier is tied to a struct variable.
		if (varSym instanceof Struct) {
			Struct structSym = (Struct)varSym;
			for (VarDecl field: structSym.std.varDecls) {
				if (field.ident.equals(fae.field)) {
					return field.type;
				}
			}
		}
		else {
			error("Attempted to treat identifier [" + var.ident + "] as a struct instance.");
			return null;
		}
		

		error("FATAL Error, should never reach here! Error occured because Struct Field access attempted to access a field that did not exist.");
        return null;
    }

	@Override
    public Type visitFunCallExpr(FunCallExpr fce) {
		Symbol funSym = currScope.lookup(fce.ident);

		// Check that we are dealing with a Procedure.
		if (funSym instanceof Procedure) {
			// Get this FunCallExpr's Decl
			Procedure procedureSym = (Procedure)funSym;
			FunDecl funDecl = procedureSym.decl;

			// Check the params match the arguments.
			// Size
			if (fce.exprs.size() != funDecl.params.size()) {
				error("Called Function [" + fce.ident + "] with incorrect number of paramaters. Expects " + funDecl.params.size() + " but received " + fce.exprs.size());
				return null;
			}
			// Individual
			for (int i=0; i < fce.exprs.size(); i++) {
				Type argType   = fce.exprs.get(i).accept(this);
				Type paramType = funDecl.params.get(i).type.accept(this);
				if (!(argType.getClass().equals(paramType.getClass()))) {
					error("Parameter " + i + " of FunCall[" + fce.ident + "] is not the correct type. Expected [" + paramType + "] but received [" + argType + "]");
					return null;
				}
			}
			return funDecl.type.accept(this);
		}
		else {
			error("Attempted to treat identifier [" + fce.ident + "] as a function.");
			return null;
		}
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
		return new ArrayType(BaseType.CHAR, Integer.toString(sl.val.length() + 1)).accept(this);
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
		return at.arrayType.accept(this);
	}

    @Override
    public Type visitOp(Op o) {
        return null;
	}
	
	@Override
	public Type visitBaseType(BaseType bt) {
		return bt;
	}

	@Override
	public Type visitStructType(StructType st) {
		return st;
	}

	@Override
	public Type visitPointerType(PointerType pt) {
		return pt.type;
	}

}
