package sem;

import ast.*;
import java.util.HashMap;
import java.util.ArrayList;

public class NameAnalysisVisitor extends BaseSemanticVisitor<Void> {

	Scope currScope;
	HashMap<String, StructTypeDecl> structTypes;
	boolean createBlockScope = true;

	@Override
	public Void visitProgram(Program p) {
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
		for (VarDecl varDecl: p.varDecls) varDecl.accept(this);
		
		// Check Names of all FunDecls
		for (FunDecl funDecl: p.funDecls) funDecl.accept(this);
		return null;
	}

	@Override
	public Void visitStructTypeDecl(StructTypeDecl std) {
		String structTypeIdent = std.structType.identifier;
		// Check if we already have a StructIdent under this ident.
		if (structTypes.containsKey(structTypeIdent)) {
			error("Attempted to declare a Struct with an identifier that is already in use: " + structTypeIdent);
			return null;
		}
		// Check if this identifier is already in current scope.
		if (currScope.lookupCurrent(structTypeIdent) != null) {
			error("Attempted to declare a Struct with an identifier that is already in use: " + structTypeIdent);
			return null;
		}
		// Else we can create a StructType with ident: structTypeIdent
		structTypes.put(structTypeIdent, std);

		// Create a new Scope for the StructTypeDecl, and check its VarDecls names dont clash.
		currScope = new Scope(currScope);
		for (VarDecl varDecl: std.varDecls) { varDecl.accept(this); }
		currScope = currScope.outer;
		return null;
	}

	@Override
	public Void visitVarDecl(VarDecl vd) {
		// If this identifier is free.
		if (currScope.lookupCurrent(vd.ident) == null) {
			// Add standard INT/CHAR/VOID Variable to Scope.
			if (vd.type instanceof BaseType) {
				currScope.put(new Variable(vd, vd.ident));
				return null;
			}
			// Attempting to declare a Struct Variable.
			if (vd.type instanceof StructType) {
				StructType vdStructType = (StructType)vd.type;

				// Check if a Struct of this type exists.
				if (structTypes.containsKey(vdStructType.identifier)) {
					currScope.put(new Struct(vd, vd.ident, structTypes.get(vdStructType.identifier)));
					return null;
				}
				// Error if not.
				else {
					error("Attempted to declare a Variable of type [" + vdStructType.identifier + "] which does not exist.");
					return null;
				}
			}
			// Attempting to declare an Array Variable.
			if (vd.type instanceof ArrayType) {
				ArrayType vdArrayType = (ArrayType)vd.type;

				// Add this VarDecl to the Scope.
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

		error("Attempted to declare a Variable with an identifier already in use: " + vd.ident);
		return null;
	}

	@Override
	public Void visitFunDecl(FunDecl fd) {
		// If this identifier is free.
		if (currScope.lookupCurrent(fd.name) == null) {
			// Add this identifier to our current scope.
			currScope.put(new Procedure(fd, fd.name));
			
			currScope = new Scope(currScope);
		
			// Check Params.
			for (VarDecl varDecl: fd.params) varDecl.accept(this);	
			// Check block.
			createBlockScope = false;	// Mark that visitBlock is not to create a new Scope.
			fd.block.accept(this);

			currScope = currScope.outer;
		}
		else {
			error("Attemped to declare a function with identifier that is already in use: " + fd.name);
		}
		return null;
	}

	@Override
	public Void visitBlock(Block b) {
		if (createBlockScope) {
			currScope = new Scope(currScope);
			// Go through all VarDecl's and Stmt's checking their scope.
			for (VarDecl varDecl: b.varDecls) varDecl.accept(this);
			for (Stmt stmt: b.stmts) stmt.accept(this);
			currScope = currScope.outer;
		}
		else {
			createBlockScope = true; // Reset blockScope informer.
			// Go through all VarDecl's and Stmt's checking their scope.
			for (VarDecl varDecl: b.varDecls) varDecl.accept(this);
			for (Stmt stmt: b.stmts) stmt.accept(this);
		}
		return null;
	}	

	@Override
	public Void visitWhile(While w) {
		// Check the while condition's scope validity.
		w.expr.accept(this);

		// Check the while block scope validity.
		w.stmt.accept(this);
		return null;
	}

	@Override
	public Void visitIf(If i) {
		// Check if the if condition's scope validity.
		i.expr.accept(this);

		// Check IF Block.
		i.stmt1.accept(this);

		// If an ELSE block exists, check it.
		if (i.stmt2 != null) {
			i.stmt2.accept(this);
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
		if (varDecl == null) error("Reference to variable that does not exist: " + v.ident);

		// Check the Symbol is a Variable. Note: Struct, Array, Pointer extend Variable.
		else if (!(varDecl instanceof Variable)) error("Variable referenced that does not exist: " + v.ident);
		
		// VarExpr is a Variable => Link this expr to its VarDecl
		else v.vd = (VarDecl)varDecl.decl;

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
		// FieldAccessExpr made up of VarExpr.IDENTIFIER  -  new FieldAccessExpr(new VarExpr(name), field);
		VarExpr struct = (VarExpr)fae.struct;

		// Get the Variable for this structVar.
		Symbol varSym = currScope.lookup(struct.ident);

		// If no such structVar exists, throw error.
		if (varSym == null) {
			error("Attempted to access field of a variable that does not exist: " + struct.ident);
			return null;
		}
		// Check that the existing item is a Struct.
		else if (varSym instanceof Struct) {
			Struct structSym  = (Struct)varSym; 
			StructTypeDecl std = structSym.std;

			// Check if this field exists in the StructTypeDecl.
			for (VarDecl field: std.varDecls) {
				if (field.ident.equals(fae.field)) return null;
			}

			// Reached here means that the field did not exist in the StructTypeDecl.
			error("Field in FieldAccessExpr did not exist for the variable: " + struct.ident + "." + fae.field);
			return null;
		}
		// Variable referenced is not a Struct.
		else {
			error("Attempted to access a field of a variable which is not a struct: " + struct.ident + "." + fae.field);
			return null;
		}
	}

    @Override
    public Void visitFunCallExpr(FunCallExpr fce) {
		// Get the Symbol associated with this identifier.
		Symbol funDecl = currScope.lookup(fce.ident);

		// If no Symbol exists, or the Symbol is not a Procedure.
		if (funDecl == null || !(funDecl instanceof Procedure)) {
			error("Reference to function that does not exist: " + fce.ident);
			return null;
		}
		// Identifier refers to a Procedure.
		else {
			// Check each of the params is valid in scope.
			for (Expr expr: fce.exprs) expr.accept(this);
			fce.fd = (FunDecl)funDecl.decl;
			return null;
		}
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
