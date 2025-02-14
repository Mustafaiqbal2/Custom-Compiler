package Compiler;

public class Main {
    public static void main(String[] args) {
        // Test input code
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

        // Create lexer instance (now handles symbol table and automata internally)
        Lexer lexer = new Lexer(code);
        
        // Perform lexical analysis
        System.out.println("=== Testing Lexical Analysis ===");
        lexer.tokenize();
        
        // Display tokens
        System.out.println("Tokens:");
        for (Token token : lexer.getTokens()) {
            System.out.printf("%s -> %s (Line: %d)%n", 
                            token.type, token.value, token.lineNumber);
        }

        // Display symbol table (now maintained by lexer)
        System.out.println("\n=== Symbol Table ===");
        lexer.getSymbolTable().displayTable();

        // Display any errors that occurred during lexical analysis
        System.out.println("\n=== Error Report ===");
        lexer.getErrorHandler().displayErrors();
    }
}