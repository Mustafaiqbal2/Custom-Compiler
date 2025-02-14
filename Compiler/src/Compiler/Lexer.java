package Compiler;

import java.util.*;
import java.util.regex.*;

// Lexer class for tokenizing input
class Lexer {
    private String input;
    private List<Token> tokens;
    private int lineNumber;

    private static final Pattern TOKEN_PATTERNS = Pattern.compile(
    	    "(?<WHITESPACE>\\s+)|" +
    	    "(?<IDENTIFIER>[a-z]+)|" +
    	    "(?<INTEGER>\\d+)|" +
    	    "(?<DECIMAL>\\d+\\.\\d{1,5})|" +
    	    "(?<BOOLEAN>true|false)|" +
    	    "(?<CHARACTER>'[a-z]')|" +
    	    "(?<ARITHMETIC>[+\\-*/%])|" +
    	    "(?<EXPONENT>\\^)|" +
    	    "(?<COMMENT>//[^\\n]*)|" +  // Fixed escaping for newline
    	    "(?<MULTILINECOMMENT>/\\*.*?\\*/)"  // No unnecessary pipe at the end
    	);

    public Lexer(String input) {
        this.input = input;
        this.tokens = new ArrayList<>();
        this.lineNumber = 1;
    }

    public void tokenize() {
        Matcher matcher = TOKEN_PATTERNS.matcher(input);
        while (matcher.find()) {
            if (matcher.group("WHITESPACE") != null) continue; // Ignore whitespace
            else if (matcher.group("IDENTIFIER") != null)
                tokens.add(new Token("IDENTIFIER", matcher.group("IDENTIFIER"), lineNumber));
            else if (matcher.group("INTEGER") != null)
                tokens.add(new Token("INTEGER", matcher.group("INTEGER"), lineNumber));
            else if (matcher.group("DECIMAL") != null)
                tokens.add(new Token("DECIMAL", matcher.group("DECIMAL"), lineNumber));
            else if (matcher.group("BOOLEAN") != null)
                tokens.add(new Token("BOOLEAN", matcher.group("BOOLEAN"), lineNumber));
            else if (matcher.group("CHARACTER") != null)
                tokens.add(new Token("CHARACTER", matcher.group("CHARACTER"), lineNumber));
            else if (matcher.group("ARITHMETIC") != null)
                tokens.add(new Token("ARITHMETIC_OPERATOR", matcher.group("ARITHMETIC"), lineNumber));
            else if (matcher.group("EXPONENT") != null)
                tokens.add(new Token("EXPONENT_OPERATOR", matcher.group("EXPONENT"), lineNumber));
            else if (matcher.group("COMMENT") != null)
                tokens.add(new Token("COMMENT", matcher.group("COMMENT"), lineNumber));
            else if (matcher.group("MULTILINE_COMMENT") != null)
                tokens.add(new Token("MULTILINE_COMMENT", matcher.group("MULTILINE_COMMENT"), lineNumber));
            else
                tokens.add(new Token("UNKNOWN", matcher.group(), lineNumber));
        }
    }

    public List<Token> getTokens() {
        return tokens;
    }
}

// Symbol table to manage identifiers
class SymbolTable {
    private Map<String, String> table;

    public SymbolTable() {
        this.table = new HashMap<>();
    }

    public void add(String identifier, String type) {
        table.put(identifier, type);
    }

    public String lookup(String identifier) {
        return table.getOrDefault(identifier, "Undefined");
    }
}

// Basic error handler
class ErrorHandler {
    public static void reportError(int line, String message) {
        System.out.println("Error at line " + line + ": " + message);
    }
}

// Finite Automata Placeholder
class FiniteAutomata {
    public void constructNFA(String regex) {
        System.out.println("Constructing NFA for: " + regex);
    }

    public void constructDFA(String regex) {
        System.out.println("Constructing DFA for: " + regex);
    }
}
