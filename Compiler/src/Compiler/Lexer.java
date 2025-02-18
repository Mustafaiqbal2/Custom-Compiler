package Compiler;

import java.util.*;

public class Lexer {
    private String input;
    private List<Token> tokens;
    private SymbolTable symbolTable;
    private ErrorHandler errorHandler;
    
    // Map each token type to its corresponding DFA for pattern matching.
    private Map<TokenType, DFA> tokenDFAs;
    
    // Fields for scope and declaration handling.
    private String currentScope = "global";
    private String pendingDataType = null;
    private boolean expectingFunctionName = false;
    
    // Used for symbol table processing (lookahead).
    private int tokenIndex = 0;

    public Lexer(String input) {
        this.input = input;
        this.tokens = new ArrayList<>();
        this.symbolTable = new SymbolTable();
        this.errorHandler = new ErrorHandler();
        this.tokenDFAs = new LinkedHashMap<>();
        initTokenDFAs();
    }
    
    // Initializes DFAs for different token patterns.
    // Initializes DFAs for different token patterns.
    private void initTokenDFAs() {
        // Regular expressions for tokens.
        // Note: Regexes are simplified.
    	// Comments
    	String singleLineCommentRegex = "//.*";
    	String multiLineCommentRegex = "/\\*([^*]|\\*+[^*/])*\\*+/";
    	
    	// Literals (using simpler patterns)
    	String stringRegex   = "\".*\"";       // greedy matching for strings
    	String charRegex     = "'.'";          // a single character between single quotes
    	String booleanRegex  = "true|false";   // boolean literals

    	// Numeric literals
    	String decimalRegex  = "[0-9]+\\.([0-9]  |[0-9][0-9]   |[0-9][0-9][0-9]   |[0-9][0-9][0-9][0-9]   |[0-9][0-9][0-9][0-9][0-9])"; // one or more digits, a dot, max 5 digits
    	String integerRegex  = "[0-9]+";

    	// Punctuation and operators
    	String assignRegex   = "=";
    	String lparenRegex   = "\\(";
    	String rparenRegex   = "\\)";
    	String lbraceRegex   = "\\{";
    	String rbraceRegex   = "\\}";
    	String operatorRegex = "(\\+|\\-|\\*|/|%|<|>|^)";

    	// Identifier (only lowercase letters per assignment)
    	String identifierRegex = "[a-z]+";
        
        // Create DFAs by converting regex -> NFA -> DFA.
        RegexToNFA regexToNFA = new RegexToNFA();
        try {
        	// In Lexer.initTokenDFAs(), using a LinkedHashMap to preserve order
        	tokenDFAs.put(TokenType.SINGLE_COMMENT, new DFA(regexToNFA.convert(singleLineCommentRegex)));
        	tokenDFAs.put(TokenType.MULTI_COMMENT, new DFA(regexToNFA.convert(multiLineCommentRegex)));
        	tokenDFAs.put(TokenType.STRING, new DFA(regexToNFA.convert(stringRegex)));
        	tokenDFAs.put(TokenType.CHAR, new DFA(regexToNFA.convert(charRegex)));
        	tokenDFAs.put(TokenType.BOOLEAN, new DFA(regexToNFA.convert(booleanRegex)));
        	tokenDFAs.put(TokenType.DECIMAL, new DFA(regexToNFA.convert(decimalRegex)));
        	tokenDFAs.put(TokenType.INTEGER, new DFA(regexToNFA.convert(integerRegex)));
        	tokenDFAs.put(TokenType.ASSIGN, new DFA(regexToNFA.convert(assignRegex)));
        	tokenDFAs.put(TokenType.LPAREN, new DFA(regexToNFA.convert(lparenRegex)));
        	tokenDFAs.put(TokenType.RPAREN, new DFA(regexToNFA.convert(rparenRegex)));
        	tokenDFAs.put(TokenType.LBRACE, new DFA(regexToNFA.convert(lbraceRegex)));
        	tokenDFAs.put(TokenType.RBRACE, new DFA(regexToNFA.convert(rbraceRegex)));
        	tokenDFAs.put(TokenType.OPERATOR, new DFA(regexToNFA.convert(operatorRegex)));
        	tokenDFAs.put(TokenType.IDENTIFIER, new DFA(regexToNFA.convert(identifierRegex)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Tokenizes the entire input, then processes tokens for the symbol table.
    public void tokenize() {
        int pos = 0;
        int lineNumber = 1;
        while (pos < input.length()) {
            char c = input.charAt(pos);
            // Update line counter for newline characters
            if (Character.isWhitespace(c)) {
                if (c == '\n') {
                    lineNumber++;
                }
                pos++;
                continue;
            }
            boolean matched = false;
            // Try each token type (order matters)
            for (Map.Entry<TokenType, DFA> entry : tokenDFAs.entrySet()) {
                DFA dfa = entry.getValue();
                String tokenValue = dfa.match(input.substring(pos));
				
                if (tokenValue != null && !tokenValue.isEmpty()) {
                    TokenType type = entry.getKey();
                    if (type == TokenType.IDENTIFIER && isKeyword(tokenValue)) {
                        type = TokenType.KEYWORD;
                    }
                    tokens.add(new Token(type, tokenValue, lineNumber));
                    pos += tokenValue.length();
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                errorHandler.addError("Unrecognized token at line " + lineNumber + ", position " + pos);
                pos++;
            }
        }
        // Process the token stream for symbol table entries
        for (tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
            processTokenForSymbolTable(tokens.get(tokenIndex));
        }
    }
    
    // Checks if a given token value is a keyword.
    private boolean isKeyword(String value) {
        Set<String> keywords = new HashSet<>(Arrays.asList(
            "if", "else", "while", "return", "int", "float", "char", "boolean",
            "global", "integer", "function", "string"
        ));
        return keywords.contains(value);
    }
    
    // Checks if the token is a data type keyword.
    private boolean isDataTypeKeyword(String value) {
        Set<String> dataTypes = new HashSet<>(Arrays.asList("integer", "float", "char", "boolean", "string"));
        return dataTypes.contains(value);
    }
    
    // Processes a token for symbol table entries.
    private void processTokenForSymbolTable(Token token) {
        if (token.value.equals("global")) {
            currentScope = "global";
        } else if (token.value.equals("function")) {
            expectingFunctionName = true;
        } else if (isDataTypeKeyword(token.value)) {
            pendingDataType = token.value;
        } else if (token.type == TokenType.IDENTIFIER) {
            if (expectingFunctionName) {
                symbolTable.addSymbol(token.value, "function", "global",
                        symbolTable.getNextMemoryLocation(), new HashMap<>(), null);
                currentScope = token.value;
                expectingFunctionName = false;
            } else if (pendingDataType != null) {
                if (tokenIndex + 2 < tokens.size() && tokens.get(tokenIndex + 1).type == TokenType.ASSIGN &&
                    isLiteralToken(tokens.get(tokenIndex + 2).type)) {
                    String literalValue = tokens.get(tokenIndex + 2).value;
                    symbolTable.addSymbol(token.value, pendingDataType, currentScope,
                        symbolTable.getNextMemoryLocation(), new HashMap<>(), literalValue);
                    pendingDataType = null;
                } else {
                    symbolTable.addSymbol(token.value, pendingDataType, currentScope,
                        symbolTable.getNextMemoryLocation(), new HashMap<>(), null);
                    pendingDataType = null;
                }
            } else if (!symbolTable.contains(token.value)) {
                symbolTable.addSymbol(token.value, "unknown", currentScope,
                        symbolTable.getNextMemoryLocation(), new HashMap<>(), null);
            }
        }
    }
    
    // Helper: Check if a token type is a literal.
    private boolean isLiteralToken(TokenType type) {
        return type == TokenType.INTEGER || type == TokenType.DECIMAL ||
               type == TokenType.STRING || type == TokenType.BOOLEAN ||
               type == TokenType.CHAR;
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
    
    public void printDFATransitionTables() {
        System.out.println("=== DFA Transition Tables ===");
        for (Map.Entry<TokenType, DFA> entry : tokenDFAs.entrySet()) {
            System.out.println("DFA for token type: " + entry.getKey());
            entry.getValue().displayTransitionTable();
            System.out.println("-----------------------------------------------------");
        	
        }
    }
    
}