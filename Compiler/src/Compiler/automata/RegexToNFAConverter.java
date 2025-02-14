package Compiler.automata;

import java.util.*;

public class RegexToNFAConverter {
    private int stateCounter = 0;

    public NFA convert(String regex) {
        return thompsonConstruction(parseRegex(regex));
    }

    private String parseRegex(String regex) {
        // First preprocess to add explicit concatenation operators
        String preprocessed = preprocess(regex);
        // Then convert from infix to postfix notation
        return infixToPostfix(preprocessed);
    }

    private NFA handleCharacterClass(String content) {
        Set<Character> chars = new HashSet<>();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (i + 2 < content.length() && content.charAt(i + 1) == '-') {
                // Handle range like a-z
                char end = content.charAt(i + 2);
                for (char ch = c; ch <= end; ch++) {
                    chars.add(ch);
                }
                i += 2;
            } else {
                chars.add(c);
            }
        }
        return characterClass(chars);
    }

    private String preprocess(String regex) {
        StringBuilder processed = new StringBuilder();
        boolean escaped = false;
        
        for (int i = 0; i < regex.length(); i++) {
            char current = regex.charAt(i);
            
            // Handle escape sequences
            if (current == '\\' && !escaped) {
                escaped = true;
                processed.append(current);
                continue;
            }
            
            if (!escaped) {
                processed.append(current);
                
                if (i + 1 < regex.length()) {
                    char next = regex.charAt(i + 1);
                    // Add explicit concatenation operator '.' in specific cases
                    if (shouldAddConcatenation(current, next)) {
                        processed.append('.');
                    }
                }
            } else {
                // Handle escaped character
                processed.append(current);
                escaped = false;
            }
        }
        
        return processed.toString();
    }

    private String infixToPostfix(String infix) {
        StringBuilder postfix = new StringBuilder();
        Stack<Character> operators = new Stack<>();
        Map<Character, Integer> precedence = new HashMap<>();
        
        // Set operator precedence
        precedence.put('|', 1);  // Union
        precedence.put('.', 2);  // Concatenation
        precedence.put('*', 3);  // Kleene star
        precedence.put('+', 3);  // One or more
        precedence.put('?', 3);  // Zero or one
        
        boolean escaped = false;
        
        for (int i = 0; i < infix.length(); i++) {
            char c = infix.charAt(i);
            
            if (c == '\\' && !escaped) {
                escaped = true;
                postfix.append(c);
                continue;
            }
            
            if (escaped) {
                postfix.append(c);
                escaped = false;
                continue;
            }
            
            if (isOperator(c)) {
                while (!operators.isEmpty() && operators.peek() != '(' &&
                       precedence.getOrDefault(operators.peek(), 0) >= precedence.get(c)) {
                    postfix.append(operators.pop());
                }
                operators.push(c);
            }
            else if (c == '(') {
                operators.push(c);
            }
            else if (c == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    postfix.append(operators.pop());
                }
                if (!operators.isEmpty() && operators.peek() == '(') {
                    operators.pop();
                } else {
                    throw new IllegalStateException("Mismatched parentheses");
                }
            }
            else if (c == '[') {
                // Handle character class
                StringBuilder classContent = new StringBuilder();
                i++;
                while (i < infix.length() && infix.charAt(i) != ']') {
                    classContent.append(infix.charAt(i++));
                }
                if (i >= infix.length()) {
                    throw new IllegalStateException("Unclosed character class");
                }
                // Add character class as a special token
                postfix.append("[").append(classContent).append("]");
            }
            else {
                postfix.append(c);
            }
        }
        
        // Pop remaining operators
        while (!operators.isEmpty()) {
            char op = operators.pop();
            if (op == '(') {
                throw new IllegalStateException("Mismatched parentheses");
            }
            postfix.append(op);
        }
        
        return postfix.toString();
    }

    private boolean shouldAddConcatenation(char current, char next) {
        // Add concatenation operator between:
        // 1. letter/digit and letter/digit
        // 2. letter/digit and (
        // 3. ) and letter/digit
        // 4. * and letter/digit
        // 5. + and letter/digit
        // 6. ? and letter/digit
        // 7. ] and letter/digit or (
        return (isLiteralChar(current) && (isLiteralChar(next) || next == '(' || next == '[')) ||
               (current == ')' && (isLiteralChar(next) || next == '(' || next == '[')) ||
               ((current == '*' || current == '+' || current == '?') && 
                (isLiteralChar(next) || next == '(' || next == '[')) ||
               (current == ']' && (isLiteralChar(next) || next == '(' || next == '['));
    }

    private boolean isLiteralChar(char c) {
        return !isOperator(c) && c != '(' && c != ')' && c != '[' && c != ']';
    }

    private boolean isOperator(char c) {
        return c == '|' || c == '*' || c == '+' || c == '?' || c == '.';
    }

    private NFA thompsonConstruction(String regex) {
        Stack<NFA> nfaStack = new Stack<>();
        boolean escaped = false;
        
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            
            if (c == '\\' && !escaped) {
                escaped = true;
                continue;
            }
            
            if (escaped) {
                nfaStack.push(handleEscape(c));
                escaped = false;
                continue;
            }
            
            switch (c) {
                case '|':
                    if (nfaStack.size() < 2) throw new IllegalStateException("Invalid regex: insufficient operands for |");
                    NFA nfa2 = nfaStack.pop();
                    NFA nfa1 = nfaStack.pop();
                    nfaStack.push(union(nfa1, nfa2));
                    break;
                case '*':
                    if (nfaStack.isEmpty()) throw new IllegalStateException("Invalid regex: insufficient operands for *");
                    nfaStack.push(kleeneStar(nfaStack.pop()));
                    break;
                case '+':
                    if (nfaStack.isEmpty()) throw new IllegalStateException("Invalid regex: insufficient operands for +");
                    nfaStack.push(kleenePlus(nfaStack.pop()));
                    break;
                case '?':
                    if (nfaStack.isEmpty()) throw new IllegalStateException("Invalid regex: insufficient operands for ?");
                    nfaStack.push(optional(nfaStack.pop()));
                    break;
                case '.':
                    if (nfaStack.size() < 2) throw new IllegalStateException("Invalid regex: insufficient operands for concatenation");
                    NFA second = nfaStack.pop();
                    NFA first = nfaStack.pop();
                    nfaStack.push(concatenate(first, second));
                    break;
                case '[':
                    StringBuilder classContent = new StringBuilder();
                    i++;
                    while (i < regex.length() && regex.charAt(i) != ']') {
                        classContent.append(regex.charAt(i++));
                    }
                    if (i >= regex.length()) {
                        throw new IllegalStateException("Unclosed character class");
                    }
                    nfaStack.push(handleCharacterClass(classContent.toString()));
                    break;
                default:
                    nfaStack.push(createBasicNFA(c));
            }
        }
        
        if (nfaStack.size() != 1) throw new IllegalStateException("Invalid regex: improper expression");
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
        result.setStartState(nfa1.getStartState());
        
        // Add ε-transitions from nfa1's accepting states to nfa2's start state
        for (State s : nfa1.getAcceptingStates()) {
            result.addEpsilonTransition(s, nfa2.getStartState());
        }
        
        // Set nfa2's accepting states as the new accepting states
        for (State s : nfa2.getAcceptingStates()) {
            result.addAcceptingState(s);
        }
        
        // Copy all transitions
        result.addAllTransitions(nfa1);
        result.addAllTransitions(nfa2);
        
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
            
            // Add transitions for each character in the class
            for (char c : chars) {
                result.addTransition(start, c, end);
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