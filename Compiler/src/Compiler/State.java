package Compiler;

import java.util.*;

public class State {
    private int id;
    private boolean isAccepting;
    private Map<Character, Set<State>> transitions;
    
    public State(int id) {
        this.id = id;
        this.isAccepting = false;
        this.transitions = new HashMap<>();
    }
    
    public void addTransition(char symbol, State target) {
        transitions.computeIfAbsent(symbol, k -> new HashSet<>()).add(target);
    }
    
    public Set<State> getTransitions(char symbol) {
        return transitions.getOrDefault(symbol, new HashSet<>());
    }
    
    // Getters and setters
    public int getId() { return id; }
    public boolean isAccepting() { return isAccepting; }
    public void setAccepting(boolean accepting) { isAccepting = accepting; }
}