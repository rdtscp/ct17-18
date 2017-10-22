package sem;

import ast.VarDecl;

public class Struct extends Variable {

    public VarDecl vd;
    public String name;
    public String type;
	
	public Struct(VarDecl vd, String name, String type) {
        super(vd, name);
        this.vd     = vd;
        this.name   = name;
        this.type   = type;
	}
}
