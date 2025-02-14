package Compiler;

public enum TokenType {
    // Keywords
    GLOBAL,
    FUNCTION,
    VAR,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    CHARACTER,
    
    // Literals
    INTEGER_LITERAL,
    DECIMAL_LITERAL,
    BOOLEAN_LITERAL,
    CHARACTER_LITERAL,
    IDENTIFIER,
    
    // Operators
    PLUS,
    MINUS,
    MULTIPLY,
    DIVIDE,
    MODULUS,
    EXPONENT,
    ASSIGN,
    
    // Delimiters
    LPAREN,
    RPAREN,
    LBRACE,
    RBRACE,
    SEMICOLON,
    
    // Comments
    SINGLE_LINE_COMMENT,
    MULTI_LINE_COMMENT,
    
    // Special
    UNKNOWN,
    EOF
}