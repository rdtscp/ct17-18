package ast;

public class SizeOfExpr extends Expr {

    public final Type type;

    public SizeOfExpr(Type type) {
        this.type = type;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitSizeOfExpr(this);
    }

}