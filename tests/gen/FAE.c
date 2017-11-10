#include "../minic-stdlib.h"

struct foo {
    int field1;
    int field2;
    char field3;
};

struct bar {
    int field4;
    char field5;
    char field6;
};

struct foo g_bar;
struct bar g_foo;

void func(int x) {
    print_i(x + 5);
}

int main() {
    struct foo bar;
    struct bar foo;
    g_bar.field1 = 11;
    bar.field2 = 12;
    g_bar.field3 = 'c';
    foo.field4 = 14;
    g_foo.field5 = 'e';
    foo.field6 = 'f';
    print_i(g_bar.field1 + bar.field2 + foo.field4);
    print_c(g_bar.field3);
    print_c(g_foo.field5);
    print_c(foo.field6);
    func(foo.field4);
}
