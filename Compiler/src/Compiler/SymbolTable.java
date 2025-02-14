package Compiler;

import java.util.*;

public class SymbolTable {
    public static class Symbol {
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

        @Override
        public String toString() {
            return String.format("%s\t| %s\t| %d\t| %b\t| %s",
                name, type, scope, isGlobal, value);
        }
    }

    private final Map<Integer, Map<String, Symbol>> symbolsByScope;
    private final Stack<Integer> scopeStack;
    private int currentScope;
    private final ErrorHandler errorHandler;

    public SymbolTable(ErrorHandler errorHandler) {
        this.symbolsByScope = new HashMap<>();
        this.scopeStack = new Stack<>();
        this.currentScope = 0;
        this.errorHandler = errorHandler;
        
        // Initialize global scope
        this.symbolsByScope.put(0, new HashMap<>());
        this.scopeStack.push(0);
    }

    public void enterFunctionScope() {
        enterScope(); // Treat function scope like any other scope
    }

    public void enterScope() {
        currentScope++;
        scopeStack.push(currentScope);
        symbolsByScope.put(currentScope, new HashMap<>());
    }

    public void exitScope() {
        if (!scopeStack.isEmpty() && scopeStack.size() > 1) { // Keep global scope
            scopeStack.pop();
            currentScope = scopeStack.peek();
        }
    }

    public void add(String name, String type, boolean isGlobal, Object value) {
        // Determine the target scope
        int targetScope = isGlobal ? 0 : currentScope;

        // Check for duplicate declarations in current scope
        Map<String, Symbol> scopeSymbols = symbolsByScope.get(targetScope);
        if (scopeSymbols != null && scopeSymbols.containsKey(name)) {
            String currentType = scopeSymbols.get(name).type;
            errorHandler.reportError(0, 0,
                String.format("Symbol '%s' already defined as %s in current scope", 
                    name, currentType),
                ErrorHandler.ErrorType.SEMANTIC);
            return;
        }

        // Create and add the new symbol
        Symbol symbol = new Symbol(name, type, isGlobal, targetScope, value);
        symbolsByScope.computeIfAbsent(targetScope, k -> new HashMap<>())
                     .put(name, symbol);
    }

    public Symbol lookup(String name) {
        // Check current scope first
        Symbol symbol = lookupInScope(currentScope, name);
        if (symbol != null) return symbol;

        // Then check enclosing scopes up to global
        for (int i = scopeStack.size() - 2; i >= 0; i--) {
            symbol = lookupInScope(scopeStack.get(i), name);
            if (symbol != null) return symbol;
        }

        return null;
    }

    private Symbol lookupInScope(int scope, String name) {
        Map<String, Symbol> scopeSymbols = symbolsByScope.get(scope);
        return scopeSymbols != null ? scopeSymbols.get(name) : null;
    }

    public void setValue(String name, Object value) {
        Symbol symbol = lookup(name);
        if (symbol == null) {
            errorHandler.reportError(0, 0,
                "Undefined symbol: " + name,
                ErrorHandler.ErrorType.SEMANTIC);
            return;
        }

        String validationError = validateValue(symbol.type, value);
        if (validationError != null) {
            errorHandler.reportError(0, 0, validationError, ErrorHandler.ErrorType.SEMANTIC);
            return;
        }

        symbol.value = value;
    }

    private String validateValue(String type, Object value) {
        if (value == null) return null;

        try {
            switch (type.toLowerCase()) {
                case "integer":
                    if (value instanceof String) {
                        Integer.parseInt((String) value);
                    }
                    break;
                case "decimal":
                    if (value instanceof String) {
                        Double.parseDouble((String) value);
                    }
                    break;
                case "boolean":
                    if (value instanceof String &&
                        !("true".equals(value) || "false".equals(value))) {
                        return "Invalid boolean value: " + value;
                    }
                    break;
                case "character":
                    if (value instanceof String) {
                        String str = (String) value;
                        if (str.length() != 3 || str.charAt(0) != '\'' || str.charAt(2) != '\'') {
                            return "Invalid character literal: " + value;
                        }
                    }
                    break;
                case "function":
                    return null; // Functions don't have values
                default:
                    return "Unknown type: " + type;
            }
        } catch (NumberFormatException e) {
            return "Invalid " + type + " value: " + value;
        }
        return null;
    }

    public void displayTable() {
        System.out.println("Symbol Table:");
        System.out.println("Name\t| Type\t| Scope\t| Global\t| Value");
        System.out.println("-".repeat(50));

        // Collect all symbols
        List<Symbol> allSymbols = new ArrayList<>();
        for (Map<String, Symbol> scopeSymbols : symbolsByScope.values()) {
            allSymbols.addAll(scopeSymbols.values());
        }

        // Sort: globals first, then by scope, then by name
        allSymbols.sort((s1, s2) -> {
            if (s1.isGlobal != s2.isGlobal) {
                return s1.isGlobal ? -1 : 1;
            }
            if (s1.scope != s2.scope) {
                return Integer.compare(s1.scope, s2.scope);
            }
            return s1.name.compareTo(s2.name);
        });

        // Display symbols
        allSymbols.forEach(symbol -> System.out.println(symbol));
    }

    public int getCurrentScope() {
        return currentScope;
    }
}