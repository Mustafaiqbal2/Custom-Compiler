package Compiler.automata;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
        this.startState = null; // Explicitly initialize to null
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
        AtomicInteger stateCounter = new AtomicInteger(1); // Using AtomicInteger for unique state IDs

        // Get initial epsilon closure
        Set<State> initialSet = epsilonClosure(Set.of(startState));
        State dfaStart = new State("d0");
        dfaStates.put(initialSet, dfaStart);
        unprocessed.add(initialSet);
        dfa.setStartState(dfaStart);

        // Mark as accepting if any NFA state in initialSet is accepting.
        if (isAnyAccepting(initialSet)) {
            dfa.addAcceptingState(dfaStart);
        }

        while (!unprocessed.isEmpty()) {
            Set<State> currentSet = unprocessed.poll();
            State currentDFAState = dfaStates.get(currentSet);

            // For each input symbol in the NFA's alphabet
            for (char symbol : alphabet) {
                Set<State> nextStateSet = epsilonClosure(move(currentSet, symbol));

                if (!nextStateSet.isEmpty()) {
                    // Create or get the existing DFA state
                    State nextDFAState = dfaStates.computeIfAbsent(nextStateSet, k -> {
                        State newState = new State("d" + stateCounter.getAndIncrement());
                        if (isAnyAccepting(nextStateSet)) {
                            dfa.addAcceptingState(newState);
                        }
                        unprocessed.add(nextStateSet);
                        return newState;
                    });

                    // Add transition in the DFA.
                    dfa.addTransition(currentDFAState, symbol, nextDFAState);
                }
            }
        }

        return dfa;
    }

    private boolean isAnyAccepting(Set<State> states) {
        for (State s : states) {
            if (s.isAccepting()) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isAcceptingStateSet(Set<State> states) {
        // For concatenation, we need to make sure we've reached an accepting state
        // through the proper sequence of transitions
        for (State state : acceptingStates) {
            if (!states.contains(state)) {
                return false;
            }
        }
        return true;
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