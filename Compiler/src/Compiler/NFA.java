package Compiler;

import java.util.*;
/////////
public class NFA {
    public State startState;
    public State acceptState;
    
    public NFA(State startState, State acceptState) {
        this.startState = startState;
        this.acceptState = acceptState;
    }
    
    // Inner class representing a state in the NFA.
    public static class State {
        public int id;
        public Map<Character, List<State>> transitions;
        public List<State> epsilonTransitions;
        
        public State(int id) {
            this.id = id;
            transitions = new HashMap<>();
            epsilonTransitions = new ArrayList<>();
        }
        
        public void addTransition(char c, State next) {
            transitions.computeIfAbsent(c, k -> new ArrayList<>()).add(next);
        }
        
        public void addEpsilonTransition(State next) {
            epsilonTransitions.add(next);
        }
    }
}