package Compiler.automata;

import java.util.*;
import java.util.stream.Collectors;

public class RegexToNFAConverter {
    private int stateCounter = 0;

    public NFA convert(String regex) {
        System.out.println("Converting regex: " + regex);
        String postfix = parseRegex(regex);
        System.out.println("Postfix notation: " + postfix);
        return thompsonConstruction(postfix);
    }
    private String parseRegex(String regex) {
        // First preprocess to add explicit concatenation operators
        String preprocessed = preprocess(regex);
        System.out.println("After preprocessing: " + preprocessed);
        // Then convert from infix to postfix notation
        return infixToPostfix(preprocessed);
    }
    private String preprocess(String regex) {
        StringBuilder processed = new StringBuilder();
        boolean inCharClass = false;

        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            
            if (c == '[') {
                inCharClass = true;
                processed.append(c);
                continue;
            }
            
            if (c == ']') {
                inCharClass = false;
                processed.append(c);
                continue;
            }
            
            processed.append(c);
            
            // Only add concatenation if not in character class and not at last character
            if (!inCharClass && i < regex.length() - 1) {
                char next = regex.charAt(i + 1);
                // Add concatenation operator if needed
                if (shouldAddConcatenation(c, next)) {
                    processed.append('.');
                }
            }
        }
        
        return processed.toString();
    }

    private String infixToPostfix(String infix) {
        StringBuilder postfix = new StringBuilder();
        Stack<Character> operators = new Stack<>();
        Map<Character, Integer> precedence = new HashMap<>();
        boolean inCharClass = false;
        
        // Set operator precedence
        precedence.put('|', 1);  // Union has lowest precedence
        precedence.put('.', 2);  // Concatenation has higher precedence than union
        precedence.put('*', 3);  // Unary operators have highest precedence
        precedence.put('+', 3);
        precedence.put('?', 3);
        
        for (int i = 0; i < infix.length(); i++) {
            char c = infix.charAt(i);
            
            if (c == '[') {
                inCharClass = true;
                postfix.append(c);
            } else if (c == ']') {
                inCharClass = false;
                postfix.append(c);
            } else if (inCharClass) {
                postfix.append(c);
            } else if (isOperator(c)) {
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
        
        while (!operators.isEmpty()) {
            char op = operators.pop();
            if (op != '(') {
                postfix.append(op);
            }
        }
        
        return postfix.toString();
    }

    private boolean shouldAddConcatenation(char current, char next) {
        // Don't add concatenation for character class boundaries
        if (current == '[' || next == ']') {
            return false;
        }
        
        // Add concatenation between:
        // 1. Two literals
        // 2. Literal and opening parenthesis
        // 3. Closing parenthesis and literal
        // 4. Closing character class and literal/opening parenthesis
        // 5. Unary operator and literal/opening parenthesis
        boolean currentCanPrefix = isLiteralChar(current) || current == ')' || current == ']' 
                                 || current == '*' || current == '+' || current == '?';
        boolean nextCanSuffix = isLiteralChar(next) || next == '(' || next == '[';
        
        return currentCanPrefix && nextCanSuffix;
    }

    private boolean isLiteralChar(char c) {
        return !isOperator(c) && c != '(' && c != ')' && c != '[' && c != ']';
    }

    private boolean isOperator(char c) {
        return c == '|' || c == '*' || c == '+' || c == '?' || c == '.';
    }

    private NFA thompsonConstruction(String regex) {
        Stack<NFA> nfaStack = new Stack<>();
        boolean inCharClass = false;
        StringBuilder classContent = new StringBuilder();

        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            
            System.out.println("Processing character: " + c); // Debug output
            
            if (c == '[' && !inCharClass) {
                inCharClass = true;
                continue;
            }
            
            if (inCharClass) {
                if (c == ']') {
                    inCharClass = false;
                    String content = classContent.toString();
                    System.out.println("Processing character class: [" + content + "]");
                    nfaStack.push(handleCharacterClass(content));
                    classContent = new StringBuilder();
                } else {
                    classContent.append(c);
                }
                continue;
            }

            try {
                switch (c) {
                    case '|':
                        if (nfaStack.size() < 2) 
                            throw new IllegalStateException("Invalid regex: insufficient operands for |");
                        NFA nfa2 = nfaStack.pop();
                        NFA nfa1 = nfaStack.pop();
                        nfaStack.push(union(nfa1, nfa2));
                        break;
                        
                    case '.': // Explicit concatenation operator
                        if (nfaStack.size() < 2) 
                            throw new IllegalStateException("Invalid regex: insufficient operands for concatenation");
                        NFA second = nfaStack.pop();
                        NFA first = nfaStack.pop();
                        nfaStack.push(concatenate(first, second));
                        break;
                        
                    case '*':
                        if (nfaStack.isEmpty()) 
                            throw new IllegalStateException("Invalid regex: insufficient operands for *");
                        nfaStack.push(kleeneStar(nfaStack.pop()));
                        break;
                        
                    case '+':
                        if (nfaStack.isEmpty()) 
                            throw new IllegalStateException("Invalid regex: insufficient operands for +");
                        nfaStack.push(kleenePlus(nfaStack.pop()));
                        break;
                        
                    case '?':
                        if (nfaStack.isEmpty()) 
                            throw new IllegalStateException("Invalid regex: insufficient operands for ?");
                        nfaStack.push(optional(nfaStack.pop()));
                        break;
                        
                    default:
                        if (c != '(' && c != ')') { // Ignore parentheses in postfix
                            nfaStack.push(createBasicNFA(c));
                        }
                }
            } catch (Exception e) {
                System.err.println("Error processing character '" + c + "' at position " + i);
                throw e;
            }
        }
        
        if (nfaStack.size() != 1) {
            System.err.println("Final stack size: " + nfaStack.size()); // Debug output
            throw new IllegalStateException("Invalid regex: improper expression");
        }
        
        return nfaStack.pop();
    }
    
    private NFA createBasicNFA(char c) {
        NFA nfa = new NFA();
        State start = new State("q" + stateCounter++);
        State end = new State("q" + stateCounter++);
        
        nfa.setStartState(start);
        nfa.addAcceptingState(end);
        nfa.addTransition(start, c, end);
        
        return nfa;
    }

    private NFA union(NFA nfa1, NFA nfa2) {
        NFA result = new NFA();
        State start = new State("q" + stateCounter++);
        State end = new State("q" + stateCounter++);
        
        result.setStartState(start);
        result.addAcceptingState(end);
        
        // Add ε-transitions from new start state
        result.addEpsilonTransition(start, nfa1.getStartState());
        result.addEpsilonTransition(start, nfa2.getStartState());
        
        // Add ε-transitions to new end state
        for (State s : nfa1.getAcceptingStates()) {
            result.addEpsilonTransition(s, end);
        }
        for (State s : nfa2.getAcceptingStates()) {
            result.addEpsilonTransition(s, end);
        }
        
        // Copy all transitions
        result.addAllTransitions(nfa1);
        result.addAllTransitions(nfa2);
        
        return result;
    }

    private NFA concatenate(NFA nfa1, NFA nfa2) {
        NFA result = new NFA();
        
        // Set start state
        result.setStartState(nfa1.getStartState());
        
        // Clear accepting status of nfa1's accepting states
        for (State s : nfa1.getAcceptingStates()) {
            s.setAccepting(false);
        }
        
        // Add epsilon transitions from nfa1's accepting states to nfa2's start state
        for (State s : nfa1.getAcceptingStates()) {
            result.addEpsilonTransition(s, nfa2.getStartState());
        }
        
        // Set accepting states from nfa2
        for (State s : nfa2.getAcceptingStates()) {
            result.addAcceptingState(s);
        }
        
        // Copy all transitions
        result.addAllTransitions(nfa1);
        result.addAllTransitions(nfa2);
        
        // Debug output without using stream operations
        System.out.println("Created concatenation NFA:");
        System.out.println("Start state: " + result.getStartState().getId());
        
        // Build accepting states string manually
        StringBuilder acceptingStates = new StringBuilder();
        boolean first = true;
        for (State s : result.getAcceptingStates()) {
            if (!first) {
                acceptingStates.append(", ");
            }
            acceptingStates.append(s.getId());
            first = false;
        }
        System.out.println("Accepting states: " + acceptingStates.toString());
        
        return result;
    }

    private NFA kleeneStar(NFA nfa) {
        NFA result = new NFA();
        State start = new State("q" + stateCounter++);
        State end = new State("q" + stateCounter++);
        
        result.setStartState(start);
        result.addAcceptingState(end);
        
        // Add ε-transitions for the star operation
        result.addEpsilonTransition(start, end);
        result.addEpsilonTransition(start, nfa.getStartState());
        
        for (State s : nfa.getAcceptingStates()) {
            result.addEpsilonTransition(s, end);
            result.addEpsilonTransition(s, nfa.getStartState());
        }
        
        // Copy all transitions
        result.addAllTransitions(nfa);
        
        return result;
    } 
    private NFA kleenePlus(NFA nfa) {
        	// Similar to kleene star but must have at least one occurrence
            NFA result = new NFA();
            result.setStartState(nfa.getStartState());
            
            State end = new State("q" + stateCounter++);
            result.addAcceptingState(end);
            
            for (State s : nfa.getAcceptingStates()) {
                result.addEpsilonTransition(s, end);
                result.addEpsilonTransition(s, nfa.getStartState());
            }
            
            // Copy all transitions
            result.addAllTransitions(nfa);
            
            return result;
        }

        private NFA optional(NFA nfa) {
            NFA result = new NFA();
            State start = new State("q" + stateCounter++);
            State end = new State("q" + stateCounter++);
            
            result.setStartState(start);
            result.addAcceptingState(end);
            
            // Add ε-transition to skip the NFA
            result.addEpsilonTransition(start, end);
            
            // Add ε-transition to enter the NFA
            result.addEpsilonTransition(start, nfa.getStartState());
            
            // Add ε-transitions from original accepting states to new end state
            for (State s : nfa.getAcceptingStates()) {
                result.addEpsilonTransition(s, end);
            }
            
            // Copy all transitions
            result.addAllTransitions(nfa);
            
            return result;
        }

        private NFA characterClass(Set<Character> chars) {
            NFA result = new NFA();
            State start = new State("q" + stateCounter++);
            State end = new State("q" + stateCounter++);
            
            result.setStartState(start);
            result.addAcceptingState(end);
            
            // Create a single state for the character class
            for (char c : chars) {
                // Add direct transitions without epsilon moves
                result.addTransition(start, c, end);
            }
            
            return result;
        }

        private NFA handleCharacterClass(String content) {
            Set<Character> chars = new HashSet<>();
            
            // Debug output
            System.out.println("Processing character class content: " + content);
            
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (i + 2 < content.length() && content.charAt(i + 1) == '-') {
                    // Handle range like a-z
                    char start = c;
                    char end = content.charAt(i + 2);
                    System.out.printf("Processing range: %c-%c%n", start, end);
                    
                    // Validate range
                    if (end < start) {
                        throw new IllegalArgumentException("Invalid range: " + start + "-" + end);
                    }
                    
                    // Add all characters in the range inclusive
                    for (char ch = start; ch <= end; ch++) {
                        chars.add(ch);
                        System.out.println("Adding character from range: " + ch);
                    }
                    i += 2; // Skip the hyphen and end character
                } else if (c != '-') { // Skip lone hyphens
                    System.out.println("Adding single character: " + c);
                    chars.add(c);
                }
            }
            
            // Debug output
            System.out.println("Final character set: " + chars);
            
            NFA result = new NFA();
            State start = new State("q" + stateCounter++);
            State end = new State("q" + stateCounter++);
            
            result.setStartState(start);
            result.addAcceptingState(end);
            
            // Add transitions for each character
            for (char c : chars) {
                result.addTransition(start, c, end);
                System.out.printf("Added transition for character: %c%n", c);
            }
            
            return result;
        }
        private NFA rangeNFA(char start, char end) {
            Set<Character> chars = new HashSet<>();
            for (char c = start; c <= end; c++) {
                chars.add(c);
            }
            return characterClass(chars);
        }

        private NFA handleEscape(char c) {
            // Handle escaped characters
            switch (c) {
                case 'd': // digits
                    return rangeNFA('0', '9');
                case 'w': // word characters
                    NFA letters = union(rangeNFA('a', 'z'), rangeNFA('A', 'Z'));
                    NFA digits = rangeNFA('0', '9');
                    return union(letters, digits);
                case 's': // whitespace
                    Set<Character> whitespace = Set.of(' ', '\t', '\n', '\r', '\f');
                    return characterClass(whitespace);
                default:
                    return createBasicNFA(c);
            }
        }
    }