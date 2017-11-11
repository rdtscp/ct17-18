#include "../minic-stdlib.h"

int main() {
    /* Output should be entirely 1's */
    int num1;
    int num2;
    int num3;
    int num4;

    print_s("assert 11111111111111111111111111111111 \n       =\n       ");

    // EQ VALID
    if (1 == 1) print_i(1);
    else print_i(0);
    // EQ INVALID
    if (1 == 0) print_i(0);
    else print_i(1);

    // ADD VALID
    if (1 + 1 == 2) print_i(1);
    else print_i(0);
    // ADD INVALID
    if (1 + 1 == 3) print_i(0);
    else print_i(1);

    // SUB VALID
    if (1 - 1 == 0) print_i(1);
    else print_i(0);
    // SUB INVALID
    if (1 - 1 == 1) print_i(0);
    else print_i(1);

    // MUL VALID
    if (1 * 2 == 2) print_i(1);
    else print_i(0);
    // MUL INVALID
    if (1 * 2 == 1) print_i(0);
    else print_i(1);

    // DIV
    num1 = 16;
    num2 = 4;
    num3 = 1;
    num4 = 0;
    if (num1 / num2 == 4) print_i(1);
    else print_i(0);
    if (num1 / num2 == 3) print_i(0);
    else print_i(1);
    if (num4 / num3 == 0) print_i(1);
    else print_i(1);
    if (num2 / num1 == 0) print_i(1);
    else print_i(0);
    if (num3 / num3 == 1) print_i(1);
    else print_i(0);

    // MOD VALID
    if (16%4 == 0) print_i(1);
    else print_i(0);
    // MOD INVALID
    if (16%4 == 3) print_i(0);
    else print_i(1);

    // GT VALID
    if (1 > 0 == 1) print_i(1);
    else print_i(0);
    // GT INVALID
    if (1 > 1 == 1) print_i(0);
    else print_i(1);

    // LT VALID
    if (0 < 1 == 1) print_i(1);
    else print_i(0);
    // LT INVALID
    if (1 < 1 == 1) print_i(0);
    else print_i(1);

    // GE VALID
    if (1 >= 1) print_i(1);
    else print_i(0);
    if (1 >= 0) print_i(1);
    else print_i(0);
    // GE INVALID
    if (0 >= 1) print_i(0);
    else print_i(1);

    // LE VALID
    if (1 <= 1) print_i(1);
    else print_i(0);
    if (0 <= 1) print_i(1);
    else print_i(0);
    // LE INVALID
    if (1 <= 0) print_i(0);
    else print_i(1);

    // NE VALID
    if (1 != 0) print_i(1);
    else print_i(0);
    // NE INVALID
    if (1 != 1) print_i(0);
    else print_i(1);

    // OR VALID
    if (1 || 1 == 1) print_i(1);
    else print_i(0);
    if (1 || 0 == 1) print_i(1);
    else print_i(0);
    if (0 || 1 == 1) print_i(1);
    else print_i(0);
    if (0 || 0 == 0) print_i(1);
    else print_i(0);

    // AND VALID
    if (1 && 1 == 1) print_i(1);
    else print_i(0);
    if (1 && 0 == 1) print_i(0);
    else print_i(1);
    if (0 && 1 == 1) print_i(0);
    else print_i(1);
    if (0 && 0 == 0) print_i(0);
    else print_i(1);
}
