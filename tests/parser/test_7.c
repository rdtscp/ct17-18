struct foobar {
    char foo;
    char bar;
};

char a;
int b;
void c;

void main() {
    char ab;
    int bc;
    void cd;

    func_call();

    b = func_call();

    while (a) {
        while_a();
    }

    if (a) {
        if (b) {
            do_b();
        }
        else {
            else_b();
        }
    }
    else {
        else_a();
    }

}
void main(int foo, char bar) {
    char ab;
    int bc;
    void cd;

    func_call(foo, bar, baz);

    b = func_call();

    sum_thing = func_call(1,2,3,4, "string", 'c');

    while (x) -x;
    while (x) if (x) return x;

    while ((x%4)) {
        arr[5].field = *((int)arr[6].field2%2) + foo.bar[4] + sizeof(int) + sizeof(int*);
        funcall(*((int)arr[6].field2%2) + foo.bar[4] + sizeof(int) + sizeof(int*));
    }
    while (*((int)arr[6].field2%2) + foo.bar[4] + sizeof(int) + sizeof(int*)) { return *((int)arr[6].field2%2) + foo.bar[4] + sizeof(int) + sizeof(int*); }
    if (*((int)arr[6].field2%2) + foo.bar[4] + sizeof(int) + sizeof(int*)) x = 5;
    else p = *((int)arr[6].field2%2) + foo.bar[4] + sizeof(int) + sizeof(int*)%2 != 5 + "string ";
}