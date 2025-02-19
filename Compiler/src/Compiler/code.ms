global integer max = 100  
global float pi = 3.14159
global boolean flag = true  
global char letter = 'a'  
global string greeting = "hello, world"
/////////
/* This is a test of
   multiline comments
   with multiple lines
   and some * asterisks *
   and / slashes /
   to make sure it works
*/

function int main() {

	integer count = 0  
    float sum = 0.0  
    boolean done = false  
    char ch = 'x'  
    sum = pi - max
    sum = pi * max
    sum = pi / max
    sum = pi % max
    sum = pi ^ max
  
    
    // single-line comment: testing loop and arithmetic operations
    while (count < max) {  
        sum = sum + pi  
        count = count + 1  
    }  
    
    if (done) {  
        return  
    } else {  
        flag = true  
    }  
    
    /* Nested comments are not allowed */
    return 0
}