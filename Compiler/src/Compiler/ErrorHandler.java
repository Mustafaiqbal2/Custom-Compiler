package Compiler;

import java.util.*;

public class ErrorHandler {
    private List<CompilerError> errors;
    private boolean hasErrors;

    public ErrorHandler() {
        this.errors = new ArrayList<>();
        this.hasErrors = false;
    }

    public void reportError(int line, int column, String message, ErrorType type) {
        errors.add(new CompilerError(line, column, message, type));
        hasErrors = true;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public void displayErrors() {
        if (!hasErrors) {
            System.out.println("No errors found.");
            return;
        }

        errors.sort(Comparator.comparingInt(CompilerError::getLine)
                             .thenComparingInt(CompilerError::getColumn));

        for (CompilerError error : errors) {
            System.out.printf("%s at line %d, column %d: %s%n",
                    error.getType(),
                    error.getLine(),
                    error.getColumn(),
                    error.getMessage());
        }
    }

    public enum ErrorType {
        LEXICAL("Lexical Error"),
        SYNTAX("Syntax Error"),
        SEMANTIC("Semantic Error");

        private final String description;

        ErrorType(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private static class CompilerError {
        private final int line;
        private final int column;
        private final String message;
        private final ErrorType type;

        public CompilerError(int line, int column, String message, ErrorType type) {
            this.line = line;
            this.column = column;
            this.message = message;
            this.type = type;
        }

        public int getLine() { return line; }
        public int getColumn() { return column; }
        public String getMessage() { return message; }
        public ErrorType getType() { return type; }
    }
}