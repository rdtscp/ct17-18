package sem;

import ast.VarDecl;
import java.util.List;

public class StructIdent {

    String typeIdent;
    List<VarDecl> fields;

    public StructIdent(String typeIdent, List<VarDecl> fields) {
        this.typeIdent = typeIdent;
        this.fields    = fields;
    }

}
