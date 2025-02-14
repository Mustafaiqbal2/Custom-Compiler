package Compiler.config;

import java.util.*;

public class LanguageConfig {
    // Keywords
    public static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "global", "function", "integer", "decimal", "boolean", "character",
        "true", "false", "if", "else", "while", "return"
    ));

    // Operators
    public static final Set<String> OPERATORS = new HashSet<>(Arrays.asList(
        "+", "-", "*", "/", "%", "^", "=", "==", "!=", "<", ">", "<=", ">="
    ));

    // Data types
    public static final Set<String> DATA_TYPES = new HashSet<>(Arrays.asList(
        "integer", "decimal", "boolean", "character"
    ));

    // File extension
    public static final String FILE_EXTENSION = ".ccl";

    // Comment delimiters
    public static final String SINGLE_LINE_COMMENT = "//";
    public static final String MULTI_LINE_COMMENT_START = "/*";
    public static final String MULTI_LINE_COMMENT_END = "*/";

    // Maximum decimal places
    public static final int MAX_DECIMAL_PLACES = 5;

    // Default values for data types
    public static final Map<String, String> DEFAULT_VALUES = Map.of(
        "integer", "0",
        "decimal", "0.0",
        "boolean", "false",
        "character", "'a'"
    );

    public static boolean isKeyword(String token) {
        return KEYWORDS.contains(token);
    }

    public static boolean isOperator(String token) {
        return OPERATORS.contains(token);
    }

    public static boolean isDataType(String token) {
        return DATA_TYPES.contains(token);
    }
}