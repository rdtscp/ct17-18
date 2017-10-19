package ast;

public class BinOp extends Expr {

    public final Expr expr1;
    public final Op op;
    public final Expr expr2;
    
    public BinOp(Expr expr1, Op op, Expr expr2){
        this.expr1  = expr1;
        this.op     = op;
        this.expr2  = expr2;
    }

    public <T> T accept(ASTVisitor<T> v) {
	    return v.visitBinOp(this);
    }
}
