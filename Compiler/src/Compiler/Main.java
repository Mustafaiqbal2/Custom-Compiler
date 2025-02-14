package Compiler;

import Compiler.automata.*;

public class Main {
    public static void main(String[] args) {
        // Test patterns that cover different regex features
        String[] patterns = {
            "a",                    // Basic character
            "a|b",                 // Union
            "ab",                  // Concatenation
            "a*",                  // Kleene star
            "a+",                  // One or more
            "a?",                  // Optional
            "[a-z]",              // Character class
            "(a|b)*abb",          // Complex pattern
            "[0-9]+",             // One or more digits
            "'[a-z]'",            // Character literal pattern
            "[a-z][a-z0-9]*"      // Identifier pattern
        };

        RegexToNFAConverter converter = new RegexToNFAConverter();

        for (String pattern : patterns) {
            System.out.println("\n=== Testing Pattern: " + pattern + " ===");
            
            try {
                // Convert regex to NFA
                System.out.println("\nGenerating NFA...");
                NFA nfa = converter.convert(pattern);
                
                // Display NFA transition table
                System.out.println("\nNFA Transition Table:");
                nfa.displayTransitionTable();
                
                // Convert NFA to DFA
                System.out.println("\nConverting to DFA...");
                DFA dfa = nfa.toDFA();
                
                // Display DFA transition table
                System.out.println("\nDFA Transition Table:");
                dfa.displayTransitionTable();
                
                // Test some sample inputs
                String[] testInputs = {
                    "a", "b", "ab", "abb", "abbb", "baa",
                    "123", "abc", "a1b2", "'a'", "x"
                };
                
                System.out.println("\nTesting sample inputs:");
                for (String input : testInputs) {
                    boolean accepts = dfa.accepts(input);
                    System.out.printf("Input %-6s : %s%n", 
                        "'" + input + "'", 
                        accepts ? "Accepted" : "Rejected");
                }
                
                System.out.println("\n" + "=".repeat(50));
                
            } catch (Exception e) {
                System.err.println("Error processing pattern '" + pattern + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}