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

        // Get the next Char.
        char c = scanner.next();

        // Parse DIV token taking into account comments.
        if (c == '/') {
            char peek = scanner.peek();
            // We have a regular line comment.
            if (peek == '/') {
                // Move into the comment content.
                scanner.next();
                // Loop until we find the end of the comment.
                while (true) {
                    c    = scanner.next();
                    peek = scanner.peek();
                    // Newline has character code 10.
                    if ((int)c == 10) {
                        break;
                    }
                }
            }
            // We have a block comment.
            else if (peek == '*') {
                // Move into the comment content.
                scanner.next();
                // Loop until we find the end of the comment.
                while (true) {
                    peek = scanner.peek();
                    if (peek == '*') {
                        scanner.next();
                        peek = scanner.peek();
                        if (peek == '/') {
                            c = scanner.next();
                            c = scanner.next();
                            break;     // If */ then stop scanning.
                        }
                    }
                    scanner.next();
                }
            }
            // We can only divide by IDENTIFIERs or INT_LITERALs.
            else if (!Character.isDigit(peek) && !Character.isLetter(peek) && peek != '_') {
                error(c, scanner.getLine(),scanner.getColumn());
                return new Token(TokenClass.INVALID, scanner.getLine(),scanner.getColumn());
            }
            else return new Token(TokenClass.DIV, scanner.getLine(),scanner.getColumn());
        }

        // Skip Whitespace.
        if (Character.isWhitespace(c)) return next();

        /* Recognise simple tokens. */
        if (c == '{') return new Token(TokenClass.LBRA, scanner.getLine(),scanner.getColumn());
        if (c == '}') return new Token(TokenClass.RBRA, scanner.getLine(),scanner.getColumn());
        if (c == '(') return new Token(TokenClass.LPAR, scanner.getLine(),scanner.getColumn());
        if (c == ')') return new Token(TokenClass.RPAR, scanner.getLine(),scanner.getColumn());
        if (c == '[') return new Token(TokenClass.LSBR, scanner.getLine(),scanner.getColumn());
        if (c == ']') return new Token(TokenClass.RSBR, scanner.getLine(),scanner.getColumn());
        if (c == ';') return new Token(TokenClass.SC, scanner.getLine(),scanner.getColumn());
        if (c == ',') return new Token(TokenClass.COMMA, scanner.getLine(),scanner.getColumn());
        if (c == '+') return new Token(TokenClass.PLUS, scanner.getLine(),scanner.getColumn());
        if (c == '-') return new Token(TokenClass.MINUS, scanner.getLine(),scanner.getColumn());
        if (c == '*') return new Token(TokenClass.ASTERIX, scanner.getLine(),scanner.getColumn());
        if (c == '%') return new Token(TokenClass.REM, scanner.getLine(),scanner.getColumn());
        if (c == '.') return new Token(TokenClass.DOT, scanner.getLine(),scanner.getColumn());
        
        // Recognise ASSIGN/EQ tokens.
        if (c == '=') {
            // EQ Token.
            if (scanner.peek() == '=') {
                return new Token(TokenClass.EQ, scanner.getLine(), scanner.getColumn());
            }
            // ASSIGN Token.
            else return new Token(TokenClass.ASSIGN, scanner.getLine(),scanner.getColumn());
        }
        // Recognise NE token.
        if (c == '!' && scanner.peek() == '=') {
            if (scanner.peek() == '=') {
                scanner.next();
                return new Token(TokenClass.NE, scanner.getLine(), scanner.getColumn());
            } else {
                return new Token(TokenClass.INVALID, scanner.getLine(),scanner.getColumn());
            }
        }
        // Recognise LT/LE tokens.
        if (c == '<') {
            if (scanner.peek() == '=') {
                scanner.next();
                return new Token(TokenClass.LE, scanner.getLine(), scanner.getColumn());
            }
            else return new Token(TokenClass.LT, scanner.getLine(),scanner.getColumn());
        }
        // Recognise GT/GE tokens.
        if (c == '>') {
            if (scanner.peek() == '=') {
                scanner.next();
                return new Token(TokenClass.GE, scanner.getLine(), scanner.getColumn());
            }
            return new Token(TokenClass.GT, scanner.getLine(),scanner.getColumn());
        }
        // Recognise AND token.
        if (c == '&') {
            if (scanner.peek() == '&') {
                scanner.next();
                return new Token(TokenClass.AND, scanner.getLine(), scanner.getColumn());
            }
            else {
                error(c, scanner.getLine(),scanner.getColumn());
                return new Token(TokenClass.INVALID, scanner.getLine(),scanner.getColumn());
            }
        }
        // Recognise OR token.
        if (c == '|') {
            if (scanner.peek() == '|') {
                scanner.next();
                return new Token(TokenClass.OR, scanner.getLine(), scanner.getColumn());
            }
            else {
                error(c, scanner.getLine(),scanner.getColumn());
                return new Token(TokenClass.INVALID, scanner.getLine(),scanner.getColumn());
            }
        }
        // Recognise INCLUDE token.
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
                    error(c, scanner.getLine(),scanner.getColumn());
                    return new Token(TokenClass.INVALID, scanner.getLine(),scanner.getColumn());
                }
                
            }
            // We have found "#include".
            return new Token(TokenClass.INCLUDE, scanner.getLine(),scanner.getColumn());
        }
        // Recognise STRING_LITERAL token. @TODO, multiple lines fix.
        if (c == '"') {
            // We are now expecting any set of characters, terminated by a single ". With care taken for escaped characters.
            StringBuilder sb = new StringBuilder();
            while (true) {
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
                    sb.append(c);
                }
                // End of string.
                else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
            }
            return new Token(TokenClass.STRING_LITERAL, sb.toString(), scanner.getLine(), scanner.getColumn());
        }

        // Recognise IDENTIFIER/keywords/types token.
        if (Character.isLetter(c) || c == '_') {
            /* Alphabetically go throuh possible keywords/types before assuming IDENTIFIER. */
            // Create a StringBuilder in case of IDENTIFIER.
            StringBuilder sb = new StringBuilder();
            sb.append(c);
            // Check for CHAR token.
            if (c == 'c') {
                String expected = "har";    // "char" but with first char removed since that has already been checked.
                char peek;                  // Peek into next char of stream.
                char expt;                  // What we expect the peek to be.
                boolean isChar = true;      // Track if this is a CHAR token.
                for (int i = 0; i < expected.length(); i++) {
                    // Peek the next char, and the expected char.
                    peek = scanner.peek();
                    expt = expected.charAt(i);
                    if (peek != expt) {
                        isChar = false;     // This is not a CHAR token.
                        break;
                    }
                    // It is potentially part of a CHAR token, save this char.
                    c = scanner.next();
                    sb.append(c);
                }
                peek = scanner.peek();
                if (isChar && !Character.isLetter(peek) && !Character.isDigit(peek)){
                    return new Token(TokenClass.CHAR, scanner.getLine(),scanner.getColumn());
                }
            }
            // Check for ELSE token.
            else if (c == 'e') {
                String expected = "lse"; // "else" but with first char removed since that has already been checked.
                // Flag to track if this stream of characters is an ELSE token.
                boolean isElse  = true;
                char expt_c;
                char peek;
                for (int i = 0; i < expected.length(); i++) {
                    // Get the next char, and the expected char.
                    peek   = scanner.peek();
                    expt_c = expected.charAt(i);
                    if (peek != expt_c) {
                         // This is not an ELSE token.
                        isElse = false;
                        break;
                    }
                    c = scanner.next();
                    sb.append(c);
                }
                peek = scanner.peek();
                if (isElse && !Character.isLetter(peek) && !Character.isDigit(peek)) {
                    return new Token(TokenClass.ELSE, scanner.getLine(),scanner.getColumn());
                }
            }
            // Check for IF/INT token.
            else if (c == 'i') {
                // Check for IF token.
                if (scanner.peek() == 'f') {
                    c = scanner.next();
                    sb.append(c);
                    if (scanner.peek() == ' ') return new Token(TokenClass.IF, scanner.getLine(),scanner.getColumn());
                }
                // Check for INT token.
                else {
                    String expected = "nt"; // "int" but with first char removed since that has already been checked.
                    // Flag to track if this stream of characters is an INT token.
                    boolean isInt  = true;
                    char expt_c;
                    char peek;
                    for (int i = 0; i < expected.length(); i++) {
                        // Get the next char, and the expected char.
                        peek   = scanner.peek();
                        expt_c = expected.charAt(i);
                        if (peek != expt_c) {
                            // This is not an ELSE token.
                            isInt = false;
                            break;
                        }
                        c = scanner.next();
                        sb.append(c);
                    }
                    peek = scanner.peek();
                    if (isInt && !Character.isLetter(peek) && !Character.isDigit(peek)) {
                        return new Token(TokenClass.INT, scanner.getLine(),scanner.getColumn());
                    }
                }
            }
            // Check for RETURN token.
            else if (c == 'r') {
                String expected = "eturn"; // "return" but with first char removed since that has already been checked.
                // Flag to track if this stream of characters is a RETURN token.
                boolean isReturn  = true;
                char expt_c;
                char peek;
                for (int i = 0; i < expected.length(); i++) {
                    // Get the next char, and the expected char.
                    peek   = scanner.peek();
                    expt_c = expected.charAt(i);
                    if (peek != expt_c) {
                        // This is not a RETURN token.
                        isReturn = false;
                        break;
                    }
                    c = scanner.next();
                    sb.append(c);
                }
                peek = scanner.peek();
                if (isReturn && !Character.isLetter(peek) && !Character.isDigit(peek)) {
                    return new Token(TokenClass.RETURN, scanner.getLine(),scanner.getColumn());
                }
            }
            // Check for SIZEOF/STRUCT token.
            else if (c == 's') {
                // Check for SIZEOF token.
                if (scanner.peek() == 'i') {
                    String expected = "izeof"; // "sizeof" but with first char removed since that has already been checked.
                    // Flag to track if this stream of characters is an SIZEOF token.
                    boolean isSizeof  = true;
                    char expt_c;
                    char peek;
                    for (int i = 0; i < expected.length(); i++) {
                        // Get the next char, and the expected char.
                        peek   = scanner.peek();
                        expt_c = expected.charAt(i);
                        if (peek != expt_c) {
                            // This is not an SIZEOF token.
                            isSizeof = false;
                            break;
                        }
                        c = scanner.next();
                        sb.append(c);
                    }
                    peek = scanner.peek();
                    if (isSizeof && !Character.isLetter(peek) && !Character.isDigit(peek)) {
                        return new Token(TokenClass.SIZEOF, scanner.getLine(),scanner.getColumn());
                    }
                }
                // Check for STRUCT token.
                else {
                    String expected = "truct"; // "struct" but with first char removed since that has already been checked.
                    // Flag to track if this stream of characters is an STRUCT token.
                    boolean isStruct  = true;
                    char expt_c;
                    char peek;
                    for (int i = 0; i < expected.length(); i++) {
                        // Get the next char, and the expected char.
                        peek   = scanner.peek();
                        expt_c = expected.charAt(i);
                        if (peek != expt_c) {
                            // This is not a STRUCT token.
                            isStruct = false;
                            break;
                        }
                        c = scanner.next();
                        sb.append(c);
                    }
                    peek = scanner.peek();
                    if (isStruct && !Character.isLetter(peek) && !Character.isDigit(peek)) {
                        return new Token(TokenClass.STRUCT, scanner.getLine(),scanner.getColumn());
                    }
                }
            }
            // Check for WHILE token.
            else if (c == 'w') {
                String expected = "hile"; // "while" but with first char removed since that has already been checked.
                // Flag to track if this stream of characters is a WHILE token.
                boolean isWhile  = true;
                char expt_c;
                char peek;
                for (int i = 0; i < expected.length(); i++) {
                    // Get the next char, and the expected char.
                    peek   = scanner.peek();
                    expt_c = expected.charAt(i);
                    if (peek != expt_c) {
                        // This is not a WHILE token.
                        isWhile = false;
                        break;
                    }
                }
                peek = scanner.peek();
                if (isWhile && !Character.isLetter(peek) && !Character.isDigit(peek)) {
                    return new Token(TokenClass.WHILE, scanner.getLine(),scanner.getColumn());
                }
                c = scanner.next();
                sb.append(c);
            }
            // Check for VOID token.
            else if (c == 'v') {
                String expected = "oid"; // "void" but with first char removed since that has already been checked.
                // Flag to track if this stream of characters is a VOID token.
                boolean isVoid  = true;
                char expt_c;
                char peek;
                for (int i = 0; i < expected.length(); i++) {
                    // Get the next char, and the expected char.
                    peek   = scanner.peek();
                    expt_c = expected.charAt(i);
                    if (peek != expt_c) {
                        // This is not a VOID token.
                        isVoid = false;
                        break;
                    }
                    c = scanner.next();
                    sb.append(c);
                }
                peek = scanner.peek();
                if (isVoid && !Character.isLetter(peek) && !Character.isDigit(peek)) {
                    return new Token(TokenClass.VOID, scanner.getLine(),scanner.getColumn());
                }
            }

            // If we have reached here, no token has been returned.
            // Now Lex an IDENTIFIER, where c is the last character in sb.
            char peek;
            while (true) {
                peek = scanner.peek();
                // If the next character is whitespace, the IDENTIFIER has been identified.
                if (Character.isWhitespace(peek)) {
                    return new Token(TokenClass.IDENTIFIER, sb.toString(), scanner.getLine(), scanner.getColumn());
                }
                // If the next character is an illegal characater for an IDENTIFIER, we have finished finding the token.
                if (!Character.isLetter(peek) && !Character.isDigit(peek) && peek != '_') {
                    return new Token(TokenClass.IDENTIFIER, sb.toString(), scanner.getLine(), scanner.getColumn());
                }
                // We are still Lexing the token.
                c = scanner.next();
                sb.append(c);
            }
        }

        // Recognise INT_LITERAL token.
        if (Character.isDigit(c)) {
            StringBuilder sb = new StringBuilder();
            sb.append(c);
            char peek;
            while (true) {
                peek = scanner.peek();
                // Check that next char is a digit.
                if (!Character.isDigit(peek)) {
                    return new Token(TokenClass.INT_LITERAL, sb.toString(), scanner.getLine(),scanner.getColumn());
                }
                c = scanner.next();
                sb.append(c);
            }
        }

        // Recognise CHAR_LITERAL token.
        if (c == '\'') {
            c = scanner.next();
            char peek = scanner.peek();
            // Check for escape character: '\t' | '\b' | '\n' | '\r' | '\f' | '\'' | '\"' | '\\'
            if (c == '\\') {
                StringBuilder sb = new StringBuilder();
                sb.append('\\'); // Append the escape char.
                // Our valid set of escaped characters.
                if (peek == 't' || peek == 'b' || peek == 'n' || peek == 'r' || peek == 'f' || peek == '\'' || peek == '"' || peek == '\\') {
                    c = scanner.next();
                    peek = scanner.peek();
                    // Next character must be a closing single quote to be a valid CHAR_LITERAL.
                    if (peek == '\'') {
                        sb.append(c);
                        scanner.next();
                        return new Token(TokenClass.CHAR_LITERAL, sb.toString(), scanner.getLine(),scanner.getColumn());
                    } else {
                        scanner.next();
                        error(c, scanner.getLine(),scanner.getColumn());
                        return new Token(TokenClass.INVALID, scanner.getLine(),scanner.getColumn());
                    }
                } else {
                    scanner.next();
                    error(c, scanner.getLine(),scanner.getColumn());
                    return new Token(TokenClass.INVALID, scanner.getLine(),scanner.getColumn());
                }
            }
            // Otherwise we have a normal char.
            else {
                // Check the CHAR_LITERAL is closed correctly.
                if (peek == '\'') {
                    scanner.next();
                    return new Token(TokenClass.CHAR_LITERAL, String.valueOf(c), scanner.getLine(),scanner.getColumn());
                } else {
                    scanner.next();
                    error(c, scanner.getLine(),scanner.getColumn());
                    return new Token(TokenClass.INVALID, scanner.getLine(),scanner.getColumn());
                }
            }
        }



        // if we reach this point, it means we did not recognise a valid token
        error(c, scanner.getLine(),scanner.getColumn());
        return new Token(TokenClass.INVALID, scanner.getLine(),scanner.getColumn());
    }


}
