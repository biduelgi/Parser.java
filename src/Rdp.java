import java.io.File;

public class Rdp {
    static public void main ( String[] argv ) {
        Parser parser  = new Parser(new Lexer("p2.cl"));
        Program prog = parser.program();
        prog.display();
    }
}
