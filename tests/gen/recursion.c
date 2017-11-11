

int rec(int x) {
    print_i(x);print_c('\n');
    if (x == 0) return 0;
    return rec(x-1);
}

int rec2(int x) {
    print_i(x);print_c('\n');
    if (x < 50) return rec2(x - 1);
    return rec(x-1);
}

int main() {
    rec2(100);
}