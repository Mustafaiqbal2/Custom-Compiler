/*
 * Context-Free Grammar Processor
 * Author: saadnadeem554
 * Date: 2025-03-16 15:59:21 UTC
 * 
 * This program processes Context-Free Grammars (CFG) by:
 * 1. Reading from file
 * 2. Removing left factoring
 * 3. Removing left recursion
 * 4. Computing FIRST sets
 * 5. Computing FOLLOW sets
 * 6. Constructing LL(1) parsing table
 */

 #include <stdio.h>
 #include <stdlib.h>
 #include <string.h>
 #include <stdbool.h>
 
#define MAX_PRODUCTIONS 100
#define MAX_SYMBOLS 100
#define MAX_RHS 10 // Limit the number of alternative productions per rule
#define MAX_LENGTH 50 // Limit production size

 // Structure to represent a production rule
 typedef struct {
     char lhs;                     // Left-hand side non-terminal
     char rhs[MAX_LENGTH][50];     // Right-hand side productions
     int num_rhs;                  // Number of alternative productions
 } Production;
 
 // Structure to represent the grammar
 typedef struct {
     Production productions[MAX_PRODUCTIONS];
     int num_productions;
     char non_terminals[MAX_SYMBOLS];
     int num_non_terminals;
     char terminals[MAX_SYMBOLS];
     int num_terminals;
 } Grammar;
 
 // Structure to represent FIRST and FOLLOW sets
 typedef struct {
     char symbol;
     char elements[MAX_SYMBOLS];
     int count;
 } SymbolSet;
 
 // Function declarations
 bool is_non_terminal(char c);
 bool is_terminal(char c);
 char get_next_non_terminal(Grammar g);
 void print_grammar(Grammar g);
 void print_sets(SymbolSet* sets, int count, const char* set_name);
 int common_prefix_length(const char* s1, const char* s2);
 
 // Helper function to check if a character is a non-terminal
 bool is_non_terminal(char c) {
     return c >= 'A' && c <= 'Z';
 }
 
 // Helper function to check if a character is a terminal
bool is_terminal(char c) {
    return (c >= 'a' && c <= 'z') || c == '(' || c == ')' || c == '*' || c == '+' || c == '&'; }
 
 // Print the grammar
 void print_grammar(Grammar g) {
     printf("\nGrammar Productions:\n");
     for (int i = 0; i < g.num_productions; i++) {
         Production p = g.productions[i];
         printf("%c -> ", p.lhs);
         for (int j = 0; j < p.num_rhs; j++) {
             printf("%s", p.rhs[j]);
             if (j < p.num_rhs - 1) {
                 printf(" | ");
             }
         }
         printf("\n");
     }
 }
 
 // Print FIRST or FOLLOW sets
 void print_sets(SymbolSet* sets, int count, const char* set_name) {
     printf("\n%s Sets:\n", set_name);
     for (int i = 0; i < count; i++) {
         printf("%s(%c) = { ", set_name, sets[i].symbol);
         for (int j = 0; j < sets[i].count; j++) {
             printf("%c", sets[i].elements[j]);
             if (j < sets[i].count - 1) printf(", ");
         }
         printf(" }\n");
     }
 }
 
 // Read grammar from file
 Grammar read_grammar_from_file(const char* filename) {
     Grammar g = {0};
     FILE* file = fopen(filename, "r");
     if (!file) {
         printf("Error: Could not open file %s\n", filename);
         exit(1);
     }
 
     char line[MAX_LENGTH];
     while (fgets(line, sizeof(line), file)) {
         // Remove newline if present
         line[strcspn(line, "\n")] = 0;
         
         // Parse the production rule
         Production* p = &g.productions[g.num_productions];
         p->lhs = line[0];
         
         // Add non-terminal to the list if not already present
         bool found = false;
         for (int i = 0; i < g.num_non_terminals; i++) {
             if (g.non_terminals[i] == p->lhs) {
                 found = true;
                 break;
             }
         }
         if (!found) {
             g.non_terminals[g.num_non_terminals++] = p->lhs;
         }
 
         // Skip the arrow (->)
         char* rhs = strstr(line, "->") + 2;
         while (*rhs == ' ') rhs++;
 
         // Split the right-hand side on |
         char* token = strtok(rhs, "|");
         while (token != NULL) {
             // Remove leading and trailing spaces
             while (*token == ' ') token++;
             char* end = token + strlen(token) - 1;
             while (end > token && *end == ' ') end--;
             *(end + 1) = '\0';
 
             strcpy(p->rhs[p->num_rhs], token);
             p->num_rhs++;
 
             // Collect terminals
             for (int i = 0; token[i]; i++) {
                 if (is_terminal(token[i])) {
                     bool found = false;
                     for (int j = 0; j < g.num_terminals; j++) {
                         if (g.terminals[j] == token[i]) {
                             found = true;
                             break;
                         }
                     }
                     if (!found) {
                         g.terminals[g.num_terminals++] = token[i];
                     }
                 }
             }
 
             token = strtok(NULL, "|");
         }
         g.num_productions++;
     }
     fclose(file);
     return g;
 }
 
 // Helper function to get next available non-terminal
 char get_next_non_terminal(Grammar g) {
     char nt = 'A';
     bool used;
     do {
         used = false;
         for (int i = 0; i < g.num_non_terminals; i++) {
             if (g.non_terminals[i] == nt) {
                 used = true;
                 nt++;
                 break;
             }
         }
     } while (used);
     return nt;
 }
 
 // Helper function to find common prefix between two strings
 int common_prefix_length(const char* s1, const char* s2) {
     int i = 0;
     while (s1[i] && s2[i] && s1[i] == s2[i])
         i++;
     return i;
 }
 
// Remove left factoring
Grammar remove_left_factoring(Grammar g) {
    Grammar result = {0};
    bool any_factoring_needed = false;
    
    // First pass: check if any factoring is needed
    for (int i = 0; i < g.num_productions && !any_factoring_needed; i++) {
        Production* p = &g.productions[i];
        for (int j = 0; j < p->num_rhs; j++) {
            for (int k = j + 1; k < p->num_rhs; k++) {
                int prefix_len = common_prefix_length(p->rhs[j], p->rhs[k]);
                if (prefix_len > 0) {
                    any_factoring_needed = true;
                    break;
                }
            }
            if (any_factoring_needed) break;
        }
    }
    
    // If no factoring needed, return original grammar
    if (!any_factoring_needed) {
        return g;
    }
    
    // Copy non-terminals and terminals
    memcpy(result.non_terminals, g.non_terminals, g.num_non_terminals);
    result.num_non_terminals = g.num_non_terminals;
    memcpy(result.terminals, g.terminals, g.num_terminals);
    result.num_terminals = g.num_terminals;

    // Process each production
    for (int i = 0; i < g.num_productions; i++) {
        Production* p = &g.productions[i];
        bool factoring_needed;
        
        do {
            factoring_needed = false;
            int max_prefix = 0;
            int first_prod = 0;
            
            // Find longest common prefix
            for (int j = 0; j < p->num_rhs; j++) {
                for (int k = j + 1; k < p->num_rhs; k++) {
                    int prefix_len = common_prefix_length(p->rhs[j], p->rhs[k]);
                    if (prefix_len > max_prefix) {
                        max_prefix = prefix_len;
                        first_prod = j;
                        factoring_needed = true;
                    }
                }
            }

            if (factoring_needed) {
                char new_nt = get_next_non_terminal(result);
                result.non_terminals[result.num_non_terminals++] = new_nt;
                
                Production new_prod = {0};
                new_prod.lhs = p->lhs;
                
                char prefix[MAX_LENGTH];
                strncpy(prefix, p->rhs[first_prod], max_prefix);
                prefix[max_prefix] = '\0';
                
                sprintf(new_prod.rhs[0], "%s%c", prefix, new_nt);
                new_prod.num_rhs = 1;
                
                Production new_nt_prod = {0};
                new_nt_prod.lhs = new_nt;
                
                int new_rhs_count = 0;
                for (int j = 0; j < p->num_rhs; j++) {
                    if (strncmp(p->rhs[j], prefix, max_prefix) == 0) {
                        if (p->rhs[j][max_prefix] == '\0') {
                            strcpy(new_nt_prod.rhs[new_rhs_count++], "ε");
                        } else {
                            strcpy(new_nt_prod.rhs[new_rhs_count++], &p->rhs[j][max_prefix]);
                        }
                    } else {
                        // Copy productions that don't share the prefix to the result
                        if (result.num_productions == 0 || 
                            result.productions[result.num_productions-1].lhs != p->lhs) {
                            Production orig_prod = {0};
                            orig_prod.lhs = p->lhs;
                            strcpy(orig_prod.rhs[0], p->rhs[j]);
                            orig_prod.num_rhs = 1;
                            result.productions[result.num_productions++] = orig_prod;
                        } else {
                            strcpy(result.productions[result.num_productions-1]
                                   .rhs[result.productions[result.num_productions-1].num_rhs++], 
                                   p->rhs[j]);
                        }
                    }
                }
                new_nt_prod.num_rhs = new_rhs_count;
                
                result.productions[result.num_productions++] = new_prod;
                result.productions[result.num_productions++] = new_nt_prod;
            } else if (result.num_productions == 0) {
                // If no factoring needed for this production, copy it as is
                result.productions[result.num_productions++] = *p;
            }
        } while (factoring_needed);
    }
    
    return result;
}
 // Remove left recursion
 Grammar remove_left_recursion(Grammar g) {
     Grammar result = {0};
     
     // Copy non-terminals and terminals
     memcpy(result.non_terminals, g.non_terminals, g.num_non_terminals);
     result.num_non_terminals = g.num_non_terminals;
     memcpy(result.terminals, g.terminals, g.num_terminals);
     result.num_terminals = g.num_terminals;
 
     for (int i = 0; i < g.num_productions; i++) {
         Production* p = &g.productions[i];
         bool has_left_recursion = false;
         
         // Check for left recursion
         for (int j = 0; j < p->num_rhs; j++) {
             if (p->rhs[j][0] == p->lhs) {
                 has_left_recursion = true;
                 break;
             }
         }
 
         if (has_left_recursion) {
             char new_nt = get_next_non_terminal(result);
             result.non_terminals[result.num_non_terminals++] = new_nt;
             
             Production non_recursive = {0};
             Production recursive = {0};
             
             non_recursive.lhs = p->lhs;
             recursive.lhs = new_nt;
             
             int non_rec_count = 0;
             int rec_count = 0;
             
             for (int j = 0; j < p->num_rhs; j++) {
                 if (p->rhs[j][0] == p->lhs) {
                     char temp[MAX_LENGTH];
                     strcpy(temp, &p->rhs[j][1]);
                     sprintf(recursive.rhs[rec_count++], "%s%c", temp, new_nt);
                 } else {
                     sprintf(non_recursive.rhs[non_rec_count++], "%s%c", p->rhs[j], new_nt);
                 }
             }
             
             strcpy(recursive.rhs[rec_count++], "&");
             
             non_recursive.num_rhs = non_rec_count;
             recursive.num_rhs = rec_count;
             
             result.productions[result.num_productions++] = non_recursive;
             result.productions[result.num_productions++] = recursive;
         } else {
             result.productions[result.num_productions++] = *p;
         }
     }
     
     return result;
 }
 
 // Compute FIRST sets
 SymbolSet* compute_first_sets(Grammar g) {
     SymbolSet* first_sets = malloc(sizeof(SymbolSet) * g.num_non_terminals);
     
     // Initialize FIRST sets
     for (int i = 0; i < g.num_non_terminals; i++) {
         first_sets[i].symbol = g.non_terminals[i];
         first_sets[i].count = 0;
     }
     
     bool changed;
     do {
         changed = false;
         
         for (int i = 0; i < g.num_productions; i++) {
             Production p = g.productions[i];
             int set_index = -1;
             
             // Find the corresponding FIRST set
             for (int j = 0; j < g.num_non_terminals; j++) {
                 if (first_sets[j].symbol == p.lhs) {
                     set_index = j;
                     break;
                 }
             }
             
             for (int j = 0; j < p.num_rhs; j++) {
                 char* rhs = p.rhs[j];
                 
                 // If first symbol is terminal
                 if (is_terminal(rhs[0])) {
                     bool exists = false;
                     for (int k = 0; k < first_sets[set_index].count; k++) {
                         if (first_sets[set_index].elements[k] == rhs[0]) {
                             exists = true;
                             break;
                         }
                     }
                     if (!exists) {
                         first_sets[set_index].elements[first_sets[set_index].count++] = rhs[0];
                         changed = true;
                     }
                 }
                 // If first symbol is non-terminal
                 else if (is_non_terminal(rhs[0])) {
                     int first_set_index = -1;
                     for (int k = 0; k < g.num_non_terminals; k++) {
                         if (first_sets[k].symbol == rhs[0]) {
                             first_set_index = k;
                             break;
                         }
                     }
                     
                     // Add all elements from the FIRST set of the non-terminal
                     for (int k = 0; k < first_sets[first_set_index].count; k++) {
                         bool exists = false;
                         for (int l = 0; l < first_sets[set_index].count; l++) {
                             if (first_sets[set_index].elements[l] == first_sets[first_set_index].elements[k]) {
                                 exists = true;
                                 break;
                             }
                         }
                         if (!exists) {
                             first_sets[set_index].elements[first_sets[set_index].count++] = 
                                 first_sets[first_set_index].elements[k];
                             changed = true;
                         }
                     }
                 }
             }
         }
     } while (changed);
     
     return first_sets;
 }
// Continue the compute_follow_sets function:
SymbolSet* compute_follow_sets(Grammar g, SymbolSet* first_sets) {
    SymbolSet* follow_sets = malloc(sizeof(SymbolSet) * g.num_non_terminals);
    
    // Initialize FOLLOW sets
    for (int i = 0; i < g.num_non_terminals; i++) {
        follow_sets[i].symbol = g.non_terminals[i];
        follow_sets[i].count = 0;
        
        // Add $ to FOLLOW set of start symbol
        if (i == 0) {
            follow_sets[i].elements[follow_sets[i].count++] = '$';
        }
    }

    bool changed;
    do {
        changed = false;

        // For each production
        for (int i = 0; i < g.num_productions; i++) {
            Production p = g.productions[i];

            // For each RHS
            for (int j = 0; j < p.num_rhs; j++) {
                char* rhs = p.rhs[j];
                int len = strlen(rhs);

                // For each symbol in RHS
                for (int k = 0; k < len; k++) {
                    // If current symbol is non-terminal
                    if (is_non_terminal(rhs[k])) {
                        int follow_index = -1;
                        // Find index of current non-terminal in follow_sets
                        for (int m = 0; m < g.num_non_terminals; m++) {
                            if (follow_sets[m].symbol == rhs[k]) {
                                follow_index = m;
                                break;
                            }
                        }

                        // If this is the last symbol
                        if (k == len - 1) {
                            // Add FOLLOW(LHS) to FOLLOW(current)
                            int lhs_follow_index = -1;
                            for (int m = 0; m < g.num_non_terminals; m++) {
                                if (follow_sets[m].symbol == p.lhs) {
                                    lhs_follow_index = m;
                                    break;
                                }
                            }

                            for (int m = 0; m < follow_sets[lhs_follow_index].count; m++) {
                                bool exists = false;
                                for (int n = 0; n < follow_sets[follow_index].count; n++) {
                                    if (follow_sets[follow_index].elements[n] == 
                                        follow_sets[lhs_follow_index].elements[m]) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    follow_sets[follow_index].elements[follow_sets[follow_index].count++] = 
                                        follow_sets[lhs_follow_index].elements[m];
                                    changed = true;
                                }
                            }
                        }
                        // If there are more symbols after current
                        else {
                            // If next symbol is terminal
                            if (is_terminal(rhs[k + 1])) {
                                bool exists = false;
                                for (int m = 0; m < follow_sets[follow_index].count; m++) {
                                    if (follow_sets[follow_index].elements[m] == rhs[k + 1]) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    follow_sets[follow_index].elements[follow_sets[follow_index].count++] = rhs[k + 1];
                                    changed = true;
                                }
                            }
                            // If next symbol is non-terminal
                            else if (is_non_terminal(rhs[k + 1])) {
                                // Add FIRST(next) - {ε} to FOLLOW(current)
                                int first_index = -1;
                                for (int m = 0; m < g.num_non_terminals; m++) {
                                    if (first_sets[m].symbol == rhs[k + 1]) {
                                        first_index = m;
                                        break;
                                    }
                                }

                                for (int m = 0; m < first_sets[first_index].count; m++) {
                                    if (first_sets[first_index].elements[m] != '&') {
                                        bool exists = false;
                                        for (int n = 0; n < follow_sets[follow_index].count; n++) {
                                            if (follow_sets[follow_index].elements[n] == 
                                                first_sets[first_index].elements[m]) {
                                                exists = true;
                                                break;
                                            }
                                        }
                                        if (!exists) {
                                            follow_sets[follow_index].elements[follow_sets[follow_index].count++] = 
                                                first_sets[first_index].elements[m];
                                            changed = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } while (changed);

    return follow_sets;
}

// Structure to represent LL(1) parsing table
typedef struct {
    char non_terminal;
    char terminal;
    char production[MAX_LENGTH];
} TableEntry;

typedef struct {
    TableEntry entries[MAX_PRODUCTIONS * MAX_SYMBOLS];
    int num_entries;
} ParsingTable;

// Construct LL(1) parsing table
ParsingTable construct_ll1_table(Grammar g, SymbolSet* first_sets, SymbolSet* follow_sets) {
    ParsingTable table = {0};
    
    // For each production
    for (int i = 0; i < g.num_productions; i++) {
        Production p = g.productions[i];
        
        // For each alternative in the production
        for (int j = 0; j < p.num_rhs; j++) {
            char* rhs = p.rhs[j];
            
            // If first symbol is terminal
            if (is_terminal(rhs[0]) && rhs[0] != '&') {
                TableEntry entry = {0};
                entry.non_terminal = p.lhs;
                entry.terminal = rhs[0];
                sprintf(entry.production, "%c -> %s", p.lhs, rhs);
                table.entries[table.num_entries++] = entry;
            }
            // If first symbol is non-terminal or ε
            else {
                int first_index = -1;
                
                // Find FIRST set of the first symbol
                if (is_non_terminal(rhs[0])) {
                    for (int k = 0; k < g.num_non_terminals; k++) {
                        if (first_sets[k].symbol == rhs[0]) {
                            first_index = k;
                            break;
                        }
                    }
                }
                
                // If ε-production or if FIRST set contains ε
                bool has_epsilon = (rhs[0] == '&');
                if (!has_epsilon && first_index != -1) {
                    for (int k = 0; k < first_sets[first_index].count; k++) {
                        if (first_sets[first_index].elements[k] == '&') {
                            has_epsilon = true;
                            break;
                        }
                    }
                }
                
                // Add entry for each terminal in FIRST set
                if (first_index != -1) {
                    for (int k = 0; k < first_sets[first_index].count; k++) {
                        if (first_sets[first_index].elements[k] != '&') {
                            TableEntry entry = {0};
                            entry.non_terminal = p.lhs;
                            entry.terminal = first_sets[first_index].elements[k];
                            sprintf(entry.production, "%c -> %s", p.lhs, rhs);
                            table.entries[table.num_entries++] = entry;
                        }
                    }
                }
                
                // If ε-production, add entries for FOLLOW set
                if (has_epsilon) {
                    int follow_index = -1;
                    for (int k = 0; k < g.num_non_terminals; k++) {
                        if (follow_sets[k].symbol == p.lhs) {
                            follow_index = k;
                            break;
                        }
                    }
                    
                    for (int k = 0; k < follow_sets[follow_index].count; k++) {
                        TableEntry entry = {0};
                        entry.non_terminal = p.lhs;
                        entry.terminal = follow_sets[follow_index].elements[k];
                        sprintf(entry.production, "%c -> %s", p.lhs, rhs);
                        table.entries[table.num_entries++] = entry;
                    }
                }
            }
        }
    }

    // Print the parsing table
    printf("\nLL(1) Parsing Table:\n");
    printf("%-15s%-15s%-30s\n", "Non-terminal", "Terminal", "Production");
    printf("------------------------------------------------\n");
    for (int i = 0; i < table.num_entries; i++) {
        printf("%-15c%-15c%-30s\n", 
            table.entries[i].non_terminal,
            table.entries[i].terminal,
            table.entries[i].production);
    }

    return table;
}

int main() {
    // Read grammar from file and print original grammar
    printf("\nOriginal Grammar:");
    
    Grammar original_grammar = read_grammar_from_file("grammar.txt");
    print_grammar(original_grammar);
    
    // Remove left factoring and print result
    Grammar factored_grammar = remove_left_factoring(original_grammar);
    printf("\nGrammar after Left Factoring:");
    print_grammar(factored_grammar);
    
    // Remove left recursion and print result
    Grammar final_grammar = remove_left_recursion(factored_grammar);
    printf("\nGrammar after Left Recursion Removal:");
    print_grammar(final_grammar);
    
    // Compute and print FIRST sets
    SymbolSet* first_sets = compute_first_sets(final_grammar);
    print_sets(first_sets, final_grammar.num_non_terminals, "FIRST");
    
    // Compute and print FOLLOW sets
    SymbolSet* follow_sets = compute_follow_sets(final_grammar, first_sets);
    print_sets(follow_sets, final_grammar.num_non_terminals, "FOLLOW");
    
    // Construct and print LL(1) parsing table
    ParsingTable parsing_table = construct_ll1_table(final_grammar, first_sets, follow_sets);
    
    // Free allocated memory
    free(first_sets);
    free(follow_sets);
    
    return 0;
}