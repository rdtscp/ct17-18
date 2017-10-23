package ast;

public class VarExpr extends Expr {

    public final String ident;
    public VarDecl vd; // to be filled in by the name analyser
    
    public VarExpr(String ident){
        this.ident = ident;
    }

    public <T> T accept(ASTVisitor<T> v) {
	    return v.visitVarExpr(this);
    }
}
