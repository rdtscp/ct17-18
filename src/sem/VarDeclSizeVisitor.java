package sem;

import ast.*;
import java.util.HashMap;
import java.util.ArrayList;

public class VarDeclSizeVisitor extends BaseSemanticVisitor<Integer> {

    HashMap<String, Integer> structTypeSizes = new HashMap<String, Integer>();
    
    FunDecl currFun;

	@Override
	public Integer visitProgram(Program p) {
        for (StructTypeDecl std: p.structTypeDecls) {
            StructType sType = std.structType;
            Integer currStructSize = std.accept(this);
            structTypeSizes.put(sType.identifier, currStructSize);
        }
        for (VarDecl vd: p.varDecls) {
            Integer varSize = vd.accept(this);
            vd.num_bytes = varSize;
        }
        for (FunDecl fd: p.funDecls) {
            fd.accept(this);
        }
		return null;
    }
    
    @Override
	public Integer visitFunDecl(FunDecl fd) {
        currFun = fd;
        for (VarDecl vd: fd.params) {
            Integer varSize = vd.accept(this);
            vd.num_bytes = varSize;
        }
        fd.block.accept(this);
        currFun = null;
		return null;
	}

	@Override
	public Integer visitVarDecl(VarDecl vd) {
        vd.parentFunc = currFun;
        Integer varSize = vd.type.accept(this);
        while (varSize%4 != 0) varSize++;
        return varSize;
    }

    @Override
	public Integer visitBlock(Block b) {
        for (VarDecl vd: b.varDecls) {
            Integer varSize = vd.accept(this);
            vd.num_bytes = varSize;
        }
        for (Stmt s: b.stmts) {
            s.accept(this);
        }
		return null;
    }
    
    @Override
    public Integer visitFunCallExpr(FunCallExpr fce) {
        // FunDecl temp = currFun;
        // currFun = fce.fd;
        // for (Expr params: fce.exprs) {
        //     exprs.accept(this);
        // }
        // currFun = temp;
		return null;
	}
    
    @Override
	public Integer visitStructTypeDecl(StructTypeDecl std) {
        String structType = std.structType.identifier;
        Integer structSize = 0;
        Integer compactSize = 0;
        for (VarDecl vd: std.varDecls) {
            Integer currField = vd.type.accept(this);
            compactSize+=currField;
            
            while (currField%4 != 0) currField++;
            structSize += currField;
        }
        std.compactSize = compactSize;
        std.allocSize   = structSize;
        structTypeSizes.put(structType, structSize);
		return structSize;
    }

    @Override
	public Integer visitBaseType(BaseType bt) {
        if (bt == BaseType.INT) {
            return 4;
        }
        if (bt == BaseType.CHAR) {
            return 1;
        }
		return null;
	}

	@Override
	public Integer visitStructType(StructType st) {
        return structTypeSizes.get(st.identifier);
	}

	@Override
	public Integer visitPointerType(PointerType pt) {
		return 4;
	}

    @Override
	public Integer visitArrayType(ArrayType at) {
        Integer elementSize = at.arrayType.accept(this);
        Integer output = elementSize * at.size;
        while (output%4 != 0) {
            output++;
        }
		return output;
	}

    

    
    
    
    
    
	

	@Override
	public Integer visitWhile(While w) {
        w.stmt.accept(this);
		return null;
	}

	@Override
	public Integer visitIf(If i) {
        i.stmt1.accept(this);
        if (i.stmt2 != null) i.stmt2.accept(this);
		return null;
	}

	@Override
	public Integer visitReturn(Return r) {
		return null;
	}

	@Override
	public Integer visitVarExpr(VarExpr v) {
		return null;
	}

	@Override
	public Integer visitAssign(Assign a) {
		return null;
	}

	@Override
	public Integer visitExprStmt(ExprStmt es) {
		return null;
	}

	@Override
    public Integer visitArrayAccessExpr(ArrayAccessExpr aae) {
		return null;
    }

	@Override
    public Integer visitFieldAccessExpr(FieldAccessExpr fae) {
		return null;
    }

	
	
	@Override
    public Integer visitTypecastExpr(TypecastExpr te) {
		return null;
	}
	
	@Override
    public Integer visitValueAtExpr(ValueAtExpr vae) {
		return null;
    }


	@Override
    public Integer visitIntLiteral(IntLiteral il) {
        return null;
    }

    @Override
    public Integer visitStrLiteral(StrLiteral sl) {
		return null;
    }

    @Override
    public Integer visitChrLiteral(ChrLiteral cl) {
        return null;
	}

	@Override
    public Integer visitBinOp(BinOp bo) {
		return null;
    }

    @Override
    public Integer visitSizeOfExpr(SizeOfExpr soe) {
        return null;
    }    

    @Override
    public Integer visitOp(Op o) {
        return null;
	}
	
	

}
