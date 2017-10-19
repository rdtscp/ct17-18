package ast;

public class FieldAccessExpr extends Expr {

    public final Expr struct;
    public final String field;

    public FieldAccessExpr(Expr struct, String field) {
        this.struct = struct;
        this.field = field;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitFieldAccessExpr(this);
    }

}