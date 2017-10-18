package ast;

public class ArrayType implements Type {

    public final Type arrayType;
    public final String size;

    public ArrayType(Type arrayType, String size) {
        this.arrayType = arrayType;
        this.size = size;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitArrayType(this);
    }

}