package ast;

import java.util.List;

public class Block extends Stmt {

    public final List<VarDecl> varDecls;
    public final List<Stmt> stmts;

    public Block(List<VarDecl> varDecls, List<Stmt> stmts) {
        this.varDecls = varDecls;
        this.stmts    = stmts;
    }

    public <T> T accept(ASTVisitor<T> v) {
	    return v.visitBlock(this);
    }
}
