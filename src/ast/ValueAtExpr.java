package ast;
import java.util.List;

public class ValueAtExpr extends Expr {

    public final Expr expr;

    public ValueAtExpr(Expr expr) {
        this.expr = expr;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitValueAtExpr(this);
    }

}