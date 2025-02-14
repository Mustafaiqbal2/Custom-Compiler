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

    private Map<String, Stack<Symbol>> symbols;
    private int currentScope;

    public SymbolTable() {
        this.symbols = new HashMap<>();
        this.currentScope = 0;
    }

    public void enterScope() {
        currentScope++;
    }

    public void exitScope() {
        // Remove all symbols in the current scope
        symbols.values().forEach(stack -> {
            while (!stack.isEmpty() && stack.peek().scope == currentScope) {
                stack.pop();
            }
        });
        currentScope--;
    }

    public void add(String name, String type, boolean isGlobal) {
        Symbol symbol = new Symbol(name, type, isGlobal, isGlobal ? 0 : currentScope);
        symbols.computeIfAbsent(name, k -> new Stack<>()).push(symbol);
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
        if (exists(name)) {
            symbols.get(name).peek().value = value;
        }
    }

    public void displayTable() {
        System.out.println("Symbol Table:");
        System.out.println("Name\t| Type\t| Scope\t| Global\t| Value");
        System.out.println("-".repeat(50));

        symbols.forEach((name, stack) -> {
            Symbol symbol = stack.peek();
            System.out.printf("%s\t| %s\t| %d\t| %b\t| %s%n",
                    name, symbol.type, symbol.scope,
                    symbol.isGlobal, symbol.value);
        });
    }
}