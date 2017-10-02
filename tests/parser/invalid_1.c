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

    while (a) {
        while_a();
    }

    if (a) {
        if (b) {
            return;
        }
    }
    else {
        else_a();
    }

}