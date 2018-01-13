int foo() {
  int a = 7;
  
  char x = 'a';    // dead
  int y = 11;      // dead
  int z = 12;      // dead

  int b = a * 2;
  int c = b - a;   // dead 
  int d = c / a;   // dead
  int e = y + z;   // dead
  b = 5 + e;

  return b;
}