package Compiler;

public class Symbol {
    private String name;
    private String type;
    private boolean isGlobal;
    private Object value;
    private int scope;
    
    public Symbol(String name, String type, boolean isGlobal, int scope) {
        this.name = name;
        this.type = type;
        this.isGlobal = isGlobal;
        this.scope = scope;
    }
    
    // Getters and setters
    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isGlobal() { return isGlobal; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
    public int getScope() { return scope; }
}