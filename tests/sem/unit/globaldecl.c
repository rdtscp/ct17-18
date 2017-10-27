struct obj {
    int num_field;
    char ltr_field;

    int * num_ptr_field;
    char * ltr_ptr_field;
};

int num;
char ltr;

int * num_ptr;
int * ltr_ptr;

int num_arr[5];
char ltr_arr[5];

struct obj obj;

void main() {
    num = 5;
    ltr = 'c';
    num_arr[0] = 5;
    ltr_arr[0] = 'c';
    
    obj.num_field = 5;
    obj.ltr_field = 'c';
}