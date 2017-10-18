package ast;

public class Return extends Stmt {

    public final Expr expr;

    public Return(Expr expr) {
        this.expr = expr;
    }

    public <T> T accept(ASTVisitor<T> v) {
	    return v.visitReturn(this);
    }
}
