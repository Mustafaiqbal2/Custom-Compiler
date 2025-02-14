package Compiler;

public class Main {
    public static void main(String[] args) {
        // Test input with proper syntax according to our language
        String code = """
            global integer max = 100
            
            function main() {
                integer x = 10
                decimal y = 3.14159
                character ch = 'a'
                boolean isValid = true
                
                // This is a single line comment
                /* This is a
                   multi-line comment */
                
                decimal result = x + y
            }
            """;

        // Initialize components
        Lexer lexer = new Lexer(code);
        SymbolTable symbolTable = new SymbolTable();
        ErrorHandler errorHandler = ErrorHandler.getInstance();

        // Perform lexical analysis
        System.out.println("Performing lexical analysis...");
        lexer.tokenize();
        
        // Print tokens
        System.out.println("\nTokens:");
        for (Token token : lexer.getTokens()) {
            System.out.printf("%s -> '%s' at line %d, column %d%n",
                token.type,
                token.value,
                token.lineNumber,
                token.column);
        }

        // Test symbol table
        System.out.println("\nTesting symbol table...");
        symbolTable.add("max", "integer", true);
        symbolTable.setValue("max", 100);
        
        symbolTable.enterScope(); // Enter main function scope
        symbolTable.add("x", "integer", false);
        symbolTable.add("y", "decimal", false);
        symbolTable.add("ch", "character", false);
        symbolTable.add("isValid", "boolean", false);
        symbolTable.add("result", "decimal", false);
        
        symbolTable.setValue("x", 10);
        symbolTable.setValue("y", 3.14159);
        symbolTable.setValue("ch", 'a');
        symbolTable.setValue("isValid", true);
        symbolTable.setValue("result", 13.14159);
        
        // Display symbol table contents
        symbolTable.displayTable();
        
        // Test error handling
        System.out.println("\nTesting error handling...");
        errorHandler.reportError(1, 1, "Test error message");
        errorHandler.reportWarning(2, 1, "Test warning message");
        
        // Clean up
        symbolTable.exitScope(); // Exit main function scope
    }
}