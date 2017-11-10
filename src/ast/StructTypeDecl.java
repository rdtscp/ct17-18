package ast;

import java.util.ArrayList;

/**
 * @author cdubach
 */
public class StructTypeDecl implements ASTNode  {

    public final StructType structType;
    public final ArrayList<VarDecl> varDecls;
    public int allocSize;
    public int compactSize;

    public StructTypeDecl(StructType structType, ArrayList<VarDecl> varDecls) {
        this.structType = structType;
        this.varDecls   = varDecls;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitStructTypeDecl(this);
    }

}
