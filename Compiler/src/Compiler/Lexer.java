package Compiler;

import Compiler.automata.*;
import java.util.*;

public class Lexer {
    private String input;
    private List<Token> tokens;
    private int currentPosition;
    private int lineNumber;
    private SymbolTable symbolTable;
    private ErrorHandler errorHandler;
    private NFA currentTokenNFA;
    private DFA currentTokenDFA;
    private String currentLexeme;
    
    public Lexer(String input) {
        this.input = input;
        this.tokens = new ArrayList<>();
        this.currentPosition = 0;
        this.lineNumber = 1;
        this.errorHandler = new ErrorHandler();
        this.symbolTable = new SymbolTable(errorHandler);
        initializeAutomata();
    }

    private void initializeAutomata() {
        // Initialize NFAs for different token patterns
        setupIdentifierNFA();
        setupNumberNFA();
        setupOperatorNFA();
        // Convert NFAs to DFAs for actual use
        currentTokenDFA = currentTokenNFA.toDFA();
    }

    private void setupIdentifierNFA() {
        // NFA for identifiers: [a-zA-Z][a-zA-Z0-9]*
        currentTokenNFA = new NFA();
        State s0 = new State();
        State s1 = new State();
        
        currentTokenNFA.setStartState(s0);
        currentTokenNFA.addAcceptingState(s1);
        
        // First character must be a letter
        for (char c = 'a'; c <= 'z'; c++) {
            currentTokenNFA.addTransition(s0, c, s1);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            currentTokenNFA.addTransition(s0, c, s1);
        }
        
        // Following characters can be letters or digits
        for (char c = 'a'; c <= 'z'; c++) {
            currentTokenNFA.addTransition(s1, c, s1);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            currentTokenNFA.addTransition(s1, c, s1);
        }
        for (char c = '0'; c <= '9'; c++) {
            currentTokenNFA.addTransition(s1, c, s1);
        }
    }

    private void setupNumberNFA() {
        // Similar setup for number literals
        // [0-9]+(\.[0-9]+)?
        // Implementation similar to identifier NFA but for numbers
    }

    private void setupOperatorNFA() {
        // Setup for operators and other tokens
        // Implementation for various operators
    }

    public void tokenize() {
        while (currentPosition < input.length()) {
            char currentChar = input.charAt(currentPosition);
            
            // Skip whitespace
            if (Character.isWhitespace(currentChar)) {
                if (currentChar == '\n') lineNumber++;
                currentPosition++;
                continue;
            }
            
            // Try to match the longest possible token
            String token = getNextToken();
            if (token != null) {
                processToken(token);
            } else {
                // Handle invalid token
                errorHandler.reportError(lineNumber, currentPosition, 
                    "Invalid token at position " + currentPosition,
                    ErrorHandler.ErrorType.LEXICAL);
                currentPosition++;
            }
        }
        
        // Add EOF token
        tokens.add(new Token(TokenType.EOF, "", lineNumber));
    }

    private String getNextToken() {
        // Try to match the longest possible token using the DFA
        StringBuilder longestMatch = new StringBuilder();
        int longestMatchEnd = currentPosition;
        
        for (int i = currentPosition; i <= input.length(); i++) {
            String currentTry = input.substring(currentPosition, i);
            if (currentTokenDFA.accepts(currentTry)) {
                longestMatch = new StringBuilder(currentTry);
                longestMatchEnd = i;
            }
        }
        
        if (longestMatch.length() > 0) {
            currentPosition = longestMatchEnd;
            return longestMatch.toString();
        }
        
        return null;
    }

    private void processToken(String tokenStr) {
        TokenType type = determineTokenType(tokenStr);
        Token token = new Token(type, tokenStr, lineNumber);
        tokens.add(token);
        
        // Update symbol table based on token type
        switch (type) {
            case IDENTIFIER:
                handleIdentifier(tokenStr);
                break;
            case INTEGER_LITERAL:
            case DECIMAL_LITERAL:
            case CHARACTER_LITERAL:
            case BOOLEAN_LITERAL:
                handleLiteral(token);
                break;
            case GLOBAL:
                // Mark next identifier as global
                break;
            // Handle other cases
        }
    }

    private void handleIdentifier(String name) {
        // Check if this is a declaration
        if (isDeclaration()) {
            String type = getCurrentDeclarationType();
            boolean isGlobal = isInGlobalScope();
            Object value = getInitialValue();
            symbolTable.add(name, type, isGlobal, value);
        }
    }

    private void handleLiteral(Token token) {
        // Update symbol table with literal value
        String identifier = getLastIdentifier();
        if (identifier != null) {
            symbolTable.setValue(identifier, token.value);
        }
    }

    private boolean isDeclaration() {
        // Check if we're in a declaration context
        // Look at previous tokens for type keywords
        return tokens.size() >= 2 && isTypeKeyword(tokens.get(tokens.size() - 2).type);
    }

    private boolean isTypeKeyword(TokenType type) {
        return type == TokenType.INTEGER || type == TokenType.DECIMAL ||
               type == TokenType.CHARACTER || type == TokenType.BOOLEAN;
    }

    private String getCurrentDeclarationType() {
        if (tokens.size() >= 2) {
            return tokens.get(tokens.size() - 2).value;
        }
        return null;
    }

    private boolean isInGlobalScope() {
        // Check if we've seen a GLOBAL keyword
        return tokens.stream()
            .anyMatch(t -> t.type == TokenType.GLOBAL);
    }

    private Object getInitialValue() {
        // Look ahead for assignment and literal
        return null; // Implementation depends on your needs
    }

    private String getLastIdentifier() {
        // Get the last seen identifier for assignment
        for (int i = tokens.size() - 1; i >= 0; i--) {
            if (tokens.get(i).type == TokenType.IDENTIFIER) {
                return tokens.get(i).value;
            }
        }
        return null;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }
}