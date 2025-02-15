package Compiler.automata;

import java.util.*;

public class RegexToNFAConverter {
    private int stateCounter = 0;


    private String parseRegex(String regex) {
        String preprocessed = preprocess(regex);
        System.out.println("After preprocessing: " + preprocessed);
        return infixToPostfix(preprocessed);
    }

   
    private String preprocess(String regex) {
        StringBuilder processed = new StringBuilder();
        boolean inCharClass = false;
        boolean escaped = false;

        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            
            if (c == '\\' && !escaped) {
                escaped = true;
                continue;
            }
            
            if (escaped) {
                // Handle escaped characters
                switch (c) {
                    case '*':
                    case '+':
                    case '?':
                    case '|':
                    case '.':
                    case '\\':
                    case '[':
                    case ']':
                    case '(':
                    case ')':
                    case '{':
                    case '}':
                        processed.append(c);
                        break;
                    case 'n':
                        processed.append('\n');
                        break;
                    case 't':
                        processed.append('\t');
                        break;
                    default:
                        processed.append(c);
                }
                escaped = false;
                continue;
            }

            if (c == '[' && !escaped) {
                inCharClass = true;
                processed.append(c);
                continue;
            }
            
            if (c == ']' && !escaped) {
                inCharClass = false;
                processed.append(c);
                continue;
            }
            
            processed.append(c);
            
            if (!inCharClass && !escaped && i < regex.length() - 1) {
                char next = regex.charAt(i + 1);
                if (shouldAddConcatenation(c, next)) {
                    processed.append('.');
                }
            }
        }
        
        return processed.toString();
    }
    private boolean shouldAddConcatenation(char current, char next) {
        // Don't add concatenation in these cases
        if (current == '(' || next == ')' ||
            current == '[' || next == ']' ||
            next == '*' || next == '+' || next == '?' ||
            next == '|' || current == '|' ||
            next == '.') {
            return false;
        }
        return true;
    }

    private String infixToPostfix(String infix) {
        StringBuilder postfix = new StringBuilder();
        Stack<Character> operators = new Stack<>();
        Map<Character, Integer> precedence = new HashMap<>();
        boolean inCharClass = false;
        boolean escaped = false;
        
        precedence.put('|', 1);
        precedence.put('.', 2);
        precedence.put('*', 3);
        precedence.put('+', 3);
        precedence.put('?', 3);
        
        for (int i = 0; i < infix.length(); i++) {
            char c = infix.charAt(i);
            
            if (c == '\\' && !escaped) {
                escaped = true;
                continue;
            }
            
            if (escaped) {
                postfix.append('\\').append(c);
                escaped = false;
                continue;
            }
            
            if (c == '[' && !escaped) {
                inCharClass = true;
                StringBuilder charClass = new StringBuilder();
                charClass.append('[');
                
                while (++i < infix.length()) {
                    char next = infix.charAt(i);
                    charClass.append(next);
                    if (next == ']' && !escaped) {
                        inCharClass = false;
                        break;
                    }
                    if (next == '\\') {
                        escaped = !escaped;
                    }
                }
                
                postfix.append(charClass);
                continue;
            }
            
            if (!inCharClass) {
                if (isOperator(c)) {
                    while (!operators.isEmpty() && operators.peek() != '(' &&
                           precedence.get(operators.peek()) >= precedence.get(c)) {
                        postfix.append(operators.pop());
                    }
                    operators.push(c);
                } else if (c == '(') {
                    operators.push(c);
                } else if (c == ')') {
                    while (!operators.isEmpty() && operators.peek() != '(') {
                        postfix.append(operators.pop());
                    }
                    if (!operators.isEmpty()) {
                        operators.pop(); // Remove '('
                    }
                } else {
                    postfix.append(c);
                }
            }
        }
        
        while (!operators.isEmpty()) {
            char op = operators.pop();
            if (op != '(') {
                postfix.append(op);
            }
        }
        
        return postfix.toString();
    }

    private boolean isOperator(char c) {
        return c == '|' || c == '.' || c == '*' || c == '+' || c == '?';
    }

    public NFA convert(String regex) {
        try {
            System.out.println("Converting regex: " + regex);
            String postfix = parseRegex(regex);
            System.out.println("Postfix notation: " + postfix);
            return thompsonConstruction(postfix);
        } catch (Exception e) {
            System.err.println("Error converting regex: " + e.getMessage());
            throw new IllegalArgumentException("Failed to convert regex: " + regex, e);
        }
    }

    private NFA thompsonConstruction(String postfix) {
        Stack<NFA> stack = new Stack<>();
        
        for (int i = 0; i < postfix.length(); i++) {
            char c = postfix.charAt(i);
            
            if (c == '[') {
                // Handle character class
                StringBuilder charClass = new StringBuilder();
                while (++i < postfix.length() && postfix.charAt(i) != ']') {
                    charClass.append(postfix.charAt(i));
                }
                stack.push(createCharacterClassNFA(charClass.toString()));
            } else if (!isOperator(c)) {
                stack.push(createBasicNFA(c));
            } else {
                switch (c) {
                    case '|':
                        NFA nfa2 = stack.pop();
                        NFA nfa1 = stack.pop();
                        stack.push(createUnionNFA(nfa1, nfa2));
                        break;
                    case '.':
                        NFA second = stack.pop();
                        NFA first = stack.pop();
                        stack.push(createConcatenationNFA(first, second));
                        break;
                    case '*':
                        stack.push(createKleeneStarNFA(stack.pop()));
                        break;
                    case '+':
                        stack.push(createPlusNFA(stack.pop()));
                        break;
                    case '?':
                        stack.push(createOptionalNFA(stack.pop()));
                        break;
                }
            }
        }
        
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Invalid regex expression");
        }
        
        return stack.pop();
    }
    
    private State createNewState() {
        return new State("s" + stateCounter++);
    }
    
    private NFA createBasicNFA(char c) {
        NFA nfa = new NFA();
        State start = createNewState();
        State end = createNewState();
        nfa.setStartState(start);
        nfa.addAcceptingState(end);
        nfa.addTransition(start, c, end);
        return nfa;
    }

    private NFA createCharacterClassNFA(String charClass) {
        Set<Character> validChars = new HashSet<>();
        boolean escaped = false;
        
        for (int i = 0; i < charClass.length(); i++) {
            char c = charClass.charAt(i);
            
            if (c == '\\' && !escaped) {
                escaped = true;
                continue;
            }
            
            if (escaped) {
                validChars.add(c);
                escaped = false;
                continue;
            }
            
            if (i + 2 < charClass.length() && charClass.charAt(i + 1) == '-') {
                char start = c;
                char end = charClass.charAt(i + 2);
                for (char ch = start; ch <= end; ch++) {
                    validChars.add(ch);
                }
                i += 2;
            } else {
                validChars.add(c);
            }
        }
        
        NFA nfa = new NFA();
        State start = createNewState();
        State end = createNewState();
        nfa.setStartState(start);
        nfa.addAcceptingState(end);
        
        for (char c : validChars) {
            nfa.addTransition(start, c, end);
        }
        
        return nfa;
    }

    private NFA createUnionNFA(NFA first, NFA second) {
        NFA nfa = new NFA();
        State start = createNewState();
        State end = createNewState();
        
        nfa.setStartState(start);
        nfa.addAcceptingState(end);
        
        // Add epsilon transitions
        nfa.addEpsilonTransition(start, first.getStartState());
        nfa.addEpsilonTransition(start, second.getStartState());
        
        // Add epsilon transitions from accept states to new end state
        for (State acceptState : first.getAcceptingStates()) {
            nfa.addEpsilonTransition(acceptState, end);
        }
        for (State acceptState : second.getAcceptingStates()) {
            nfa.addEpsilonTransition(acceptState, end);
        }
        
        // Merge all transitions
        nfa.addAllTransitions(first);
        nfa.addAllTransitions(second);
        
        return nfa;
    }

    private NFA createConcatenationNFA(NFA first, NFA second) {
        NFA nfa = new NFA();
        nfa.setStartState(first.getStartState());
        
        // Connect first's accept states to second's start state
        for (State acceptState : first.getAcceptingStates()) {
            nfa.addEpsilonTransition(acceptState, second.getStartState());
        }
        
        // Set second's accept states as the new accept states
        for (State acceptState : second.getAcceptingStates()) {
            nfa.addAcceptingState(acceptState);
        }
        
        // Merge all transitions
        nfa.addAllTransitions(first);
        nfa.addAllTransitions(second);
        
        return nfa;
    }

    private NFA createKleeneStarNFA(NFA nfa) {
        NFA result = new NFA();
        State start = createNewState();
        State end = createNewState();
        
        result.setStartState(start);
        result.addAcceptingState(end);
        
        // Add epsilon transitions
        result.addEpsilonTransition(start, end); // for zero occurrences
        result.addEpsilonTransition(start, nfa.getStartState());
        
        for (State acceptState : nfa.getAcceptingStates()) {
            result.addEpsilonTransition(acceptState, nfa.getStartState());
            result.addEpsilonTransition(acceptState, end);
        }
        
        result.addAllTransitions(nfa);
        return result;
    }

    private NFA createPlusNFA(NFA nfa) {
        NFA result = new NFA();
        State start = createNewState();
        State end = createNewState();
        
        result.setStartState(start);
        result.addAcceptingState(end);
        
        result.addEpsilonTransition(start, nfa.getStartState());
        
        for (State acceptState : nfa.getAcceptingStates()) {
            result.addEpsilonTransition(acceptState, nfa.getStartState());
            result.addEpsilonTransition(acceptState, end);
        }
        
        result.addAllTransitions(nfa);
        return result;
    }

    private NFA createOptionalNFA(NFA nfa) {
        NFA result = new NFA();
        State start = createNewState();
        State end = createNewState();
        
        result.setStartState(start);
        result.addAcceptingState(end);
        
        result.addEpsilonTransition(start, end);
        result.addEpsilonTransition(start, nfa.getStartState());
        
        for (State acceptState : nfa.getAcceptingStates()) {
            result.addEpsilonTransition(acceptState, end);
        }
        
        result.addAllTransitions(nfa);
        return result;
    }
}