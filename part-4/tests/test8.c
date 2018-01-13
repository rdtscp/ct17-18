int a = 5;

int func() {
    int b = 10;
    int c = a + b;  // Dead
    a = b + 1;
    return b + a;
}