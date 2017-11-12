int x;

int func2() {

}

int func(int x, int y) {
    print_i(x);
    print_i(y);
}

int main() {
    int x;
    x = 1;
    print_i(x);
    {
        int x;
        x = 2;
        print_i(x);
    }
    print_i(x);
    {
        int x;
        x = 3;
        print_i(x);
    }
    print_i(x);
}
