package Compiler;

import java.util.*;
import java.util.regex.*;

public class Lexer {
    private String input;
    private List<Token> tokens;
    private int lineNumber;
    private int columnNumber;

    private static final Pattern TOKEN_PATTERNS = Pattern.compile(
        "(?<WHITESPACE>\\s+)|" +
        "(?<KEYWORD>global|function|var|integer|decimal|boolean|character)|" +
        "(?<IDENTIFIER>[a-z][a-z]*)|" +
        "(?<INTEGER>\\d+)|" +
        "(?<DECIMAL>\\d+\\.\\d{1,5})|" +
        "(?<BOOLEAN>true|false)|" +
        "(?<CHARACTER>'[a-z]')|" +
        "(?<OPERATOR>[+\\-*/%=^])|" +
        "(?<DELIMITER>[(){}])|" +
        "(?<SINGLE_COMMENT>//[^\\n]*)|" +
        "(?<MULTI_COMMENT>/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/>)"  // Fixed the regex here
    );

    public Lexer(String input) {
        this.input = input;
        this.tokens = new ArrayList<>();
        this.lineNumber = 1;
        this.columnNumber = 1;
    }

    public void tokenize() {
        Matcher matcher = TOKEN_PATTERNS.matcher(input);
        int lastEnd = 0;

        while (matcher.find()) {
            // Update column counter
            columnNumber += matcher.start() - lastEnd;
            
            if (matcher.group("WHITESPACE") != null) {
                // Count newlines in whitespace
                String ws = matcher.group("WHITESPACE");
                for (char c : ws.toCharArray()) {
                    if (c == '\n') {
                        lineNumber++;
                        columnNumber = 1;
                    }
                }
            }
            else if (matcher.group("KEYWORD") != null) {
                tokens.add(new Token(TokenType.valueOf(matcher.group("KEYWORD").toUpperCase()), 
                                   matcher.group("KEYWORD"), lineNumber, columnNumber));
            }
            else if (matcher.group("IDENTIFIER") != null) {
                tokens.add(new Token(TokenType.IDENTIFIER, 
                                   matcher.group("IDENTIFIER"), lineNumber, columnNumber));
            }
            else if (matcher.group("INTEGER") != null) {
                tokens.add(new Token(TokenType.INTEGER_LITERAL, 
                                   matcher.group("INTEGER"), lineNumber, columnNumber));
            }
            else if (matcher.group("DECIMAL") != null) {
                tokens.add(new Token(TokenType.DECIMAL_LITERAL, 
                                   matcher.group("DECIMAL"), lineNumber, columnNumber));
            }
            else if (matcher.group("BOOLEAN") != null) {
                tokens.add(new Token(TokenType.BOOLEAN_LITERAL, 
                                   matcher.group("BOOLEAN"), lineNumber, columnNumber));
            }
            else if (matcher.group("CHARACTER") != null) {
                tokens.add(new Token(TokenType.CHARACTER_LITERAL, 
                                   matcher.group("CHARACTER"), lineNumber, columnNumber));
            }
            else if (matcher.group("OPERATOR") != null) {
                String op = matcher.group("OPERATOR");
                TokenType type = switch(op) {
                    case "+" -> TokenType.PLUS;
                    case "-" -> TokenType.MINUS;
                    case "*" -> TokenType.MULTIPLY;
                    case "/" -> TokenType.DIVIDE;
                    case "%" -> TokenType.MODULUS;
                    case "^" -> TokenType.EXPONENT;
                    case "=" -> TokenType.ASSIGN;
                    default -> TokenType.UNKNOWN;
                };
                tokens.add(new Token(type, op, lineNumber, columnNumber));
            }
            else if (matcher.group("DELIMITER") != null) {
                String delim = matcher.group("DELIMITER");
                TokenType type = switch(delim) {
                    case "(" -> TokenType.LPAREN;
                    case ")" -> TokenType.RPAREN;
                    case "{" -> TokenType.LBRACE;
                    case "}" -> TokenType.RBRACE;
                    default -> TokenType.UNKNOWN;
                };
                tokens.add(new Token(type, delim, lineNumber, columnNumber));
            }
            else if (matcher.group("SINGLE_COMMENT") != null) {
                tokens.add(new Token(TokenType.SINGLE_LINE_COMMENT, 
                                   matcher.group("SINGLE_COMMENT"), lineNumber, columnNumber));
            }
            else if (matcher.group("MULTI_COMMENT") != null) {
                tokens.add(new Token(TokenType.MULTI_LINE_COMMENT, 
                                   matcher.group("MULTI_COMMENT"), lineNumber, columnNumber));
            }
            else {
                tokens.add(new Token(TokenType.UNKNOWN, 
                                   matcher.group(), lineNumber, columnNumber));
            }
            
            lastEnd = matcher.end();
        }
        
        // Add EOF token
        tokens.add(new Token(TokenType.EOF, "", lineNumber, columnNumber));
    }

    public List<Token> getTokens() {
        return Collections.unmodifiableList(tokens);
    }
}