package lexer;

import lexer.Token.TokenClass;

import java.io.EOFException;
import java.io.IOException;

/**
 * @author cdubach
 */
public class Tokeniser {

    private Scanner scanner;

    private int error = 0;
    public int getErrorCount() {
	return this.error;
    }

    public Tokeniser(Scanner scanner) {
        this.scanner = scanner;
    }

    private void error(char c, int line, int col) {
        System.out.println("Lexing error: unrecognised character ("+c+") at "+line+":"+col);
	    error++;
    }


    public Token nextToken() {
        Token result;
        try {
             result = next();
        } catch (EOFException eof) {
            // end of file, nothing to worry about, just return EOF token
            return new Token(TokenClass.EOF, scanner.getLine(), scanner.getColumn());
        } catch (IOException ioe) {
            ioe.printStackTrace();
            // something went horribly wrong, abort
            System.exit(-1);
            return null;
        }
        return result;
    }

    /*
     * To be completed
     */
    private Token next() throws IOException {

        int line = scanner.getLine();
        int column = scanner.getColumn();

        // get the next character
        char c = scanner.next();

        // Skip white spaces
        if (Character.isWhitespace(c))
            return next();

        /* Recognise simple tokens. */
        if (c == '{') return new Token(TokenClass.LBRA, line, column);
        if (c == '}') return new Token(TokenClass.RBRA, line, column);
        if (c == '(') return new Token(TokenClass.LPAR, line, column);
        if (c == ')') return new Token(TokenClass.RPAR, line, column);
        if (c == '[') return new Token(TokenClass.LSBR, line, column);
        if (c == ']') return new Token(TokenClass.RSBR, line, column);
        if (c == ';') return new Token(TokenClass.SC, line, column);
        if (c == ',') return new Token(TokenClass.COMMA, line, column);
        if (c == '+') return new Token(TokenClass.PLUS, line, column);
        if (c == '-') return new Token(TokenClass.MINUS, line, column);
        if (c == '*') return new Token(TokenClass.ASTERIX, line, column);
        if (c == '/') return new Token(TokenClass.DIV, line, column);
        if (c == '%') return new Token(TokenClass.REM, line, column);
        if (c == '.') return new Token(TokenClass.DOT, line, column);

        // Recognise ASSIGN/EQ tokens.
        if (c == '=') {
            // EQ Token.
            if (scanner.peek() == '=') {
                scanner.next();
                return new Token(TokenClass.EQ, line, column);
            }
            // ASSIGN Token.
            else return new Token(TokenClass.ASSIGN, line, column);
        }

        // Recognise NE token.
        if (c == '!' && scanner.peek() == '=') {
            scanner.next();
            return new Token(TokenClass.NE, line, column);
        }

        // Recognise LT/LE tokens.
        if (c == '<') {
            if (scanner.peek() == '=') {
                scanner.next();
                return new Token(TokenClass.LE, line, column);
            }
            else return new Token(TokenClass.LT, line, column);
        }

        // Recognise GT/GE tokens.
        if (c == '>') {
            if (scanner.peek() == '=') {
                scanner.next();
                return new Token(TokenClass.GE, line, column);
            }
            return new Token(TokenClass.GT, line, column);
        }

        // Recognise INCLUDE token.
        if (c == '#') {
            // The only valid characters that can proceed a '#'' are "include"
            String expected = "include";
            char expt_c;
            for (int i = 0; i < expected.length(); i++) {
                line = scanner.getLine();
                column = scanner.getColumn();
                // Get the current and expected char.
                c      = scanner.next();
                expt_c = expected.charAt(i);
                // If the current character is not expected.
                if (c != expt_c) {
                    error(c, line, column);
                    return new Token(TokenClass.INVALID, line, column);
                }
                
            }
            // We have found "#include".
            return new Token(TokenClass.INCLUDE, line, column);
        }

        // Recognise STRING_LITERAL token.
        if (c == '"') {
            // We are now expecting any set of characters, terminated by a single ". With care taken for escaped characters.
            StringBuilder sb = new StringBuilder();
            while (true) {
                line   = scanner.getLine();
                column = scanner.getColumn();
                // Try get the current char.
                try {
                    c = scanner.next();
                } catch (EOFException eof) {
                    // Reached end of file before terminating string.
                    error(c, scanner.getLine(), scanner.getColumn());
                    return new Token(TokenClass.INVALID, scanner.getLine(), scanner.getColumn());
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    // something went horribly wrong, abort
                    System.exit(-1);
                    return null;
                }
                // Check if we are about to see an escaped character.
                if (c == '\\') {
                    sb.append(c);
                    c = scanner.next();
                    line   = scanner.getLine();
                    column = scanner.getColumn();
                    sb.append(c);
                }
                // End of string.
                else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
            }
            return new Token(TokenClass.STRING_LITERAL, sb.toString(), line, column);
        }

        // Recognise IDENTIFIER/keywords/types token.
        if (Character.isLetter(c)) {

        }

        // if we reach this point, it means we did not recognise a valid token
        error(c, line, column);
        return new Token(TokenClass.INVALID, line, column);
    }


}
