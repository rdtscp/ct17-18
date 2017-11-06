package ast;

public class ChrLiteral extends Expr {
    public final String val;
    
    public ChrLiteral(String val){
        // System.out.println("New char literal: " + val);
        this.val = val;
    }

    public <T> T accept(ASTVisitor<T> v) {
	    return v.visitChrLiteral(this);
    }
}