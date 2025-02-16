package Compiler;

public class Token {
    public final TokenType type;
    public final String value;
    public final int lineNumber;
    public final int column;
    
    public Token(TokenType type, String value, int lineNumber, int column) {
        this.type = type;
        this.value = value;
        this.lineNumber = lineNumber;
        this.column = column;
    }
    
    public Token(String type, String value, int lineNumber) {
        this(TokenType.valueOf(type), value, lineNumber, 0);
    }

	public String getValue() {
		return value;
	}
    
    @Override
    public String toString() {
        return String.format("Token[type=%s, value='%s', position=(%d,%d)]", 
                           type, value, lineNumber, column);
    }
}