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
        int i = 0;
        
        while (i < regex.length()) {
            char c = regex.charAt(i);
            
            // Handle escape sequences
            if (c == '\\' && !escaped) {
                escaped = true;
                i++;
                continue;
            }
            
            if (escaped) {
                // Keep special characters escaped
                processed.append('\\').append(c);
                escaped = false;
                i++;
                continue;
            }

            // Handle character classes
            if (c == '[' && !escaped) {
                inCharClass = true;
                processed.append(c);
                i++;
                continue;
            }
            
            if (c == ']' && !escaped) {
                inCharClass = false;
                processed.append(c);
                i++;
                continue;
            }
            
            // When in character class, don't add concatenation
            if (inCharClass) {
                processed.append(c);
                i++;
                continue;
            }
            
            // Handle special cases for operators
            if (c == '*' || c == '+' || c == '?' || c == '|') {
                processed.append(c);
                i++;
                continue;
            }
            
            processed.append(c);
            
            // Add concatenation only between appropriate characters
            if (i < regex.length() - 1) {
                char next = regex.charAt(i + 1);
                if (shouldAddConcatenation(c, next)) {
                    processed.append('.');
                }
            }
            i++;
        }
        
        return processed.toString();
    }

    private boolean shouldAddConcatenation(char current, char next) {
        // Don't add concatenation before operators or after escape
        if (next == '*' || next == '+' || next == '?' || next == '|' || 
            next == ')' || next == ']' || current == '\\' ||
            current == '(' || current == '[' || current == '|') {
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
        boolean escaped = false;
        
        for (int i = 0; i < postfix.length(); i++) {
            char c = postfix.charAt(i);
            
            if (c == '\\' && !escaped) {
                escaped = true;
                continue;
            }
            
            if (escaped) {
                stack.push(createBasicNFA(c));
                escaped = false;
                continue;
            }
            
            if (c == '[') {
                StringBuilder charClass = new StringBuilder();
                while (++i < postfix.length() && postfix.charAt(i) != ']') {
                    if (postfix.charAt(i) == '\\') {
                        charClass.append(postfix.charAt(++i));
                    } else {
                        charClass.append(postfix.charAt(i));
                    }
                }
                stack.push(createCharacterClassNFA(charClass.toString()));
            } else if (!isOperator(c)) {
                stack.push(createBasicNFA(c));
            } else {
                try {
                    switch (c) {
                        case '|':
                            if (stack.size() < 2) throw new IllegalArgumentException("Invalid union operation");
                            NFA nfa2 = stack.pop();
                            NFA nfa1 = stack.pop();
                            stack.push(createUnionNFA(nfa1, nfa2));
                            break;
                        case '.':
                            if (stack.size() < 2) throw new IllegalArgumentException("Invalid concatenation operation");
                            NFA second = stack.pop();
                            NFA first = stack.pop();
                            stack.push(createConcatenationNFA(first, second));
                            break;
                        case '*':
                            if (stack.isEmpty()) throw new IllegalArgumentException("Invalid Kleene star operation");
                            stack.push(createKleeneStarNFA(stack.pop()));
                            break;
                        case '+':
                            if (stack.isEmpty()) throw new IllegalArgumentException("Invalid plus operation");
                            stack.push(createPlusNFA(stack.pop()));
                            break;
                        case '?':
                            if (stack.isEmpty()) throw new IllegalArgumentException("Invalid optional operation");
                            stack.push(createOptionalNFA(stack.pop()));
                            break;
                    }
                } catch (EmptyStackException e) {
                    throw new IllegalArgumentException("Invalid regex expression: missing operand", e);
                }
            }
        }
        
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Invalid regex expression: empty result");
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
        boolean negated = false;
        int i = 0;
        
        // Check for negation
        if (charClass.startsWith("^")) {
            negated = true;
            i++;
        }
        
        // Process character class content
        while (i < charClass.length()) {
            char c = charClass.charAt(i);
            
            if (c == '\\' && !escaped) {
                escaped = true;
                i++;
                continue;
            }
            
            if (escaped) {
                validChars.add(charClass.charAt(i));
                escaped = false;
                i++;
                continue;
            }
            
            if (i + 2 < charClass.length() && charClass.charAt(i + 1) == '-') {
                char start = c;
                char end = charClass.charAt(i + 2);
                for (char ch = start; ch <= end; ch++) {
                    validChars.add(ch);
                }
                i += 3;
            } else {
                validChars.add(c);
                i++;
            }
        }
        
        if (negated) {
            Set<Character> allChars = new HashSet<>();
            for (char c = 0; c < 128; c++) { // ASCII characters
                allChars.add(c);
            }
            allChars.removeAll(validChars);
            validChars = allChars;
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