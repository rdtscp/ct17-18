package sem;

import ast.VarDecl;
import ast.Type;

public class Variable extends Symbol {
	
	public VarDecl decl;
	public String name;
	public Type type;

	public Variable(VarDecl vd, String name) {
		super(vd, name);
		this.decl = vd;
		this.name = name;
	}
}
