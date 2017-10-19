package ast;

public class IntLiteral extends Expr {
    public final int val;
    
    public IntLiteral(String val){
        this.val = Integer.parseInt(val);
    }

    public <T> T accept(ASTVisitor<T> v) {
	    return v.visitIntLiteral(this);
    }
}