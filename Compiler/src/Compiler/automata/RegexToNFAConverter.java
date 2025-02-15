package Compiler.automata;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RegexToNFAConverter {
    private int stateCounter = 0;

    public NFA convert(String regex) {
        // Special handling for comment patterns
        if (regex.startsWith("//")) {
            return handleSingleLineComment();
        } else if (regex.startsWith("/\\*")) {
            return handleMultiLineComment();
        }
        
        // Regular pattern conversion
        String postfix = parseRegex(regex);
        return thompsonConstruction(postfix);
    }
    
    private NFA handleSingleLineComment() {
        NFA nfa = new NFA();
        State start = new State("q" + stateCounter++);
        State afterFirstSlash = new State("q" + stateCounter++);
        State afterSecondSlash = new State("q" + stateCounter++);
        State end = new State("q" + stateCounter++);
        
        nfa.setStartState(start);
        nfa.addAcceptingState(end);
        
        // Match "//"
        nfa.addTransition(start, '/', afterFirstSlash);
        nfa.addTransition(afterFirstSlash, '/', afterSecondSlash);
        
        // Match any character except newline
        for (char c = 0; c < 127; c++) {
            if (c != '\n' && c != '\r') {
                nfa.addTransition(afterSecondSlash, c, afterSecondSlash);
            }
        }
        
        // Optional transition to accepting state
        nfa.addEpsilonTransition(afterSecondSlash, end);
        
        return nfa;
    }

    private NFA handleMultiLineComment() {
        NFA nfa = new NFA();
        State start = new State("q" + stateCounter++);
        State afterFirstSlash = new State("q" + stateCounter++);
        State afterStar = new State("q" + stateCounter++);
        State beforeEnd = new State("q" + stateCounter++);
        State end = new State("q" + stateCounter++);
        
        nfa.setStartState(start);
        nfa.addAcceptingState(end);
        
        // Match "/*"
        nfa.addTransition(start, '/', afterFirstSlash);
        nfa.addTransition(afterFirstSlash, '*', afterStar);
        
        // Match any character
        for (char c = 0; c < 127; c++) {
            if (c != '*') {
                nfa.addTransition(afterStar, c, afterStar);
            }
        }
        
        // Match "*/"
        nfa.addTransition(afterStar, '*', beforeEnd);
        nfa.addTransition(beforeEnd, '/', end);
        
        return nfa;
    }
    
    private String parseRegex(String regex) {
        // Special cases that need direct NFA construction
        if (regex.equals("[A-Za-z_][A-Za-z0-9_]*") ||   // IDENTIFIER
            regex.equals("'[^']'") ||                    // CHARACTER_LITERAL
            regex.equals("\"[^\"]*\"") ||               // STRING_LITERAL
            regex.equals("[0-9]+\\.[0-9]+")) {          // DECIMAL_LITERAL
            return regex; // These will be handled specially in thompsonConstruction
        }
        
        String preprocessed = preprocess(regex);
        return infixToPostfix(preprocessed);
    }
    
    // Insert concatenation operators only between “simple” tokens.
    private String preprocess(String regex) {
        StringBuilder processed = new StringBuilder();
        boolean inCharClass = false;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            // Enter or exit a character class.
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
            // If escape, copy both characters.
            if (!inCharClass && c == '\\') {
                processed.append(c);
                if (i + 1 < regex.length()) {
                    processed.append(regex.charAt(i + 1));
                    i++;
                }
                continue;
            }
            processed.append(c);
            // Insert concatenation operator only if both current and next are "simple".
            if (!inCharClass && i < regex.length() - 1) {
                char next = regex.charAt(i + 1);
                if (shouldAddConcatenation(c, next)) {
                    processed.append('.');
                }
            }
        }
        return processed.toString();
    }
    
    // Only add concatenation if current is a literal (letter, digit, or closing bracket/quantifier)
    // and next is a literal or an opening bracket.
    private boolean shouldAddConcatenation(char current, char next) {
        // Do not insert if current is an operator or escape.
        if (current == '\\') return false;
        // If current is a literal or a closing token.
        boolean currOk = Character.isLetterOrDigit(current) || current == ']' ||
                         current == '*' || current == '+' || current == '?' || current == ')';
        // Next must be a literal or an opening bracket or an escape.
        boolean nextOk = Character.isLetterOrDigit(next) || next == '[' || next == '(' || next == '\\';
        return currOk && nextOk;
    }
    
    // Infix-to-postfix: treat a character class as one literal token.
    private String infixToPostfix(String infix) {
        StringBuilder postfix = new StringBuilder();
        Stack<Character> operators = new Stack<>();
        Map<Character, Integer> precedence = new HashMap<>();
        precedence.put('|', 1);
        precedence.put('.', 2);
        precedence.put('*', 3);
        precedence.put('+', 3);
        precedence.put('?', 3);
        
        for (int i = 0; i < infix.length(); i++) {
            char c = infix.charAt(i);
            // If a character class begins, read until the closing ']'
            if (c == '[') {
                StringBuilder charClass = new StringBuilder();
                charClass.append(c);
                while (i < infix.length() && infix.charAt(i) != ']') {
                    i++;
                    charClass.append(infix.charAt(i));
                }
                // Wrap the character class in braces to treat as literal.
                postfix.append("{").append(charClass.toString()).append("}");
                continue;
            }
            // Handle parentheses normally.
            if (c == '(') {
                operators.push(c);
            } else if (c == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    postfix.append(operators.pop());
                }
                if (!operators.isEmpty()) {
                    operators.pop();
                }
            }
            // If escape, copy as a unit.
            else if (c == '\\') {
                if (i + 1 < infix.length()) {
                    postfix.append("\\").append(infix.charAt(++i));
                }
            }
            // If operator, pop higher or equal precedence.
            else if (isOperator(c)) {
                while (!operators.isEmpty() && operators.peek() != '(' &&
                        precedence.get(operators.peek()) >= precedence.get(c)) {
                    postfix.append(operators.pop());
                }
                operators.push(c);
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
    
    private boolean isOperator(char c) {
        return c == '|' || c == '*' || c == '+' || c == '?' || c == '.';
    }
    
    // Thompson construction using the postfix expression.
    private NFA thompsonConstruction(String regex) {
    	if (regex.equals("[A-Za-z_][A-Za-z0-9_]*")) {
            return createIdentifierNFA();
        } else if (regex.equals("'[^']'")) {
            return createCharacterLiteralNFA();
       
        } else if (regex.equals("[0-9]+\\.[0-9]+")) {
            return createDecimalLiteralNFA();
        }
        Stack<NFA> nfaStack = new Stack<>();
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            System.out.println("Processing character: " + c);
            // If token is a grouped literal from a character class.
            if (c == '{') {
                int j = regex.indexOf('}', i);
                if (j == -1) throw new IllegalStateException("Unterminated character class");
                String token = regex.substring(i + 1, j); // e.g., "[A-Za-z_]"
                // Remove the surrounding brackets.
                if (token.startsWith("[") && token.endsWith("]")) {
                    String content = token.substring(1, token.length() - 1);
                    nfaStack.push(handleCharacterClass(content));
                } else {
                    // Fallback: treat each char literally.
                    for (char ch : token.toCharArray()) {
                        nfaStack.push(createBasicNFA(ch));
                    }
                }
                i = j;
                continue;
            }
            // Handle escapes.
            if (c == '\\') {
                i++;
                if (i < regex.length()) {
                    char escapedChar = regex.charAt(i);
                    System.out.println("Processing escape sequence: \\" + escapedChar);
                    nfaStack.push(handleEscape(escapedChar));
                    continue;
                } else {
                    throw new IllegalStateException("Dangling escape at end of regex");
                }
            }
            // Process operators.
            try {
                switch (c) {
                    case '|':
                        if (nfaStack.size() < 2) throw new IllegalStateException("Insufficient operands for |");
                        NFA nfa2 = nfaStack.pop();
                        NFA nfa1 = nfaStack.pop();
                        nfaStack.push(union(nfa1, nfa2));
                        break;
                    case '.':
                        if (nfaStack.size() < 2) throw new IllegalStateException("Insufficient operands for concatenation");
                        NFA second = nfaStack.pop();
                        NFA first = nfaStack.pop();
                        nfaStack.push(concatenate(first, second));
                        break;
                    case '*':
                        if (nfaStack.isEmpty()) throw new IllegalStateException("Insufficient operands for *");
                        nfaStack.push(kleeneStar(nfaStack.pop()));
                        break;
                    case '+':
                        if (nfaStack.isEmpty()) throw new IllegalStateException("Insufficient operands for +");
                        nfaStack.push(kleenePlus(nfaStack.pop()));
                        break;
                    case '?':
                        if (nfaStack.isEmpty()) throw new IllegalStateException("Insufficient operands for ?");
                        nfaStack.push(optional(nfaStack.pop()));
                        break;
                    default:
                        if (c != '(' && c != ')') {
                            nfaStack.push(createBasicNFA(c));
                        }
                }
            } catch (Exception e) {
                System.err.println("Error processing character '" + c + "' at position " + i);
                throw e;
            }
        }
        if (nfaStack.size() != 1) {
            System.err.println("Final stack size: " + nfaStack.size());
            throw new IllegalStateException("Improper expression");
        }
        return nfaStack.pop();
    }
    
    private NFA createIdentifierNFA() {
        NFA nfa = new NFA();
        State start = new State("q" + stateCounter++);
        State middle = new State("q" + stateCounter++);
        State end = new State("q" + stateCounter++);
        
        nfa.setStartState(start);
        nfa.addAcceptingState(end);
        
        // First character: letter or underscore
        for (char c = 'A'; c <= 'Z'; c++) nfa.addTransition(start, c, middle);
        for (char c = 'a'; c <= 'z'; c++) nfa.addTransition(start, c, middle);
        nfa.addTransition(start, '_', middle);
        
        // Subsequent characters: letter, digit, or underscore
        for (char c = 'A'; c <= 'Z'; c++) {
            nfa.addTransition(middle, c, middle);
            nfa.addTransition(middle, c, end);
        }
        for (char c = 'a'; c <= 'z'; c++) {
            nfa.addTransition(middle, c, middle);
            nfa.addTransition(middle, c, end);
        }
        for (char c = '0'; c <= '9'; c++) {
            nfa.addTransition(middle, c, middle);
            nfa.addTransition(middle, c, end);
        }
        nfa.addTransition(middle, '_', middle);
        nfa.addTransition(middle, '_', end);
        
        return nfa;
    }

    private NFA createCharacterLiteralNFA() {
        NFA nfa = new NFA();
        State start = new State("q" + stateCounter++);
        State afterQuote = new State("q" + stateCounter++);
        State end = new State("q" + stateCounter++);
        
        nfa.setStartState(start);
        nfa.addAcceptingState(end);
        
        nfa.addTransition(start, '\'', afterQuote);
        for (char c = 0; c < 127; c++) {
            if (c != '\'') nfa.addTransition(afterQuote, c, end);
        }
        nfa.addTransition(afterQuote, '\'', end);
        
        return nfa;
    }

    private NFA createDecimalLiteralNFA() {
        NFA nfa = new NFA();
        State start = new State("q" + stateCounter++);
        State beforeDot = new State("q" + stateCounter++);
        State afterDot = new State("q" + stateCounter++);
        State end = new State("q" + stateCounter++);
        
        nfa.setStartState(start);
        nfa.addAcceptingState(end);
        
        // Before decimal point
        for (char c = '0'; c <= '9'; c++) {
            nfa.addTransition(start, c, beforeDot);
            nfa.addTransition(beforeDot, c, beforeDot);
        }
        
        // Decimal point
        nfa.addTransition(beforeDot, '.', afterDot);
        
        // After decimal point (at least one digit required)
        for (char c = '0'; c <= '9'; c++) {
            nfa.addTransition(afterDot, c, end);
            nfa.addTransition(end, c, end);
        }
        
        return nfa;
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
        result.addEpsilonTransition(start, nfa1.getStartState());
        result.addEpsilonTransition(start, nfa2.getStartState());
        for (State s : nfa1.getAcceptingStates()) result.addEpsilonTransition(s, end);
        for (State s : nfa2.getAcceptingStates()) result.addEpsilonTransition(s, end);
        result.addAllTransitions(nfa1);
        result.addAllTransitions(nfa2);
        return result;
    }
    
    private NFA concatenate(NFA nfa1, NFA nfa2) {
        NFA result = new NFA();
        result.setStartState(nfa1.getStartState());
        for (State s : nfa1.getAcceptingStates()) {
            s.setAccepting(false);
            result.addEpsilonTransition(s, nfa2.getStartState());
        }
        for (State s : nfa2.getAcceptingStates()) result.addAcceptingState(s);
        result.addAllTransitions(nfa1);
        result.addAllTransitions(nfa2);
        System.out.println("Created concatenation NFA:");
        System.out.println("Start state: " + result.getStartState().getId());
        StringBuilder acc = new StringBuilder();
        boolean first = true;
        for (State s : result.getAcceptingStates()) {
            if (!first) acc.append(", ");
            acc.append(s.getId());
            first = false;
        }
        System.out.println("Accepting states: " + acc.toString());
        return result;
    }
    
    private NFA kleeneStar(NFA nfa) {
        NFA result = new NFA();
        State start = new State("q" + stateCounter++);
        State end = new State("q" + stateCounter++);
        result.setStartState(start);
        result.addAcceptingState(end);
        result.addEpsilonTransition(start, end);
        result.addEpsilonTransition(start, nfa.getStartState());
        for (State s : nfa.getAcceptingStates()) {
            result.addEpsilonTransition(s, end);
            result.addEpsilonTransition(s, nfa.getStartState());
        }
        result.addAllTransitions(nfa);
        return result;
    }
    
    private NFA kleenePlus(NFA nfa) {
        NFA result = new NFA();
        result.setStartState(nfa.getStartState());
        State end = new State("q" + stateCounter++);
        result.addAcceptingState(end);
        for (State s : nfa.getAcceptingStates()) {
            result.addEpsilonTransition(s, end);
            result.addEpsilonTransition(s, nfa.getStartState());
        }
        result.addAllTransitions(nfa);
        return result;
    }
    
    private NFA optional(NFA nfa) {
        NFA result = new NFA();
        State start = new State("q" + stateCounter++);
        State end = new State("q" + stateCounter++);
        result.setStartState(start);
        result.addAcceptingState(end);
        result.addEpsilonTransition(start, end);
        result.addEpsilonTransition(start, nfa.getStartState());
        for (State s : nfa.getAcceptingStates()) result.addEpsilonTransition(s, end);
        result.addAllTransitions(nfa);
        return result;
    }
    
    private NFA characterClass(Set<Character> chars) {
        NFA result = new NFA();
        State start = new State("q" + stateCounter++);
        State end = new State("q" + stateCounter++);
        result.setStartState(start);
        result.addAcceptingState(end);
        for (char c : chars) {
            result.addTransition(start, c, end);
            System.out.printf("Added transition for character: %c%n", c);
        }
        return result;
    }
    
    // Expects content without the surrounding brackets.
    private NFA handleCharacterClass(String content) {
        Set<Character> chars = new HashSet<>();
        System.out.println("Processing character class content: " + content);
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\\' && i + 1 < content.length()) {
                // Handle escaped characters
                char escapedChar = content.charAt(i + 1);
                chars.add(escapedChar);
                i++; // Skip the next character
            } else if (i + 2 < content.length() && content.charAt(i + 1) == '-') {
                // Handle ranges
                char start = c;
                char end = content.charAt(i + 2);
                System.out.printf("Processing range: %c-%c%n", start, end);
                if (end < start) throw new IllegalArgumentException("Invalid range: " + start + "-" + end);
                for (char ch = start; ch <= end; ch++) {
                    chars.add(ch);
                    System.out.println("Adding character from range: " + ch);
                }
                i += 2;
            } else if (c != '-') {
                System.out.println("Adding single character: " + c);
                chars.add(c);
            }
        }
        System.out.println("Final character set: " + chars);
        return characterClass(chars);
    }
    
    private NFA rangeNFA(char start, char end) {
        Set<Character> chars = new HashSet<>();
        for (char c = start; c <= end; c++) {
            chars.add(c);
        }
        return characterClass(chars);
    }
    
    private NFA handleEscape(char c) {
        switch (c) {
            case 'd': return rangeNFA('0', '9');
            case 'w':
                NFA letters = union(rangeNFA('a', 'z'), rangeNFA('A', 'Z'));
                NFA digits = rangeNFA('0', '9');
                NFA underscore = createBasicNFA('_');
                return union(union(letters, digits), underscore);
            case 's':
                Set<Character> whitespace = Set.of(' ', '\t', '\n', '\r', '\f');
                return characterClass(whitespace);
            default: return createBasicNFA(c);
        }
    }
}
