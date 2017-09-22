import lexer.Scanner;
import lexer.Token;
import lexer.Tokeniser;
import parser.Parser;

import java.io.File;
import java.io.FileNotFoundException;


/**
 * The Main file implies an interface for the subsequent components, e.g.
 *   * The Tokeniser must have a constructor which accepts a Scanner,
 *     moreover Tokeniser must provide a public method getErrorCount
 *     which returns the total number of lexing errors.
 *     
 *   * The Parser must have a constructor which accepts a Tokeniser,
 *     furthermore Parser must provide a public method getErrorCount
 *     which returns the total number of parsing errors.
 *     In particular, the Parser must have a public method parse which
 *     returns an AST.
 *     
 *   * ASTPrinter must provide a constructor which accepts a PrintWriter,
 *     and it must also implement the visitor interface.
 *     
 *   * SemanticAnalyzer must provide a nullary constructor and a public method
 *     analyze which accepts an AST, and returns the total number of semantic errors.
 */
public class Main {
	private static final int FILE_NOT_FOUND = 2;
    private static final int MODE_FAIL      = 254;
    private static final int LEXER_FAIL     = 250;
    private static final int PARSER_FAIL    = 245;
    private static final int PASS           = 0;
    
    private enum Mode {
        LEXER, PARSER
    }

    private static void usage() {
        System.out.println("Usage: java "+Main.class.getSimpleName()+" pass inputfile");
        System.out.println("where pass is either: -lexer, -parser");
        System.exit(-1);
    }

    public static void main(String[] args) {

        if (args.length != 2)
            usage();

        Mode mode = null;
        switch (args[0]) {
            case "-lexer": mode = Mode.LEXER; break;
            case "-parser": mode = Mode.PARSER; break;
            default:
                usage();
                break;
        }     

        File inputFile = new File(args[1]);

        Scanner scanner;
        try {
            scanner = new Scanner(inputFile);
        } catch (FileNotFoundException e) {
            System.out.println("File "+inputFile.toString()+" does not exist.");
            System.exit(FILE_NOT_FOUND);
            return;
        }

        Tokeniser tokeniser = new Tokeniser(scanner);
        if (mode == Mode.LEXER) {
            for (Token t = tokeniser.nextToken(); t.tokenClass != Token.TokenClass.EOF; t = tokeniser.nextToken()) 
            	System.out.println(t);
            if (tokeniser.getErrorCount() == 0)
        		System.out.println("Lexing: pass");
    	    else
        		System.out.println("Lexing: failed ("+tokeniser.getErrorCount()+" errors)");	
            System.exit(tokeniser.getErrorCount() == 0 ? PASS : LEXER_FAIL);
        } else if (mode == Mode.PARSER) {
		    Parser parser = new Parser(tokeniser);
		    parser.parse();
		    if (parser.getErrorCount() == 0)
		    	System.out.println("Parsing: pass");
		    else
		    	System.out.println("Parsing: failed ("+parser.getErrorCount()+" errors)");
		    System.exit(parser.getErrorCount() == 0 ? PASS : PARSER_FAIL);
        } else {
        	System.exit(MODE_FAIL);
        }
    }
}