package sem;

import ast.FunDecl;

public class Procedure extends Symbol {
	
	public FunDecl decl;
	public String name;
	
	public Procedure(FunDecl decl, String name) {
        super(decl, name);
		this.name = name;
		this.decl = decl;
	}
}
