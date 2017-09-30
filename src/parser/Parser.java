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
        while (parseFunDecls()) { /* Keep parsing funDecls */ }
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
            // Try parse varDecls, we need at least one, so if it
            // returns false, we must increment our errors.
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
        if (accept(TokenClass.STRUCT)) {
            expect(TokenClass.STRUCT);
            expect(TokenClass.IDENTIFIER);
        }
        else if (accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID);
        }
        // Current token isn't a valid start of a varDecl, so return false.
        else {
            return false;
        }
        // Can have an optional ASTERIX.
        if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
        // Expect an IDENTIFIER.
        expect(TokenClass.IDENTIFIER);
        // Could possibly have an array declaration.
        if (accept(TokenClass.LSBR)) {
            expect(TokenClass.LSBR);
            expect(TokenClass.INT_LITERAL);
            expect(TokenClass.RSBR);
        }
        expect(TokenClass.SC);
        // We have finished parsing this varDecl, return true.
        return true;
    }

    /* Returns TRUE or FALSE if a func declaration existed. */
    private boolean parseFunDecls() {
        // Function declarations can begin with TYPE: [ INT, CHAR, VOID, STRUCT ]
        if (accept(TokenClass.STRUCT)) {
            expect(TokenClass.STRUCT);
            expect(TokenClass.IDENTIFIER);
        }
        else if (accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID);
        }
        // Current token isn't a valid token, so return.
        else {
            return false;
        }
        // Can have an optional ASTERIX.
        if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
        // Expect an IDENTIFIER.
        expect(TokenClass.IDENTIFIER);
        expect(TokenClass.LPAR);
        parseParams();
        expect(TokenClass.RPAR);
        parseBlock();
        // If we have reached here, we have parsed a func decl succesfully.
        return true;
    }

    private void parseParams() {
        // Params must start with a type declaration such as:
        // [ INT, CHAR, VOID, STRUCT ], or be empty
        if (accept(TokenClass.STRUCT)) {
            expect(TokenClass.STRUCT);
            expect(TokenClass.IDENTIFIER);
        }
        else if (accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID);
            if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
        }
        // There are no params here.
        else {
            return;
        }
    }

    private void parseBlock() {
        expect(TokenClass.LBRA);
        while(parseVarDecls())   { /* Keep parsing varDecls   */ }
        while(parseStatements()) { /* Keep parsing statements */ }
        expect(TokenClass.RBRA);
    }

    /* Returns TRUE or FALSE if a statement existed. */
    private boolean parseStatements() {
        // Check for: exp = exp;
        if (lookAhead(1).tokenClass == TokenClass.EQ) {
            if (!parseExpression()) { System.out.println("Expected an expression."); error++; }
            expect(TokenClass.EQ);
            if (!parseExpression()) { System.out.println("Assignment expression did not assign any expression."); error++; }
            expect(TokenClass.SC);
            return true;
        }
        // Check for: exp;
        else if (lookAhead(1).tokenClass == TokenClass.SC) {
            if (!parseExpression()) { System.out.println("Expected an expression."); error++; }
            expect(TokenClass.SC);
            return true;
        }
        // Check for: while( exp ) stmt
        else if (accept(TokenClass.WHILE)) {
            expect(TokenClass.WHILE);
            expect(TokenClass.LBRA);
            if (!parseExpression()) { System.out.println("WHILE statement did not contain any conditional expression."); error++; }
            while(parseExpression()) { /* Keep parsing expressions. */ }
            expect(TokenClass.RBRA);
            parseStatements();
            return true;
        }
        // Check for: if( exp ) stmt [else stmt]
        else if (accept(TokenClass.IF)) {
            expect(TokenClass.IF);
            expect(TokenClass.LBRA);
            if (!parseExpression()) { System.out.println("WHILE statement did not contain any conditional expression."); error++; }
            while(parseExpression()) { /* Keep parsing expressions. */ }
            expect(TokenClass.RBRA);
            parseStatements();
            // Can have a possible ELSE statement.
            if (accept(TokenClass.LSBR)) {
                expect(TokenClass.LSBR);
                parseStatements();
            }
            return true;
        }
        // Check for: return [exp];
        else if (accept(TokenClass.RETURN)) {
            expect(TokenClass.RETURN);
            parseExpression();
            expect(TokenClass.SC);
            return true;
        }
        else {
            return false;
        }
    }

    /* Returns TRUE or FALSE if an expression existed. */
    private boolean parseExpression() {
        // Check for: CHAR_LITERAL
        if (accept(TokenClass.CHAR_LITERAL)) {
            expect(TokenClass.CHAR_LITERAL);
            return true;
        }
        // Check for: STRING_LITERAL
        else if (accept(TokenClass.STRING_LITERAL)) {
            expect(TokenClass.STRING_LITERAL);
            return true;
        }
        // Check for: * exp
        else if (accept(TokenClass.ASTERIX)) {
            expect(TokenClass.ASTERIX);
            if (!parseExpression()) { System.out.println("Expected an expression proceeding a *"); error++; return false; }
            else return true;
        }
        // Check for: sizeof( exp )
        else if (accept(TokenClass.SIZEOF)) {
            expect(TokenClass.SIZEOF);
            expect(TokenClass.LBRA);
            // Expect a type.
            if (accept(TokenClass.STRUCT)) {
                expect(TokenClass.STRUCT);
                expect(TokenClass.IDENTIFIER);
            }
            else if (accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
                expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID);
            }
            // Can have an optional ASTERIX.
            if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
            expect(TokenClass.RBRA);
            return true;
        }
        // Check for: (exp) OR (type)exp
        else if (accept(TokenClass.LBRA)) {
            expect(TokenClass.LBRA);
            // Check for: (exp)
            if (parseExpression()) {
                expect(TokenClass.RBRA);
                return true;
            }
            // Check for: (type)exp
            else {
                // Expect a type.
                if (accept(TokenClass.STRUCT)) {
                    expect(TokenClass.STRUCT);
                    expect(TokenClass.IDENTIFIER);
                }
                else if (accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
                    expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID);
                }
                else {
                    return false;
                }
                // Can have an optional ASTERIX.
                if (accept(TokenClass.ASTERIX)) expect(TokenClass.ASTERIX);
                return true;
            }
        }
        // Check for: [-] (IDENT | INT_LITERAL)
        else if (lookAhead(1).tokenClass == TokenClass.LBRA) {
            // Optional MINUS token.
            if (accept(TokenClass.MINUS)) expect(TokenClass.MINUS);
            expect(TokenClass.LBRA);
            expect(TokenClass.IDENTIFIER, TokenClass.INT_LITERAL);
            expect(TokenClass.RBRA);
            return true;
        }
        // Check for: IDENT([exp (, exp)*]) OR IDENT
        else if (accept(TokenClass.IDENTIFIER)) {
            // We are about to see the former case.
            if (accept(TokenClass.LBRA)) {
                expect(TokenClass.LBRA);
                // Accept optional arguments.
                if (parseExpression()) {
                    // While there are more arguments.
                    while (accept(TokenClass.COMMA)) {
                        expect(TokenClass.COMMA);
                        if (!parseExpression()) { System.out.println("Expected an expression in function arguments."); error++; return false; }
                    }
                }
                expect(TokenClass.RBRA);
                return true;
            }
            else {
                return true;
            }
        }
        // @TODO Check for: exp[exp] AND exp.IDENT AND exp OP exp
        else {
            return false;
        }
    }
}
