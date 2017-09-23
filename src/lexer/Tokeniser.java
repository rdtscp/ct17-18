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

        // skip white spaces
        if (Character.isWhitespace(c))
            return next();

        // recognises the plus operator
        if (c == '+')
            return new Token(TokenClass.PLUS, line, column);

        // ... to be completed
        if (c == '#') {
            // The only valid characters that can proceed a '#'' are "include"
            String expected = "include";
            char expt_c;
            for (int i = 0; i < expected.length(); i++) {
                // Get the current and expected char.
                c      = scanner.next();
                expt_c = expected.charAt(i);
                // If the current character is not expected.
                if (c != expt_c) {
                    error(c, line, column);
                    return new Token(TokenClass.INVALID, scanner.getLine(), scanner.getColumn());
                }
                
            }
            // If we have reached here, we have a valid "#include" token; now consume its data.
            

            System.out.println("Succesfully Lexed an #include term.");
            return new Token(TokenClass.INCLUDE, line, column);
        }


        // if we reach this point, it means we did not recognise a valid token
        error(c, line, column);
        return new Token(TokenClass.INVALID, line, column);
    }


}
