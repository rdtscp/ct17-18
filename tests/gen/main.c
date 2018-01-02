struct foo {
    int id;
    char ltr;
};

struct bar {
    int index;
    struct foo id;
    int val;
};

int main() {
    struct bar obj;
    (obj.id).id = 1;
}