package sem;

import ast.VarDecl;

public class Array extends Variable {

    public VarDecl decl;    
    public String name;
	
	public Array(VarDecl vd, String name) {
        super(vd, name);
        this.decl = vd;
        this.name = name;
	}
}
