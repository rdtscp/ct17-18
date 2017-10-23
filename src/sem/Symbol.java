package sem;

import ast.*;

public abstract class Symbol {
	
	ASTNode decl;
	public String name;
	
	public Symbol(ASTNode decl, String name) {
		this.name = name;
		this.decl = decl;
	}
}
