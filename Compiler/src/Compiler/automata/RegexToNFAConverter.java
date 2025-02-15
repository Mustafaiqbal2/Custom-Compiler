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
        boolean inGroup = false;
        
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            
            if (c == '\\' && !escaped) {
                escaped = true;
                processed.append(c);
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
            
            if (c == '(' && !escaped) {
                inGroup = true;
                processed.append(c);
                continue;
            }
            
            if (c == ')' && !escaped) {
                inGroup = false;
                processed.append(c);
                continue;
            }
            
            if (escaped) {
                processed.append(c);
                escaped = false;
                continue;
            }
            
            processed.append(c);
            
            // Only add concatenation if:
            // 1. Not in a character class
            // 2. Not in a group
            // 3. Not escaped
            // 4. Next character exists and needs concatenation
            if (!inCharClass && !inGroup && !escaped && i + 1 < regex.length()) {
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
        return !(current == '\\' || next == '\\' ||  // Escapes
                current == '(' || next == ')' ||     // Groups
                current == '[' || next == ']' ||     // Character classes
                current == '|' || next == '|' ||     // Alternation
                next == '*' || next == '+' || next == '?' || // Quantifiers
                next == '.' ||                       // Explicit concatenation
                (current == '.' && isMetaChar(next))); // Special case for dots
    }

    private boolean isMetaChar(char c) {
        return c == '*' || c == '+' || c == '?' || c == '|' || 
               c == '(' || c == ')' || c == '[' || c == ']' || 
               c == '\\' || c == '.' || c == '^' || c == '$';
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
                postfix.append(c);
                escaped = true;
                continue;
            }
            
            if (escaped) {
                postfix.append(c);
                escaped = false;
                continue;
            }
            
            if (c == '[') {
                inCharClass = true;
                StringBuilder charClass = new StringBuilder("[");
                while (++i < infix.length()) {
                    char curr = infix.charAt(i);
                    charClass.append(curr);
                    if (curr == ']') {
                        inCharClass = false;
                        break;
                    }
                    if (curr == '\\') {
                        charClass.append(infix.charAt(++i));
                    }
                }
                postfix.append(charClass);
                continue;
            }
            
            if (!inCharClass) {
                if (c == '(') {
                    operators.push(c);
                } else if (c == ')') {
                    while (!operators.isEmpty() && operators.peek() != '(') {
                        postfix.append(operators.pop());
                    }
                    if (!operators.isEmpty()) operators.pop(); // Remove '('
                } else if (isOperator(c)) {
                    while (!operators.isEmpty() && operators.peek() != '(' && 
                           precedence.get(operators.peek()) >= precedence.get(c)) {
                        postfix.append(operators.pop());
                    }
                    operators.push(c);
                } else {
                    postfix.append(c);
                }
            }
        }
        
        while (!operators.isEmpty()) {
            char op = operators.pop();
            if (op != '(') postfix.append(op);
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
        StringBuilder group = new StringBuilder();
        boolean inGroup = false;

        for (int i = 0; i < postfix.length(); i++) {
            char c = postfix.charAt(i);

            // Handle character class groups
            if (c == '[' && !inGroup) {
                inGroup = true;
                continue;
            }
            
            if (inGroup) {
                if (c == ']') {
                    inGroup = false;
                    stack.push(createCharacterClassNFA(group.toString()));
                    group = new StringBuilder();
                } else {
                    group.append(c);
                }
                continue;
            }

            // Handle special cases for literals
            if (isLiteralStart(c)) {
                StringBuilder literal = new StringBuilder();
                literal.append(c);
                while (i + 1 < postfix.length() && isLiteralPart(postfix.charAt(i + 1))) {
                    literal.append(postfix.charAt(++i));
                }
                stack.push(createLiteralNFA(literal.toString()));
                continue;
            }

            // Handle operators
            if (isOperator(c)) {
                try {
                    switch (c) {
                        case '.':
                            NFA right = stack.pop();
                            NFA left = stack.pop();
                            stack.push(createConcatenationNFA(left, right));
                            break;
                        case '|':
                            NFA alt2 = stack.pop();
                            NFA alt1 = stack.pop();
                            stack.push(createUnionNFA(alt1, alt2));
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
                } catch (EmptyStackException e) {
                    throw new IllegalArgumentException("Invalid regex expression: missing operand");
                }
            } else {
                stack.push(createBasicNFA(c));
            }
        }

        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Invalid regex expression");
        }

        return stack.pop();
    }

    private boolean isLiteralStart(char c) {
        return c == '\'' || Character.isDigit(c);
    }

    private boolean isLiteralPart(char c) {
        return Character.isDigit(c) || c == '.' || c == '\\' || 
               (c >= 'a' && c <= 'z') || c == '\'';
    }

    private NFA createLiteralNFA(String literal) {
        if (literal.startsWith("'")) {
            // Character literal
            return createCharacterLiteralNFA(literal);
        } else if (literal.contains(".")) {
            // Decimal literal
            return createDecimalLiteralNFA(literal);
        }
        // Other literals
        return null;
    }

    private NFA createDecimalLiteralNFA(String decimal) {
        NFA nfa = new NFA();
        State start = createNewState();
        State end = createNewState();
        nfa.setStartState(start);
        nfa.addAcceptingState(end);

        State current = start;
        for (int i = 0; i < decimal.length(); i++) {
            State next = i == decimal.length() - 1 ? end : createNewState();
            nfa.addTransition(current, decimal.charAt(i), next);
            current = next;
        }
        return nfa;
    }

    private NFA createCharacterLiteralNFA(String charLiteral) {
        NFA nfa = new NFA();
        State start = createNewState();
        State end = createNewState();
        nfa.setStartState(start);
        nfa.addAcceptingState(end);

        State current = start;
        for (int i = 0; i < charLiteral.length(); i++) {
            State next = i == charLiteral.length() - 1 ? end : createNewState();
            nfa.addTransition(current, charLiteral.charAt(i), next);
            current = next;
        }
        return nfa;
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