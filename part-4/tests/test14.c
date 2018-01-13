volatile int a = 0;
int b = 5;

int main() {
    volatile int c = 10;
    volatile int d = 15;
    int e = a + b;
    b = e;
    c = e;
    d = e;
    a = d;
    e = e + 1;
    c = c + 1;
    return b;
}