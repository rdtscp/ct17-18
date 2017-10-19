package ast;

public interface ASTVisitor<T> {
    public T visitBaseType(BaseType bt);
    public T visitStructTypeDecl(StructTypeDecl st);
    public T visitBlock(Block b);
    public T visitFunDecl(FunDecl p);
    public T visitProgram(Program p);
    public T visitVarDecl(VarDecl vd);
    public T visitVarExpr(VarExpr v);
    public T visitStructType(StructType st);
    public T visitPointerType(PointerType pt);
    public T visitArrayType(ArrayType at);
    public T visitWhile(While w);
    public T visitIf(If i);
    public T visitAssign(Assign a);
    public T visitReturn(Return r);
    public T visitExprStmt(ExprStmt es);
    public T visitIntLiteral(IntLiteral il);
    public T visitStrLiteral(StrLiteral sl);
    public T visitChrLiteral(ChrLiteral cl);
    public T visitArrayAccessExpr(ArrayAccessExpr aae);
    public T visitBinOp(BinOp bo);
    public T visitOp(Op o);
    public T visitFieldAccessExpr(FieldAccessExpr fae);
    public T visitFunCallExpr(FunCallExpr fce);
    public T visitSizeOfExpr(SizeOfExpr soe);
    public T visitTypecastExpr(TypecastExpr te);
    public T visitValueAtExpr(ValueAtExpr vae);

    // to complete ... (should have one visit method for each concrete AST node class)
}
