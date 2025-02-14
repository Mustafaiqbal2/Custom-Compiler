package Compiler.util;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PatternMatcher {
    // Patterns for our language constructs
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z]+$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^\\d+\\.\\d{1,5}$");
    private static final Pattern CHARACTER_PATTERN = Pattern.compile("^'[a-z]'$");

    public static boolean isValidIdentifier(String input) {
        return IDENTIFIER_PATTERN.matcher(input).matches();
    }

    public static boolean isValidInteger(String input) {
        return INTEGER_PATTERN.matcher(input).matches();
    }

    public static boolean isValidDecimal(String input) {
        if (!DECIMAL_PATTERN.matcher(input).matches()) {
            return false;
        }
        // Check if decimal places are within limit (5)
        String[] parts = input.split("\\.");
        return parts[1].length() <= 5;
    }

    public static boolean isValidCharacter(String input) {
        return CHARACTER_PATTERN.matcher(input).matches();
    }

    public static String validateValue(String type, String value) {
        switch (type.toLowerCase()) {
            case "integer":
                if (!isValidInteger(value)) {
                    return "Invalid integer value: " + value;
                }
                break;
            case "decimal":
                if (!isValidDecimal(value)) {
                    return "Invalid decimal value or too many decimal places: " + value;
                }
                break;
            case "character":
                if (!isValidCharacter(value)) {
                    return "Invalid character value: " + value;
                }
                break;
            case "boolean":
                if (!value.equals("true") && !value.equals("false")) {
                    return "Invalid boolean value: " + value;
                }
                break;
            default:
                return "Unknown type: " + type;
        }
        return null; // null means no error
    }
}