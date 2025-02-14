package Compiler;

import java.util.*;

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

    public SymbolTable() {
        this.symbols = new HashMap<>();
        this.currentScope = 0;
        this.errorHandler = ErrorHandler.getInstance();
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
            
            while (!stack.isEmpty() && stack.peek().getScope() == currentScope) {
                stack.pop();
            }
            
            if (stack.isEmpty()) {
                it.remove();
            }
        }
        currentScope--;
    }

    public void add(String name, String type, boolean isGlobal) {
        if (!isValidIdentifier(name)) {
            errorHandler.reportError(0, 0, "Invalid identifier: " + name);
            return;
        }

        Stack<Symbol> stack = symbols.computeIfAbsent(name, k -> new Stack<>());
        
        // Check if identifier is already defined in current scope
        if (!stack.isEmpty() && stack.peek().getScope() == currentScope) {
            errorHandler.reportError(0, 0, 
                "Symbol '" + name + "' already defined in current scope");
            return;
        }

        Symbol symbol = new Symbol(name, type, isGlobal, isGlobal ? 0 : currentScope);
        stack.push(symbol);
    }

    public Symbol lookup(String name) {
        Stack<Symbol> stack = symbols.get(name);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.peek();
    }

    private boolean isValidIdentifier(String name) {
        return name.matches("[a-z]+");
    }

    public void setValue(String name, Object value) {
        Symbol symbol = lookup(name);
        if (symbol != null) {
            // Type checking could be added here
            symbol.setValue(value);
        } else {
            errorHandler.reportError(0, 0, "Undefined symbol: " + name);
        }
    }

    public void displayTable() {
        System.out.println("\nSymbol Table Contents:");
        System.out.println("Name\tType\tScope\tGlobal\tValue");
        System.out.println("-".repeat(50));

        symbols.forEach((name, stack) -> {
            if (!stack.isEmpty()) {
                Symbol symbol = stack.peek();
                System.out.printf("%s\t%s\t%d\t%b\t%s%n",
                    symbol.getName(),
                    symbol.getType(),
                    symbol.getScope(),
                    symbol.isGlobal(),
                    symbol.getValue());
            }
        });
    }
}
}