// Structs

struct foo {
    int ident;
    int arr[5];
};

struct foo2 {
    int field1;
    int field2;
};

// Return standard functions.

int retInt() {
    int out;
    return out;
}

char retChar() {
    char out;
    return out;
}

struct foo retStruct() {
    struct foo output;
    return output;
}

struct foo2 retStruct2() {
    struct foo2 output;
    return output;
}

// Return pointer functions.

int * retIntPtr() {
    int *out;
    return out;
}

char * retCharPtr() {
    char *out;
    return out;
}

struct foo * retStructFooPtr() {
    struct foo * out;
    return out;
}

struct foo2 * retStuctFoo2Ptr() {
    struct foo2 * out;
    return out;
}

// Standard argument functions.

void argInt(int num) {
    print_i(num);
}

void argChar(char ltr) {
    print_c(ltr);
}

void argStructFoo(struct foo obj) {
    print_i(obj.ident);
}

void argStructFoo2(struct foo2 obj) {
    print_i(obj.field1);
}

// Pointer argument functions.

void argIntPtr(int * num) {
    print_i(*num);
}

void argCharPtr(char * str) {
    print_c(*str);
    print_s(str);
    print_s("string");
}

void argStructFooPtr(struct foo * obj) {
    print_i((*obj).ident);
}

void argStructFoo2Ptr(struct foo2 * obj) {
    print_i((*obj).field1);
}

// Misc Tests

void arrayTests() {
    int arr[5];
    int arr2[5];
    int * arr_ptr[5];

    int num;

    arr = arr2;
    num = arr[0];
    num = *arr_ptr[0];
}

int main() {
    struct foo obj1;
    struct foo2 obj2;
    int num;
    char ltr;

    struct foo * obj1_ptr;
    struct foo2 * obj2_ptr;
    int * num_ptr;
    char * ltr_ptr;

    // Test Strings
    argCharPtr("string");

    num = retInt();
    ltr = retChar();

    obj1 = retStruct();
    obj2 = retStruct2();

    num_ptr = retIntPtr();
    ltr_ptr = retCharPtr();

    obj1 = *retStructFooPtr();
    obj1_ptr = retStructFooPtr();

    obj2 = *retStuctFoo2Ptr();
    obj2_ptr = retStuctFoo2Ptr();

    argInt(num);
    argChar(ltr);

    argInt(*num_ptr);
    argChar(*ltr_ptr);

    argStructFoo(obj1);
    argStructFoo2(obj2);

    argStructFoo(*obj1_ptr);
    argStructFoo2(*obj2_ptr);
    
    argIntPtr(num_ptr);
    argCharPtr(ltr_ptr);

    argStructFooPtr(obj1_ptr);
    argStructFoo2Ptr(obj2_ptr);

    arrayTests();

    return 1;
}