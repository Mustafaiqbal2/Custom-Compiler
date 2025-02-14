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
    private final Map<Integer, Map<String, Symbol>> scopedSymbols;
    private final ErrorHandler errorHandler;
    private Stack<Integer> scopeStack;
    
    public SymbolTable(ErrorHandler errorHandler) {
    	this.symbols = new HashMap<>();
        this.scopedSymbols = new HashMap<>();
        this.scopeStack = new Stack<>();
        this.currentScope = 0;
        this.scopeStack.push(currentScope);
        this.errorHandler = errorHandler;
        this.scopedSymbols.put(0, new HashMap<>()); // Initialize global scope
    }


    public void enterScope() {
        currentScope++;
        scopeStack.push(currentScope);
        scopedSymbols.put(currentScope, new HashMap<>()); // Initialize new scope
    }

    public void exitScope() {
        if (!scopeStack.isEmpty()) {
            int scopeToRemove = scopeStack.pop();
            scopedSymbols.remove(scopeToRemove); // Remove entire scope
            currentScope = scopeStack.isEmpty() ? 0 : scopeStack.peek();
        }
    }

    private boolean existsInCurrentScope(String name) {
        Map<String, Symbol> currentScopeSymbols = scopedSymbols.get(currentScope);
        return currentScopeSymbols != null && currentScopeSymbols.containsKey(name);
    }

    public void add(String name, String type, boolean isGlobal, Object value) {
        // Validate identifier
        if (!PatternMatcher.isValidIdentifier(name)) {
            errorHandler.reportError(0, 0, 
                "Invalid identifier name: " + name, 
                ErrorHandler.ErrorType.SEMANTIC);
            return;
        }

        int targetScope = isGlobal ? 0 : currentScope;
        Map<String, Symbol> scopeSymbols = scopedSymbols.get(targetScope);

        // Special handling for functions
        if (type.equals("function")) {
            if (currentScope != 0) {
                errorHandler.reportError(0, 0,
                    "Functions can only be defined in global scope",
                    ErrorHandler.ErrorType.SEMANTIC);
                return;
            }
            if (scopeSymbols.containsKey(name)) {
                errorHandler.reportError(0, 0,
                    "Function '" + name + "' already defined",
                    ErrorHandler.ErrorType.SEMANTIC);
                return;
            }
            Symbol symbol = new Symbol(name, type, false, 0, value);
            scopeSymbols.put(name, symbol);
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

        // Add the symbol to the appropriate scope
        Symbol symbol = new Symbol(name, type, isGlobal, targetScope, value);
        scopeSymbols.put(name, symbol);
    }
    
    public Symbol lookup(String name) {
        // First check current scope
        Map<String, Symbol> currentScopeSymbols = scopedSymbols.get(currentScope);
        if (currentScopeSymbols != null && currentScopeSymbols.containsKey(name)) {
            return currentScopeSymbols.get(name);
        }

        // Then check global scope if we're not already in it
        if (currentScope != 0) {
            Map<String, Symbol> globalScope = scopedSymbols.get(0);
            if (globalScope != null && globalScope.containsKey(name)) {
                return globalScope.get(name);
            }
        }

        errorHandler.reportError(0, 0, 
            "Undefined symbol: " + name, 
            ErrorHandler.ErrorType.SEMANTIC);
        return null;
    }
    
    public boolean exists(String name) {
        Stack<Symbol> stack = symbols.get(name);
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        // Check if the symbol is visible in current scope
        Symbol symbol = stack.peek();
        return symbol.isGlobal || symbol.scope <= currentScope;
    }
    
    public void setValue(String name, Object value) {
        Stack<Symbol> stack = symbols.get(name);
        if (stack == null || stack.isEmpty()) {
            errorHandler.reportError(0, 0, 
                "Undefined symbol: " + name, 
                ErrorHandler.ErrorType.SEMANTIC);
            return;
        }

        // Find the most recent visible symbol
        Symbol symbol = null;
        for (int i = stack.size() - 1; i >= 0; i--) {
            Symbol temp = stack.get(i);
            if (temp.isGlobal || temp.scope <= currentScope) {
                symbol = temp;
                break;
            }
        }

        if (symbol == null) {
            errorHandler.reportError(0, 0, 
                "Symbol not accessible in current scope: " + name, 
                ErrorHandler.ErrorType.SEMANTIC);
            return;
        }

        String validationError = validateValue(symbol.type, value);
        if (validationError != null) {
            errorHandler.reportError(0, 0, 
                validationError, 
                ErrorHandler.ErrorType.SEMANTIC);
            return;
        }

        symbol.value = value;
    }

    public void displayTable() {
        System.out.println("Symbol Table:");
        System.out.println("Name\t| Type\t| Scope\t| Global\t| Value");
        System.out.println("-".repeat(50));

        // First collect all visible symbols at current scope
        Map<String, Symbol> visibleSymbols = new HashMap<>();
        
        for (Map.Entry<String, Stack<Symbol>> entry : symbols.entrySet()) {
            Stack<Symbol> stack = entry.getValue();
            
            // Find the most recent visible symbol for this name
            for (int i = stack.size() - 1; i >= 0; i--) {
                Symbol symbol = stack.get(i);
                if (symbol.isGlobal || symbol.scope <= currentScope) {
                    visibleSymbols.put(entry.getKey(), symbol);
                    break;
                }
            }
        }

        // Sort and display symbols
        List<Symbol> sortedSymbols = new ArrayList<>(visibleSymbols.values());
        sortedSymbols.sort((s1, s2) -> {
            if (s1.isGlobal != s2.isGlobal) {
                return s1.isGlobal ? -1 : 1;
            }
            if (s1.scope != s2.scope) {
                return Integer.compare(s1.scope, s2.scope);
            }
            return s1.name.compareTo(s2.name);
        });

        for (Symbol symbol : sortedSymbols) {
            System.out.printf("%s\t| %s\t| %d\t| %b\t| %s%n",
                symbol.name, symbol.type, symbol.scope,
                symbol.isGlobal, symbol.value);
        }
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

    public int getCurrentScope() {
        return currentScope;
    }
}