package Compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        String filename = "code.ms";
        try {
        	////
            // Get the absolute path for the source file
            Path sourcePath = Paths.get("src", "Compiler", filename);
            
            // Check if file exists
            if (!Files.exists(sourcePath)) {
                System.err.println("Error: Source file '" + filename + "' not found!");
                System.err.println("Expected location: " + sourcePath.toAbsolutePath());
                return;
            }

            // Read the content from the .ms file
            String code = readSourceFile(sourcePath);
            System.out.println("=== Source Code ===");
            System.out.println(code);
            System.out.println("\n=================");
            
            // Create lexer instance
            Lexer lexer = new Lexer(code);
            lexer.printDFATransitionTables();
            
            // Perform lexical analysis
            System.out.println("=== Testing Lexical Analysis ===");
            lexer.tokenize();
            
            // Display tokens
            System.out.println("Tokens:");
            for (Token token : lexer.getTokens()) {
                System.out.printf("%s -> %s (Line: %d)%n", 
                                token.type, token.value, token.lineNumber);
            }
            // Display token count
            System.out.println("\nTotal Tokens: " + lexer.getTokens().size());

            // Display symbol table
            System.out.println("\n=== Symbol Table ===");
            lexer.getSymbolTable().displayTable();

            // Display any errors
            System.out.println("\n=== Error Report ===");
            lexer.getErrorHandler().displayErrors();
            
        } catch (IOException e) {
            System.err.println("Error reading source file: " + e.getMessage());
            System.err.println("Stack trace:");
            e.printStackTrace();
        }
    }


    private static String readSourceFile(Path path) throws IOException {
        try {
            return new String(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new IOException("Could not read file: " + path.toAbsolutePath() + 
                                "\nPlease ensure the file exists and has read permissions.", e);
        }
    }
}