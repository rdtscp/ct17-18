
int main() {
    int x;
    x = 50;
    while (x) {
        if (x > 0) {
            int foo;
            foo = 5;
            print_i(foo);
            {
                int foo;
                foo = 0;
                print_i(foo);
            }
        }
        print_i(x);print_c('\n');
        x = x - 1;
    }
    
}