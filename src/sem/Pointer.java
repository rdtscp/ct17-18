package sem;

import ast.VarDecl;

public class Pointer extends Variable {

    public VarDecl decl;    
    public String name;
	
	public Pointer(VarDecl vd, String name) {
        super(vd, name);
        this.decl = vd;
        this.name = name;
	}
}
