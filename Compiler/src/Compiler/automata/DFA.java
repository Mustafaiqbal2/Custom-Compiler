package Compiler.automata;

import java.util.*;

public class DFA {
    private State startState;
    private Set<State> acceptingStates;
    private Set<State> allStates;
    private Set<Character> alphabet;
    private Map<State, Map<Character, State>> transitions;

    public DFA() {
        this.acceptingStates = new HashSet<>();
        this.allStates = new HashSet<>();
        this.alphabet = new HashSet<>();
        this.transitions = new HashMap<>();
    }

    public void setStartState(State state) {
        this.startState = state;
        allStates.add(state);
        transitions.putIfAbsent(state, new HashMap<>());
    }

    public void addAcceptingState(State state) {
        state.setAccepting(true);
        acceptingStates.add(state);
        allStates.add(state);
        transitions.putIfAbsent(state, new HashMap<>());
    }

    public void addTransition(State from, char symbol, State to) {
        alphabet.add(symbol);
        allStates.add(from);
        allStates.add(to);
        
        transitions.putIfAbsent(from, new HashMap<>());
        transitions.putIfAbsent(to, new HashMap<>());
        transitions.get(from).put(symbol, to);
    }

    public boolean accepts(String input) {
        if (startState == null) return false;
        
        State currentState = startState;
        for (char c : input.toCharArray()) {
            if (!alphabet.contains(c) || !transitions.containsKey(currentState)) {
                return false;
            }
            
            Map<Character, State> stateTransitions = transitions.get(currentState);
            if (!stateTransitions.containsKey(c)) {
                return false;
            }
            
            currentState = stateTransitions.get(c);
        }
        
        return acceptingStates.contains(currentState);
    }

    public void displayTransitionTable() {
        System.out.println("DFA Transition Table:");
        System.out.printf("%-10s|", "State");
        
        List<Character> sortedAlphabet = new ArrayList<>(alphabet);
        Collections.sort(sortedAlphabet);
        
        for (char c : sortedAlphabet) {
            System.out.printf(" %-15s|", c);
        }
        System.out.println("\n" + "-".repeat(10 + alphabet.size() * 16));

        List<State> sortedStates = new ArrayList<>(allStates);
        sortedStates.sort(Comparator.comparingInt(State::getId));

        for (State state : sortedStates) {
            System.out.printf("%-8s%s |", state.getId(), state.isAccepting() ? "*" : " ");
            
            for (char symbol : sortedAlphabet) {
                State nextState = transitions.get(state).get(symbol);
                String transStr = nextState == null ? "-" : String.valueOf(nextState.getId());
                System.out.printf(" %-15s|", transStr);
            }
            System.out.println();
        }
    }

    public State getStartState() {
        return startState;
    }

    public Set<State> getAcceptingStates() {
        return Collections.unmodifiableSet(acceptingStates);
    }

    public Set<State> getAllStates() {
        return Collections.unmodifiableSet(allStates);
    }

    public Set<Character> getAlphabet() {
        return Collections.unmodifiableSet(alphabet);
    }

    public Map<Character, State> getTransitions(State state) {
        return transitions.containsKey(state) ? 
               Collections.unmodifiableMap(transitions.get(state)) : 
               Collections.emptyMap();
    }
}