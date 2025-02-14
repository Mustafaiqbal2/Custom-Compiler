package Compiler;

import Compiler.automata.*;

public class Main {
    public static void main(String[] args) {
        // Test the lexer
        String code = """
                global integer max = 100
                
                /* This is a multi-line comment
                   describing the main function */
                function main() {
                    integer x = 10
                    decimal pi = 3.14159
                    character ch = 'a'
                    boolean isValid = true
                    
                    // Arithmetic operations
                    decimal result = x * pi
                }
                """;

        System.out.println("=== Testing Lexical Analysis ===");
        Lexer lexer = new Lexer(code);
        lexer.tokenize();
        
        System.out.println("Tokens:");
        for (Token token : lexer.getTokens()) {
            System.out.printf("%s -> %s (Line: %d)%n", 
                            token.type, token.value, token.lineNumber);
        }

        // Test Symbol Table
        System.out.println("\n=== Testing Symbol Table ===");
        SymbolTable symbolTable = new SymbolTable();
        
        // Add global variable
        symbolTable.add("max", "integer", true);
        symbolTable.setValue("max", 100);
        
        // Enter new scope
        symbolTable.enterScope();
        symbolTable.add("x", "integer", false);
        symbolTable.add("pi", "decimal", false);
        symbolTable.setValue("x", 10);
        symbolTable.setValue("pi", 3.14159);
        
        symbolTable.displayTable();

        // Test Error Handler
        System.out.println("\n=== Testing Error Handler ===");
        ErrorHandler errorHandler = new ErrorHandler();
        
        // Example error reporting
        errorHandler.reportError(5, 10, "Invalid decimal precision", 
                               ErrorHandler.ErrorType.LEXICAL);
        errorHandler.reportError(6, 15, "Undefined variable used", 
                               ErrorHandler.ErrorType.SEMANTIC);
        
        errorHandler.displayErrors();

        // Test NFA/DFA
        System.out.println("\n=== Testing Automata ===");
        // Create a simple NFA for the pattern: (a|b)*abb
        NFA nfa = new NFA();
        State s0 = new State();
        State s1 = new State();
        State s2 = new State();
        State s3 = new State();

        nfa.setStartState(s0);
        nfa.addAcceptingState(s3);

        s0.addTransition('a', s0);
        s0.addTransition('b', s0);
        s0.addTransition('a', s1);
        s1.addTransition('b', s2);
        s2.addTransition('b', s3);

        System.out.println("NFA Transition Table:");
        nfa.displayTransitionTable();

        DFA dfa = nfa.toDFA();
        System.out ▋