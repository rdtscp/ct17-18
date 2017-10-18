package ast;

public class StructType implements Type {

    public final String structType;

    public StructType(String structType) {
        this.structType = structType;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitStructType(this);
    }

}