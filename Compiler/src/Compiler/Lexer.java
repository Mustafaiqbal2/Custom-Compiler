package Compiler;

import java.util.*;
import java.util.regex.Matcher;
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

    
    // Define all static patterns first
    private static final Map<String, String> REGEX_PATTERNS;

    static {
        REGEX_PATTERNS = new HashMap<>();
        
        // Keywords - match exact strings
        REGEX_PATTERNS.put("GLOBAL_KEYWORD", "global");
        REGEX_PATTERNS.put("FUNCTION_KEYWORD", "function");
        REGEX_PATTERNS.put("VAR_KEYWORD", "var");
        REGEX_PATTERNS.put("INTEGER_KEYWORD", "integer");
        REGEX_PATTERNS.put("DECIMAL_KEYWORD", "decimal");
        REGEX_PATTERNS.put("BOOLEAN_KEYWORD", "boolean");
        REGEX_PATTERNS.put("CHARACTER_KEYWORD", "character");
        
        // Simple patterns
        REGEX_PATTERNS.put("WHITESPACE", " ");
        REGEX_PATTERNS.put("TAB", "\t");
        REGEX_PATTERNS.put("NEWLINE", "\n");
        
        // Operators as individual patterns
        REGEX_PATTERNS.put("PLUS_OP", "\\+");
        REGEX_PATTERNS.put("MINUS_OP", "\\-");
        REGEX_PATTERNS.put("MULTIPLY_OP", "\\*");
        REGEX_PATTERNS.put("DIVIDE_OP", "\\/");
        REGEX_PATTERNS.put("MODULUS_OP", "\\%");
        REGEX_PATTERNS.put("ASSIGN_OP", "\\=");
        REGEX_PATTERNS.put("EXPONENT_OP", "\\^");
        
        // Delimiters as individual patterns
        REGEX_PATTERNS.put("LPAREN_DELIM", "\\(");
        REGEX_PATTERNS.put("RPAREN_DELIM", "\\)");
        REGEX_PATTERNS.put("LBRACE_DELIM", "\\{");
        REGEX_PATTERNS.put("RBRACE_DELIM", "\\}");
        
        // Build identifier pattern - single letters first, we'll handle longer ones in the lexer
        StringBuilder letters = new StringBuilder();
        for (char c = 'a'; c <= 'z'; c++) {
            if (letters.length() > 0) letters.append("|");
            letters.append(c);
        }
        REGEX_PATTERNS.put("IDENTIFIER", letters.toString());
        
        // Numbers - single digits first, we'll handle sequences in the lexer
        StringBuilder digits = new StringBuilder();
        for (char c = '0'; c <= '9'; c++) {
            if (digits.length() > 0) digits.append("|");
            digits.append(c);
        }
        REGEX_PATTERNS.put("DIGIT", digits.toString());
        
        // Boolean literals
        REGEX_PATTERNS.put("TRUE_LITERAL", "true");
        REGEX_PATTERNS.put("FALSE_LITERAL", "false");
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

                // Handle whitespace specially
                if (tokenType.equals("WHITESPACE")) {
                    for (char c : matchedValue.toCharArray()) {
                        if (c == '\n') {
                            lineNumber++;
                            columnNumber = 1;
                        } else {
                            columnNumber++;
                        }
                    }
                } else {
                    // Create and add token
                    Token token = createToken(tokenType, matchedValue);
                    if (token != null) {
                        tokens.add(token);
                        updateSymbolTable(token);
                    }
                    columnNumber += matchedValue.length();
                }
                currentPosition += matchedValue.length();
            } else {
                // Report error for unrecognized character
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

        // Try each pattern and find the longest match
        for (Map.Entry<String, DFA> entry : dfaPatterns.entrySet()) {
            String patternType = entry.getKey();
            DFA dfa = entry.getValue();
            
            // Skip undefined patterns
            if (dfa == null) continue;
            
            // Find the longest prefix that the DFA accepts
            int length = findLongestAcceptingPrefix(dfa, input);
            
            if (length > maxLength) {
                maxLength = length;
                String matchedValue = input.substring(0, length);
                String tokenType = mapPatternTypeToTokenType(patternType);
                longestMatch = new TokenMatch(tokenType, matchedValue);
            }
        }

        return longestMatch;
    }


    private String mapPatternTypeToTokenType(String patternType) {
        return switch (patternType) {
            case "WHITESPACE", "TAB", "NEWLINE" -> "WHITESPACE";
            case "GLOBAL_KEYWORD" -> "GLOBAL";
            case "FUNCTION_KEYWORD" -> "FUNCTION";
            case "VAR_KEYWORD" -> "VAR";
            case "INTEGER_KEYWORD" -> "INTEGER";
            case "DECIMAL_KEYWORD" -> "DECIMAL";
            case "BOOLEAN_KEYWORD" -> "BOOLEAN";
            case "CHARACTER_KEYWORD" -> "CHARACTER";
            case "IDENTIFIER" -> "IDENTIFIER";
            case "DIGIT" -> "INTEGER_LITERAL";
            case "TRUE_LITERAL", "FALSE_LITERAL" -> "BOOLEAN_LITERAL";
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
            default -> throw new IllegalStateException("Unknown pattern type: " + patternType);
        };
    }

    private int findLongestAcceptingPrefix(DFA dfa, String input) {
        int maxAcceptingPos = 0;
        State currentState = dfa.getStartState();
        
        for (int i = 0; i < input.length() && currentState != null; i++) {
            char c = input.charAt(i);
            Map<Character, State> transitions = dfa.getTransitions(currentState);
            
            // Check if there's a transition for this character
            if (!transitions.containsKey(c)) {
                break;
            }
            
            currentState = transitions.get(c);
            if (currentState != null && dfa.isAccepting(currentState)) {
                maxAcceptingPos = i + 1;
            }
        }
        
        return maxAcceptingPos;
    }

    private Token createOperatorToken(String op) {
        return new Token(switch (op) {
            case "+" -> TokenType.PLUS;
            case "-" -> TokenType.MINUS;
            case "*" -> TokenType.MULTIPLY;
            case "/" -> TokenType.DIVIDE;
            case "%" -> TokenType.MODULUS;
            case "^" -> TokenType.EXPONENT;
            case "=" -> TokenType.ASSIGN;
            default -> TokenType.UNKNOWN;
        }, op, lineNumber, columnNumber);
    }

    private Token createDelimiterToken(String delim) {
        return new Token(switch (delim) {
            case "(" -> TokenType.LPAREN;
            case ")" -> TokenType.RPAREN;
            case "{" -> TokenType.LBRACE;
            case "}" -> TokenType.RBRACE;
            default -> TokenType.UNKNOWN;
        }, delim, lineNumber, columnNumber);
    }
    private void updateSymbolTable(Token token) {
        if (token == null) return;

        switch (token.type) {
            case GLOBAL -> {
                isNextGlobal = true;
                isDeclaration = true;
            }
            case FUNCTION -> {
                isFunctionDeclaration = true;
                currentDeclType = "function";
                isDeclaration = true;
            }
            case INTEGER, DECIMAL, BOOLEAN, CHARACTER -> {
                currentDeclType = token.value.toLowerCase();
                isDeclaration = true;
            }
            case IDENTIFIER -> {
                lastIdentifier = token.value;
                
                if (isDeclaration && currentDeclType != null) {
                    // Only add if this is actually a declaration
                    symbolTable.add(token.value, currentDeclType, isNextGlobal, null);
                    // Reset declaration state immediately after adding the symbol
                    if (!isFunctionDeclaration) {
                        isDeclaration = false;
                        currentDeclType = null;
                        isNextGlobal = false;
                    }
                } else {
                    // This is a reference - just lookup
                    Symbol symbol = symbolTable.lookup(token.value);
                    if (symbol == null) {
                        errorHandler.reportError(token.lineNumber, token.column,
                            "Undefined symbol: " + token.value,
                            ErrorHandler.ErrorType.SEMANTIC);
                    }
                }
            }
            case INTEGER_LITERAL, DECIMAL_LITERAL, BOOLEAN_LITERAL, CHARACTER_LITERAL -> {
                if (lastIdentifier != null) {
                    symbolTable.setValue(lastIdentifier, token.value);
                    lastIdentifier = null;
                }
            }
            case LPAREN -> {
                if (isFunctionDeclaration) {
                    symbolTable.enterFunctionScope();
                }
            }
            case RPAREN -> {
                // Keep function parameter scope
            }
            case LBRACE -> {
                if (isFunctionDeclaration) {
                    isFunctionDeclaration = false;
                    currentDeclType = null;
                } else if (!isNextGlobal) {
                    symbolTable.enterScope();
                }
            }
            case RBRACE -> {
                symbolTable.exitScope();
                isFunctionDeclaration = false;
                isDeclaration = false;
                currentDeclType = null;
                isNextGlobal = false;
                lastIdentifier = null;
            }
            case MULTIPLY, PLUS, MINUS, DIVIDE -> {
                // For operators, we just need to keep lastIdentifier for expressions
                // But make sure we're not in declaration mode
                isDeclaration = false;
                currentDeclType = null;
            }
            case ASSIGN -> {
                // Don't reset anything on assignment, just keep the state
                // but if we're not in a declaration, ensure declaration flags are off
                if (!isDeclaration) {
                    currentDeclType = null;
                }
            }
            default -> {
                if (!token.type.toString().contains("COMMENT")) {
                    if (!isDeclaration) {
                        lastIdentifier = null;
                    }
                }
            }
        }
    }
 // Add this helper method to build identifiers
    private String buildIdentifier(String input, int startPos) {
        StringBuilder identifier = new StringBuilder();
        int pos = startPos;
        
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isLetterOrDigit(c)) {
                identifier.append(c);
                pos++;
            } else {
                break;
            }
        }
        
        return identifier.toString();
    }
 // Add this helper method to build numbers from digits
    private String buildNumber(String input, int startPos) {
        StringBuilder number = new StringBuilder();
        int pos = startPos;
        boolean hasDecimal = false;
        
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isDigit(c)) {
                number.append(c);
                pos++;
            } else if (c == '.' && !hasDecimal) {
                number.append(c);
                hasDecimal = true;
                pos++;
            } else {
                break;
            }
        }
        
        return number.toString();
    }
 // Add method to get current scope
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

// Record to store token match information
record TokenMatch(String type, String value) {}