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
        REGEX_PATTERNS.put("WHITESPACE", "[ \t\r\n]");
        REGEX_PATTERNS.put("KEYWORD", "(global|function|var|integer|decimal|boolean|character)");
        REGEX_PATTERNS.put("IDENTIFIER", "[a-z][a-z0-9]*");
        REGEX_PATTERNS.put("INTEGER", "[0-9]+");
        REGEX_PATTERNS.put("DECIMAL", "[0-9]+[.][0-9]+");
        REGEX_PATTERNS.put("BOOLEAN", "(true|false)");
        REGEX_PATTERNS.put("CHARACTER", "'[a-z]'");
        REGEX_PATTERNS.put("OPERATOR", "[+\\-*/%=^]");
        REGEX_PATTERNS.put("DELIMITER", "[(){}]");
        REGEX_PATTERNS.put("SINGLECOMMENT", "//[^\n]*");
        REGEX_PATTERNS.put("MULTICOMMENT", "/[*]([^*]|[*]+[^*/])*[*]+/");
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
        // Reset all state
        currentDeclType = null;
        isNextGlobal = false;
        isFunctionDeclaration = false;
        lastIdentifier = null;

        while (currentPosition < input.length()) {
            String remaining = input.substring(currentPosition);
            boolean matched = false;

            TokenMatch bestMatch = findLongestMatch(remaining);
            
            if (bestMatch != null) {
                // Create and process the token directly
                Token token = createToken(bestMatch.type(), bestMatch.value());
                if (token != null) {
                    if (!isWhitespaceOrComment(token.type)) {
                        tokens.add(token);
                        updateSymbolTable(token);
                    }
                    if (token.type == TokenType.NEWLINE) {
                        lineNumber++;
                        columnNumber = 1;
                    } else {
                        columnNumber += bestMatch.value().length();
                    }
                }
                currentPosition += bestMatch.value().length();
                matched = true;
            }

            if (!matched) {
                errorHandler.reportError(lineNumber, columnNumber,
                    "Invalid token at position " + currentPosition,
                    ErrorHandler.ErrorType.LEXICAL);
                currentPosition++;
                columnNumber++;
            }
        }

        tokens.add(new Token(TokenType.EOF, "", lineNumber, columnNumber));
    }
    
    private boolean isWhitespaceOrComment(TokenType type) {
        return type == TokenType.WHITESPACE || 
               type == TokenType.SINGLE_LINE_COMMENT || 
               type == TokenType.MULTI_LINE_COMMENT;
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
            case "INTEGER_LITERAL" -> new Token(TokenType.INTEGER_LITERAL, value, lineNumber, columnNumber);
            case "DECIMAL_LITERAL" -> new Token(TokenType.DECIMAL_LITERAL, value, lineNumber, columnNumber);
            case "BOOLEAN_LITERAL" -> new Token(TokenType.BOOLEAN_LITERAL, value, lineNumber, columnNumber);
            case "CHARACTER_LITERAL" -> new Token(TokenType.CHARACTER_LITERAL, value, lineNumber, columnNumber);
            case "OPERATOR" -> createOperatorToken(value);
            case "DELIMITER" -> createDelimiterToken(value);
            case "SINGLE_LINE_COMMENT" -> new Token(TokenType.SINGLE_LINE_COMMENT, value, lineNumber, columnNumber);
            case "MULTI_LINE_COMMENT" -> new Token(TokenType.MULTI_LINE_COMMENT, value, lineNumber, columnNumber);
            default -> null;
        };
    }
    private TokenMatch findLongestMatch(String input) {
        TokenMatch longestMatch = null;
        int maxLength = 0;

        // Try each pattern and find the longest match
        for (Map.Entry<String, DFA> entry : dfaPatterns.entrySet()) {
            String patternType = entry.getKey();
            DFA dfa = entry.getValue();
            
            // Find the longest prefix that the DFA accepts
            int length = findLongestAcceptingPrefix(dfa, input);
            
            if (length > maxLength) {
                maxLength = length;
                String matchedValue = input.substring(0, length);
                
                // Map DFA pattern types to token pattern types
                String tokenType = mapPatternTypeToTokenType(patternType);
                longestMatch = new TokenMatch(tokenType, matchedValue);
            }
        }

        return longestMatch;
    }
    
    private String mapPatternTypeToTokenType(String patternType) {
        return switch (patternType) {
            case "WHITESPACE" -> "WHITESPACE";
            case "KEYWORD" -> "KEYWORD";
            case "IDENTIFIER" -> "IDENTIFIER";
            case "INTEGER" -> "INTEGER_LITERAL";
            case "DECIMAL" -> "DECIMAL_LITERAL";
            case "BOOLEAN" -> "BOOLEAN_LITERAL";
            case "CHARACTER" -> "CHARACTER_LITERAL";
            case "OPERATOR" -> "OPERATOR";
            case "DELIMITER" -> "DELIMITER";
            case "SINGLECOMMENT" -> "SINGLE_LINE_COMMENT";
            case "MULTICOMMENT" -> "MULTI_LINE_COMMENT";
            default -> throw new IllegalStateException("Unknown pattern type: " + patternType);
        };
    }

    private int findLongestAcceptingPrefix(DFA dfa, String input) {
        int maxAcceptingPos = 0;
        State currentState = dfa.getStartState();
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            Map<Character, State> transitions = dfa.getTransitions(currentState);
            if (!transitions.containsKey(c)) {
                break; // No valid transition
            }
            
            currentState = transitions.get(c);
            
            if (dfa.isAccepting(currentState)) {
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