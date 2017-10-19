package ast;

public class StrLiteral extends Expr {
    public final String val;
    
    public StrLiteral(String val){
        this.val = val;
    }

    public <T> T accept(ASTVisitor<T> v) {
	    return v.visitStrLiteral(this);
    }
}