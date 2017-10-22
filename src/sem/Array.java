package sem;

import ast.VarDecl;

public class Array extends Variable {

    public VarDecl vd;    
    public String name;
    public int size;
	
	public Array(VarDecl vd, String name, int size) {
        super(vd, name);
        this.vd   = vd;
        this.name = name;
        this.size = size;
	}
}
