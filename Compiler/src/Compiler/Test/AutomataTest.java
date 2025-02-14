package Compiler.Test;

import static org.junit.Assert.*;
import org.junit.Test;

import Compiler.automata.*;

// The class needs to be public
public class AutomataTest {

    @Test
    public void testBasicRegexPatterns() {
        RegexToNFAConverter converter = new RegexToNFAConverter();
        
        String[][] testCases = {
            {"a", "a", "true"},
            {"a", "b", "false"},
            {"[a-z]", "x", "true"},
            {"[a-z]", "1", "false"},
            {"a|b", "a", "true"},
            {"a|b", "b", "true"},
            {"a|b", "c", "false"},
            {"ab", "ab", "true"},
            {"ab", "a", "false"},
            {"a*", "", "true"},
            {"a*", "aaa", "true"},
            {"a+", "", "false"},
            {"a+", "aaa", "true"},
            {"a?", "", "true"},
            {"a?", "a", "true"},
            {"a?", "aa", "false"}
        };
        
        for (String[] testCase : testCases) {
            String pattern = testCase[0];
            String input = testCase[1];
            boolean expectedResult = Boolean.parseBoolean(testCase[2]);
            
            try {
                NFA nfa = converter.convert(pattern);
                DFA dfa = nfa.toDFA();
                boolean result = dfa.accepts(input);
                
                assertEquals(
                    String.format("Pattern: %s, Input: %s", pattern, input),
                    expectedResult,
                    result
                );
            } catch (Exception e) {
                fail(String.format("Test failed for pattern '%s' with input '%s': %s", 
                    pattern, input, e.getMessage()));
            }
        }
    }
    @Test
    public void testCharacterRange() {
        RegexToNFAConverter converter = new RegexToNFAConverter();
        String pattern = "[a-z]";
        
        // Test all lowercase letters
        for (char c = 'a'; c <= 'z'; c++) {
            String input = String.valueOf(c);
            NFA nfa = converter.convert(pattern);
            DFA dfa = nfa.toDFA();
            boolean result = dfa.accepts(input);
            
            assertTrue(
                String.format("Pattern [a-z] should accept '%c'", c),
                result
            );
        }
        
        // Test some characters that should be rejected
        char[] invalidChars = {'A', '0', '9', '@', '[', ']'};
        for (char c : invalidChars) {
            String input = String.valueOf(c);
            NFA nfa = converter.convert(pattern);
            DFA dfa = nfa.toDFA();
            boolean result = dfa.accepts(input);
            
            assertFalse(
                String.format("Pattern [a-z] should reject '%c'", c),
                result
            );
        }
    }
}