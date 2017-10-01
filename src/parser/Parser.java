package parser;

import lexer.Token;
import lexer.Tokeniser;
import lexer.Token.TokenClass;

import java.util.LinkedList;
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

    public void parse() {
        // get the first token
        nextToken();

        parseProgram();
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
    private void parseProgram() {
        parseIncludes();
        parseStructDecls();
        parseVarDecls(false);
        parseFunDecls(false);
        expect(TokenClass.EOF);
    }

    // include -> "#include" STRING_LITERAL
    private void parseIncludes() {
	    if (accept(TokenClass.INCLUDE)) {
            nextToken();
            expect(TokenClass.STRING_LITERAL);
            parseIncludes();
        }
    }

    // structdecl -> structtype "{" (vardecl)+ "}" ";" 
    private void parseStructDecls() {
        if(accept(TokenClass.STRUCT)) {
            expect(TokenClass.STRUCT);
            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.LBRA);
            parseVarDecls(true);
            expect(TokenClass.RBRA);
            expect(TokenClass.SC);
        }
    }

    // vardecl -> type IDENT ";" | type IDENT "[" INT_LITERAL "]" ";"
    private void parseVarDecls(boolean required) {
        // Check for function declaration.
        if (lookAhead(2).tokenClass == TokenClass.LPAR) {
            if (required) error(token.tokenClass);
            return;
        }
        if (accept(TokenClass.STRUCT, TokenClass.INT, TokenClass.CHAR , TokenClass.VOID)) {
            parseType(required);
            expect(TokenClass.IDENTIFIER);
            // Check for: Array declaration.
            if (accept(TokenClass.LSBR)) {
                expect(TokenClass.LSBR);
                expect(TokenClass.INT_LITERAL);
                expect(TokenClass.RSBR);
            }
            expect(TokenClass.SC);
    
            parseVarDecls(false);
        }
        else {
            if (required) error(token.tokenClass);
        }
    }

    // fundecl -> type IDENT "(" params ")" block
    private void parseFunDecls(boolean required) {
        if (accept(TokenClass.STRUCT, TokenClass.INT, TokenClass.CHAR , TokenClass.VOID)) {
            parseType(required);
            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.LPAR);
            parseParams();
            expect(TokenClass.RPAR);
            // Parse block
            expect(TokenClass.LBRA);
            parseVarDecls(false);
            parseStmts(false);
            expect(TokenClass.RBRA);
    
            parseFunDecls(false);
        }
        else {
            if (required) error(token.tokenClass);
        }
    }

    // type -> ("int" | "char" | "void" | structtype) ["*"]
    private void parseType(boolean required) {
        // Check for: structtype
        if (accept(TokenClass.STRUCT)) {
            expect(TokenClass.STRUCT);
            expect(TokenClass.IDENTIFIER);
            // Can have an optional ASTERIX.
            if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
        }
        // Check for: int/char/void
        else if (accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID);
            // Can have an optional ASTERIX.
            if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
        }
        // Current token isn't a valid type.
        else {
            if (required) error(TokenClass.STRUCT, TokenClass.INT, TokenClass.CHAR, TokenClass.VOID);
        }
    }
    
    // params -> [ type IDENT ("," type IDENT)* ]
    private void parseParams() {
        // If we see closing parenthesis; there are no params.
        if (!accept(TokenClass.RPAR)) {
            parseType(true);
            expect(TokenClass.IDENTIFIER);
            // Now see if there are multiple params.
            while (accept(TokenClass.COMMA)) {
                parseType(true);
                expect(TokenClass.IDENTIFIER);
            }
        }
    }

    // stmt -> "return" [exp] ";"
    //      -> "while" "(" exp ")" stmt
    //      -> "if" "(" exp ")" stmt ["else" stmt]
    //      -> "{" (vardecl)* (stmt)* "}"
    //      -> exp ["=" exp] ";"
    private void parseStmts(boolean required) {
        // Check for: "return" [exp] ";"
        if (accept(TokenClass.RETURN)) {
            expect(TokenClass.RETURN);
            parseExps(false);
            expect(TokenClass.SC);
        }
        // Check for: "while" "(" exp ")" stmt
        else if (accept(TokenClass.WHILE)) {
            expect(TokenClass.WHILE);
            expect(TokenClass.LPAR);
            parseExps(true);
            expect(TokenClass.RPAR);
            parseStmts(true);
        }
        // Check for: "if" "(" exp ")" stmt ["else" stmt]
        else if (accept(TokenClass.IF)) {
            expect(TokenClass.IF);
            expect(TokenClass.LPAR);
            parseExps(true);
            expect(TokenClass.RPAR);
            parseStmts(true);
            if (accept(TokenClass.ELSE)) {
                expect(TokenClass.ELSE);
                parseStmts(true);
            }
        }
        // Check for: "{" (vardecl)* (stmt)* "}"
        else if (accept(TokenClass.LBRA)) {
            expect(TokenClass.LBRA);
            parseVarDecls(false);
            parseStmts(false);
            expect(TokenClass.RBRA);
        }
        // Check for: exp ["=" exp] ";"
        else {
            // If nextToken is in [ CHAR_LITERAL, STRING_LITERAL, ASTERIX, SIZEOF, LPAR, IDENTIFIER, INT_LITERAL, MINUS ]
            //                    ^ (List of exp starting Tokens)
            if (accept(TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.ASTERIX, TokenClass.SIZEOF, TokenClass.LPAR,TokenClass.IDENTIFIER, TokenClass.INT_LITERAL, TokenClass.MINUS)) {
                parseExps(true);
                if (accept(TokenClass.ASSIGN)) {
                    expect(TokenClass.ASSIGN);
                    parseExps(true);
                }
                expect(TokenClass.SC);
                parseStmts(false);
            }
            // If we don't require a stmt, and therefore don't require an exp.
            else if (!required) {
                return;
            }
            // If we do require a stmt, and therefore an exp, throw error since we haven't found one.
            else {
                error(token.tokenClass);
            }
        }
    }

    // exp  -> CHAR_LITERAL
    //      -> STRING_LITERAL
    //      -> "*" exp
    //      -> "sizeof" "(" type ")"
    //      -> "(" exp ")"
    //      -> "(" type ")" exp
    //      -> IDENT
    //      -> IDENT "(" [ exp ("," exp)* ] ")"
    //      -> ["-"] (IDENT | INT_LITERAL)
    //      -> exp (">" | "<" | ">=" | "<=" | "!=" | "==" | "+" | "-" | "/" | "*" | "%" | "||" | "&&") exp
    //      -> exp "[" exp "]"
    //      -> exp "." IDENT
    private void parseExps(boolean required) {
        Token curr = token;
        System.out.println(" --- Parsing exp with token: " + curr + " at pos: " + curr.position);
        // Check for: CHAR_LITERAL
        if (accept(TokenClass.CHAR_LITERAL)) {
            expect(TokenClass.CHAR_LITERAL);
        }
        // Check for: STRING_LITERAL
        else if (accept(TokenClass.STRING_LITERAL)) {
            expect(TokenClass.STRING_LITERAL);
        }
        // Check for: "*" exp
        else if (accept(TokenClass.ASTERIX)) {
            expect(TokenClass.ASTERIX);
            parseExps(true);
        }
        // Check for: "sizeof" "(" type ")"
        else if (accept(TokenClass.SIZEOF)) {
            expect(TokenClass.SIZEOF);
            expect(TokenClass.LPAR);
            parseType(true);
            expect(TokenClass.RPAR);
        }
        // Check for: "(" exp ")" AND "(" type ")" exp
        else if (accept(TokenClass.LPAR)) {
            expect(TokenClass.LPAR);
            // Check if this is a type
            if (accept(TokenClass.STRUCT, TokenClass.INT, TokenClass.CHAR , TokenClass.VOID)) {
                parseType(true);
                expect(TokenClass.RPAR);
                parseExps(true);
            }
            else {
                parseExps(true);
                expect(TokenClass.RPAR);
            }
        }
        // Check for: IDENT AND IDENT "(" [ exp ("," exp)* ] ")"
        else if (accept(TokenClass.IDENTIFIER)) {
            expect(TokenClass.IDENTIFIER);
            if (accept(TokenClass.LPAR)) {
                expect(TokenClass.LPAR);
                // Check for no arguments.
                if (accept(TokenClass.RPAR)) { expect(TokenClass.RPAR); return; }
                parseExps(true);
                while (accept(TokenClass.COMMA)) {
                    expect(TokenClass.COMMA);
                    parseExps(true);
                }
                expect(TokenClass.RPAR);
            }
            System.out.println("Finished parsing ident");
        }
        // Check for: ["-"] (IDENT | INT_LITERAL)
        else if (accept(TokenClass.INT_LITERAL)) {
            expect(TokenClass.INT_LITERAL);
        }
        // Check for: ["-"] (IDENT | INT_LITERAL)
        else if (accept(TokenClass.MINUS)) {
            expect(TokenClass.MINUS);
            expect(TokenClass.IDENTIFIER, TokenClass.INT_LITERAL);
        }
        // Check for recursive exp.
        //      -> exp (">" | "<" | ">=" | "<=" | "!=" | "==" | "+" | "-" | "/" | "*" | "%" | "||" | "&&") exp
        //      -> exp "[" exp "]"
        //      -> exp "." IDENT
        else {
            // If nextToken is in [ CHAR_LITERAL, STRING_LITERAL, ASTERIX, SIZEOF, LPAR, IDENTIFIER, INT_LITERAL, MINUS ]
            //                    ^ (List of exp starting Tokens)
            if (accept(TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL, TokenClass.ASTERIX, TokenClass.SIZEOF, TokenClass.LPAR, TokenClass.IDENTIFIER, TokenClass.INT_LITERAL, TokenClass.MINUS)) {
                parseExps(true);
                // Check for: exp "[" exp "]"
                if (accept(TokenClass.LSBR)) {
                    expect(TokenClass.LSBR);
                    parseExps(true);
                    expect(TokenClass.RSBR);
                }
                // Check for: exp "." IDENT
                else if (accept(TokenClass.DOT)) {
                    expect(TokenClass.DOT);
                    expect(TokenClass.IDENTIFIER);
                }
                // Check for: exp (">" | "<" | ">=" | "<=" | "!=" | "==" | "+" | "-" | "/" | "*" | "%" | "||" | "&&") exp
                else if (accept(TokenClass.GT, TokenClass.LT, TokenClass.GE, TokenClass.LE, TokenClass.NE, TokenClass.EQ, TokenClass.PLUS, TokenClass.MINUS, TokenClass.DIV, TokenClass.ASTERIX, TokenClass.REM, TokenClass.OR, TokenClass.AND)) {
                    expect(TokenClass.GT, TokenClass.LT, TokenClass.GE, TokenClass.LE, TokenClass.NE, TokenClass.EQ, TokenClass.PLUS, TokenClass.MINUS, TokenClass.DIV, TokenClass.ASTERIX, TokenClass.REM, TokenClass.OR, TokenClass.AND);
                    parseExps(true);
                }
                // No valid exp found.
                else {
                    if (required) {
                        error(token.tokenClass);
                    }
                }
            }
            // No valid start to an exp found.
            else {
                if (required) {
                    error(token.tokenClass);
                }
            }
        }
    }
}
