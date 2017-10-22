package sem;

import ast.*;

public class Procedure extends Symbol {
	
	public FunDecl decl;
	public String name;
	public Type type;
	
	public Procedure(FunDecl decl, String name, Type type) {
        super(decl, name);
		this.name = name;
		this.type = type;
	}
}
