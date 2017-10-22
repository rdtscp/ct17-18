package sem;

import ast.VarDecl;

public class Struct extends Variable {

    public VarDecl vd;
    public String name;
    public StructIdent type;
	
	public Struct(VarDecl vd, StructIdent type, String name) {
        super(vd, name);
        this.vd     = vd;
        this.name   = name;
        this.type   = type;
	}
}
