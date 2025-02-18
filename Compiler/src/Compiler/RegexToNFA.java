package Compiler;

import java.util.*;

public class RegexToNFA {
    private int stateCount = 0;
    
    public NFA convert(String regex) {
        //System.out.println("Original regex: " + regex);
        String preprocessed = preprocessRegex(regex);
        //System.out.println("Preprocessed regex: " + preprocessed);
        
        List<String> tokens = tokenizeRegex(preprocessed);
        //System.out.println("Tokenized regex: " + tokens);
        
        List<String> concatTokens = insertConcatenationOperator(tokens);
        //System.out.println("Tokens after concatenation insertion: " + concatTokens);
        
        List<String> postfixTokens = infixToPostfix(concatTokens);
        System.out.println("Postfix tokens: " + postfixTokens);
        
        Stack<NFA> stack = new Stack<>();
        for (String token : postfixTokens) {
            //System.out.println("Processing token: " + token + ", Stack size: " + stack.size());
            if (token.equals("*")) {
                if (stack.isEmpty()) {
                    throw new RuntimeException("Stack empty when expecting operand for '*'");
                }
                NFA nfaStar = stack.pop();
                stack.push(applyKleeneStar(nfaStar));
            } else if (token.equals("·")) { // explicit concatenation operator
                if (stack.size() < 2) {
                    throw new RuntimeException("Stack has fewer than 2 operands for concatenation");
                }
                NFA nfa2 = stack.pop();
                NFA nfa1 = stack.pop();
                stack.push(applyConcatenation(nfa1, nfa2));
            } else if (token.equals("|")) {
                if (stack.size() < 2) {
                    throw new RuntimeException("Stack has fewer than 2 operands for union");
                }
                NFA nfaB = stack.pop();
                NFA nfaA = stack.pop();
                stack.push(applyUnion(nfaA, nfaB));
            } else {
                // token is a literal (which might be an escaped sequence)
                stack.push(buildBasicNFA(token));
            }
        }
        if (stack.size() != 1) {
            throw new RuntimeException("Regex conversion error: stack size is not 1 after processing. Stack size: " + stack.size());
        }
        return stack.pop();
    }
    
    // Preprocess the regex (e.g., expand character classes, handle '+' operator if needed)
    private String preprocessRegex(String regex) {
        // Expand [a-z] into (a|b|...|z)
        regex = regex.replaceAll("\\[a-z\\]", "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z)");
        // Expand [0-9] into (0|1|2|3|4|5|6|7|8|9)
        regex = regex.replaceAll("\\[0-9\\]", "(0|1|2|3|4|5|6|7|8|9)");
        // Convert + for group operands: (X)+ becomes X·X*
        regex = regex.replaceAll("(\\([^\\)]+\\))\\+", "$1·$1*");
        // Convert + for single literal operands: a+ becomes a·a*
        regex = regex.replaceAll("([a-zA-Z0-9])\\+", "$1·$1*");
        return regex;
    }
    
    // Tokenizes the regex string into a list of tokens.
    // An escaped character (e.g., "\(") is treated as a single token.
    private List<String> tokenizeRegex(String regex) {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (c == '\\' && i + 1 < regex.length()) {
                tokens.add(regex.substring(i, i + 2));
                i++; // Skip next character as it's part of the escape
            } else {
                tokens.add(Character.toString(c));
            }
        }
        return tokens;
    }
    
    // Inserts an explicit concatenation operator "·" into the list of tokens where needed.
    private List<String> insertConcatenationOperator(List<String> tokens) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            result.add(token);
            if (i < tokens.size() - 1) {
                String token2 = tokens.get(i + 1);
                // If token is a literal or a closing parenthesis or a Kleene star
                // and token2 is a literal or an opening parenthesis, insert "·".
                if ((isLiteral(token) || token.equals("*") || token.equals(")"))
                        && (isLiteral(token2) || token2.equals("("))) {
                    result.add("·");
                }
            }
        }
        return result;
    }
    
    // Converts a list of tokens from infix to postfix notation.
    private List<String> infixToPostfix(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> stack = new Stack<>();
        for (String token : tokens) {
            if (isLiteral(token)) {
                output.add(token);
            } else if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (stack.isEmpty()) {
                    throw new RuntimeException("Mismatched parentheses in regex");
                }
                stack.pop(); // Remove "("
            } else {
                // Operator: "*", "·", or "|"
                while (!stack.isEmpty() && precedence(stack.peek()) >= precedence(token)) {
                    output.add(stack.pop());
                }
                stack.push(token);
            }
        }
        while (!stack.isEmpty()) {
            output.add(stack.pop());
        }
        return output;
    }
    
    // Determines if a token is considered a literal.
    // Here, tokens that are operators ("*", "·", "|", "(", ")") are not literals.
    private boolean isLiteral(String token) {
        return !(token.equals("*") || token.equals("·") || token.equals("|") || token.equals("(") || token.equals(")"));
    }
    
    // Defines operator precedence.
    private int precedence(String op) {
        switch (op) {
            case "*": return 3;
            case "·": return 2;
            case "|": return 1;
            default:  return 0;
        }
    }
    
    // Builds a basic NFA for a literal token.
    // If the token is an escape sequence (like "\("), we use the character after '\' as the literal.
    // If the token is ".", we treat it as a wildcard.
    private NFA buildBasicNFA(String token) {
        // If the token is an escape sequence like "\(" or "\{", take the character after the backslash.
        // Special-case "\n" to represent a newline.
        if (token.length() > 1 && token.charAt(0) == '\\') {
            char literal;
            if (token.equals("\\n")) {
                literal = '\n';
            } else {
                literal = token.charAt(1);
            }
            NFA.State start = new NFA.State(stateCount++);
            NFA.State accept = new NFA.State(stateCount++);
            start.addTransition(literal, accept);
            return new NFA(start, accept);
        } else if (token.equals(".")) {
            // Wildcard: match any printable ASCII character (32 to 126)
            NFA.State start = new NFA.State(stateCount++);
            NFA.State accept = new NFA.State(stateCount++);
            for (char ch = 32; ch < 127; ch++) {
                start.addTransition(ch, accept);
            }
            return new NFA(start, accept);
        } else {
            // Otherwise, create an NFA for the single literal character.
            char literal = token.charAt(0);
            NFA.State start = new NFA.State(stateCount++);
            NFA.State accept = new NFA.State(stateCount++);
            start.addTransition(literal, accept);
            return new NFA(start, accept);
        }
    }
    
    // Concatenates two NFAs.
    private NFA applyConcatenation(NFA nfa1, NFA nfa2) {
        nfa1.acceptState.addEpsilonTransition(nfa2.startState);
        return new NFA(nfa1.startState, nfa2.acceptState);
    }
    
    // Creates a union (alternation) of two NFAs.
    private NFA applyUnion(NFA nfa1, NFA nfa2) {
        NFA.State start = new NFA.State(stateCount++);
        NFA.State accept = new NFA.State(stateCount++);
        start.addEpsilonTransition(nfa1.startState);
        start.addEpsilonTransition(nfa2.startState);
        nfa1.acceptState.addEpsilonTransition(accept);
        nfa2.acceptState.addEpsilonTransition(accept);
        return new NFA(start, accept);
    }
    
    // Applies the Kleene star operation to an NFA.
    private NFA applyKleeneStar(NFA nfa) {
        NFA.State start = new NFA.State(stateCount++);
        NFA.State accept = new NFA.State(stateCount++);
        start.addEpsilonTransition(nfa.startState);
        start.addEpsilonTransition(accept);
        nfa.acceptState.addEpsilonTransition(nfa.startState);
        nfa.acceptState.addEpsilonTransition(accept);
        return new NFA(start, accept);
    }
}