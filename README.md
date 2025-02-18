# Custom Compiler Implementation (CCL)

A custom compiler implementation written in Java that processes Mustafa Saad (MS) files. This project implements lexical analysis (Phase 1) and symbol table management (Phase 2) for a custom programming language.

![Language Composition](https://img.shields.io/badge/Java-97.7%25-orange)
![Language Composition](https://img.shields.io/badge/MAXScript-2.3%25-blue)
![Last Updated](https://img.shields.io/badge/last%20updated-2025--02--18-brightgreen)

## Features

### Lexical Analysis
- **Token Recognition**: Complete lexical analysis using DFA (Deterministic Finite Automata)
- **Pattern Matching**: Efficient token pattern recognition through regex to NFA to DFA conversion
- **Error Handling**: Robust error detection and reporting during lexical analysis
- **Symbol Table Management**: Comprehensive symbol table with scope recognition

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
│   └── ErrorHandler.java    # Error handling and reporting
```

## Getting Started

### Prerequisites
- Java Development Kit (JDK) 8 or higher

### Running the Compiler
1. Clone the repository:
```bash
git clone https://github.com/Mustafaiqbal2/Compiler-Phase-1-2.git
```

2. Place your source code in a file with `.ms` extension

3. Run the compiler:
```bash
java -cp bin Compiler.Main <your_file.ccl>
```

## Current Implementation Status

### Completed Features
- ✅ Lexical Analysis (Phase 1)
- ✅ Symbol Table Management (Phase 2)
- ✅ Error Handling
- ✅ DFA-based Token Recognition
- ✅ Scope Recognition (only in symbol table)

### In Progress
- 🔄 Syntax Analysis
- 🔄 Semantic Analysis

## Authors

- [@Mustafaiqbal2](https://github.com/Mustafaiqbal2)
- [@saadnadeem554](https://github.com/saadnadeem554)

## License

This project is available as open source under the terms of the MIT License.

---
Last updated: 2025-02-18 10:45:15 UTC
