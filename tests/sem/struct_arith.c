struct foo {
    int x;
    int y;
    char z;
};

struct bar {
    int a;
    int b;
    char c;
};

void main () {
    struct foo f;
    struct bar b;
    f.x = b.a;
    b.c = f.z;
    b.b = f.y;
}