

int fn1() {
    return fn2();
}

int fn2() {
    return fn1();
}

int main() {
    return fn2();
}