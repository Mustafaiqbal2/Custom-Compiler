package Compiler;
/////////
public class Token {
    public TokenType type;
    public String value;
    public int lineNumber;
    
    public Token(TokenType type, String value, int lineNumber) {
        this.type = type;
        this.value = value;
        this.lineNumber = lineNumber;
    }
}