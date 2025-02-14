package Compiler.automata;

import java.util.*;

public class State {
    private int id;
    private boolean isAccepting;
    private Map<Character, Set<State>> transitions;
    private static int stateCounter = 0;

    public State() {
        this.id = stateCounter++;
        this.isAccepting = false;
        this.transitions = new HashMap<>();
    }

    public void addTransition(char symbol, State target) {
        transitions.computeIfAbsent(symbol, k -> new HashSet<>()).add(target);
    }

    public Set<State> getTransitions(char symbol) {
        return transitions.getOrDefault(symbol, new HashSet<>());
    }

    public Map<Character, Set<State>> getAllTransitions() {
        return transitions;
    }

    public int getId() {
        return id;
    }

    public void setAccepting(boolean accepting) {
        this.isAccepting = accepting;
    }

    public boolean isAccepting() {
        return isAccepting;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return id == state.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}