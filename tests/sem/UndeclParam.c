int x;

int func(int arg1) {
    return 1;
}

int main() {
    func(1);
    x = 5;
    {
        int x;
        x = 10;
    }
    return 0;
}