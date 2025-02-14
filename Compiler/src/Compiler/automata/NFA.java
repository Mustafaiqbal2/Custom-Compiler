package Compiler.automata;

import java.util.*;

public class NFA {
    private State startState;
    private Set<State> states;
    private Set<State> acceptingStates;
    private Set<Character> alphabet;

    public NFA() {
        this.states = new HashSet<>();
        this.acceptingStates = new HashSet<>();
        this.alphabet = new HashSet<>();
    }

    public void addState(State state) {
        states.add(state);
    }

    public void setStartState(State state) {
        this.startState = state;
        addState(state);
    }

    public void addAcceptingState(State state) {
        state.setAccepting(true);
        acceptingStates.add(state);
        addState(state);
    }

    public Set<State> getEpsilonClosure(Set<State> states) {
        Stack<State> stack = new Stack<>();
        Set<State> closure = new HashSet<>(states);
        states.forEach(stack::push);

        while (!stack.empty()) {
            State current = stack.pop();
            Set<State> epsilonTransitions = current.getTransitions('\u0000');
            
            for (State nextState : epsilonTransitions) {
                if (closure.add(nextState)) {
                    stack.push(nextState);
                }
            }
        }
        return closure;
    }

    public DFA toDFA() {
        DFA dfa = new DFA();
        Map<Set<State>, State> dfaStates = new HashMap<>();
        Queue<Set<State>> unprocessedStates = new LinkedList<>();

        // Start with epsilon closure of NFA start state
        Set<State> initialStates = getEpsilonClosure(Set.of(startState));
        State dfaStartState = new State();
        dfaStates.put(initialStates, dfaStartState);
        unprocessedStates.offer(initialStates);
        dfa.setStartState(dfaStartState);

        while (!unprocessedStates.isEmpty()) {
            Set<State> currentStates = unprocessedStates.poll();
            State currentDFAState = dfaStates.get(currentStates);

            // Check if this set of NFA states contains an accepting state
            if (currentStates.stream().anyMatch(s -> acceptingStates.contains(s))) {
                dfa.addAcceptingState(currentDFAState);
            }

            // Process each symbol in the alphabet
            for (char symbol : alphabet) {
                Set<State> nextStates = new HashSet<>();
                
                // Get all possible transitions for the current set of states
                for (State state : currentStates) {
                    Set<State> transitions = state.getTransitions(symbol);
                    nextStates.addAll(transitions);
                }

                // Include epsilon transitions
                nextStates = getEpsilonClosure(nextStates);

                if (!nextStates.isEmpty()) {
                    State nextDFAState = dfaStates.computeIfAbsent(nextStates, k -> new State());
                    currentDFAState.addTransition(symbol, nextDFAState);
                    
                    if (!dfaStates.containsKey(nextStates)) {
                        unprocessedStates.offer(nextStates);
                    }
                }
            }
        }

        return dfa;
    }

    public void displayTransitionTable() {
        System.out.println("NFA Transition Table:");
        System.out.println("State\t| " + String.join("\t| ", alphabet.stream()
                                                    .map(String::valueOf)
                                                    .toArray(String[]::new)));
        System.out.println("-".repeat(50));

        for (State state : states) {
            StringBuilder row = new StringBuilder();
            row.append(state.getId()).append(state.isAccepting() ? "*" : "")
               .append("\t| ");

            for (char symbol : alphabet) {
                Set<State> transitions = state.getTransitions(symbol);
                String transitionStr = transitions.isEmpty() ? "-" :
                    transitions.stream()
                             .map(s -> String.valueOf(s.getId()))
                             .reduce((a, b) -> a + "," + b)
                             .orElse("-");
                row.append(transitionStr).append("\t| ");
            }
            System.out.println(row);
        }
    }
}