package parser;

import ast.FunDecl;
import ast.Program;
import ast.StructTypeDecl;
import ast.VarDecl;
import lexer.Token;
import lexer.Tokeniser;
import lexer.Token.TokenClass;

import java.util.LinkedList;
import java.util.List;
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
                // System.out.println("     " + cur);
                nextToken();
                return cur;
            }
        }

        error(expected);
        return null;
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
        // Check for struct being used as a vardecl.
        TokenClass twoAhead;
        twoAhead = lookAhead(2).tokenClass;
        if (twoAhead != TokenClass.LBRA) {
            return null;
        }
        if (accept(TokenClass.STRUCT)) {
            expect(TokenClass.STRUCT);
            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.LBRA);
            expectVarDecl();
            parseVarDecls();
            expect(TokenClass.RBRA);
            expect(TokenClass.SC);
            parseStructDecls();
        }
    }

    // Expects a vardecl
    // vardecl -> type IDENT SC
    //         -> type IDENT LSBR INT_LITERAL RSBR SC
    private List<VarDecl> expectVarDecl() {
        expectType();
        expect(TokenClass.IDENTIFIER);
        if (accept(TokenClass.LSBR)) {
            expect(TokenClass.LSBR);
            expect(TokenClass.INT_LITERAL);
            expect(TokenClass.RSBR);
        }
        expect(TokenClass.SC);
    }

    // Parses: (vardecl)*
    // vardecl -> type IDENT SC
    //         -> type IDENT LSBR INT_LITERAL RSBR SC
    private List<VarDecl> parseVarDecls() {
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
                            return null;
                            }
                        }
                    }
                }
            }
        }
        if (accept(TokenClass.STRUCT, TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            if (accept(TokenClass.STRUCT)) {
                expect(TokenClass.STRUCT);
                expect(TokenClass.IDENTIFIER);
            }
            else if(accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
                expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID);
            }

            if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
            expect(TokenClass.IDENTIFIER);
            // Check for array declaration.
            if (accept(TokenClass.LSBR)) {
                expect(TokenClass.LSBR);
                expect(TokenClass.INT_LITERAL);
                expect(TokenClass.RSBR);
            }
            expect(TokenClass.SC);
            // Try to parse more vardecl
            parseVarDecls();
        }
    }

    // Parses: (fundecl)*
    // fundecl -> type IDENT LPAR params RPAR LBRA (vardecl)* (stmt)* RBRA
    private List<FunDecl> parseFunDecls() {
        // Check if this will be an invalid fundecl.
        TokenClass twoAhead   = lookAhead(2).tokenClass;
        TokenClass threeAhead = lookAhead(3).tokenClass;
        TokenClass fourAhead  = lookAhead(4).tokenClass;
        if (twoAhead != TokenClass.LPAR) {
            if (threeAhead != TokenClass.LPAR) {
                if (fourAhead != TokenClass.LPAR) {
                    return null;
                }
            }
        }
        if (accept(TokenClass.STRUCT, TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            if (accept(TokenClass.STRUCT)) {
                expect(TokenClass.STRUCT);
                expect(TokenClass.IDENTIFIER);
            }
            else if (accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
                expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID);
            }
            if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.LPAR);
            expectParams();
            expect(TokenClass.RPAR);
            expect(TokenClass.LBRA);
            parseVarDecls();
            parseStmts();
            expect(TokenClass.RBRA);
            // Try to parse more fundecl
            parseFunDecls();
        }
    }


    // Expects a type
    // type    -> ( INT | CHAR | VOID | structtype) [ASTERIX]
    private void expectType() {
        if (accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID);
        }
        else if (accept(TokenClass.STRUCT)) {
            expect(TokenClass.STRUCT);
            expect(TokenClass.IDENTIFIER);
        }
        else {
            error(token.tokenClass);
        }
        if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
    }

    // Expects params
    // params  -> [ type IDENT (COMMA type IDENT)* ]
    private void expectParams() {
        // Check for vardecl && fundecl.
        TokenClass twoAhead   = lookAhead(2).tokenClass;
        TokenClass threeAhead = lookAhead(3).tokenClass;
        if (twoAhead != TokenClass.RPAR) {
            if (twoAhead != TokenClass.COMMA) {
                if (threeAhead != TokenClass.RPAR) {
                    if (threeAhead != TokenClass.COMMA) {
                        return;
                    }
                }
            }
        }
        if (accept(TokenClass.STRUCT, TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            if (accept(TokenClass.STRUCT)) {
                expect(TokenClass.STRUCT);
                expect(TokenClass.IDENTIFIER);
            }
            else if (accept(TokenClass.STRUCT, TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
                expect(TokenClass.STRUCT, TokenClass.INT, TokenClass.CHAR, TokenClass.VOID);
            }

            if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
            expect(TokenClass.IDENTIFIER);
            while (accept(TokenClass.COMMA)) {
                expect(TokenClass.COMMA);
                expectType();
                expect(TokenClass.IDENTIFIER);
            }
        }
    }

    // Expects stmt
    // stmt    -> LBRA (vardecl)* (stmt)* RBRA
    //         -> WHILE LPAR exp RPAR stmt
    //         -> IF LPAR exp RPAR stmt [ELSE stmt]
    //         -> RETURN [exp] SC
    //         -> exp ASSIGN exp SC
    //         -> exp SC
    private void expectStmt() {
        if (accept(TokenClass.LBRA)) {
            expect(TokenClass.LBRA);
            parseVarDecls();
            parseStmts();
            expect(TokenClass.RBRA);
        }
        else if (accept(TokenClass.WHILE)) {
            expect(TokenClass.WHILE);
            expect(TokenClass.LPAR);
            expectExp();
            expect(TokenClass.RPAR);
            expectStmt();
        }
        else if (accept(TokenClass.IF)) {
            expect(TokenClass.IF);
            expect(TokenClass.LPAR);
            expectExp();
            expect(TokenClass.RPAR);
            expectStmt();
            if (accept(TokenClass.ELSE)) {
                expect(TokenClass.ELSE);
                expectStmt();
            }
        }
        else if (accept(TokenClass.RETURN)) {
            expect(TokenClass.RETURN);
            // If we can accept any of the starting Tokens of an exp
            if (accept(TokenClass.LPAR, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.IDENTIFIER, TokenClass.INT_LITERAL, TokenClass.MINUS, TokenClass.ASTERIX, TokenClass.SIZEOF)) {
                expectExp();
            }
            expect(TokenClass.SC);
        }
        else if (accept(TokenClass.LPAR, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.IDENTIFIER, TokenClass.INT_LITERAL, TokenClass.MINUS, TokenClass.ASTERIX, TokenClass.SIZEOF)) {
            expectExp();
            if (accept(TokenClass.ASSIGN)) {
                expect(TokenClass.ASSIGN);
                expectExp();
            }
            expect(TokenClass.SC);
        }
        else {
            System.out.println("Error: Expected a stmt");
            error(TokenClass.LBRA, TokenClass.WHILE, TokenClass.IF, TokenClass.RETURN, TokenClass.LPAR, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.MINUS, TokenClass.IDENTIFIER, TokenClass.ASTERIX, TokenClass.SIZEOF);
        }
    }

    // Parses: (stmt)*
    // stmt    -> LBRA (vardecl)* (stmt)* RBRA
    //         -> WHILE LPAR exp RPAR stmt
    //         -> IF LPAR exp RPAR stmt [ELSE stmt]
    //         -> RETURN [exp] SC
    //         -> exp ASSIGN exp SC
    //         -> exp SC
    private void parseStmts() {
        if (accept(TokenClass.LBRA)) {
            expect(TokenClass.LBRA);
            parseVarDecls();
            parseStmts();
            expect(TokenClass.RBRA);
            parseStmts();
        }
        else if (accept(TokenClass.WHILE)) {
            expect(TokenClass.WHILE);
            expect(TokenClass.LPAR);
            expectExp();
            expect(TokenClass.RPAR);
            expectStmt();
            parseStmts();
        }
        else if (accept(TokenClass.IF)) {
            expect(TokenClass.IF);
            expect(TokenClass.LPAR);
            expectExp();
            expect(TokenClass.RPAR);
            expectStmt();
            if (accept(TokenClass.ELSE)) {
                expect(TokenClass.ELSE);
                expectStmt();
            }
            parseStmts();
        }
        else if (accept(TokenClass.RETURN)) {
            expect(TokenClass.RETURN);
            // If we can accept any of the starting Tokens of an exp
            if (accept(TokenClass.LPAR, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.IDENTIFIER, TokenClass.INT_LITERAL, TokenClass.MINUS, TokenClass.ASTERIX, TokenClass.SIZEOF)) {
                expectExp();
            }
            expect(TokenClass.SC);
            parseStmts();
        }
        else if (accept(TokenClass.LPAR, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.IDENTIFIER, TokenClass.INT_LITERAL, TokenClass.MINUS, TokenClass.ASTERIX, TokenClass.SIZEOF)) {
            expectExp();
            if (accept(TokenClass.ASSIGN)) {
                expect(TokenClass.ASSIGN);
                expectExp();
            }
            expect(TokenClass.SC);
            parseStmts();
        }
    }

    // Expects exp
    // exp     -> LPAR ( type RPAR exp | exp RPAR ) [postexp] 
    //         -> CHAR_LITERAL [postexp]
    //         -> STRING_LITERAL [postexp]
    //         -> IDENT [postexp] 
    //         -> IDENT LPAR [ exp (COMMA exp)* ] RPAR [postexp]
    //         -> INT_LITERAL [postexp] 
    //         -> MINUS (IDENT|INT_LITERL) [postexp]
    //         -> ASTERIX exp [postexp]
    //         -> SIZEOF LPAR type [postexp]
    private void expectExp() {
        if (accept(TokenClass.LPAR)) {
            expect(TokenClass.LPAR);
            // If we are about to see (type RPAR exp)
            if (accept(TokenClass.STRUCT, TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
                if (accept(TokenClass.STRUCT)) {
                    expect(TokenClass.STRUCT);
                    expect(TokenClass.IDENTIFIER);                    
                }
                else if (accept(TokenClass.STRUCT, TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
                    expect(TokenClass.STRUCT, TokenClass.INT, TokenClass.CHAR, TokenClass.VOID);
                }

                if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
                expect(TokenClass.RPAR);
                expectExp();
            }
            // Else we expect to see  (exp RPAR)
            else {
                expectExp();
                expect(TokenClass.RPAR);
            }

            parsePostExp();
        }
        else if (accept(TokenClass.CHAR_LITERAL)) {
            expect(TokenClass.CHAR_LITERAL);

            parsePostExp();
        }
        else if (accept(TokenClass.STRING_LITERAL)) {
            expect(TokenClass.STRING_LITERAL);

            parsePostExp();
        }
        else if (accept(TokenClass.IDENTIFIER)) {
            expect(TokenClass.IDENTIFIER);
            if (accept(TokenClass.LPAR)) {
                expect(TokenClass.LPAR);
                if (!accept(TokenClass.RPAR)) {
                    expectExp();
                    while (accept(TokenClass.COMMA)) {
                        expect(TokenClass.COMMA);
                        expectExp();
                    }
                }
                expect(TokenClass.RPAR);
            }

            parsePostExp();
        }
        else if (accept(TokenClass.INT_LITERAL)) {
            expect(TokenClass.INT_LITERAL);

            parsePostExp();
        }
        else if (accept(TokenClass.MINUS)) {
            expect(TokenClass.MINUS);
            expect(TokenClass.IDENTIFIER, TokenClass.INT_LITERAL);

            parsePostExp();
        }
        else if (accept(TokenClass.ASTERIX)) {
            expect(TokenClass.ASTERIX);
            expectExp();

            parsePostExp();
        }
        else if (accept(TokenClass.SIZEOF)) {
            expect(TokenClass.SIZEOF);
            expect(TokenClass.LPAR);
            expectType();
            expect(TokenClass.RPAR);

            parsePostExp();
        }
        else {
            System.out.println("Error: Expected an exp");
            error(TokenClass.LPAR, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.MINUS, TokenClass.IDENTIFIER, TokenClass.ASTERIX, TokenClass.SIZEOF);
        }
    }

    // Parses postexp
    // postexp -> (LSBR exp RSBR | DOT IDENT | op exp) [postexp]
    //
    // op      -> ( MINUS | ASTERIX | GT | LT | GE | LE | NE | EQ | PLUS | DIV | REM | OR | AND )
    private void parsePostExp() {
        // Check for postexp
        // Check if we are about to see LSBR
        if (accept(TokenClass.LSBR)) {
            expect(TokenClass.LSBR);
            expectExp();
            expect(TokenClass.RSBR);
            parsePostExp();
        }
        // Check if we are about to see DOT
        else if (accept(TokenClass.DOT)) {
            expect(TokenClass.DOT);
            expect(TokenClass.IDENTIFIER);
            parsePostExp();
        }
        // Check if we are about to see an op
        else if (accept(TokenClass.GT, TokenClass.LT, TokenClass.GE, TokenClass.LE, TokenClass.NE, TokenClass.EQ, TokenClass.PLUS, TokenClass.MINUS, TokenClass.DIV, TokenClass.ASTERIX, TokenClass.REM, TokenClass.OR, TokenClass.AND)) {
            expect(TokenClass.GT, TokenClass.LT, TokenClass.GE, TokenClass.LE, TokenClass.NE, TokenClass.EQ, TokenClass.PLUS, TokenClass.MINUS, TokenClass.DIV, TokenClass.ASTERIX, TokenClass.REM, TokenClass.OR, TokenClass.AND);
            expectExp();
            parsePostExp();
        }
    }

}
