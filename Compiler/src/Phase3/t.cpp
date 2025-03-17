#include <iostream>
#include <fstream>
#include <sstream>
#include <vector>
#include <map>
#include <set>
#include <algorithm>


#include <string>




using namespace std;

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

// Function to perform left factoring on a CFG
void leftFactorCFG(map<string, vector<string>>& grammar) {
    map<string, vector<string>> newGrammar;
    int newVarCount = 1;

    for (auto& [nonterminal, productions] : grammar) {
        string prefix = commonPrefix(productions);
        if (!prefix.empty() && prefix.length() > 0) {
            string newNonTerminal = nonterminal + "'" + to_string(newVarCount++);
            vector<string> factoredPart, remainingPart;
            
            for (const string& prod : productions) {
                if (prod.substr(0, prefix.size()) == prefix) {
                    string suffix = prod.substr(prefix.size());
                    factoredPart.push_back(suffix.empty() ? "ε" : suffix);
                } else {
                    remainingPart.push_back(prod);
                }
            }
            
            newGrammar[nonterminal] = {prefix + newNonTerminal};
            newGrammar[newNonTerminal] = factoredPart;
            
            if (!remainingPart.empty()) {
                for (const auto& prod : remainingPart) {
                    newGrammar[nonterminal].push_back(prod);
                }
            }
        } else {
            newGrammar[nonterminal] = productions;
        }
    }

    grammar = newGrammar;
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

// Helper function to check if a symbol is non-terminal
bool isNonTerminal(const string& symbol) {
    return isupper(symbol[0]); // Assuming non-terminals start with uppercase letters
}

// Function to compute FIRST sets for all non-terminals
map<string, set<string>> computeFirstSets(const map<string, vector<string>>& grammar) {
    map<string, set<string>> firstSets;
    bool changed;

    // Initialize FIRST sets
    for (const auto& [nonTerminal, productions] : grammar) {
        firstSets[nonTerminal] = {};
    }

    do {
        changed = false;
        
        // For each production rule
        for (const auto& [nonTerminal, productions] : grammar) {
            for (const string& production : productions) {
                size_t pos = 0;
                bool canBeEmpty = true;
                
                // Handle empty production
                if (production == "&") {
                    if (firstSets[nonTerminal].insert("&").second) {
                        changed = true;
                    }
                    continue;
                }

                // Process each symbol in the production
                while (pos < production.length() && canBeEmpty) {
                    string symbol;
                    if (pos + 1 < production.length() && !isupper(production[pos]) && !isupper(production[pos + 1])) {
                        // Handle terminal symbols that are two characters long (like id)
                        symbol = production.substr(pos, 4);
                        pos += 2;
                    } else {
                        symbol = string(1, production[pos]);
                        pos++;
                    }

                    if (!isNonTerminal(symbol)) {
                        // If it's a terminal, add it to FIRST set
                        if (firstSets[nonTerminal].insert(symbol).second) {
                            changed = true;
                        }
                        canBeEmpty = false;
                    } else {
                        // If it's a non-terminal, add its FIRST set
                        bool hasEpsilon = false;
                        for (const string& first : firstSets[symbol]) {
                            if (first != "&") {
                                if (firstSets[nonTerminal].insert(first).second) {
                                    changed = true;
                                }
                            } else {
                                hasEpsilon = true;
                            }
                        }
                        if (!hasEpsilon) {
                            canBeEmpty = false;
                        }
                    }
                }

                if (canBeEmpty) {
                    if (firstSets[nonTerminal].insert("&").second) {
                        changed = true;
                    }
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
    map<string, set<string>> followSets;

    // Initialize FOLLOW sets
    for (const auto& [nonTerminal, _] : grammar) {
        followSets[nonTerminal] = {};
    }

    // Add $ to FOLLOW(S) where S is the start symbol
    followSets[startSymbol].insert("$");

    bool changed;
    do {
        changed = false;
        
        // For each production rule
        for (const auto& [nonTerminal, productions] : grammar) {
            for (const string& production : productions) {
                if (production == "&") continue;
                
                vector<string> symbols;
                size_t pos = 0;
                
                // Split production into symbols
                while (pos < production.length()) {
                    if (pos + 1 < production.length() && 
                        production[pos] == ' ' && 
                        (production[pos + 1] == '+' || production[pos + 1] == '*')) {
                        symbols.push_back(string(1, production[pos + 1]));
                        pos += 2;
                    } else if (!isspace(production[pos])) {
                        string symbol;
                        while (pos < production.length() && !isspace(production[pos])) {
                            symbol += production[pos++];
                        }
                        symbols.push_back(symbol);
                    } else {
                        pos++;
                    }
                }

                // Process each symbol in the production
                for (size_t i = 0; i < symbols.size(); i++) {
                    if (!isNonTerminal(symbols[i])) continue;

                    // For all symbols following the current one
                    bool allCanBeEmpty = true;
                    set<string> firstOfRemaining;
                    
                    for (size_t j = i + 1; j < symbols.size() && allCanBeEmpty; j++) {
                        if (!isNonTerminal(symbols[j])) {
                            firstOfRemaining.insert(symbols[j]);
                            allCanBeEmpty = false;
                        } else {
                            for (const string& first : firstSets.at(symbols[j])) {
                                if (first != "&") {
                                    firstOfRemaining.insert(first);
                                }
                            }
                            allCanBeEmpty = firstSets.at(symbols[j]).count("&") > 0;
                        }
                    }

                    // Add computed FIRST set to FOLLOW set
                    for (const string& symbol : firstOfRemaining) {
                        if (followSets[symbols[i]].insert(symbol).second) {
                            changed = true;
                        }
                    }

                    // If all following symbols can derive ε, or if this is the last symbol
                    if (allCanBeEmpty || i == symbols.size() - 1) {
                        for (const string& follow : followSets[nonTerminal]) {
                            if (followSets[symbols[i]].insert(follow).second) {
                                changed = true;
                            }
                        }
                    }
                }
            }
        }
    } while (changed);

    return followSets;
}

// Helper function to print sets
void printSet(const string& name, const set<string>& set) {
    cout << name << " = { ";
    for (const auto& symbol : set) {
        cout << symbol << " ";
    }
    cout << "}" << endl;
}

int main() {
    string filename = "grammar.txt";
    map<string, vector<string>> grammar = readCFG(filename);
    
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
    auto followSets =  computeFollowSets(grammar, firstSets, "E");
    
    // Print FOLLOW sets
    cout << "\nFOLLOW sets:" << endl;
    for (const auto& [nonTerminal, followSet] : followSets) {
        printSet("FOLLOW(" + nonTerminal + ")", followSet);
    }

    
    return 0;
}
