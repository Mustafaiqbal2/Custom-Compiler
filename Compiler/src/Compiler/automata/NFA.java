package Compiler.automata;

import java.util.*;

public class NFA {
    private State startState;
    private Set<State> acceptingStates;
    private Set<State> allStates;
    private Set<Character> alphabet;
    private static final char EPSILON = '\0';


    public State getStartState() {
        return startState;
    }

    public Set<State> getAcceptingStates() {
        return Collections.unmodifiableSet(acceptingStates);
    }


    public void addTransition(State from, char symbol, State to) {
        from.addTransition(symbol, to);
        if (symbol != EPSILON) {
            alphabet.add(symbol);
        }
        allStates.add(from);
        allStates.add(to);
    }

    public void addEpsilonTransition(State from, State to) {
        addTransition(from, EPSILON, to);
    }

    public void addAllTransitions(NFA other) {
        // Copy all states
        this.allStates.addAll(other.allStates);
        
        // Copy all transitions
        for (State state : other.allStates) {
            Map<Character, Set<State>> transitions = state.getAllTransitions();
            for (Map.Entry<Character, Set<State>> entry : transitions.entrySet()) {
                char symbol = entry.getKey();
                if (symbol != EPSILON) {
                    alphabet.add(symbol);
                }
                for (State target : entry.getValue()) {
                    addTransition(state, symbol, target);
                }
            }
        }
    }
    public NFA() {
        this.acceptingStates = new HashSet<>();
        this.allStates = new HashSet<>();
        this.alphabet = new HashSet<>();
    }

    public void setStartState(State state) {
        this.startState = state;
        allStates.add(state);
    }

    public void addAcceptingState(State state) {
        state.setAccepting(true);
        acceptingStates.add(state);
        allStates.add(state);
    }

    public void displayTransitionTable() {
        System.out.println("NFA Transition Table:");
        System.out.printf("%-10s|", "State");
        for (char c : alphabet) {
            System.out.printf(" %-15s|", c);
        }
        System.out.println("\n" + "-".repeat(10 + alphabet.size() * 16));

        List<State> sortedStates = new ArrayList<>(allStates);
        sortedStates.sort(Comparator.comparingInt(State::getId));

        for (State state : sortedStates) {
            System.out.printf("%-8s%s |", state.getId(), state.isAccepting() ? "*" : " ");
            for (char c : alphabet) {
                Set<State> transitions = state.getTransitions(c);
                String transStr = transitions.isEmpty() ? "-" : 
                    transitions.stream()
                             .map(s -> String.valueOf(s.getId()))
                             .reduce((a, b) -> a + "," + b)
                             .orElse("-");
                System.out.printf(" %-15s|", transStr);
            }
            System.out.println();
        }
    }

    public DFA toDFA() {
        DFA dfa = new DFA();
        Map<Set<State>, State> dfaStates = new HashMap<>();
        Queue<Set<State>> unprocessed = new LinkedList<>();

        // Get initial epsilon closure
        Set<State> initialSet = epsilonClosure(Set.of(startState));
        State dfaStart = new State();
        dfaStates.put(initialSet, dfaStart);
        unprocessed.add(initialSet);
        dfa.setStartState(dfaStart);

        // Handle accepting states in initial set
        if (initialSet.stream().anyMatch(State::isAccepting)) {
            dfa.addAcceptingState(dfaStart);
        }

        while (!unprocessed.isEmpty()) {
            Set<State> currentSet = unprocessed.poll();
            State currentDFAState = dfaStates.get(currentSet);

            // For each input symbol
            for (char symbol : alphabet) {
                // Get next state set including epsilon closure
                Set<State> nextStateSet = epsilonClosure(move(currentSet, symbol));

                if (!nextStateSet.isEmpty()) {
                    // Create or get existing DFA state
                    State nextDFAState = dfaStates.computeIfAbsent(nextStateSet, k -> {
                        State newState = new State();
                        if (nextStateSet.stream().anyMatch(State::isAccepting)) {
                            dfa.addAcceptingState(newState);
                        }
                        unprocessed.add(nextStateSet);
                        return newState;
                    });

                    // Add transition in DFA
                    dfa.addTransition(currentDFAState, symbol, nextDFAState);
                }
            }
        }

        return dfa;
    }
    private Set<State> epsilonClosure(Set<State> states) {
        Set<State> closure = new HashSet<>(states);
        Stack<State> stack = new Stack<>();
        stack.addAll(states);

        while (!stack.isEmpty()) {
            State current = stack.pop();
            for (State next : current.getTransitions('\0')) {
                if (closure.add(next)) {
                    stack.push(next);
                }
            }
        }

        return closure;
    }

    private Set<State> move(Set<State> states, char symbol) {
        Set<State> result = new HashSet<>();
        for (State state : states) {
            result.addAll(state.getTransitions(symbol));
        }
        return result;
	}
}