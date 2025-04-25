#include <iostream>
#include <fstream>
#include <sstream>
#include <vector>
#include <map>
#include <set>
#include <algorithm>
#include <iomanip>


#include <string>




using namespace std;

//-----------------------------------------------------------------------Assignment 2 -------------------------------//
// Function to find the common prefix among production rules
string commonPrefix(const vector<string>& productions) {
    if (productions.empty()) return "";
    string prefix = productions[0];
    for (const string& prod : productions) {
        size_t i = 0;
        while (i < prefix.length() && i < prod.length() && prefix[i] == prod[i]) {
            i++;
        }
        prefix = prefix.substr(0, i);
    }
    return prefix;
}

void leftFactorCFG(map<string, vector<string>>& grammar) {
    // Helper function to find common prefix length between two strings
    auto commonPrefixLength = [](const string& s1, const string& s2) {
        size_t minLen = min(s1.length(), s2.length());
        size_t common = 0;
        while (common < minLen && s1[common] == s2[common]) {
            common++;
        }
        return common;
    };

    // Helper function to get next available non-terminal name
    auto getNewNonTerminal = [&](const string& base) {
        int counter = 1;
        string newName;
        do {
            newName = base + "'" + to_string(counter++);
        } while (grammar.find(newName) != grammar.end());
        return newName;
    };

    bool changes;
    do {
        changes = false;
        map<string, vector<string>> newGrammar;

        // Process each non-terminal
        for (const auto& [nonterm, productions] : grammar) {
            if (productions.size() <= 1) {
                newGrammar[nonterm] = productions;
                continue;
            }

            // Group productions by their first symbol
            map<string, vector<string>> groups;
            for (const string& prod : productions) {
                string firstSymbol = prod.empty() ? "&" : prod.substr(0, prod.find(' '));
                groups[firstSymbol].push_back(prod);
            }

            // Process each group
            bool factored = false;
            for (const auto& [first, group] : groups) {
                if (group.size() <= 1) {
                    if (!factored) {
                        for (const string& prod : group) {
                            newGrammar[nonterm].push_back(prod);
                        }
                    }
                    continue;
                }

                // Find minimum common prefix length
                size_t minCommon = group[0].length();
                for (size_t i = 1; i < group.size(); i++) {
                    minCommon = min(minCommon, commonPrefixLength(group[0], group[i]));
                }

                if (minCommon > 0) {
                    factored = true;
                    changes = true;

                    // Extract common prefix and suffixes
                    string prefix = group[0].substr(0, minCommon);
                    string newNonTerm = getNewNonTerminal(nonterm);
                    
                    // Add new production with common prefix
                    newGrammar[nonterm].push_back(prefix + " " + newNonTerm);
                    
                    // Add productions for new non-terminal
                    for (const string& prod : group) {
                        string suffix = prod.substr(minCommon);
                        while (!suffix.empty() && suffix[0] == ' ') suffix = suffix.substr(1);
                        newGrammar[newNonTerm].push_back(suffix.empty() ? "&" : suffix);
                    }
                } else if (!factored) {
                    for (const string& prod : group) {
                        newGrammar[nonterm].push_back(prod);
                    }
                }
            }
        }
        
        grammar = newGrammar;
    } while (changes);
}
void removeLeftRecursion(map<string, vector<string>>& grammar) {
    vector<string> nonterminals;
    for (const auto& [nonterminal, _] : grammar) {
        nonterminals.push_back(nonterminal);
    }

    for (size_t i = 0; i < nonterminals.size(); i++) {
        string Ai = nonterminals[i];
        
        // Handle indirect left recursion
        for (size_t j = 0; j < i; j++) {
            string Aj = nonterminals[j];
            vector<string> updatedProductions;
            
            for (const string& prod : grammar[Ai]) {
                // Check if this is a genuine case of indirect recursion
                // We need to verify that Aj leads to Ai through some derivation
                bool isIndirectRecursion = false;
                if (prod.find(Aj) == 0) {
                    // Check if Aj can derive a string starting with Ai
                    for (const string& ajProd : grammar[Aj]) {
                        if (ajProd.find(Ai) == 0) {
                            isIndirectRecursion = true;
                            break;
                        }
                    }
                }
                
                if (isIndirectRecursion) {
                    cout << "Indirect recursion detected: " << Ai << " -> " << Aj << endl;
                    for (const string& beta : grammar[Aj]) {
                        updatedProductions.push_back(beta + prod.substr(Aj.length()));
                    }
                } else {
                    updatedProductions.push_back(prod);
                }
            }
            grammar[Ai] = updatedProductions;
        }

        // Handle direct left recursion
        vector<string> alpha, beta;
        for (const string& prod : grammar[Ai]) {
            if (prod.find(Ai) == 0) {
                alpha.push_back(prod.substr(Ai.length())); // Left-recursive part
            } else {
                beta.push_back(prod); // Non-left-recursive part
            }
        }

        if (!alpha.empty()) { // If left recursion exists
            string newNonTerminal = Ai + "'";
            grammar[Ai].clear();
            
            // Add proper spacing between non-terminals
            for (const auto& b : beta) {
                if (b == "&") {
                    grammar[Ai].push_back(newNonTerminal);
                } else {
                    // Add space before the new non-terminal
                    grammar[Ai].push_back(b + " " + newNonTerminal);
                }
            }
            
            grammar[newNonTerminal].clear();
            for (const auto& a : alpha) {
                // Ensure proper spacing in the recursive part
                string spaced_a = a;
                if (!a.empty() && a[0] != ' ') {
                    spaced_a = " " + a;
                }
                grammar[newNonTerminal].push_back(spaced_a + " " + newNonTerminal);
            }
            grammar[newNonTerminal].push_back("&"); // Add epsilon production
        }
    
    }
}

// Function to read a CFG from a file
map<string, vector<string>> readCFG(const string& filename) {
    ifstream file(filename);
    map<string, vector<string>> grammar;
    string line;
    
    while (getline(file, line)) {
        stringstream ss(line);
        string nonterminal, arrow, production;
        ss >> nonterminal >> arrow;
        
        vector<string> productions;
        while (getline(ss, production, '|')) {
            production.erase(0, production.find_first_not_of(" ")); // Trim leading spaces
            productions.push_back(production);
        }
        
        grammar[nonterminal] = productions;
    }

    
    
    return grammar;
}

// Function to print the CFG
void printCFG(const map<string, vector<string>>& grammar) {
    for (const auto& [nonterminal, productions] : grammar) {
        cout << nonterminal << " -> ";
        for (size_t i = 0; i < productions.size(); i++) {
            cout << productions[i];
            if (i < productions.size() - 1) cout << " | ";
        }
        cout << endl;
    }
}

bool isNonTerminal(const string& symbol) {
    // Check if string is empty
    if(symbol.empty()) return false;
    
    // First character must be uppercase
    if(!isupper(symbol[0])) return false;
    
    // Rest of the characters can be:
    // - Uppercase letters (A-Z)
    // - Numbers (0-9)
    // - Apostrophe (')
    for(size_t i = 1; i < symbol.length(); i++) {
        char c = symbol[i];
        if(!(isupper(c) || isdigit(c) || c == '\'')) {
            return false;
        }
    }
    return true;
}
// Function to compute FIRST sets for all non-terminals
map<string, set<string>> computeFirstSets(const map<string, vector<string>>& grammar) {
    map<string, set<string>> firstSets;
    
    // Initialize FIRST sets
    for (const auto& [nonTerminal, _] : grammar) {
        firstSets[nonTerminal] = {};
    }

    bool changed;
    do {
        changed = false;
        
        // For each production rule
        for (const auto& [nonTerminal, productions] : grammar) {
            for (const string& production : productions) {
                if (production == "&") {
                    // Add epsilon to FIRST set
                    if (firstSets[nonTerminal].insert("&").second) {
                        changed = true;
                    }
                    continue;
                }

                // Split production into symbols
                vector<string> symbols;
                size_t pos = 0;
                while (pos < production.length()) {
                    if (!isspace(production[pos])) {
                        string symbol;
                        while (pos < production.length() && !isspace(production[pos])) {
                            symbol += production[pos++];
                        }
                        symbols.push_back(symbol);
                    } else {
                        pos++;
                    }
                }

                if (symbols.empty()) continue;

                // Process first symbol and subsequent ones if needed
                bool allCanBeEmpty = true;
                size_t symbolIndex = 0;
                
                while (allCanBeEmpty && symbolIndex < symbols.size()) {
                    string currentSymbol = symbols[symbolIndex];
                    
                    if (!isNonTerminal(currentSymbol)) {
                        // If terminal, add to FIRST set
                        if (firstSets[nonTerminal].insert(currentSymbol).second) {
                            changed = true;
                        }
                        allCanBeEmpty = false;
                    } else {
                        // If non-terminal, add its FIRST set (except ε)
                        bool hasEpsilon = false;
                        for (const string& first : firstSets[currentSymbol]) {
                            if (first == "&") {
                                hasEpsilon = true;
                            } else if (firstSets[nonTerminal].insert(first).second) {
                                changed = true;
                            }
                        }
                        allCanBeEmpty = hasEpsilon;
                    }
                    symbolIndex++;
                }

                // If all symbols can derive ε, add ε to FIRST set
                if (allCanBeEmpty && firstSets[nonTerminal].insert("&").second) {
                    changed = true;
                }
            }
        }
    } while (changed);

    return firstSets;
}
map<string, set<string>> computeFollowSets(
    const map<string, vector<string>>& grammar,
    const map<string, set<string>>& firstSets,
    const string& startSymbol
) {
    map<string, set<string>> follow;
    
    // Initialize follow set for all non-terminals
    for (const auto& [nonTerm, _] : grammar) {
        follow[nonTerm] = {};
    }
    
    // Add $ to follow set of start symbol
    follow[startSymbol].insert("$");
    
    // Keep computing until no changes occur
    bool changed;
    do {
        changed = false;
        
        // For each production rule
        for (const auto& [nonTerm, productions] : grammar) {
            // For each alternative production
            for (const string& production : productions) {
                if (production == "&") continue;
                
                // Split production into symbols
                vector<string> symbols;
                istringstream ss(production);
                string symbol;
                while (ss >> symbol) {
                    symbols.push_back(symbol);
                }
                
                // For each symbol in the production
                for (size_t i = 0; i < symbols.size(); i++) {
                    // If it's not a non-terminal, skip
                    if (!isNonTerminal(symbols[i])) continue;
                    
                    bool nullableRest = true;  // Can everything after current position derive null?
                    
                    // If not at the end, process the rest of the symbols
                    if (i < symbols.size() - 1) {
                        nullableRest = false;
                        
                        // Get the following symbols
                        vector<string> rest(symbols.begin() + i + 1, symbols.end());
                        
                        // For each symbol that follows
                        for (size_t j = 0; j < rest.size(); j++) {
                            // If it's a terminal
                            if (!isNonTerminal(rest[j])) {
                                if (follow[symbols[i]].insert(rest[j]).second) {
                                    changed = true;
                                }
                                break;
                            }
                            
                            // If it's a non-terminal, add its FIRST set (except &)
                            for (const string& first : firstSets.at(rest[j])) {
                                if (first != "&") {
                                    if (follow[symbols[i]].insert(first).second) {
                                        changed = true;
                                    }
                                }
                            }
                            
                            // Check if this symbol can derive empty
                            if (firstSets.at(rest[j]).count("&") == 0) {
                                break;
                            }
                            
                            // If we reached the end and everything was nullable
                            if (j == rest.size() - 1 && firstSets.at(rest[j]).count("&") > 0) {
                                nullableRest = true;
                            }
                        }
                    }
                    
                    // If rest is nullable or we're at the end, add FOLLOW(nonTerm)
                    if (nullableRest || i == symbols.size() - 1) {
                        for (const string& followSymbol : follow[nonTerm]) {
                            if (follow[symbols[i]].insert(followSymbol).second) {
                                changed = true;
                            }
                        }
                    }
                }
            }
        }
    } while (changed);

    return follow;
}
// Helper function to print sets
void printSet(const string& name, const set<string>& set) {
    cout << name << " = { ";
    for (const auto& symbol : set) {
        cout << symbol << " ";
    }
    cout << "}" << endl;
}

// Structure to represent a production rule
struct Production {
    string left;      // Left-hand side
    string right;     // Right-hand side
    int ruleNumber;   // Rule number for reference
};

set<string> getTerminals(
    const map<string, vector<string>>& grammar,
    const map<string, set<string>>& firstSets,
    const map<string, set<string>>& followSets
) {
    set<string> terminals;
    
    // Add $ as it's always a terminal
    terminals.insert("$");
    
    // Scan through grammar rules
    for (const auto& [nonTerm, productions] : grammar) {
        for (const string& prod : productions) {
            if (prod == "&") continue;
            
            size_t pos = 0;
            while (pos < prod.length()) {
                if (!isspace(prod[pos])) {
                    string symbol;
                    while (pos < prod.length() && !isspace(prod[pos])) {
                        symbol += prod[pos++];
                    }
                    if (!isNonTerminal(symbol)) {  // Use the corrected isNonTerminal function
                        terminals.insert(symbol);
                    }
                } else {
                    pos++;
                }
            }
        }
    }

    // Also scan through FIRST and FOLLOW sets
    for (const auto& [_, firstSet] : firstSets) {
        for (const string& symbol : firstSet) {
            if (symbol != "&" && !isNonTerminal(symbol)) {
                terminals.insert(symbol);
            }
        }
    }

    for (const auto& [_, followSet] : followSets) {
        for (const string& symbol : followSet) {
            if (!isNonTerminal(symbol)) {
                terminals.insert(symbol);
            }
        }
    }

    return terminals;
}
// Function to create and display the parsing table
void createParseTable(
    map<pair<string, string>, string>& parseTable,
    const map<string, vector<string>>& grammar,
    const map<string, set<string>>& firstSets,
    const map<string, set<string>>& followSets,
    const string& startSymbol
) {
    // Get terminals and non-terminals
    set<string> terminals = getTerminals(grammar, firstSets, followSets);
    vector<string> nonTerminals;
    for (const auto& [nonTerm, _] : grammar) {
        nonTerminals.push_back(nonTerm);
    }
    
    // Create parsing table (using pair of strings as key)
    //map<pair<string, string>, string> parseTable;
    
    // For each production rule
    for (const auto& [nonTerm, productions] : grammar) {
        for (const string& prod : productions) {
            if (prod == "&") {
                // For ε-productions, use FOLLOW set
                for (const string& follow : followSets.at(nonTerm)) {
                    auto entry = make_pair(nonTerm, follow);
                    if (parseTable.find(entry) != parseTable.end()) {
                        cout << "Warning: Grammar is not LL(1)! Conflict at " << nonTerm << " with " << follow << endl;
                    }
                    parseTable[entry] = prod;
                }
            } else {
                // Split production into symbols
                vector<string> symbols;
                size_t pos = 0;
                while (pos < prod.length()) {
                    if (!isspace(prod[pos])) {
                        string symbol;
                        while (pos < prod.length() && !isspace(prod[pos])) {
                            symbol += prod[pos++];
                        }
                        symbols.push_back(symbol);
                    } else {
                        pos++;
                    }
                }

                // Get first symbol
                if (!symbols.empty()) {
                    string firstSymbol = symbols[0];
                    if (!isupper(firstSymbol[0])) {
                        // If first symbol is terminal
                        auto entry = make_pair(nonTerm, firstSymbol);
                        if (parseTable.find(entry) != parseTable.end()) {
                            cout << "Warning: Grammar is not LL(1)! Conflict at " << nonTerm << " with " << firstSymbol << endl;
                        }
                        parseTable[entry] = prod;
                    } else {
                        // If first symbol is non-terminal, use its FIRST set
                        for (const string& first : firstSets.at(firstSymbol)) {
                            if (first != "&") {
                                auto entry = make_pair(nonTerm, first);
                                if (parseTable.find(entry) != parseTable.end()) {
                                    cout << "Warning: Grammar is not LL(1)! Conflict at " << nonTerm << " with " << first << endl;
                                }
                                parseTable[entry] = prod;
                            }
                        }
                        // If first symbol can derive ε, need to look at FIRST of next symbols or FOLLOW
                        if (firstSets.at(firstSymbol).count("&") > 0) {
                            bool allCanBeEmpty = true;
                            size_t symIdx = 1;
                            while (allCanBeEmpty && symIdx < symbols.size()) {
                                if (!isupper(symbols[symIdx][0])) {
                                    auto entry = make_pair(nonTerm, symbols[symIdx]);
                                    if (parseTable.find(entry) != parseTable.end()) {
                                        cout << "Warning: Grammar is not LL(1)! Conflict at " << nonTerm << " with " << symbols[symIdx] << endl;
                                    }
                                    parseTable[entry] = prod;
                                    allCanBeEmpty = false;
                                } else {
                                    for (const string& first : firstSets.at(symbols[symIdx])) {
                                        if (first != "&") {
                                            auto entry = make_pair(nonTerm, first);
                                            if (parseTable.find(entry) != parseTable.end()) {
                                                cout << "Warning: Grammar is not LL(1)! Conflict at " << nonTerm << " with " << first << endl;
                                            }
                                            parseTable[entry] = prod;
                                        }
                                    }
                                    allCanBeEmpty = firstSets.at(symbols[symIdx]).count("&") > 0;
                                }
                                symIdx++;
                            }
                            if (allCanBeEmpty) {
                                for (const string& follow : followSets.at(nonTerm)) {
                                    auto entry = make_pair(nonTerm, follow);
                                    if (parseTable.find(entry) != parseTable.end()) {
                                        cout << "Warning: Grammar is not LL(1)! Conflict at " << nonTerm << " with " << follow << endl;
                                    }
                                    parseTable[entry] = prod;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Print the parsing table
    cout << "\nLL(1) Parsing Table:\n\n";
    
    // Print header row with terminals
    cout << setw(10) << "";
    vector<string> sortedTerminals(terminals.begin(), terminals.end());
    sort(sortedTerminals.begin(), sortedTerminals.end());
    for (const string& terminal : sortedTerminals) {
        cout << setw(15) << terminal;
    }
    cout << "\n" << string(10 + sortedTerminals.size() * 15, '-') << "\n";

    // Print each row
    sort(nonTerminals.begin(), nonTerminals.end());
    for (const string& nonTerm : nonTerminals) {
        cout << setw(10) << nonTerm;
        for (const string& terminal : sortedTerminals) {
            auto entry = parseTable.find({nonTerm, terminal});
            if (entry != parseTable.end()) {
                cout << setw(15) << (nonTerm + "->" + entry->second);
            } else {
                cout << setw(15) << "";
            }
        }
        cout << "\n";
    }

    
}


//-----------------------------------------------------------------------Assignment 3 -------------------------------//
// Structure to represent the parser state
struct ParserState {
    vector<string> stack;
    string input;
    size_t position;
    bool error;
    string errorMessage;
};

// Function to tokenize input string
vector<string> tokenizeInput(const string& input) {
    vector<string> tokens;
    istringstream iss(input);
    string token;
    while (iss >> token) {
        tokens.push_back(token);
    }
    tokens.push_back("$"); // Add end marker
    return tokens;
}

// Function to print parser state
void printParserState(const ParserState& state, const string& action) {
    cout << "\nStack: [ ";
    for (const string& symbol : state.stack) {
        cout << symbol << " ";
    }
    cout << "]";
    
    cout << "\nInput: ";
    for (size_t i = state.position; i < state.input.length(); i++) {
        cout << state.input[i];
    }
    
    cout << "\nAction: " << action << endl;
    cout << string(50, '-') << endl;
}

// Function to parse a single input string
bool parseString(
    const string& input,
    const map<pair<string, string>, string>& parseTable,
    const string& startSymbol,
    const set<string>& terminals
) {
    ParserState state;
    state.input = input;
    state.position = 0;
    state.error = false;
    
    // Initialize stack with start symbol and end marker
    state.stack.push_back("$");
    state.stack.push_back(startSymbol);
    
    vector<string> inputTokens = tokenizeInput(input);
    size_t inputPos = 0;
    
    cout << "\nParsing input: " << input << "\n";
    
    while (!state.stack.empty() && inputPos <= inputTokens.size()) {
        string stackTop = state.stack.back();
        string currentInput = inputPos < inputTokens.size() ? inputTokens[inputPos] : "$";
        
        // Print current state
        cout << "\nStack: [ ";
        for (const string& sym : state.stack) {
            cout << sym << " ";
        }
        cout << "]";
        cout << "\nCurrent Input: " << currentInput;
        
        if (stackTop == "$" && currentInput == "$") {
            cout << "\nAction: Accept - Parsing Complete\n";
            return true;
        }
        
        if (stackTop == currentInput) {
            // Terminal match
            state.stack.pop_back();
            inputPos++;
            cout << "\nAction: Match and advance\n";
        } else if (!isNonTerminal(stackTop)) {
            // Terminal mismatch
            cout << "\nError: Terminal mismatch - Expected " << stackTop 
                 << " but got " << currentInput << "\n";
            return false;
        } else {
            // Non-terminal on top
            auto entry = parseTable.find({stackTop, currentInput});
            if (entry == parseTable.end()) {
                cout << "\nError: No parsing table entry for "
                     << stackTop << " with input " << currentInput << "\n";
                return false;
            }
            
            // Replace non-terminal with its production
            state.stack.pop_back();
            string production = entry->second;
            
            if (production != "&") {  // If not epsilon
                vector<string> symbols;
                istringstream iss(production);
                string symbol;
                while (iss >> symbol) {
                    symbols.push_back(symbol);
                }
                
                // Push symbols in reverse order
                for (auto it = symbols.rbegin(); it != symbols.rend(); ++it) {
                    state.stack.push_back(*it);
                }
            }
            
            cout << "\nAction: Replace " << stackTop << " with " << production << "\n";
        }
        
        cout << string(50, '-');
    }
    
    if (!state.stack.empty()) {
        cout << "\nError: Stack not empty at end of input\n";
        return false;
    }
    
    return true;
}

// Function to parse input file
void parseInputFile(
    const string& filename,
    const map<pair<string, string>, string>& parseTable,
    const string& startSymbol,
    const set<string>& terminals
) {
    ifstream file(filename);
    if (!file.is_open()) {
        cout << "Error: Could not open input file " << filename << endl;
        return;
    }
    
    string line;
    int lineNumber = 0;
    while (getline(file, line)) {
        lineNumber++;
        cout << "\n\nParsing line " << lineNumber << ": " << line << "\n";
        cout << string(50, '=') << endl;
        
        bool success = parseString(line, parseTable, startSymbol, terminals);
        
        cout << "\nResult for line " << lineNumber << ": "
             << (success ? "SUCCESS" : "FAILED") << endl;
        cout << string(50, '=') << endl;
    }
}



//------------------------------------------------------------------------------------------------------
int main() {
    string filename = "grammar.txt";
    string inputFile = "input.txt"; 
    map<string, vector<string>> grammar = readCFG(filename);
    // Assignment 2 //
    cout << "Original CFG:" << endl;
    printCFG(grammar);
    leftFactorCFG(grammar);
    cout << "\nLeft Factored CFG:" << endl;
    printCFG(grammar);
    removeLeftRecursion(grammar);
    cout << "\nLeft Recursion Removed CFG:" << endl;
    printCFG(grammar);
    // Compute FIRST sets
    auto firstSets = computeFirstSets(grammar);
    // Print FIRST sets
    cout << "FIRST sets:" << endl;
    for (const auto& [nonTerminal, firstSet] : firstSets) {
        printSet("FIRST(" + nonTerminal + ")", firstSet);
    }
    // Compute FOLLOW sets (assuming E is the start symbol)
    auto followSets =  computeFollowSets(grammar, firstSets, "EXP");
    // Print FOLLOW sets
    cout << "\nFOLLOW sets:" << endl;
    for (const auto& [nonTerminal, followSet] : followSets) {
        printSet("FOLLOW(" + nonTerminal + ")", followSet);
    }
    // Create parsing table (modified to return the table)
    map<pair<string, string>, string> parseTable;
    set<string> terminals = getTerminals(grammar, firstSets, followSets);
    createParseTable(parseTable, grammar, firstSets, followSets, "EXP");

    // Assignment 3 //

    // Parse input file
    cout << "\nParsing input file: " << inputFile << "\n";
    parseInputFile(inputFile, parseTable, "EXP", terminals);
    

    
    return 0;
}
