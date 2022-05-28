import java.io.IOException;
import java.util.*;

public class Parser {
    // Recursive descent parser that inputs a C++Lite program and 
    // generates its abstract syntax.  Each method corresponds to
    // a concrete syntax grammar rule, which appears as a comment
    // at the beginning of the method.
  
    Token token;          // current token from the input stream
    Lexer lexer;
  
    public Parser(Lexer ts) { // Open the C++Lite source program
        lexer = ts;                          // as a token stream, and
        token = lexer.next();            // retrieve its first Token
    }
  
    private String match (TokenType t) { // * return the string of a token if it matches with t *
        String value = token.value();
        if (token.type().equals(t))
            token = lexer.next();
        else
            error(t);
        return value;
    }
  
    private void error(TokenType tok) {
        System.err.println("Syntax error: expecting: " + tok 
                           + "; saw: " + token);
        System.exit(1);
    }
  
    private void error(String tok) {
        System.err.println("Syntax error: expecting: " + tok 
                           + "; saw: " + token);
        System.exit(1);
    }
  
    public Program program() {
        // Program --> void main ( ) '{' Declarations Statements '}'
        TokenType[ ] header = {TokenType.Int, TokenType.Main,
                          TokenType.LeftParen, TokenType.RightParen};
        for (int i=0; i<header.length; i++)   // bypass "int main ( )"
            match(header[i]);
        match(TokenType.LeftBrace);
        Declarations decs = declarations();
        Block block = progstatements();
        // student exercise
        match(TokenType.RightBrace);
        return new Program(decs, block);  // student exercise
    }
  
    private Declarations declarations () {
        Declarations decs = new Declarations();
        while(isType()){
            declaration(decs);
        }
        // Declarations --> { Declaration }
        return decs;  // student exercise
    }
  
    private void declaration (Declarations ds) {
        // Declaration  --> Type Identifier { , Identifier } ;
        Type t = type();
        Variable var = new Variable(match(TokenType.Identifier));
        Declaration dec = new Declaration(var, t);
        ds.add(dec);
        while(token.type().equals(TokenType.Comma)){
            token = lexer.next();
            var = new Variable(match(TokenType.Identifier));
            dec = new Declaration(var, t);
            ds.add(dec);
        }
        match(TokenType.Semicolon);
        // student exercise
    }
  
    private Type type () {
        // Type  -->  int | bool | float | char 
        Type t = null;
        if(token.type().equals(TokenType.Int)) t= Type.INT;
        else if(token.type().equals(TokenType.Bool)) t= Type.BOOL;
        else if(token.type().equals(TokenType.Float)) t= Type.FLOAT;
        else if(token.type().equals(TokenType.Char)) t= Type.CHAR;
        else error("Type");
        token = lexer.next();
        // student exercise
        return t;          
    }
  
    private Statement statement() {
        // Statement --> ; | Block | Assignment | IfStatement | WhileStatement
        Statement s = new Skip();
        if(token.type().equals(TokenType.Semicolon)) token = lexer.next();
        else if(token.type().equals(TokenType.LeftBrace)) s =statements();
        else if(token.type().equals(TokenType.Identifier)) s =assignment();
        else if(token.type().equals(TokenType.If)) s =ifStatement();
        else if(token.type().equals(TokenType.While)) s =whileStatement();
        else if(token.type().equals(TokenType.Print)) s =printStatement();
        else error("Statement");
        // student exercise
        return s;
    }

    private Block progstatements () {   //첫 main block은 중괄호 check를 미리하기에 분리
        // Block --> '{' Statements '}'
        Block b = new Block();
        Statement s;

        while(isStatement()){
            s = statement();
            b.members.add(s);
        }
        // student exercise
        return b;
    }
  
    private Block statements () {
        // Block --> '{' Statements '}'
        Block b = new Block();
        Statement s;
        match(TokenType.LeftBrace);

        while(isStatement()){
            s = statement();
            b.members.add(s);
        }
        match(TokenType.RightBrace);
        // student exercise
        return b;
    }
  
    private Assignment assignment () {
        // Assignment --> Identifier = Expression ;
        Variable var = new Variable(match(TokenType.Identifier));
        match(TokenType.Assign);
        Expression exp = expression();
        match(TokenType.Semicolon);
        return new Assignment(var, exp);  // student exercise
    }
  
    private Conditional ifStatement () {
        // IfStatement --> if ( Expression ) Statement [ else Statement ]
        match(TokenType.If);
        Conditional cond;
        match(TokenType.LeftParen);
        Expression exp = expression();
        match(TokenType.RightParen);
        Statement s = statement();
        if(token.type().equals(TokenType.Else)){
            token = lexer.next();
            Statement elsestatement = statement();
            cond = new Conditional(exp, s, elsestatement);
        }
        else cond = new Conditional(exp, s);
        return cond;  // student exercise
    }
  
    private Loop whileStatement () {
        // WhileStatement --> while ( Expression ) Statement
        match(TokenType.While);
        match(TokenType.LeftParen);
        Expression exp = expression();
        match(TokenType.RightParen);
        Statement s = statement();
        return new Loop(exp, s);  // student exercise
    }

    private Print printStatement () {
        // PrintStatement --> print IntValue || Identification or printCh CharValue || Id
        match(TokenType.Print);
        Expression exp = expression();
        return new Print(exp);  // student exercise
    }

    private Expression expression () {
        // Expression --> Conjunction { || Conjunction }
        Expression conj = conjunction();
        while(token.type().equals(TokenType.Or)){
            Operator op = new Operator(match(token.type()));
            Expression exp = expression();
            conj = new Binary(op, conj, exp);
        }
        return conj;  // student exercise
    }
  
    private Expression conjunction () {
        // Conjunction --> Equality { && Equality }
        Expression e = equality();
        while(token.type().equals(TokenType.And)){
            Operator op = new Operator(match(token.type()));
            Expression conj = conjunction();
            e = new Binary(op, e, conj);
        }
        return e;  // student exercise
    }
  
    private Expression equality () {
        // Equality --> Relation [ EquOp Relation ]
        Expression r = relation();
        if(isEqualityOp()){
            Operator op = new Operator(match(token.type()));
            Expression r2 = relation();
            r = new Binary(op, r, r2);
        }
        return r;  // student exercise
    }

    private Expression relation (){
        // Relation --> Addition [RelOp Addition]
        Expression a = addition();
        if(isRelationalOp()){
            Operator op = new Operator(match(token.type()));
            Expression a2 = addition();
            a = new Binary(op, a, a2);
        }
        return a;  // student exercise
    }
  
    private Expression addition () {
        // Addition --> Term { AddOp Term }
        Expression e = term();
        while (isAddOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = term();
            e = new Binary(op, e, term2);
        }
        return e;
    }
  
    private Expression term () {
        // Term --> Factor { MultiplyOp Factor }
        Expression e = factor();
        while (isMultiplyOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = factor();
            e = new Binary(op, e, term2);
        }
        return e;
    }
  
    private Expression factor() {
        // Factor --> [ UnaryOp ] Primary 
        if (isUnaryOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term = primary();
            return new Unary(op, term);
        }
        else return primary();
    }
  
    private Expression primary () {
        // Primary --> Identifier | Literal | ( Expression )
        //             | Type ( Expression )
        Expression e = null;
        if (token.type().equals(TokenType.Identifier)) {
            e = new Variable(match(TokenType.Identifier));
        } else if (isLiteral()) {
            e = literal();
        } else if (token.type().equals(TokenType.LeftParen)) {
            token = lexer.next();
            e = expression();       
            match(TokenType.RightParen);
        } else if (isType( )) {
            Operator op = new Operator(match(token.type()));
            match(TokenType.LeftParen);
            Expression term = expression();
            match(TokenType.RightParen);
            e = new Unary(op, term);
        } else error("Identifier | Literal | ( | Type");
        return e;
    }

    private Value literal( ) {

        Value v = null;
        String str = token.value();
        if(token.type().equals(TokenType.IntLiteral)){
            v = new IntValue(Integer.parseInt(str));
            token = lexer.next();
        }
        else if(token.type().equals(TokenType.FloatLiteral)){
            v = new FloatValue(Float.parseFloat(str));
            token = lexer.next();
        }
        else if(token.type().equals(TokenType.CharLiteral)){
            if(str.charAt(0)=='\\') v = new CharValue('\n');
            else v = new CharValue(str.charAt(0));
            token = lexer.next();
        }
        else if(token.type().equals(TokenType.True)){
            v = new BoolValue(true);
            token = lexer.next();
        }
        else if(token.type().equals(TokenType.False)){
            v = new BoolValue(false);
            token = lexer.next();
        }
        else error("Literal value");
        return v;
    }// student exercise
  

    private boolean isAddOp( ) {
        return token.type().equals(TokenType.Plus) ||
               token.type().equals(TokenType.Minus);
    }
    
    private boolean isMultiplyOp( ) {
        return token.type().equals(TokenType.Multiply) ||
               token.type().equals(TokenType.Divide);
    }
    
    private boolean isUnaryOp( ) {
        return token.type().equals(TokenType.Not) ||
               token.type().equals(TokenType.Minus);
    }
    
    private boolean isEqualityOp( ) {
        return token.type().equals(TokenType.Equals) ||
            token.type().equals(TokenType.NotEqual);
    }
    
    private boolean isRelationalOp( ) {
        return token.type().equals(TokenType.Less) ||
               token.type().equals(TokenType.LessEqual) || 
               token.type().equals(TokenType.Greater) ||
               token.type().equals(TokenType.GreaterEqual);
    }
    
    private boolean isType( ) {
        return token.type().equals(TokenType.Int)
            || token.type().equals(TokenType.Bool) 
            || token.type().equals(TokenType.Float)
            || token.type().equals(TokenType.Char);
    }
    
    private boolean isLiteral( ) {
        return token.type().equals(TokenType.IntLiteral) ||
            isBooleanLiteral() ||
            token.type().equals(TokenType.FloatLiteral) ||
            token.type().equals(TokenType.CharLiteral);
    }
    
    private boolean isBooleanLiteral( ) {
        return token.type().equals(TokenType.True) ||
            token.type().equals(TokenType.False);
    }

    private boolean isStatement( ) {
        return token.type().equals(TokenType.Semicolon)||
                token.type().equals(TokenType.LeftBrace)||
                token.type().equals(TokenType.Identifier)||
                token.type().equals(TokenType.If)||
                token.type().equals(TokenType.While)||
                token.type().equals(TokenType.Print);
    }

    public static void main(String[] args) {
        Parser parser  = new Parser(new Lexer("p2.cl"));
        Program prog = parser.program();
        prog.display();           // display abstract syntax tree
    } //main

} // Parser
