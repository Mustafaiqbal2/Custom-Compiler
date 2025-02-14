package Compiler.automata;

import java.util.*;

public class DFA {
    private State startState;
    private Set<State> states;
    private Set<State> acceptingStates;
    private Set<Character> alphabet;

    public DFA() {
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

    public boolean accepts(String input) {
        State currentState = startState;
        
        for (char c : input.toCharArray()) {
            Set<State> transitions = currentState.getTransitions(c);
            if (transitions.isEmpty()) {
                return false;
            }
            currentState = transitions.iterator().next();
        }
        
        return currentState.isAccepting();
    }

    public void displayTransitionTable() {
        System.out.println("DFA Transition Table:");
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
                    String.valueOf(transitions.iterator().next().getId());
                row.append(transitionStr).append("\t| ");
            }
            System.out.println(row);
        }
    }
}