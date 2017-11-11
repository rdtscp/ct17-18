#include "../minic-stdlib.h"

int main() {
    int * x;
    print_s("assert 1337666999 == ");
    x = (int*)mcmalloc(8);

    *x = 1337; 
    print_i(*x);

    x =(int *)(x + 4);
    *x = 666;
    print_i(*x);

    x =(int *)(x + 4);
    *x = 999;
    print_i(*x);
}
