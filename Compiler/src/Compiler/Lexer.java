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

    private static class TokenPattern {
        final String type;
        final Pattern pattern;
        final boolean skip;

        TokenPattern(String type, String regex, boolean skip) {
            this.type = type;
            this.pattern = Pattern.compile("^(" + regex + ")");
            this.skip = skip;
        }
    }
    // Define all static patterns first
    private static final Map<String, String> REGEX_PATTERNS;
    private static final List<TokenPattern> TOKEN_PATTERNS;

    static {
        // Initialize REGEX_PATTERNS first
        REGEX_PATTERNS = new HashMap<>();
        REGEX_PATTERNS.put("WHITESPACE", "([ \t\r\n])");
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

        // Initialize TOKEN_PATTERNS after
        TOKEN_PATTERNS = new ArrayList<>();
        TOKEN_PATTERNS.add(new TokenPattern("WHITESPACE", "[ \t\r\n]+", true));
        TOKEN_PATTERNS.add(new TokenPattern("MULTI_LINE_COMMENT", "/[*]([^*]|[*]+[^*/])*[*]+/", false));
        TOKEN_PATTERNS.add(new TokenPattern("SINGLE_LINE_COMMENT", "//[^\n]*", false));
        TOKEN_PATTERNS.add(new TokenPattern("KEYWORD", "\\b(global|function|var|integer|decimal|boolean|character)\\b", false));
        TOKEN_PATTERNS.add(new TokenPattern("DECIMAL_LITERAL", "\\b\\d+\\.\\d+\\b", false));
        TOKEN_PATTERNS.add(new TokenPattern("INTEGER_LITERAL", "\\b\\d+\\b", false));
        TOKEN_PATTERNS.add(new TokenPattern("BOOLEAN_LITERAL", "\\b(true|false)\\b", false));
        TOKEN_PATTERNS.add(new TokenPattern("CHARACTER_LITERAL", "'[a-z]'", false));
        TOKEN_PATTERNS.add(new TokenPattern("IDENTIFIER", "\\b[a-z][a-z0-9]*\\b", false));
        TOKEN_PATTERNS.add(new TokenPattern("OPERATOR", "[+\\-*/%^=]", false));
        TOKEN_PATTERNS.add(new TokenPattern("DELIMITER", "[(){}]", false));
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
                processToken(bestMatch);
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
    
    private void processToken(TokenMatch match) {
        String type = match.type();
        String value = match.value();

        TokenPattern matchedPattern = TOKEN_PATTERNS.stream()
            .filter(p -> p.type.equals(type))
            .findFirst()
            .orElse(null);

        if (matchedPattern != null && matchedPattern.skip) {
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

        for (TokenPattern pattern : TOKEN_PATTERNS) {
            Matcher matcher = pattern.pattern.matcher(input);
            if (matcher.find()) {
                String value = matcher.group(1);
                if (value.length() > maxLength) {
                    maxLength = value.length();
                    longestMatch = new TokenMatch(pattern.type, value);
                }
            }
        }

        return longestMatch;
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
                isDeclaration = true;  // Also set declaration flag for global
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
                    // This is a declaration (either function or variable)
                    boolean isGlobal = isNextGlobal || currentDeclType.equals("function");
                    symbolTable.add(token.value, currentDeclType, isGlobal, null);
                } else {
                    // This is a reference
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
                    if (!isFunctionDeclaration) {
                        // Reset declaration flags after value assignment for variables
                        isDeclaration = false;
                        currentDeclType = null;
                        isNextGlobal = false;
                    }
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
                    // We're already in function scope from LPAREN
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
                // Don't reset lastIdentifier for expressions
            }
            case ASSIGN -> {
                // Don't reset states during assignment
            }
            default -> {
                if (!token.type.toString().contains("COMMENT")) {
                    if (!isDeclaration && !token.type.toString().contains("LITERAL")) {
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