package ast;

public class Block extends Stmt {

    // to complete ...

    public <T> T accept(ASTVisitor<T> v) {
	    return v.visitBlock(this);
    }
}
