package sem;

import ast.*;
import java.util.HashMap;
import java.util.ArrayList;

public class VarDeclSizeVisitor extends BaseSemanticVisitor<Integer> {

    HashMap<String, Integer> structTypeSizes = new HashMap<String, Integer>();
    

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
        for (VarDecl vd: fd.params) {
            Integer varSize = vd.accept(this);
            vd.num_bytes = varSize;
        }
		return null;
	}

	@Override
	public Integer visitVarDecl(VarDecl vd) {
        return vd.type.accept(this);
    }
    
    @Override
	public Integer visitStructTypeDecl(StructTypeDecl std) {
        String structType = std.structType.identifier;
        Integer structSize = 0;
        for (VarDecl vd: std.varDecls) {
            Integer currField = vd.accept(this);
            while (currField%4 != 0) currField++;
            structSize += currField;
        }
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
	public Integer visitBlock(Block b) {
		return null;
	}

	@Override
	public Integer visitWhile(While w) {
		return null;
	}

	@Override
	public Integer visitIf(If i) {
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
    public Integer visitFunCallExpr(FunCallExpr fce) {
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
