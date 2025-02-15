package Compiler;

import java.util.*;
import java.util.regex.Pattern;

import Compiler.SymbolTable.Symbol;
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
    private boolean isFunctionDeclaration = false;
    private String lastIdentifier = null;
    private boolean isDeclaration = false;
    private String currentDeclType = null;
    private boolean isNextGlobal = false;
 // Inside your Lexer class, add a new field:
    private String pendingAssignment = null;

    // Use a LinkedHashMap so that insertion order (priority) is preserved.
    private static final Map<String, String> REGEX_PATTERNS;

    static {
        REGEX_PATTERNS = new LinkedHashMap<>();
        
        // Fix multi-line comment pattern
        REGEX_PATTERNS.put("MULTI_LINE_COMMENT", "/\\*([^*]|\\*+[^*/])*\\*+/");
        
        // Fix single-line comment pattern
        REGEX_PATTERNS.put("SINGLE_LINE_COMMENT", "//[^\n]*");
        
        // Fix character literal pattern - allow for escaped characters
        REGEX_PATTERNS.put("CHARACTER_LITERAL", "'([^'\\\\]|\\\\.)'");
        
        // Fix decimal literal pattern
        REGEX_PATTERNS.put("DECIMAL_LITERAL", "[0-9]+\\.[0-9]+");
        
        // Fix arithmetic operators
        REGEX_PATTERNS.put("PLUS_OP", "\\+");
        REGEX_PATTERNS.put("MINUS_OP", "-");
        REGEX_PATTERNS.put("MULTIPLY_OP", "\\*");
        REGEX_PATTERNS.put("DIVIDE_OP", "/");
        REGEX_PATTERNS.put("MODULO_OP", "%");
        REGEX_PATTERNS.put("ASSIGN_OP", "=");
        REGEX_PATTERNS.put("EXPONENT_OP", "\\^");
        
        // Fix parentheses and braces
        REGEX_PATTERNS.put("LPAREN_DELIM", "\\(");
        REGEX_PATTERNS.put("RPAREN_DELIM", "\\)");
        REGEX_PATTERNS.put("LBRACE_DELIM", "\\{");
        REGEX_PATTERNS.put("RBRACE_DELIM", "\\}");
        REGEX_PATTERNS.put("SEMICOLON", ";");
        
        // Keep existing patterns
        REGEX_PATTERNS.put("WHITESPACE", "[ \t\n]+");
        REGEX_PATTERNS.put("BOOLEAN_LITERAL", "(true|false)");
        REGEX_PATTERNS.put("INTEGER_LITERAL", "[0-9]+");
        REGEX_PATTERNS.put("IDENTIFIER", "[A-Za-z_][A-Za-z0-9_]*");
        
        
        
        
        // Keywords
        REGEX_PATTERNS.put("GLOBAL_KEYWORD", "global");
        REGEX_PATTERNS.put("FUNCTION_KEYWORD", "function");
        REGEX_PATTERNS.put("VAR_KEYWORD", "var");
        REGEX_PATTERNS.put("INTEGER_KEYWORD", "integer");
        REGEX_PATTERNS.put("DECIMAL_KEYWORD", "decimal");
        REGEX_PATTERNS.put("BOOLEAN_KEYWORD", "boolean");
        REGEX_PATTERNS.put("CHARACTER_KEYWORD", "character");

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

        for (Map.Entry<String, String> entry : REGEX_PATTERNS.entrySet()) {
            try {
                NFA nfa = converter.convert(entry.getValue());
                nfaPatterns.put(entry.getKey(), nfa);
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
        currentPosition = 0;
        lineNumber = 1;
        columnNumber = 1;
        tokens.clear();

        while (currentPosition < input.length()) {
            String remaining = input.substring(currentPosition);
            TokenMatch match = findLongestMatch(remaining);

            if (match != null) {
                String matchedValue = match.value();
                String tokenType = match.type();

                // Skip whitespace and comments.
                if (tokenType.equals("WHITESPACE") ||
                    tokenType.equals("SINGLE_LINE_COMMENT") ||
                    tokenType.equals("MULTI_LINE_COMMENT")) {
                    for (char c : matchedValue.toCharArray()) {
                        if (c == '\n') {
                            lineNumber++;
                            columnNumber = 1;
                        } else {
                            columnNumber++;
                        }
                    }
                } else {
                    Token token = createToken(tokenType, matchedValue);
                    if (token != null) {
                        tokens.add(token);
                        updateSymbolTable(token);
                    }
                    columnNumber += matchedValue.length();
                }
                currentPosition += matchedValue.length();
            } else {
                errorHandler.reportError(lineNumber, columnNumber,
                    "Invalid token at position " + currentPosition,
                    ErrorHandler.ErrorType.LEXICAL);
                currentPosition++;
                columnNumber++;
            }
        }
        tokens.add(new Token(TokenType.EOF, "", lineNumber, columnNumber));
    }

    private Token createToken(String type, String value) {
        try {
            TokenType tokenType = TokenType.valueOf(type);
            return new Token(tokenType, value, lineNumber, columnNumber);
        } catch (IllegalArgumentException e) {
            errorHandler.reportError(lineNumber, columnNumber,
                "Invalid token type: " + type,
                ErrorHandler.ErrorType.INTERNAL);
            return null;
        }
    }

    private TokenMatch findLongestMatch(String input) {
        TokenMatch longestMatch = null;
        int maxLength = 0;

        // Check for comments first (they have highest priority)
        if (input.startsWith("//")) {
            int endIdx = input.indexOf('\n');
            if (endIdx == -1) endIdx = input.length();
            return new TokenMatch("SINGLE_LINE_COMMENT", input.substring(0, endIdx));
        }
        
        if (input.startsWith("/*")) {
            int endIdx = input.indexOf("*/");
            if (endIdx != -1) {
                return new TokenMatch("MULTI_LINE_COMMENT", input.substring(0, endIdx + 2));
            }
        }

        // Try each DFA pattern
        for (Map.Entry<String, DFA> entry : dfaPatterns.entrySet()) {
            String patternType = entry.getKey();
            DFA dfa = entry.getValue();
            
            if (dfa == null) continue;
            
            int length = findLongestAcceptingPrefix(dfa, input);
            if (length > maxLength) {
                maxLength = length;
                String matchedValue = input.substring(0, length);
                String tokenType = patternType;
                
                // Handle special cases for literals
                if (patternType.equals("INTEGER_LITERAL") || 
                    patternType.equals("DECIMAL_LITERAL") ||
                    patternType.equals("BOOLEAN_LITERAL") ||
                    patternType.equals("CHARACTER_LITERAL")) {
                    tokenType = patternType;  // Keep the literal type
                } else {
                    tokenType = mapPatternTypeToTokenType(patternType);
                }
                
                longestMatch = new TokenMatch(tokenType, matchedValue);
            }
        }
        
        return longestMatch;
    }

    private String mapPatternTypeToTokenType(String patternType) {
        return switch (patternType) {
            case "WHITESPACE" -> "WHITESPACE";
            case "SINGLE_LINE_COMMENT" -> "SINGLE_LINE_COMMENT";
            case "MULTI_LINE_COMMENT" -> "MULTI_LINE_COMMENT";
            case "BOOLEAN_LITERAL" -> "BOOLEAN_LITERAL";
            case "CHARACTER_LITERAL" -> "CHARACTER_LITERAL";
            case "GLOBAL_KEYWORD" -> "GLOBAL";
            case "FUNCTION_KEYWORD" -> "FUNCTION";
            case "VAR_KEYWORD" -> "VAR";
            case "INTEGER_KEYWORD" -> "INTEGER";
            case "DECIMAL_KEYWORD" -> "DECIMAL";
            case "BOOLEAN_KEYWORD" -> "BOOLEAN";
            case "CHARACTER_KEYWORD" -> "CHARACTER";
            case "NUMBER" -> "INTEGER_LITERAL"; // (Handled specially in findLongestMatch)
            case "IDENTIFIER" -> "IDENTIFIER";
            case "PLUS_OP" -> "PLUS";
            case "MINUS_OP" -> "MINUS";
            case "MULTIPLY_OP" -> "MULTIPLY";
            case "DIVIDE_OP" -> "DIVIDE";
            case "MODULUS_OP" -> "MODULUS";
            case "ASSIGN_OP" -> "ASSIGN";
            case "EXPONENT_OP" -> "EXPONENT";
            case "LPAREN_DELIM" -> "LPAREN";
            case "RPAREN_DELIM" -> "RPAREN";
            case "LBRACE_DELIM" -> "LBRACE";
            case "RBRACE_DELIM" -> "RBRACE";
            case "SEMICOLON" -> "SEMICOLON";
            default -> "UNKNOWN";
        };
    }

    private int findLongestAcceptingPrefix(DFA dfa, String input) {
        int maxAcceptingPos = 0;
        State currentState = dfa.getStartState();

        for (int i = 0; i < input.length() && currentState != null; i++) {
            char c = input.charAt(i);
            Map<Character, State> transitions = dfa.getTransitions(currentState);
            if (!transitions.containsKey(c))
                break;
            currentState = transitions.get(c);
            if (currentState != null && dfa.isAccepting(currentState))
                maxAcceptingPos = i + 1;
        }
        return maxAcceptingPos;
    }

 // In Lexer.java, modify updateSymbolTable:
    private void updateSymbolTable(Token token) {
        if (token == null) return;

        switch (token.type) {
            case GLOBAL -> {
                isNextGlobal = true;
                isDeclaration = true;
                currentDeclType = null;  // Reset current type
            }
            case FUNCTION -> {
                isFunctionDeclaration = true;
                isDeclaration = true;
                currentDeclType = "function";
            }
            case INTEGER, DECIMAL, BOOLEAN, CHARACTER -> {
                currentDeclType = token.type.toString().toLowerCase();
                isDeclaration = true;
            }
            case IDENTIFIER -> {
                lastIdentifier = token.value;
                if (isDeclaration && currentDeclType != null) {
                    System.out.println("Adding symbol: " + token.value + " type: " + currentDeclType + " global: " + isNextGlobal); // Debug
                    symbolTable.add(token.value, currentDeclType, isNextGlobal, null);
                    
                    // Only reset declaration state if not in function declaration
                    if (!isFunctionDeclaration) {
                        isDeclaration = false;
                        currentDeclType = null;
                        isNextGlobal = false;
                    }
                }
            }
            case ASSIGN -> {
                // Don't reset declaration state on assignment
                pendingAssignment = lastIdentifier;
            }
            case INTEGER_LITERAL, DECIMAL_LITERAL, BOOLEAN_LITERAL, CHARACTER_LITERAL -> {
                if (pendingAssignment != null) {
                    symbolTable.setValue(pendingAssignment, token.value);
                    pendingAssignment = null;
                }
            }
            case LBRACE -> {
                if (isFunctionDeclaration) {
                    symbolTable.enterScope();
                    isFunctionDeclaration = false;
                }
            }
            case RBRACE -> {
                symbolTable.exitScope();
                isFunctionDeclaration = false;
                isDeclaration = false;
                currentDeclType = null;
                isNextGlobal = false;
                lastIdentifier = null;
                pendingAssignment = null;
            }
        }
    }


    public int getCurrentScope() {
        return symbolTable.getCurrentScope();
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

// Record to store token match information.
record TokenMatch(String type, String value) {}
