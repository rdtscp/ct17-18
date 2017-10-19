package ast;

import java.util.List;

public class ArrayAccessExpr extends Expr {

    public final Expr array;
    public final Expr index;

    public ArrayAccessExpr(Expr array, Expr index) {
        this.array = array;
        this.index = index;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitArrayAccessExpr(this);
    }

}