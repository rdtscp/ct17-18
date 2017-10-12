package ast;

/**
 * @author cdubach
 */
public class StructTypeDecl implements ASTNode {


    // to be completed

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitStructTypeDecl(this);
    }

}
