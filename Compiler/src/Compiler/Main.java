package Compiler;

//Main class to integrate everything
public class Main {
 public static void main(String[] args) {
     String code = "var x = 10\nvar y = 3.14159\n// This is a comment\n/* Multi-line\n comment */\nvar z = x + y";

     Lexer lexer = new Lexer(code);
     lexer.tokenize();
     
     System.out.println("Tokens:");
     for (Token token : lexer.getTokens()) {
         System.out.println(token.type + " -> " + token.value + " (Line: " + token.lineNumber + ")");
     }

     SymbolTable symbolTable = new SymbolTable();
     symbolTable.add("x", "Integer");
     symbolTable.add("y", "Decimal");
     System.out.println("Symbol Table Lookup: x -> " + symbolTable.lookup("x"));

     ErrorHandler.reportError(1, "Example error message");

     FiniteAutomata fa = new FiniteAutomata();
     fa.constructNFA("a*b");
     fa.constructDFA("a*b");
 }
}
