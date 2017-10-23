package sem;

import ast.*;
import java.util.HashMap;
import java.util.List;

public class NameAnalysisVisitor extends BaseSemanticVisitor<Void> {

	Scope currScope;
	HashMap<String, StructIdent> structTypes;
	boolean createBlockScope = true;

	@Override
	public Void visitProgram(Program p) {
		structTypes = new HashMap<String, StructIdent>();
		currScope = new Scope();
		for (StructTypeDecl structTypeDecl : p.structTypeDecls) structTypeDecl.accept(this);
		for (VarDecl varDecl: p.varDecls) varDecl.accept(this);
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
		// Check if this identifier is already in use elsewhere.
		if (currScope.lookupCurrent(structTypeIdent) != null) {
			error("Attempted to declare a Struct with an identifier that is already in use: " + structTypeIdent);
			return null;
		}
		// Else we can create a StructType with ident: structTypeIdent
		StructIdent newStruct = new StructIdent(structTypeIdent, std.varDecls);
		structTypes.put(std.structType.identifier, newStruct);
		return null;
	}

	@Override
	public Void visitVarDecl(VarDecl vd) {
		String varIdent = vd.ident;
		if (varIdent.equals("print_s") || varIdent.equals("print_c") || varIdent.equals("print_i") || varIdent.equals("read_c") || varIdent.equals("read_i")) {
			error("Tried to declare a Variable with identifier already in use: " + varIdent);
			return null;
		}

		// Create the VarDecl's Symbol.
		Symbol varDecl = null;
		// TYPE IDENT;
		if (vd.type instanceof BaseType) {
			varDecl = new Variable(vd, varIdent);
		}
		// struct IDENT IDENT;
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
		if (fd.name.equals("print_s") || fd.name.equals("print_c") || fd.name.equals("print_i") || fd.name.equals("read_c") || fd.name.equals("read_i")) {
			error("Tried to declare a Function with identifier already in use: " + fd.name);
			return null;
		}
		// Check if anything else exists under this identifier in the current scope.
		if (currScope.lookupCurrent(fd.name) == null) {
			// Add this identifier to our current scope.
			currScope.put(new Procedure(fd, fd.name, fd.type));
			
			// Create a Scope for this FunDecl, and check the scope of all its items.
			currScope = new Scope(currScope);
			// Check Params.
			for (VarDecl varDecl: fd.params) varDecl.accept(this);
			// Check block.
			createBlockScope = false;
			visitBlock(fd.block);
			createBlockScope = true;

			// Return to previous scope.
			currScope = currScope.outer;
		}
		else {
			error("Attemped to declare a function with identifier that is already in use: " + fd.name);
		}
		return null;
	}

	@Override
	public Void visitBlock(Block b) {
		if (createBlockScope) currScope = new Scope(currScope);
		// Go through all VarDecl's and Stmt's checking their scope.
		for (VarDecl varDecl: b.varDecls) varDecl.accept(this);
		for (Stmt stmt: b.stmts) stmt.accept(this);
		if (createBlockScope) currScope = currScope.outer;
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
		VarExpr var = (VarExpr)fae.struct;

		Symbol structDecl = currScope.lookup(var.ident);
		if (structDecl == null) {
			error("Attempted to access field of a variable that does not exist: " + var.ident);
		}
		else if (structDecl instanceof Struct) {
			for (VarDecl field: ((Struct)structDecl).type.fields) {
				if (field.ident.equals(fae.field)) {
					return null;
				}
			}
			error("Attempted to access a field of a struct which does not exist: " + var.ident + "." + fae.field);
		}
		else {
			error("Attempted to access a field of a variable which is not a struct: " + var.ident);
		}
		return null;
    }

    @Override
    public Void visitFunCallExpr(FunCallExpr fce) {
		// Hardcode imported functions as valid.
		if (fce.ident.equals("print_s") || fce.ident.equals("print_c") || fce.ident.equals("print_i") || fce.ident.equals("read_c") || fce.ident.equals("read_i")) {
			return null;
		}

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
