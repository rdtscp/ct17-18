package ast;

public class VarDecl implements ASTNode {
    public final Type type;
    public final String ident;
    public int num_bytes;

    public VarDecl(Type type, String ident) {
	    this.type  = type;
	    this.ident = ident;
    }

    public <T> T accept(ASTVisitor<T> v) {
	    return v.visitVarDecl(this);
    }
}
