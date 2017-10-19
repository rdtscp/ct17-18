package ast;

import java.util.ArrayList;

/**
 * @author cdubach
 */
public class StructTypeDecl implements ASTNode {

    public final StructType structName;
    public final ArrayList<VarDecl> varDecls;

    public StructTypeDecl(StructType structName, ArrayList<VarDecl> varDecls) {
        this.structName = structName;
        this.varDecls   = varDecls;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitStructTypeDecl(this);
    }

}
