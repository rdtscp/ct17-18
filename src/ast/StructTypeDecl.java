package ast;

import java.util.ArrayList;

/**
 * @author cdubach
 */
public class StructTypeDecl implements ASTNode {

    public final String structName;
    public final ArrayList<VarDecl> varDecls;

    public StructTypeDecl(String structName, ArrayList<VarDecl> varDecls) {
        this.structName = structName;
        this.varDecls   = varDecls;
    }

    // to be completed

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitStructTypeDecl(this);
    }

}
