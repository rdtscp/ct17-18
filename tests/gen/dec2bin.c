/**
 * Converts a given decimal number into its binary form representation.
 */
 #include "io.h"
 
 int power(int base, int exponent) {
   if (exponent == 0) return 1;
   exponent = exponent - 1;
   return base * power(base, exponent);
 }
 
 int dec2bin(int decimal, int exponent) {
   int remainder; int exponent_plus_one;
   int two;
   two = 2;
   if (decimal == 0) { print_s("0"); return 0; }
   else if (decimal / power(two, exponent)) {
     exponent_plus_one = exponent + 1;
     remainder = dec2bin(decimal, exponent_plus_one);
     if (remainder / power(two, exponent)) {
       print_s("1");
       return remainder - power(two, exponent);
     } else {
       print_s("0");
       return remainder;
     }
   } else {
     return decimal;
   }
 }
 
 void main() {
   int n;
   int zero;
   zero = 0;
   print_s("Enter integer> ");
   n = read_i();
   dec2bin(n, zero);
   print_s("\n");
 }