package Compiler;

import java.util.*;
import Compiler.util.PatternMatcher;

public class SymbolTable {
    private static class Symbol {
        String name;
        String type;
        boolean isGlobal;
        Object value;
        int scope;

        Symbol(String name, String type, boolean isGlobal, int scope) {
            this.name = name;
            this.type = type;
            this.isGlobal = isGlobal;
            this.scope = scope;
        }
    }

    private final Map<String, Stack<Symbol>> symbols;
    private int currentScope;
    private final ErrorHandler errorHandler;

    public SymbolTable(ErrorHandler errorHandler) {
        this.symbols = new HashMap<>();
        this.currentScope = 0;
        this.errorHandler = errorHandler;
    }

    public void enterScope() {
        currentScope++;
    }

    public void exitScope() {
        // Remove all symbols in the current scope
        Iterator<Map.Entry<String, Stack<Symbol>>> it = symbols.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Stack<Symbol>> entry = it.next();
            Stack<Symbol> stack = entry.getValue();
            
            while (!stack.isEmpty() && stack.peek().scope == currentScope) {
                stack.pop();
            }
            
            if (stack.isEmpty()) {
                it.remove();
            }
        }
        currentScope--;
    }

    public void add(String name, String type, boolean isGlobal) {
        // Validate identifier
        if (!PatternMatcher.isValidIdentifier(name)) {
            errorHandler.reportError(0, 0, 
                "Invalid identifier name: " + name, 
                ErrorHandler.ErrorType.SEMANTIC);
            return;
        }

        // Check if identifier already exists in current scope
        if (existsInCurrentScope(name)) {
            errorHandler.reportError(0, 0, 
                "Symbol '" + name + "' already defined in current scope", 
                ErrorHandler.ErrorType.SEMANTIC);
            return;
        }

        Symbol symbol = new Symbol(name, type, isGlobal, isGlobal ? 0 : currentScope);
        symbols.computeIfAbsent(name, k -> new Stack<>()).push(symbol);
    }

    private boolean existsInCurrentScope(String name) {
        Stack<Symbol> stack = symbols.get(name);
        return stack != null && !stack.isEmpty() && stack.peek().scope == currentScope;
    }

    public boolean exists(String name) {
        return symbols.containsKey(name) && !symbols.get(name).isEmpty();
    }

    public Symbol lookup(String name) {
        if (!exists(name)) {
            return null;
        }
        return symbols.get(name).peek();
    }

    public void setValue(String name, Object value) {
        if (!exists(name)) {
            errorHandler.reportError(0, 0, 
                "Undefined symbol: " + name, 
                ErrorHandler.ErrorType.SEMANTIC);
            return;
        }

        Symbol symbol = lookup(name);
        String validationError = validateValue(symbol.type, value);
        if (validationError != null) {
            errorHandler.reportError(0, 0, 
                validationError, 
                ErrorHandler.ErrorType.SEMANTIC);
            return;
        }

        symbol.value = value;
    }

    private String validateValue(String type, Object value) {
        if (value == null) {
            return "Cannot assign null value";
        }

        switch (type.toLowerCase()) {
            case "integer":
                if (!(value instanceof Integer) && !(value instanceof String)) {
                    return "Expected integer value, got: " + value.getClass().getSimpleName();
                }
                if (value instanceof String && !PatternMatcher.isValidInteger((String)value)) {
                    return "Invalid integer value: " + value;
                }
                break;
            case "decimal":
                if (!(value instanceof Double) && !(value instanceof String)) {
                    return "Expected decimal value, got: " + value.getClass().getSimpleName();
                }
                if (value instanceof String && !PatternMatcher.isValidDecimal((String)value)) {
                    return "Invalid decimal value: " + value;
                }
                break;
            case "character":
                if (!(value instanceof Character) && !(value instanceof String)) {
                    return "Expected character value, got: " + value.getClass().getSimpleName();
                }
                if (value instanceof String && !PatternMatcher.isValidCharacter((String)value)) {
                    return "Invalid character value: " + value;
                }
                break;
            case "boolean":
                if (!(value instanceof Boolean) && !(value instanceof String)) {
                    return "Expected boolean value, got: " + value.getClass().getSimpleName();
                }
                if (value instanceof String && 
                    !value.equals("true") && !value.equals("false")) {
                    return "Invalid boolean value: " + value;
                }
                break;
            default:
                return "Unknown type: " + type;
        }
        return null; // null means no error
    }

    public void displayTable() {
        System.out.println("Symbol Table:");
        System.out.println("Name\t| Type\t| Scope\t| Global\t| Value");
        System.out.println("-".repeat(50));

        symbols.forEach((name, stack) -> {
            if (!stack.isEmpty()) {
                Symbol symbol = stack.peek();
                System.out.printf("%s\t| %s\t| %d\t| %b\t| %s%n",
                        name, symbol.type, symbol.scope,
                        symbol.isGlobal, symbol.value);
            }
        });
    }
}