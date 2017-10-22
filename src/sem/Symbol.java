package sem;

import ast.ASTNode;

public abstract class Symbol {
	
	public String name;
	ASTNode decl;
	
	public Symbol(ASTNode decl, String name) {
		this.name = name;
		this.decl = decl;
	}
}
