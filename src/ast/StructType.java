package ast;

public class StructType implements Type {

    public final String identifier;

    public StructType(String identifier) {
        this.identifier = identifier;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitStructType(this);
    }

}