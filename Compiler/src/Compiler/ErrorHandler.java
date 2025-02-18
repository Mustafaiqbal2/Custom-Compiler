package Compiler;

import java.util.ArrayList;
import java.util.List;

public class ErrorHandler {
    private List<String> errors;
    
    public ErrorHandler() {
        errors = new ArrayList<>();
    }
    
    public void addError(String error) {
        errors.add(error);
    }
    
    public void displayErrors() {
        if (errors.isEmpty()) {
            System.out.println("No errors found.");
        } else {
            for (String error : errors) {
                System.out.println(error);
            }
        }
    }
}