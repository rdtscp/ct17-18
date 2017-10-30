struct foo {
    int ident;
    int arr[5];
};

struct foo2 {
    int field1;
    int field2;
};

int x;
int array[5];
struct foo s_arr[5];
struct foo bar;

struct foo func() {
    struct foo bar;
    return bar;
}

void main() {
    char ltr;
    char str[6];
    int num;
    num = read_i();
    ltr = read_c();
    print_i(num);
    print_c(ltr);
    print_s("string");
    // num = (bar.arr)[2];
    // num = (s_arr[0]).ident;
    // num = func().ident;
}