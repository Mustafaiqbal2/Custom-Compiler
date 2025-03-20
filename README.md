# Custom Compiler Implementation (CCL)

A custom compiler implementation written in Java that processes Mustafa Saad (MS) files. This project implements lexical analysis (Phase 1), symbol table management (Phase 2), and syntax analysis (Phase 3) for a custom programming language.

![Language Composition](https://img.shields.io/badge/Java-43.6%25-orange)
![Language Composition](https://img.shields.io/badge/C-29.2%25-blue)
![Language Composition](https://img.shields.io/badge/C++-26.3%25-red)
![Last Updated](https://img.shields.io/badge/last%20updated-2025--03--20-brightgreen)

## Features

### Lexical Analysis
- **Token Recognition**: Complete lexical analysis using DFA (Deterministic Finite Automata)
- **Pattern Matching**: Efficient token pattern recognition through regex to NFA to DFA conversion
- **Error Handling**: Robust error detection and reporting during lexical analysis
- **Symbol Table Management**: Comprehensive symbol table with scope recognition

### Syntax Analysis (Phase 3)
- **Parsing**: Implemented using LL(1) parsing technique
- **AST Construction**: Abstract Syntax Tree construction for parsed code
- **Error Handling**: Syntax error detection and reporting

### Language Specifications

#### Data Types
- `boolean`: For true/false values
- `integer`: For whole numbers
- `float`: For decimal numbers (up to 5 decimal places)
- `char`: For single letters
- `string`: For text values

#### Variable Declaration & Scope
- **Global Variables**: Must be declared at program start with global keyword
- **Local Variables**: Limited to their block scope
- **Naming Rules**:
  - Only lowercase letters (a-z)

#### Operators
- **Arithmetic**: 
  - Addition (`+`)
  - Subtraction (`-`)
  - Multiplication (`*`)
  - Division (`/`)
  - Modulus (`%`)
  - Exponentiation (`^`)
- **Comparison**:
  - Less than (`<`)
  - Greater than (`>`)

#### Comments
- Single-line: Begin with `//`
- Multi-line: Begin with `/*` and end with `*/`

### File Format
- Source files use the `.ms` extension (Mustafa Saad)

## Project Structure

```
src/
├── compiler/
│   │── Lexer.java           # Tokenization and lexical analysis
│   │── Token.java           # Token definition
│   │── TokenType.java       # Enum of token types
│   │── NFA.java             # NFA implementation
│   │── DFA.java             # DFA implementation
│   │── State.java           # Small class for state
│   │── SymbolTable.java     # Symbol table implementation
│   │── Main.java
│   │── RegexToNFA           # Class for implementation of Thompson's Construction
│   └── ErrorHandler.java    # Error handling and reporting
│── src/Phase3/
│   │── SyntaxAnalyzer.java  # Syntax analysis implementation
│   │── ASTNode.java         # Abstract Syntax Tree node
│   │── ASTBuilder.java      # AST builder implementation
│   │── SemanticAnalyzer.java# Semantic analysis implementation
│   └── CodeGenerator.java   # Code generation implementation
```

## Getting Started

### Prerequisites
- Java Development Kit (JDK) 8 or higher

### Running the Compiler
1. Clone the repository:
```bash
git clone https://github.com/Mustafaiqbal2/Custom-Compiler.git
```

2. Place your source code in a file with `.ms` extension

3. Run the compiler:
```bash
java -cp bin Compiler.Main
```

## Current Implementation Status

### Completed Features
- ✅ Lexical Analysis (Phase 1)
- ✅ Symbol Table Management (Phase 2)
- ✅ Syntax Analysis (Phase 3)
- ✅ Error Handling
- ✅ DFA-based Token Recognition
- ✅ Scope Recognition (only in symbol table)

### In Progress
- 🔄 Semantic Analysis
- 🔄 Code Generation

## Authors

- [@Mustafaiqbal2](https://github.com/Mustafaiqbal2)
- [@saadnadeem554](https://github.com/saadnadeem554)

## License

This project is available as open source under the terms of the MIT License.

---
Last updated: 2025-03-20 19:28:07 UTC
