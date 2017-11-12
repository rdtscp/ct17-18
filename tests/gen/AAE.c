int g_arr[5];
int sum;

int main() {
    int idx;
    idx = 0;
    // Populate array.
    while (idx < 5) {
        g_arr[idx] = idx * 3;
        idx = idx + 1;
    }
    /*
        [0,3,6,9,12]
    */
    idx = 0;
    // Print sum of array.
    sum = 0;
    while (idx < 5) {
        sum = sum + g_arr[idx];
        idx = idx + 1;
    }
    print_i(sum);
}