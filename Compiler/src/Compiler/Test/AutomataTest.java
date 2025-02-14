package Compiler.Test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

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
    @Test
    public void testComplexPatterns() {
        RegexToNFAConverter converter = new RegexToNFAConverter();
        
        // Test cases with expected results - starting with simpler patterns
        Map<String, Map<String, Boolean>> testCases = new HashMap<>();
        
        // Test case 1: Simple character
        Map<String, Boolean> test1 = new HashMap<>();
        test1.put("a", true);
        test1.put("b", false);
        testCases.put("a", test1);
        
        // Test case 2: Simple concatenation
        Map<String, Boolean> test2 = new HashMap<>();
        test2.put("ab", true);
        test2.put("a", false);
        test2.put("b", false);
        testCases.put("ab", test2);
        
        // Test case 3: Simple alternation
        Map<String, Boolean> test3 = new HashMap<>();
        test3.put("a", true);
        test3.put("b", true);
        test3.put("c", false);
        testCases.put("a|b", test3);
        
        // Run all test cases
        for (Map.Entry<String, Map<String, Boolean>> testCase : testCases.entrySet()) {
            String pattern = testCase.getKey();
            System.out.println("\nTesting pattern: " + pattern);
            
            try {
                NFA nfa = converter.convert(pattern);
                DFA dfa = nfa.toDFA();
                
                for (Map.Entry<String, Boolean> test : testCase.getValue().entrySet()) {
                    String input = test.getKey();
                    boolean expectedResult = test.getValue();
                    boolean actualResult = dfa.accepts(input);
                    
                    System.out.printf("Input: %-6s Expected: %-5b Got: %-5b%n", 
                                    input, expectedResult, actualResult);
                    
                    assertEquals(
                        String.format("Pattern: %s, Input: %s expected:<%b> but was:<%b>",
                                    pattern, input, expectedResult, actualResult),
                        expectedResult,
                        actualResult
                    );
                }
            } catch (Exception e) {
                fail(String.format("Failed to process pattern '%s': %s", pattern, e.getMessage()));
            }
        }
    }
}