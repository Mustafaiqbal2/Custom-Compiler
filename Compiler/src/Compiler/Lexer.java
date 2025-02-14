package Compiler;

import java.util.*;
import java.util.regex.*;
import Compiler.automata.*;

public class Lexer {
    private String input;
    private List<Token> tokens;
    private int lineNumber;
    private int columnNumber;
    private int currentPosition;
    private SymbolTable symbolTable;
    private ErrorHandler errorHandler;
    private Map<String, NFA> nfaPatterns;
    private Map<String, DFA> dfaPatterns;

 // Keeping the original regex patterns as they are well tested
    private static final Map<String, String> REGEX_PATTERNS;
    static {
        REGEX_PATTERNS = new HashMap<>();
        REGEX_PATTERNS.put("WHITESPACE", "\\s+");
        REGEX_PATTERNS.put("KEYWORD", "global|function|var|integer|decimal|boolean|character");
        REGEX_PATTERNS.put("IDENTIFIER", "[a-z][a-z]*");
        REGEX_PATTERNS.put("INTEGER", "\\d+");
        REGEX_PATTERNS.put("DECIMAL", "\\d+\\.\\d{1,5}");
        REGEX_PATTERNS.put("BOOLEAN", "true|false");
        REGEX_PATTERNS.put("CHARACTER", "'[a-z]'");
        REGEX_PATTERNS.put("OPERATOR", "[+\\-*/%=^]");
        REGEX_PATTERNS.put("DELIMITER", "[(){}]");
        REGEX_PATTERNS.put("SINGLECOMMENT", "//[^\\n]*");
        REGEX_PATTERNS.put("MULTICOMMENT", "/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/");
    }

    public Lexer(String input) {
        this.input = input;
        this.tokens = new ArrayList<>();
        this.lineNumber = 1;
        this.columnNumber = 1;
        this.currentPosition = 0;
        this.errorHandler = new ErrorHandler();
        this.symbolTable = new SymbolTable(errorHandler);
        this.nfaPatterns = new HashMap<>();
        this.dfaPatterns = new HashMap<>();
        convertRegexToDFA();
    }

    private void convertRegexToDFA() {
        RegexToNFAConverter converter = new RegexToNFAConverter();
        
        // Convert each regex pattern to NFA using Thompson's Construction
        for (Map.Entry<String, String> entry : REGEX_PATTERNS.entrySet()) {
            try {
                NFA nfa = converter.convert(entry.getValue());
                nfaPatterns.put(entry.getKey(), nfa);
                // Convert NFA to DFA using subset construction
                DFA dfa = nfa.toDFA();
                dfaPatterns.put(entry.getKey(), dfa);
            } catch (Exception e) {
                errorHandler.reportError(0, 0, 
                    "Failed to convert pattern " + entry.getKey() + ": " + e.getMessage(),
                    ErrorHandler.ErrorType.INTERNAL);
            }
        }
    }

    public void tokenize() {
        while (currentPosition < input.length()) {
            boolean matched = false;
            String remaining = input.substring(currentPosition);

            // Try each DFA for matching
            TokenMatch bestMatch = findLongestMatch(remaining);

            if (bestMatch != null) {
                processToken(bestMatch);
                matched = true;
            }

            if (!matched) {
                // Report error for unrecognized token
                errorHandler.reportError(lineNumber, columnNumber, 
                    "Invalid token at position " + currentPosition,
                    ErrorHandler.ErrorType.LEXICAL);
                currentPosition++;
                columnNumber++;
            }
        }

        tokens.add(new Token(TokenType.EOF, "", lineNumber, columnNumber));
    }

    private TokenMatch findLongestMatch(String input) {
        TokenMatch longestMatch = null;
        int maxLength = 0;

        for (Map.Entry<String, DFA> entry : dfaPatterns.entrySet()) {
            String prefix = findLongestAcceptedPrefix(input, entry.getValue());
            if (prefix != null && prefix.length() > maxLength) {
                maxLength = prefix.length();
                longestMatch = new TokenMatch(entry.getKey(), prefix);
            }
        }

        return longestMatch;
    }

    private String findLongestAcceptedPrefix(String input, DFA dfa) {
        String longestAccepted = null;
        for (int i = 1; i <= input.length(); i++) {
            String prefix = input.substring(0, i);
            if (dfa.accepts(prefix)) {
                longestAccepted = prefix;
            }
        }
        return longestAccepted;
    }

    private void processToken(TokenMatch match) {
        String type = match.type();
        String value = match.value();

        if (type.equals("WHITESPACE")) {
            handleWhitespace(value);
        } else {
            Token token = createToken(type, value);
            if (token != null) {
                tokens.add(token);
                updateSymbolTable(token);
            }
            columnNumber += value.length();
        }
        
        currentPosition += value.length();
    }

    private void handleWhitespace(String ws) {
        for (char c : ws.toCharArray()) {
            if (c == '\n') {
                lineNumber++;
                columnNumber = 1;
            } else {
                columnNumber++;
            }
        }
    }

    private Token createToken(String type, String value) {
        return switch (type) {
            case "KEYWORD" -> new Token(TokenType.valueOf(value.toUpperCase()), value, lineNumber, columnNumber);
            case "IDENTIFIER" -> new Token(TokenType.IDENTIFIER, value, lineNumber, columnNumber);
            case "INTEGER" -> new Token(TokenType.INTEGER_LITERAL, value, lineNumber, columnNumber);
            case "DECIMAL" -> new Token(TokenType.DECIMAL_LITERAL, value, lineNumber, columnNumber);
            case "BOOLEAN" -> new Token(TokenType.BOOLEAN_LITERAL, value, lineNumber, columnNumber);
            case "CHARACTER" -> new Token(TokenType.CHARACTER_LITERAL, value, lineNumber, columnNumber);
            case "OPERATOR" -> createOperatorToken(value);
            case "DELIMITER" -> createDelimiterToken(value);
            case "SINGLECOMMENT" -> new Token(TokenType.SINGLE_LINE_COMMENT, value, lineNumber, columnNumber);
            case "MULTICOMMENT" -> new Token(TokenType.MULTI_LINE_COMMENT, value, lineNumber, columnNumber);
            default -> new Token(TokenType.UNKNOWN, value, lineNumber, columnNumber);
        };
    }

    private Token createOperatorToken(String op) {
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
        return new Token(type, op, lineNumber, columnNumber);
    }

    private Token createDelimiterToken(String delim) {
        TokenType type = switch(delim) {
            case "(" -> TokenType.LPAREN;
            case ")" -> TokenType.RPAREN;
            case "{" -> TokenType.LBRACE;
            case "}" -> TokenType.RBRACE;
            default -> TokenType.UNKNOWN;
        };
        return new Token(type, delim, lineNumber, columnNumber);
    }

    private void updateSymbolTable(Token token) {
        switch (token.type) {
            case IDENTIFIER:
                if (isDeclaration()) {
                    String type = getCurrentDeclarationType();
                    boolean isGlobal = isInGlobalScope();
                    symbolTable.add(token.value, type, isGlobal, null);
                }
                break;
            case INTEGER_LITERAL:
            case DECIMAL_LITERAL:
            case BOOLEAN_LITERAL:
            case CHARACTER_LITERAL:
                String lastIdentifier = getLastIdentifier();
                if (lastIdentifier != null) {
                    symbolTable.setValue(lastIdentifier, token.value);
                }
                break;
		default:
			break;
        }
    }

    private boolean isDeclaration() {
        if (tokens.isEmpty()) return false;
        Token lastToken = tokens.get(tokens.size() - 1);
        return lastToken.type == TokenType.INTEGER || 
               lastToken.type == TokenType.DECIMAL ||
               lastToken.type == TokenType.BOOLEAN ||
               lastToken.type == TokenType.CHARACTER;
    }

    private String getCurrentDeclarationType() {
        if (tokens.isEmpty()) return null;
        return tokens.get(tokens.size() - 1).value;
    }

    private boolean isInGlobalScope() {
        return tokens.stream()
            .anyMatch(t -> t.type == TokenType.GLOBAL);
    }

    private String getLastIdentifier() {
        for (int i = tokens.size() - 1; i >= 0; i--) {
            if (tokens.get(i).type == TokenType.IDENTIFIER) {
                return tokens.get(i).value;
            }
        }
        return null;
    }

    public List<Token> getTokens() {
        return Collections.unmodifiableList(tokens);
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }
}

// Record to store token match information
record TokenMatch(String type, String value) {}