package Compiler;

public class ErrorHandler {
    private static final ErrorHandler instance = new ErrorHandler();
    
    private ErrorHandler() {}
    
    public static ErrorHandler getInstance() {
        return instance;
    }
    
    public void reportError(int line, int column, String message) {
        System.err.printf("Error at line %d, column %d: %s%n", line, column, message);
    }
    
    public void reportWarning(int line, int column, String message) {
        System.out.printf("Warning at line %d, column %d: %s%n", line, column, message);
    }
}