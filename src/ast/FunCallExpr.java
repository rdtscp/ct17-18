package ast;

import java.util.List;

public class FunCallExpr extends Expr {

    public FunDecl fd; // to be filled in by the name analyser

    public final String ident;
    public final List<Expr> exprs;
    
    public FunCallExpr(String ident, List<Expr> exprs){
        this.ident = ident;
        this.exprs = exprs;
    }

    public <T> T accept(ASTVisitor<T> v) {
	    return v.visitFunCallExpr(this);
    }
}
