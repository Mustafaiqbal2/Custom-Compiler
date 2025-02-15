package Compiler;

import java.util.*;
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
        
        // Whitespace and comments (highest priority)
        REGEX_PATTERNS.put("WHITESPACE", "[ \t\n]+");
        REGEX_PATTERNS.put("MULTI_LINE_COMMENT", "/\\*([^*]|\\*+[^*/])*\\*+/");
        REGEX_PATTERNS.put("SINGLE_LINE_COMMENT", "//[^\n]*");
        
        // Keywords must come before identifiers and must match exactly
        REGEX_PATTERNS.put("GLOBAL", "global[^A-Za-z0-9_]");
        REGEX_PATTERNS.put("FUNCTION", "function[^A-Za-z0-9_]");
        REGEX_PATTERNS.put("VAR", "var[^A-Za-z0-9_]");
        REGEX_PATTERNS.put("INTEGER", "integer[^A-Za-z0-9_]");
        REGEX_PATTERNS.put("DECIMAL", "decimal[^A-Za-z0-9_]");
        REGEX_PATTERNS.put("BOOLEAN", "boolean[^A-Za-z0-9_]");
        REGEX_PATTERNS.put("CHARACTER", "character[^A-Za-z0-9_]");
        
        // Literals
        REGEX_PATTERNS.put("DECIMAL_LITERAL", "[0-9]+\\.[0-9]+");  // Must come before INTEGER_LITERAL
        REGEX_PATTERNS.put("INTEGER_LITERAL", "[0-9]+");
        REGEX_PATTERNS.put("BOOLEAN_LITERAL", "true[^A-Za-z0-9_]|false[^A-Za-z0-9_]");
        REGEX_PATTERNS.put("CHARACTER_LITERAL", "'[^'\\\\]'|'\\\\[ntr\\\\]'");
        
        // Operators
        REGEX_PATTERNS.put("MULTIPLY", "\\*");
        REGEX_PATTERNS.put("PLUS", "\\+");
        REGEX_PATTERNS.put("MINUS", "-");
        REGEX_PATTERNS.put("DIVIDE", "/");
        REGEX_PATTERNS.put("MODULO", "%");
        REGEX_PATTERNS.put("EXPONENT", "\\^");
        REGEX_PATTERNS.put("ASSIGN", "=");
        
        // Delimiters
        REGEX_PATTERNS.put("LPAREN", "\\(");
        REGEX_PATTERNS.put("RPAREN", "\\)");
        REGEX_PATTERNS.put("LBRACE", "\\{");
        REGEX_PATTERNS.put("RBRACE", "\\}");
        REGEX_PATTERNS.put("SEMICOLON", ";");
        
        // Identifier must come last
        REGEX_PATTERNS.put("IDENTIFIER", "[A-Za-z_][A-Za-z0-9_]*");
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
            // Handle special cases first
            if (type.equals("INTEGER_LITERAL")) {
                return new Token(TokenType.INTEGER_LITERAL, value, lineNumber, columnNumber);
            }
            if (type.equals("DECIMAL_LITERAL")) {
                return new Token(TokenType.DECIMAL_LITERAL, value, lineNumber, columnNumber);
            }
            if (type.equals("CHARACTER_LITERAL")) {
                // Strip quotes from character literals
                String charValue = value;
                if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
                    charValue = value.substring(1, value.length() - 1);
                }
                return new Token(TokenType.CHARACTER_LITERAL, charValue, lineNumber, columnNumber);
            }
            if (type.equals("BOOLEAN_LITERAL")) {
                return new Token(TokenType.BOOLEAN_LITERAL, value, lineNumber, columnNumber);
            }

            // Direct conversion for other types
            return new Token(TokenType.valueOf(type), value, lineNumber, columnNumber);
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

        // Handle comments first
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

        // Try each pattern in order of definition
        for (Map.Entry<String, DFA> entry : dfaPatterns.entrySet()) {
            String patternType = entry.getKey();
            DFA dfa = entry.getValue();
            
            if (dfa == null) continue;
            
            int length = findLongestAcceptingPrefix(dfa, input);
            if (length > maxLength) {
                String matchedValue = input.substring(0, length);
                
                // Special handling for decimal literals to ensure they're not split
                if (patternType.equals("DECIMAL_LITERAL")) {
                    if (matchedValue.contains(".")) {
                        maxLength = length;
                        longestMatch = new TokenMatch(patternType, matchedValue);
                        continue;
                    }
                }
                
                // Handle all other cases
                maxLength = length;
                longestMatch = new TokenMatch(patternType, matchedValue);
            }
        }
        
        return longestMatch;
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
