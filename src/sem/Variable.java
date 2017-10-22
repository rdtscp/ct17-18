package sem;

import ast.VarDecl;

public class Variable extends Symbol {
	
	public VarDecl vd;
	public String name;
	
	public Variable(VarDecl vd, String name) {
		super(vd, name);
		this.vd   = vd;
		this.name = name;
	}
}
