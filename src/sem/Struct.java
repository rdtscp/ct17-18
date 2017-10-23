package sem;

import ast.VarDecl;
import ast.StructTypeDecl;

public class Struct extends Variable {

    public VarDecl decl;
    public String name;
    public StructTypeDecl std;
	
	public Struct(VarDecl vd, String name, StructTypeDecl std) {
        super(vd, name);
        this.decl  = vd;
        this.name  = name;
        this.std   = std;
	}
}
