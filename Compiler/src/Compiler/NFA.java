package Compiler;

import java.util.*;

public class NFA {
    private State startState;
    private Set<State> states;
    private Set<State> acceptingStates;
    
    public NFA() {
        this.states = new HashSet<>();
        this.acceptingStates = new HashSet<>();
    }
    
    public void addState(State state) {
        states.add(state);
    }
    
    public void setStartState(State state) {
        this.startState = state;
    }
    
    public void addAcceptingState(State state) {
        state.setAccepting(true);
        acceptingStates.add(state);
    }
    
    // Convert NFA to DFA
    public DFA toDFA() {
        // Implementation of subset construction algorithm
        // This will be implemented in Phase 2
        return new DFA();
    }
    
    // Get total number of states
    public int getStateCount() {
        return states.size();
    }
    
    // Display transition table
    public void displayTransitionTable() {
        // Implementation for displaying the transition table
    }
}