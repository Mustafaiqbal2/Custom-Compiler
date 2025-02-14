# Custom Compiler Implementation - Phase 1 & 2

## Language Specifications

### Data Types
- Boolean: For true/false values
- Integer: For whole numbers
- Decimal: For decimal numbers (up to 5 decimal places)
- Character: For single letters

### Variable Naming Rules
- Only lowercase letters (a-z) are allowed for variable names
- Must start with a letter
- No special characters or numbers allowed

### Operators
1. Arithmetic Operators:
   - Addition (+)
   - Subtraction (-)
   - Multiplication (*)
   - Division (/)
   - Modulus (%)
   - Exponentiation (^)

### Comments
1. Single-line comments: Begin with `//`
2. Multi-line comments: Begin with `/*` and end with `*/`

### Scope
- Supports both global and local variables
- Global variables must be declared at the program start
- Local variables are limited to their block scope

### File Extension
- `.ccl` (Custom Compiler Language)

## Project Structure

```java
src/
├── compiler/
│   ├── lexer/
│   │   ├── Lexer.java           // Tokenization and lexical analysis
│   │   ├── Token.java           // Token definition
│   │   └── TokenType.java       // Enum of token types
│   ├── automata/
│   │   ├── RegularExpression.java
│   │   ├── NFA.java
│   │   ├── DFA.java
│   │   └── State.java
│   ├── symboltable/
│   │   ├── SymbolTable.java     // Symbol table implementation
│   │   ├── Symbol.java          // Symbol entry definition
│   │   └── Scope.java          // Scope management
│   └── error/
│       └── ErrorHandler.java    // Error handling and reporting
