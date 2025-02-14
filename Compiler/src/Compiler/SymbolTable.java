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

        Symbol(String name, String type, boolean isGlobal, int scope, Object value) {
            this.name = name;
            this.type = type;
            this.isGlobal = isGlobal;
            this.scope = scope;
            this.value = value;
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

    public void add(String name, String type, boolean isGlobal, Object value) {
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

        // Validate value if provided
        if (value != null) {
            String validationError = validateValue(type, value);
            if (validationError != null) {
                errorHandler.reportError(0, 0, validationError, ErrorHandler.ErrorType.SEMANTIC);
                return;
            }
        }

        Symbol symbol = new Symbol(name, type, isGlobal, isGlobal ? 0 : currentScope, value);
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
            errorHandler.reportError(0, 0, 
                "Undefined symbol: " + name, 
                ErrorHandler.ErrorType.SEMANTIC);
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
            return null; // Allow null values for initialization
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

        // Sort symbols by scope (globals first) and then by name
        List<Map.Entry<String, Stack<Symbol>>> sortedSymbols = new ArrayList<>(symbols.entrySet());
        sortedSymbols.sort((e1, e2) -> {
            Symbol s1 = e1.getValue().peek();
            Symbol s2 = e2.getValue().peek();
            if (s1.isGlobal != s2.isGlobal) {
                return s1.isGlobal ? -1 : 1;
            }
            if (s1.scope != s2.scope) {
                return Integer.compare(s1.scope, s2.scope);
            }
            return e1.getKey().compareTo(e2.getKey());
        });

        for (Map.Entry<String, Stack<Symbol>> entry : sortedSymbols) {
            if (!entry.getValue().isEmpty()) {
                Symbol symbol = entry.getValue().peek();
                System.out.printf("%s\t| %s\t| %d\t| %b\t| %s%n",
                        symbol.name, symbol.type, symbol.scope,
                        symbol.isGlobal, symbol.value);
            }
        }
    }
}