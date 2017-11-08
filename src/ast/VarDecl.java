package ast;

public class VarDecl implements ASTNode {
    public final Type type;
    public final String ident;
    public int num_bytes;
    public FunDecl parentFunc;
    public int fpOffset;

    public VarDecl(Type type, String ident) {
	    this.type       = type;
        this.ident      = ident;
        this.fpOffset   = -1; // Not a param by default.
    }

    public <T> T accept(ASTVisitor<T> v) {
	    return v.visitVarDecl(this);
    }
}
