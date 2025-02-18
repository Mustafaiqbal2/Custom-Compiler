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
 // In DFA.java
    public void displayDFA() {
        System.out.println("\nDFA Structure:");
        System.out.println("Start State: " + startState.getId());
        
        // Display accepting states
        System.out.print("Accepting States: ");
        StringBuilder accepting = new StringBuilder();
        for (State state : acceptingStates) {
            accepting.append(state.getId()).append(", ");
        }
        if (accepting.length() > 0) {
            accepting.setLength(accepting.length() - 2); // Remove last ", "
        }
        System.out.println(accepting);
        
        // Get all states and sort them for consistent display
        List<State> sortedStates = new ArrayList<>(getAllStates());
        Collections.sort(sortedStates, new Comparator<State>() {
            @Override
            public int compare(State s1, State s2) {
                return Integer.compare(s1.getId(), s2.getId());
            }
        });
        
        // Get all input symbols
        Set<Character> symbols = new TreeSet<>();
        for (State state : sortedStates) {
            symbols.addAll(getTransitions(state).keySet());
        }
        
        // Print transition table header
        System.out.println("\nTransition Table:");
        System.out.printf("%-10s|", "State");
        for (char symbol : symbols) {
            System.out.printf(" %-8s|", String.valueOf(symbol));
        }
        System.out.println();
        
        // Print separator line
        StringBuilder separator = new StringBuilder();
        for (int i = 0; i < 10 + symbols.size() * 9; i++) {
            separator.append("-");
        }
        System.out.println(separator);
        
        // Print transitions for each state
        for (State state : sortedStates) {
            String stateStr = state.getId() + (acceptingStates.contains(state) ? "*" : " ");
            System.out.printf("%-9s|", stateStr);
            
            Map<Character, State> stateTransitions = getTransitions(state);
            for (char symbol : symbols) {
                State target = stateTransitions.get(symbol);
                System.out.printf(" %-8s|", target != null ? target.getId() : "-");
            }
            System.out.println();
        }
        System.out.println();
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

	public boolean isAccepting(State currentState) {
		if (acceptingStates.contains(currentState)) {
			return true;
		}
		return false;
	}
}