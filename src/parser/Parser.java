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


    private void parseProgram() {
        parseIncludes();
        parseStructDecls();
        while (parseVarDecls()) { /* Keep parsing varDecls */ } 
        parseFunDecls();
        expect(TokenClass.EOF);
    }

    private void parseIncludes() {
	    if (accept(TokenClass.INCLUDE)) {
            nextToken();
            expect(TokenClass.STRING_LITERAL);
            parseIncludes();
        }
    }

    private void parseStructDecls() {
        if(accept(TokenClass.STRUCT)) {
            expect(TokenClass.STRUCT);
            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.LBRA);
            // Try parse varDecls, we need at least one, so if it returns
            // false, we must increment our errors.
            if(!parseVarDecls()) { System.out.println("STRUCT Declaration did not have any variable declarations."); error++; }
            else { while(parseVarDecls()) { /* Keep parsing varDecls */ } }
            expect(TokenClass.RBRA);
            expect(TokenClass.SC);
        }
    }

    /* Returns TRUE or FALSE if a var declaration existed. */
    private boolean parseVarDecls() {
        // If we are about to see a fun decl, return immediately.
        // onto parseFunDecls();
        if (lookAhead(2).tokenClass == TokenClass.LPAR){
            return false;
        }
        // Else we are still parsing varDecls
        // Var declarations can begin with TYPE: [int, char, void]
        if (accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID);
            // We can have a single asterix optionally.
            if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
        }
        // Can also begin with STRUCT IDENT
        else if (accept(TokenClass.STRUCT)) {
            expect(TokenClass.STRUCT);
            expect(TokenClass.IDENTIFIER);
        }
        // Current token isn't a valid start of a varDecl, so return false.
        else {
            return false;
        }
        // If we have reached here, we have parsed the type.
        // Expect an IDENTIFIER.
        expect(TokenClass.IDENTIFIER);
        // Could possibly have an array declaration.
        if (accept(TokenClass.LSBR)) {
            nextToken();
            expect(TokenClass.INT_LITERAL);
            expect(TokenClass.RSBR);
        }
        expect(TokenClass.SC);
        // We have finished parsing this varDecl, return true.
        return true;
    }

    private void parseFunDecls() {
        // Function declarations can begin with TYPE: [int, char, void]
        if (accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            nextToken();
            if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
        }
        // Can also begin with STRUCT IDENT
        else if (accept(TokenClass.STRUCT)) {
            nextToken();
            expect(TokenClass.IDENTIFIER);
        }
        // Current token isn't a valid token, so return.
        else {
            return;
        }
        // Expect an IDENTIFIER.
        expect(TokenClass.IDENTIFIER);
        expect(TokenClass.LPAR);
        parseParams();
        expect(TokenClass.RPAR);
        parseBlock();
        // If we have reached here, we might have more functions to parse.
        parseFunDecls();
    }

    private void parseType() {
        if (accept(TokenClass.STRUCT)) {
            expect(TokenClass.STRUCT);
            expect(TokenClass.IDENTIFIER);
        }
        else {
            expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID);
        }
        if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
        expect(TokenClass.IDENTIFIER);
    }

    private void parseParams() {
        // Params must start with a type declaration such as:
        // [ INT, CHAR, VOID ]
        if (accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            nextToken();
            if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
        }
        // Can also begin with STRUCT IDENT
        else if (accept(TokenClass.STRUCT)) {
            nextToken();
            expect(TokenClass.IDENTIFIER);
        }
        // Current token isn't a valid token, so return.
        else {
            return;
        }
    }

    private void parseBlock() {
        expect(TokenClass.LBRA);
        parseVarDecls();
        parseStatements();
        expect(TokenClass.RBRA);
    }

    private void parseStatements() {
        // @TODO
    }

}
