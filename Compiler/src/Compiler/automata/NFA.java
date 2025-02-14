package Compiler.automata;

import java.util.*;

public class NFA {
    private State startState;
    private Set<State> acceptingStates;
    private Set<State> allStates;
    private Set<Character> alphabet;

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

    public void addTransition(State from, char symbol, State to) {
        from.addTransition(symbol, to);
        alphabet.add(symbol);
        allStates.add(from);
        allStates.add(to);
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
        // Subset construction algorithm
        DFA dfa = new DFA();
        Map<Set<State>, State> dfaStates = new HashMap<>();
        Queue<Set<State>> unprocessed = new LinkedList<>();

        // Start with ε-closure of start state
        Set<State> initialSet = epsilonClosure(Set.of(startState));
        State dfaStart = new State();
        dfaStates.put(initialSet, dfaStart);
        unprocessed.add(initialSet);
        dfa.setStartState(dfaStart);

        // If initial set contains any accepting state, make DFA start state accepting
        if (initialSet.stream().anyMatch(State::isAccepting)) {
            dfa.addAcceptingState(dfaStart);
        }

        while (!unprocessed.isEmpty()) {
            Set<State> currentSet = unprocessed.poll();
            State currentDFAState = dfaStates.get(currentSet);

            for (char symbol : alphabet) {
                // Create a final reference to the next set of states
                final Set<State> nextStateSet = epsilonClosure(move(currentSet, symbol));

                if (!nextStateSet.isEmpty()) {
                    // Create or get the DFA state for this set
                    State nextDFAState = dfaStates.get(nextStateSet);
                    if (nextDFAState == null) {
                        nextDFAState = new State();
                        if (nextStateSet.stream().anyMatch(State::isAccepting)) {
                            dfa.addAcceptingState(nextDFAState);
                        }
                        dfaStates.put(nextStateSet, nextDFAState);
                        unprocessed.add(nextStateSet);
                    }

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