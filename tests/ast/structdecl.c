struct object {
    int num_field;
    char letter_field;
    void void_field;
    struct obj obj_field;

    int * num_ptr;
    char * letter_ptr;
    void * void_ptr;
    struct obj * obj_ptr;

    int num_arr[5];
    char letter_arr[5];
    void void_arr[5];
    struct obj obj_arr[5];

    int * num_arr[5];
    char * letter_arr[5];
    void * void_arr[5];
    struct obj * obj_arr[5];
};