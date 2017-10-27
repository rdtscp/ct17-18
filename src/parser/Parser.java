package parser;

import ast.*;
import lexer.Token;
import lexer.Tokeniser;
import lexer.Token.TokenClass;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;


/**
 * @author cdubach
 */
public class Parser {

    private Token token;

    // use for backtracking (useful for distinguishing decls from procs when parsing a program for instance)
    private Queue<Token> buffer = new LinkedList<>();

    private final Tokeniser tokeniser;



    public Parser(Tokeniser tokeniser) {
        this.tokeniser = tokeniser;
    }

    public Program parse() {
        // get the first token
        nextToken();

        return parseProgram();
    }

    public int getErrorCount() {
        return error;
    }

    private int error = 0;
    private Token lastErrorToken;

    private void error(TokenClass... expected) {

        if (lastErrorToken == token) {
            // skip this error, same token causing trouble
            return;
        }

        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (TokenClass e : expected) {
            sb.append(sep);
            sb.append(e);
            sep = "|";
        }
        System.out.println("Parsing error: expected ("+sb+") found ("+token+") at "+token.position);
        
        error++;
        lastErrorToken = token;
    }

    /*
     * Look ahead the i^th element from the stream of token.
     * i should be >= 1
     */
    private Token lookAhead(int i) {
        // ensures the buffer has the element we want to look ahead
        while (buffer.size() < i)
            buffer.add(tokeniser.nextToken());
        assert buffer.size() >= i;

        int cnt=1;
        for (Token t : buffer) {
            if (cnt == i)
                return t;
            cnt++;
        }

        assert false; // should never reach this
        return null;
    }


    /*
     * Consumes the next token from the tokeniser or the buffer if not empty.
     */
    private void nextToken() {
        if (!buffer.isEmpty())
            token = buffer.remove();
        else
            token = tokeniser.nextToken();
    }

    /*
     * If the current token is equals to the expected one, then skip it, otherwise report an error.
     * Returns the expected token or null if an error occurred.
     */
    private Token expect(TokenClass... expected) {
        for (TokenClass e : expected) {
            if (e == token.tokenClass) {
                Token cur = token;
                nextToken();
                return cur;
            }
        }

        error(expected);
        return token;
    }

    /*
     * Returns true if the current token is equals to any of the expected ones.
     */
    private boolean accept(TokenClass... expected) {
        boolean result = false;
        for (TokenClass e : expected)
            result |= (e == token.tokenClass);
        return result;
    }

    // program -> (include)* (structdecl)* (vardecl)* (fundecl)* EOF
    private Program parseProgram() {
        parseIncludes();
        List<StructTypeDecl> stds = parseStructDecls();
        List<VarDecl> vds = parseVarDecls();
        List<FunDecl> fds = parseFunDecls();
        expect(TokenClass.EOF);
        return new Program(stds, vds, fds);
    }

    // Parses (include)*
    // include -> INCLUDE STRING_LITERAL
    private void parseIncludes() {
        if (accept(TokenClass.INCLUDE)) {
            expect(TokenClass.INCLUDE);
            expect(TokenClass.STRING_LITERAL);
            parseIncludes();
        }
    }

    // Parses (structdecl)*
    // structdecl -> structtype LBRA (vardecl)+ RBRA SC
    private List<StructTypeDecl> parseStructDecls() {
        ArrayList<StructTypeDecl> output = new ArrayList<StructTypeDecl>();

        // Check for struct being used as a vardecl.
        if (lookAhead(2).tokenClass != TokenClass.LBRA) return output;

        // Parse the struct.
        if (accept(TokenClass.STRUCT)) {

            // Define the struct info.
            ArrayList<VarDecl> varDecls = new ArrayList<VarDecl>();

            expect(TokenClass.STRUCT);
            String structName = expect(TokenClass.IDENTIFIER).data;
            expect(TokenClass.LBRA);

            // Add all VarDecl's to our varDecl List.
            varDecls.add(expectVarDecl());
            varDecls.addAll(parseVarDecls());
            
            expect(TokenClass.RBRA);
            expect(TokenClass.SC);

            // Add this StructDecl to our output.
            output.add(new StructTypeDecl(new StructType(structName), varDecls));

            // Try to parse more StructDecl's to add to our output.
            output.addAll(parseStructDecls());
        }
        return output;
    }

    // Expects a vardecl
    // vardecl -> type IDENT SC
    //         -> type IDENT LSBR INT_LITERAL RSBR SC
    private VarDecl expectVarDecl() {
        Type type;
        String varName;
        type = expectType();
        varName = expect(TokenClass.IDENTIFIER).data;
        if (accept(TokenClass.LSBR)) {
            expect(TokenClass.LSBR);
            expect(TokenClass.INT_LITERAL);
            expect(TokenClass.RSBR);
        }
        expect(TokenClass.SC);
        return new VarDecl(type, varName);
    }

    // Parses: (vardecl)*
    // vardecl -> type IDENT SC
    //         -> type IDENT LSBR INT_LITERAL RSBR SC
    private List<VarDecl> parseVarDecls() {
        // Declare the output.
        List<VarDecl> output = new ArrayList<VarDecl>();

        // Check if this will be an invalid vardecl.
        TokenClass twoAhead   = lookAhead(2).tokenClass;
        TokenClass threeAhead = lookAhead(3).tokenClass;
        TokenClass fourAhead =  lookAhead(4).tokenClass;
        if (twoAhead != TokenClass.SC) {
            if (twoAhead != TokenClass.LSBR) {
                if (threeAhead != TokenClass.SC) {
                    if (threeAhead != TokenClass.LSBR) {
                        if (fourAhead != TokenClass.SC) {
                            if (fourAhead != TokenClass.LSBR) {
                                return output;
                            }
                        }
                    }
                }
            }
        }
        if (accept(TokenClass.STRUCT, TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            if (accept(TokenClass.STRUCT)) {
                String structType;
                String varName;
                Type varType;

                expect(TokenClass.STRUCT);
                structType = expect(TokenClass.IDENTIFIER).data;
                varType = new StructType(structType);

                // Check if this is a pointer.
                if (accept(TokenClass.ASTERIX)) {
                    expect(TokenClass.ASTERIX);
                    varType = new PointerType(varType);
                }
                varName = expect(TokenClass.IDENTIFIER).data;

                // Check for array declaration.
                if (accept(TokenClass.LSBR)) {
                    expect(TokenClass.LSBR);
                    String arraySize = expect(TokenClass.INT_LITERAL).data;
                    expect(TokenClass.RSBR);
                    varType = new ArrayType(varType, arraySize);
                }
                expect(TokenClass.SC);

                output.add(new VarDecl(varType, varName));
            }
            else if(accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
                String varName;
                Type varType;

                varType = tokenToType(expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID).tokenClass);

                if (accept(TokenClass.ASTERIX)) {
                    expect(TokenClass.ASTERIX);
                    varType = new PointerType(varType);
                }

                if (accept(TokenClass.ASTERIX)) {
                    expect(TokenClass.ASTERIX);
                    varType = new PointerType(varType);
                }
                varName = expect(TokenClass.IDENTIFIER).data;

                // Check for array declaration.
                if (accept(TokenClass.LSBR)) {
                    expect(TokenClass.LSBR);
                    String arraySize = expect(TokenClass.INT_LITERAL).data;
                    expect(TokenClass.RSBR);
                    varType = new ArrayType(varType, arraySize);
                }
                expect(TokenClass.SC);

                output.add(new VarDecl(varType, varName));
            }
            
            // Try to parse more vardecl
            output.addAll(parseVarDecls());
        }
        return output;
    }

    // Parses: (fundecl)*
    // fundecl -> type IDENT LPAR params RPAR LBRA (vardecl)* (stmt)* RBRA
    private List<FunDecl> parseFunDecls() {
        List<FunDecl> output = new ArrayList<FunDecl>();

        // Check if this will be an invalid fundecl.
        TokenClass twoAhead   = lookAhead(2).tokenClass;
        TokenClass threeAhead = lookAhead(3).tokenClass;
        TokenClass fourAhead  = lookAhead(4).tokenClass;
        if (twoAhead != TokenClass.LPAR) {
            if (threeAhead != TokenClass.LPAR) {
                if (fourAhead != TokenClass.LPAR) {
                    return output;
                }
            }
        }
        if (accept(TokenClass.STRUCT, TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            Type funType;
            String funName;
            List<VarDecl> funArgs;

            Block funBlock;
            List<VarDecl> blockVars;
            List<Stmt> blockStmts;

            if (accept(TokenClass.STRUCT)) {
                String structType;
                expect(TokenClass.STRUCT);
                structType = expect(TokenClass.IDENTIFIER).data;
                funType = new StructType(structType);
                if (accept(TokenClass.ASTERIX)) {
                    expect(TokenClass.ASTERIX);
                    funType = new PointerType(funType);
                }
                funName = expect(TokenClass.IDENTIFIER).data;
                expect(TokenClass.LPAR);
                funArgs = expectParams();
                expect(TokenClass.RPAR);
                expect(TokenClass.LBRA);

                blockVars = parseVarDecls();
                blockStmts = parseStmts();
                funBlock = new Block(blockVars, blockStmts);

                expect(TokenClass.RBRA);
                
                output.add(new FunDecl(funType, funName, funArgs, funBlock));
            }
            else if (accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
                funType = tokenToType(expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID).tokenClass);
                if (accept(TokenClass.ASTERIX)) {
                    expect(TokenClass.ASTERIX);
                    funType = new PointerType(funType);
                }

                funName = expect(TokenClass.IDENTIFIER).data;
                expect(TokenClass.LPAR);
                funArgs = expectParams();
                expect(TokenClass.RPAR);
                expect(TokenClass.LBRA);

                blockVars = parseVarDecls();
                blockStmts = parseStmts();
                funBlock = new Block(blockVars, blockStmts);

                expect(TokenClass.RBRA);
                
                output.add(new FunDecl(funType, funName, funArgs, funBlock));
            }
            
            // Try to parse more fundecl
            output.addAll(parseFunDecls());
        }
        return output;
    }


    // Expects a type
    // type    -> ( INT | CHAR | VOID | structtype) [ASTERIX]
    private Type expectType() {
        Type output = null;
        if (accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            output = tokenToType(expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID).tokenClass);
            if (accept(TokenClass.ASTERIX)) {
                expect(TokenClass.ASTERIX);
                return new PointerType(output);
            }
            return output;
        }
        else if (accept(TokenClass.STRUCT)) {
            expect(TokenClass.STRUCT);
            output = new StructType(expect(TokenClass.IDENTIFIER).data);
            if (accept(TokenClass.ASTERIX)) {
                expect(TokenClass.ASTERIX);
                return new PointerType(output);
            }
            return output;
        }
        else {
            error(token.tokenClass);
            return null;
        }
    }

    // Expects params
    // params  -> [ type IDENT (COMMA type IDENT)* ]
    private List<VarDecl> expectParams() {
        ArrayList<VarDecl> output = new ArrayList<VarDecl>();
        // Check for vardecl && fundecl.
        TokenClass twoAhead   = lookAhead(2).tokenClass;
        TokenClass threeAhead = lookAhead(3).tokenClass;
        if (twoAhead != TokenClass.RPAR) {
            if (twoAhead != TokenClass.COMMA) {
                if (threeAhead != TokenClass.RPAR) {
                    if (threeAhead != TokenClass.COMMA) {
                        return output;
                    }
                }
            }
        }
        if (accept(TokenClass.STRUCT, TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            if (accept(TokenClass.STRUCT)) {
                expect(TokenClass.STRUCT);
                Type paramType = new StructType(expect(TokenClass.IDENTIFIER).data);

                if (accept(TokenClass.ASTERIX)) {
                    expect(TokenClass.ASTERIX);
                    paramType = new PointerType(paramType);
                }
                String paramName = expect(TokenClass.IDENTIFIER).data;

                output.add(new VarDecl(paramType, paramName));
            }
            else if (accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
                Type paramType = tokenToType(expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID).tokenClass);
                String paramName = expect(TokenClass.IDENTIFIER).data;

                output.add(new VarDecl(paramType, paramName));                
            }
            
            while (accept(TokenClass.COMMA)) {
                expect(TokenClass.COMMA);
                Type paramType = expectType();
                String paramName = expect(TokenClass.IDENTIFIER).data;

                output.add(new VarDecl(paramType, paramName));                
            }
        }
        return output;
    }

    // Expects stmt
    // stmt    -> LBRA (vardecl)* (stmt)* RBRA
    //         -> WHILE LPAR exp RPAR stmt
    //         -> IF LPAR exp RPAR stmt [ELSE stmt]
    //         -> RETURN [exp] SC
    //         -> exp ASSIGN exp SC
    //         -> exp SC
    private Stmt expectStmt() {
        // Block
        if (accept(TokenClass.LBRA)) {
            expect(TokenClass.LBRA);
            List<VarDecl> blockVars  = parseVarDecls();
            List<Stmt> blockStmts = parseStmts();
            expect(TokenClass.RBRA);

            return new Block(blockVars, blockStmts);
        }
        // While
        else if (accept(TokenClass.WHILE)) {
            expect(TokenClass.WHILE);
            expect(TokenClass.LPAR);
            Expr expr = expectExp();
            expect(TokenClass.RPAR);
            Stmt stmt = expectStmt();

            return new While(expr, stmt);
        }
        // If
        else if (accept(TokenClass.IF)) {
            expect(TokenClass.IF);
            expect(TokenClass.LPAR);
            Expr expr = expectExp();
            expect(TokenClass.RPAR);
            Stmt stmt1 = expectStmt();
            if (accept(TokenClass.ELSE)) {
                expect(TokenClass.ELSE);
                Stmt stmt2 = expectStmt();
                return new If(expr, stmt1, stmt2);
            }
            else {
                return new If(expr, stmt1, null);
            }
        }
        // Return
        else if (accept(TokenClass.RETURN)) {
            Expr expr = null;
            expect(TokenClass.RETURN);
            // If we can accept any of the starting Tokens of an exp
            if (accept(TokenClass.LPAR, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.IDENTIFIER, TokenClass.INT_LITERAL, TokenClass.MINUS, TokenClass.ASTERIX, TokenClass.SIZEOF)) {
                expr = expectExp();
            }
            expect(TokenClass.SC);
            return new Return(expr);
        }
        // Assign || ExprStmt
        else if (accept(TokenClass.LPAR, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.IDENTIFIER, TokenClass.INT_LITERAL, TokenClass.MINUS, TokenClass.ASTERIX, TokenClass.SIZEOF)) {
            Expr expr1 = expectExp();
            // Assign
            if (accept(TokenClass.ASSIGN)) {
                expect(TokenClass.ASSIGN);
                Expr expr2 = expectExp();
                expect(TokenClass.SC);
                return new Assign(expr1, expr2);
            }
            // ExprStmt
            else {
                expect(TokenClass.SC);
                return new ExprStmt(expr1);
            } 
        }
        // Error; no Stmt.
        else {
            System.out.println("Error: Expected a stmt");
            error(TokenClass.LBRA, TokenClass.WHILE, TokenClass.IF, TokenClass.RETURN, TokenClass.LPAR, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.MINUS, TokenClass.IDENTIFIER, TokenClass.ASTERIX, TokenClass.SIZEOF);
            return null;
        }
    }

    // Parses: (stmt)*
    // stmt    -> LBRA (vardecl)* (stmt)* RBRA
    //         -> WHILE LPAR exp RPAR stmt
    //         -> IF LPAR exp RPAR stmt [ELSE stmt]
    //         -> RETURN [exp] SC
    //         -> exp ASSIGN exp SC
    //         -> exp SC
    private List<Stmt> parseStmts() {
        ArrayList<Stmt> output = new ArrayList<Stmt>();
        // Block
        if (accept(TokenClass.LBRA)) {
            ArrayList<VarDecl> blockVars = new ArrayList<VarDecl>();
            ArrayList<Stmt> blockStmts   = new ArrayList<Stmt>();

            expect(TokenClass.LBRA);
            blockVars.addAll(parseVarDecls());
            blockStmts.addAll(parseStmts());
            expect(TokenClass.RBRA);
            
            output.add(new Block(blockVars, blockStmts));
            output.addAll(parseStmts());
        }
        // While
        else if (accept(TokenClass.WHILE)) {
            Expr expr;
            Stmt stmt;

            expect(TokenClass.WHILE);
            expect(TokenClass.LPAR);
            expr = expectExp();
            expect(TokenClass.RPAR);
            stmt = expectStmt();

            output.add(new While(expr, stmt));
            output.addAll(parseStmts());
        }
        // If
        else if (accept(TokenClass.IF)) {
            expect(TokenClass.IF);
            expect(TokenClass.LPAR);
            Expr expr = expectExp();
            expect(TokenClass.RPAR);
            Stmt stmt1 = expectStmt();
            if (accept(TokenClass.ELSE)) {
                expect(TokenClass.ELSE);
                Stmt stmt2 = expectStmt();
                output.add(new If(expr, stmt1, stmt2));
            }
            else {
                output.add(new If(expr, stmt1, null));
            }
            output.addAll(parseStmts());
        }
        // Return
        else if (accept(TokenClass.RETURN)) {
            Expr expr = null;

            expect(TokenClass.RETURN);
            // If we can accept any of the starting Tokens of an exp
            if (accept(TokenClass.LPAR, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.IDENTIFIER, TokenClass.INT_LITERAL, TokenClass.MINUS, TokenClass.ASTERIX, TokenClass.SIZEOF)) {
                expr = expectExp();
            }
            expect(TokenClass.SC);

            output.add(new Return(expr));
            output.addAll(parseStmts());
        }
        // Assign || ExprStmt
        else if (accept(TokenClass.LPAR, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.IDENTIFIER, TokenClass.INT_LITERAL, TokenClass.MINUS, TokenClass.ASTERIX, TokenClass.SIZEOF)) {
            Expr expr1 = expectExp();
            // Assign
            if (accept(TokenClass.ASSIGN)) {
                expect(TokenClass.ASSIGN);
                Expr expr2 = expectExp();
                expect(TokenClass.SC);
                Assign assign = new Assign(expr1, expr2);
                output.add(assign);
                output.addAll(parseStmts());
            }
            // ExprStmt
            else {
                expect(TokenClass.SC);
                output.add(new ExprStmt(expr1));
                output.addAll(parseStmts());
            }
        }
        return output;
    }

    // exp     -> exp2 [OR exp]
    private Expr expectExp() {
        Expr exp2 = expectExp2();
        while (accept(TokenClass.OR)) {
            expect(TokenClass.OR);
            Expr exp = expectExp2();
            exp2 = new BinOp(exp2, Op.OR, exp);
        }
        if (accept(TokenClass.DOT, TokenClass.LSBR)) {
            return expectPostExp(exp2);
        }
        return exp2;
    }

    // exp2    -> exp3 (AND exp)*
    private Expr expectExp2() {
        Expr exp3 = expectExp3();
        while(accept(TokenClass.AND)) {
            expect(TokenClass.AND);
            Expr exp = expectExp3();
            exp3 = new BinOp(exp3, Op.AND, exp);
        }
        if (accept(TokenClass.DOT, TokenClass.LSBR)) {
            return expectPostExp(exp3);
        }
        return exp3;
    }

    // exp3    -> exp4 ((EQ | NE) exp)*
    private Expr expectExp3() {
        Expr exp4 = expectExp4();
        while (accept(TokenClass.EQ, TokenClass.NE)) {
            if (accept(TokenClass.EQ)) {
                expect(TokenClass.EQ);
                Expr exp = expectExp4();
                exp4 = new BinOp(exp4, Op.EQ, exp);
            }
            else if (accept(TokenClass.NE)) {
                expect(TokenClass.NE);
                Expr exp = expectExp4();
                exp4 = new BinOp(exp4, Op.NE, exp);
            }
        }
        if (accept(TokenClass.DOT, TokenClass.LSBR)) {
            return expectPostExp(exp4);
        }
        return exp4;
    }

    // exp4    -> exp5 ((LT | LE | GT | GE) exp)*
    private Expr expectExp4() {
        Expr exp5 = expectExp5();
        while (accept(TokenClass.LT, TokenClass.LE, TokenClass.GT, TokenClass.GE)) {
            if (accept(TokenClass.LT)) {
                expect(TokenClass.LT);
                Expr exp = expectExp5();
                exp5 = new BinOp(exp5, Op.LT, exp);
            }
            else if (accept(TokenClass.LE)) {
                expect(TokenClass.LE);
                Expr exp = expectExp5();
                exp5 = new BinOp(exp5, Op.LE, exp);
            }
            else if (accept(TokenClass.GT)) {
                expect(TokenClass.GT);
                Expr exp = expectExp5();
                exp5 = new BinOp(exp5, Op.GT, exp);
            }
            else if (accept(TokenClass.GE)) {
                expect(TokenClass.GE);
                Expr exp = expectExp5();
                exp5 = new BinOp(exp5, Op.GE, exp);
            }
        }
        if (accept(TokenClass.DOT, TokenClass.LSBR)) {
            return expectPostExp(exp5);
        }
        return exp5;
    }

    // exp5    -> exp6 ((PLUS | MINUS) exp)*
    private Expr expectExp5() {
        Expr exp6 = expectExp6();
        while (accept(TokenClass.PLUS, TokenClass.MINUS)) {
            if (accept(TokenClass.PLUS)) {
                expect(TokenClass.PLUS);
                Expr exp = expectExp6();
                exp6 = new BinOp(exp6, Op.ADD, exp);
            }
            else if (accept(TokenClass.MINUS)) {
                expect(TokenClass.MINUS);
                Expr exp = expectExp6();
                exp6 = new BinOp(exp6, Op.SUB, exp);
            }
        }
        if (accept(TokenClass.DOT, TokenClass.LSBR)) {
            return expectPostExp(exp6);
        }
        return exp6;
    }

    // exp6    -> exp7 ((ASTERIX | DIV | REM) exp)*
    private Expr expectExp6() {
        Expr exp7 = expectExp7();
        while (accept(TokenClass.ASTERIX, TokenClass.DIV, TokenClass.REM)) {
            if (accept(TokenClass.ASTERIX)) {
                expect(TokenClass.ASTERIX);
                Expr exp = expectExp7();
                exp7 = new BinOp(exp7, Op.MUL, exp);
            }
            else if (accept(TokenClass.DIV)) {
                expect(TokenClass.DIV);
                Expr exp = expectExp7();
                exp7 = new BinOp(exp7, Op.DIV, exp);
            }
            else if (accept(TokenClass.REM)) {
                expect(TokenClass.REM);
                Expr exp = expectExp7();
                exp7 = new BinOp(exp7, Op.MOD, exp);
            }
        }
        return exp7;
    }

    // exp7    -> MINUS exp
    //         -> LPAR type RPAR exp
    //         -> ASTERIX exp
    //         -> SIZEOF LPAR type RPAR
    //         -> exp8
    private Expr expectExp7() {
        if (accept(TokenClass.MINUS)) {
            expect(TokenClass.MINUS);
            Expr exp = expectExp8();
            if (accept(TokenClass.DOT, TokenClass.LSBR)) {
                return expectPostExp(new BinOp(new IntLiteral("0"), Op.SUB, exp));
            }
            return new BinOp(new IntLiteral("0"), Op.SUB, exp);
        }
        else if (accept(TokenClass.LPAR)) {
            TokenClass oneAhead = lookAhead(1).tokenClass;
            if (oneAhead == TokenClass.INT || oneAhead == TokenClass.CHAR || oneAhead == TokenClass.VOID || oneAhead == TokenClass.STRUCT) {
                expect(TokenClass.LPAR);
                Type type = expectType();
                expect(TokenClass.RPAR);
                Expr exp = expectExp8();
                if (accept(TokenClass.DOT, TokenClass.LSBR)) {
                    return expectPostExp(new TypecastExpr(type, exp));
                }
                return new TypecastExpr(type, exp);
            }
            else {
                return expectExp8();
            }
        }
        else if (accept(TokenClass.ASTERIX)) {
            expect(TokenClass.ASTERIX);
            Expr exp = expectExp8();
            if (accept(TokenClass.DOT, TokenClass.LSBR)) {
                return expectPostExp(new ValueAtExpr(exp));
            }
            return new ValueAtExpr(exp);
        }
        else if (accept(TokenClass.SIZEOF)) {
            expect(TokenClass.SIZEOF);
            expect(TokenClass.LPAR);
            Type type = expectType();
            expect(TokenClass.RPAR);
            if (accept(TokenClass.DOT, TokenClass.LSBR)) {
                return expectPostExp(new SizeOfExpr(type));
            }
            return new SizeOfExpr(type);
        }
        else {
            return expectExp8();
        }
    }

    // exp8    -> IDENT [pIdent] [pExp]
    //         -> INT_LITERAL [pExp]
    //         -> CHAR_LITERAL [pExp]
    //         -> STRING_LITERAL [pExp]
    //         -> LPAR exp RPAR [pExp]
    //
    // pIdent  -> LPAR [ exp (COMMA exp)* ] RPAR
    private Expr expectExp8() {
        if (accept(TokenClass.IDENTIFIER)) {
            String name = expect(TokenClass.IDENTIFIER).data;
            if (accept(TokenClass.LPAR)) {
                expect(TokenClass.LPAR);
                if (accept(TokenClass.INT_LITERAL, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.IDENTIFIER, TokenClass.INT_LITERAL, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.LPAR)) {
                    ArrayList<Expr> args = new ArrayList<Expr>();
                    args.add(expectExp());
                    while (accept(TokenClass.COMMA)) {
                        expect(TokenClass.COMMA);
                        args.add(expectExp());
                    }
                    expect(TokenClass.RPAR);
                    return new FunCallExpr(name, args);
                }
                else {
                    expect(TokenClass.RPAR);
                    if (accept(TokenClass.DOT, TokenClass.LSBR)) {
                        return expectPostExp(new FunCallExpr(name, new ArrayList<Expr>()));
                    }
                    return new FunCallExpr(name, new ArrayList<Expr>());
                }
            }
            else {
                if (accept(TokenClass.DOT, TokenClass.LSBR)) {
                    return expectPostExp(new VarExpr(name));
                }
                return new VarExpr(name);
            }
        }
        else if (accept(TokenClass.INT_LITERAL)) {
            String val = expect(TokenClass.INT_LITERAL).data;
            if (accept(TokenClass.DOT, TokenClass.LSBR)) {
                return expectPostExp(new IntLiteral(val));
            }
            return new IntLiteral(val);
        }
        else if (accept(TokenClass.CHAR_LITERAL)) {
            String val = expect(TokenClass.CHAR_LITERAL).data;
            if (accept(TokenClass.DOT, TokenClass.LSBR)) {
                return expectPostExp(new ChrLiteral(val.charAt(0)));
            }
            return new ChrLiteral(val.charAt(0));
        }
        else if (accept(TokenClass.STRING_LITERAL)) {
            String val = expect(TokenClass.STRING_LITERAL).data;
            if (accept(TokenClass.DOT, TokenClass.LSBR)) {
                return expectPostExp(new StrLiteral(val));
            }
            return new StrLiteral(val);
        }
        else if (accept(TokenClass.LPAR)) {
            expect(TokenClass.LPAR);
            Expr exp = expectExp();
            expect(TokenClass.RPAR);
            if (accept(TokenClass.DOT, TokenClass.LSBR)) {
                return expectPostExp(exp);
            }
            return exp;
        }
        else if (accept(TokenClass.ASTERIX)) {
            return expectExp();
        }
        else if (accept(TokenClass.MINUS)) {
            return expectExp();
        }
        else {
            expect(TokenClass.IDENTIFIER, TokenClass.INT_LITERAL, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.LPAR);
            return null;
        }
    }

    // pExp    -> LSBR exp RSBR
    //         -> DOT IDENT
    private Expr expectPostExp(Expr expr) {
        if (accept(TokenClass.LSBR)) {
            expect(TokenClass.LSBR);
            Expr expr2 = expectExp();
            expect(TokenClass.RSBR);
            return new ArrayAccessExpr(expr, expr2);
        }
        else {
            expect(TokenClass.DOT);
            String field = expect(TokenClass.IDENTIFIER).data;
            return new FieldAccessExpr(expr, field);
        }
    }

    /*****************************************\
                       HELPERS
    \*****************************************/

    /* Converts a TokenClass enum to a BaseType enum */
    private Type tokenToType(TokenClass input) {
        Type output;
        switch (input) {
            case INT:
                output = BaseType.INT;
                break;
            case CHAR:
                output = BaseType.CHAR;
                break;
            case VOID:
                output = BaseType.VOID;
                break;
            default:
                output = null;
                break;
        }
        return output;
    }

}
