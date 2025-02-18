package Compiler;

import java.util.*;

public class DFA {
    private DFANode startNode;
    private Set<DFANode> allNodes;
    
    // Construct DFA by converting from the provided NFA.
    public DFA(NFA nfa) {
        convertNfaToDfa(nfa);
    }
    
    private void convertNfaToDfa(NFA nfa) {
        Map<Set<NFA.State>, DFANode> dfaStates = new HashMap<>();
        allNodes = new HashSet<>();
        Set<NFA.State> startSet = epsilonClosure(Collections.singleton(nfa.startState));
        DFANode startDfa = new DFANode(startSet, startSet.contains(nfa.acceptState));
        startNode = startDfa;
        dfaStates.put(startSet, startDfa);
        allNodes.add(startDfa);
        
        Queue<DFANode> queue = new LinkedList<>();
        queue.add(startDfa);
        
        while (!queue.isEmpty()) {
            DFANode current = queue.poll();
            Map<Character, Set<NFA.State>> transitions = new HashMap<>();
            for (NFA.State state : current.nfaStates) {
                for (Map.Entry<Character, List<NFA.State>> entry : state.transitions.entrySet()) {
                    char symbol = entry.getKey();
                    for (NFA.State next : entry.getValue()) {
                        transitions.computeIfAbsent(symbol, k -> new HashSet<>())
                                   .addAll(epsilonClosure(Collections.singleton(next)));
                    }
                }
            }
            for (Map.Entry<Character, Set<NFA.State>> entry : transitions.entrySet()) {
                char symbol = entry.getKey();
                Set<NFA.State> targetSet = entry.getValue();
                DFANode targetDfa = dfaStates.get(targetSet);
                if (targetDfa == null) {
                    targetDfa = new DFANode(targetSet, targetSet.contains(nfa.acceptState));
                    dfaStates.put(targetSet, targetDfa);
                    queue.add(targetDfa);
                    allNodes.add(targetDfa);
                }
                current.transitions.put(symbol, targetDfa);
            }
        }
    }
    
    // Computes the epsilon-closure of a set of NFA states.
    private Set<NFA.State> epsilonClosure(Set<NFA.State> states) {
        Set<NFA.State> closure = new HashSet<>(states);
        Stack<NFA.State> stack = new Stack<>();
        stack.addAll(states);
        while (!stack.isEmpty()) {
            NFA.State state = stack.pop();
            for (NFA.State next : state.epsilonTransitions) {
                if (!closure.contains(next)) {
                    closure.add(next);
                    stack.push(next);
                }
            }
        }
        return closure;
    }
    
    // Matches the longest prefix of the input string that the DFA accepts.
    public String match(String input) {
        DFANode current = startNode;
        int lastAcceptIndex = -1;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (current.transitions.containsKey(c)) {
                current = current.transitions.get(c);
                if (current.isAccept) {
                    lastAcceptIndex = i;
                }
            } else {
                break;
            }
        }
        if (lastAcceptIndex != -1) {
            return input.substring(0, lastAcceptIndex + 1);
        }
        return null;
    }
    
    // Displays the DFA transition table.
    public void displayTransitionTable() {
        // Build a mapping from each DFA state to a sequential id.
        Map<DFANode, Integer> stateIdMap = new HashMap<>();
        int id = 0;
        for (DFANode node : allNodes) { // Assume allNodes is a collection of all DFA states
            stateIdMap.put(node, id++);
        }
        
        // Print header for the table.
        System.out.println("DFA Transition Table:");
        System.out.println("---------------------------------------------------------------");
        System.out.printf("| %-8s | %-40s |\n", "State", "Transitions (char -> state)");
        System.out.println("---------------------------------------------------------------");
        
        // For each state, annotate start and accepting states.
        for (DFANode node : allNodes) {
            int stateId = stateIdMap.get(node);
            String marker = "";
            if (node == startNode) {
                marker += "-"; // start state marker
            }
            if (node.isAccept) {
                marker += "+"; // accepting state marker
            }
            // Display the new ID with the marker appended.
            String displayId = stateId + marker;
            StringBuilder transStr = new StringBuilder();
            for (Map.Entry<Character, DFANode> entry : node.transitions.entrySet()) {
                int targetId = stateIdMap.get(entry.getValue());
                transStr.append(entry.getKey()).append("->").append(targetId).append("  ");
            }
            System.out.printf("| %-8s | %-40s |\n", displayId, transStr.toString());
        }
        System.out.println("---------------------------------------------------------------");
    }
    
    // Inner class representing a node (state) in the DFA.
    public static class DFANode {
        public Set<NFA.State> nfaStates;
        public boolean isAccept;
        public Map<Character, DFANode> transitions;
        
        public DFANode(Set<NFA.State> nfaStates, boolean isAccept) {
            this.nfaStates = nfaStates;
            this.isAccept = isAccept;
            this.transitions = new HashMap<>();
        }
    }
}