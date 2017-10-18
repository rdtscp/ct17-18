package ast;

public class StructType implements Type {

    public final String structType;
    public final String structName;

    public StructType(String structType, String structName) {
        this.structType = structType;
        this.structName = structName;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitStructType(this);
    }

}