package Compiler;

import java.util.*;

public class SymbolTable {
    public static class SymbolEntry {
        public String identifierName;
        public String dataType;
        public String scope;
        public int memoryLocation;
        public Map<String, String> attributes;
        public String value;  // Literal value

        public SymbolEntry(String identifierName, String dataType, String scope, int memoryLocation, Map<String, String> attributes, String value) {
            this.identifierName = identifierName;
            this.dataType = dataType;
            this.scope = scope;
            this.memoryLocation = memoryLocation;
            this.attributes = attributes;
            this.value = value;
        }
    }
    
    private Map<String, SymbolEntry> table;
    private int memoryCounter;
    
    public SymbolTable() {
        table = new HashMap<>();
        memoryCounter = 1000; // starting memory address
    }
    
    public void addSymbol(String lexeme, String dataType, String scope, int memoryLocation, Map<String, String> attributes, String value) {
        if (!table.containsKey(lexeme)) {
            SymbolEntry entry = new SymbolEntry(lexeme, dataType, scope, memoryLocation, attributes, value);
            table.put(lexeme, entry);
        }
    }
    
    public boolean contains(String lexeme) {
        return table.containsKey(lexeme);
    }
    
    public int getNextMemoryLocation() {
        return memoryCounter++;
    }
    
    public void displayTable() {
        System.out.println("----------------------------------------------------------------------------");
        System.out.printf("| %-15s | %-10s | %-10s | %-15s | %-15s |\n", 
                          "Identifier", "Data Type", "Scope", "Memory Location", "Value");
        System.out.println("----------------------------------------------------------------------------");
        for (SymbolEntry entry : table.values()) {
            System.out.printf("| %-15s | %-10s | %-10s | %-15d | %-15s |\n",
                              entry.identifierName, entry.dataType, entry.scope, entry.memoryLocation, 
                              entry.value == null ? "" : entry.value);
        }
        System.out.println("----------------------------------------------------------------------------");
    }
}