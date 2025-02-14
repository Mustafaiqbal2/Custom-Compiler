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
        int parentScope;  // Add parentScope to track hierarchy

        Symbol(String name, String type, boolean isGlobal, int scope, int parentScope, Object value) {
            this.name = name;
            this.type = type;
            this.isGlobal = isGlobal;
            this.scope = scope;
            this.parentScope = parentScope;
            this.value = value;
        }
    }
    

    private final Map<String, Stack<Symbol>> symbols;
    private int currentScope;
    private int parentScope; 
    private Stack<Integer> parentScopeStack; 

    private final ErrorHandler errorHandler;
    private Stack<Integer> scopeStack;
    
    public SymbolTable(ErrorHandler errorHandler) {
        this.symbols = new HashMap<>();
        this.scopeStack = new Stack<>();
        this.parentScopeStack = new Stack<>();
        this.currentScope = 0;
        this.parentScope = 0;
        this.scopeStack.push(currentScope);
        this.parentScopeStack.push(parentScope);
        this.errorHandler = errorHandler;
    }




    public void enterScope() {
        parentScope = currentScope;  // Current becomes parent
        parentScopeStack.push(parentScope);
        currentScope++;
        scopeStack.push(currentScope);
    }

    public void exitScope() {
        if (!scopeStack.isEmpty()) {
            int scopeToRemove = scopeStack.pop();
            
            // Remove all symbols in the current scope
            Iterator<Map.Entry<String, Stack<Symbol>>> it = symbols.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Stack<Symbol>> entry = it.next();
                Stack<Symbol> stack = entry.getValue();
                
                while (!stack.isEmpty() && stack.peek().scope == scopeToRemove) {
                    stack.pop();
                }
                
                if (stack.isEmpty()) {
                    it.remove();
                }
            }
            
            currentScope = scopeStack.isEmpty() ? 0 : scopeStack.peek();
            if (!parentScopeStack.isEmpty()) {
                parentScopeStack.pop();
                parentScope = parentScopeStack.isEmpty() ? 0 : parentScopeStack.peek();
            }
        }
    }

    private boolean existsInCurrentScope(String name) {
        Stack<Symbol> stack = symbols.get(name);
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        // Check only in the exact current scope
        return stack.stream()
            .anyMatch(s -> s.scope == currentScope);
    }

    public void add(String name, String type, boolean isGlobal, Object value) {
        // Validate identifier
        if (!PatternMatcher.isValidIdentifier(name)) {
            errorHandler.reportError(0, 0, 
                "Invalid identifier name: " + name, 
                ErrorHandler.ErrorType.SEMANTIC);
            return;
        }

        // Special handling for functions
        if (type.equals("function")) {
            if (currentScope != 0) {
                errorHandler.reportError(0, 0,
                    "Functions can only be defined in global scope",
                    ErrorHandler.ErrorType.SEMANTIC);
                return;
            }
            Symbol symbol = new Symbol(name, type, false, 0, 0, value);
            symbols.computeIfAbsent(name, k -> new Stack<>()).push(symbol);
            return;
        }

        // Check if identifier already exists in current scope only
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

        // Use proper scope settings
        int scope = isGlobal ? 0 : currentScope;
        int parent = isGlobal ? 0 : parentScope;
        Symbol symbol = new Symbol(name, type, isGlobal, scope, parent, value);
        symbols.computeIfAbsent(name, k -> new Stack<>()).push(symbol);
    }
    
    public Symbol lookup(String name) {
        Stack<Symbol> stack = symbols.get(name);
        if (stack == null || stack.isEmpty()) {
            errorHandler.reportError(0, 0, 
                "Undefined symbol: " + name, 
                ErrorHandler.ErrorType.SEMANTIC);
            return null;
        }

        // First try current scope
        for (int i = stack.size() - 1; i >= 0; i--) {
            Symbol symbol = stack.get(i);
            if (symbol.scope == currentScope) {
                return symbol;
            }
        }

        // Then check parent scopes in hierarchy
        for (int i = stack.size() - 1; i >= 0; i--) {
            Symbol symbol = stack.get(i);
            // Check if symbol is global or in a valid parent scope
            if (symbol.isGlobal || isParentScope(symbol.scope)) {
                return symbol;
            }
        }

        errorHandler.reportError(0, 0, 
            "Symbol '" + name + "' not accessible in current scope", 
            ErrorHandler.ErrorType.SEMANTIC);
        return null;
    }

    private boolean isParentScope(int scope) {
        // Check if the given scope is a parent of current scope
        int checkScope = parentScope;
        while (checkScope > 0) {
            if (checkScope == scope) {
                return true;
            }
            // Find parent of parent by looking in stack
            int parentIndex = parentScopeStack.indexOf(checkScope);
            if (parentIndex > 0) {
                checkScope = parentScopeStack.get(parentIndex - 1);
            } else {
                break;
            }
        }
        return false;
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
    
    public int getCurrentScope() {
        return currentScope;
    }
}