int main() {
    (int)x;
    (char)x;
    (void)x;
    (struct foo)x;
    ( int ) x ;
    ( char ) x ;
    ( void ) x ;
    ( struct foo ) x ;
    (int)((char)x);
    ( int ) ( ( char ) x ) ;
    (char)((int)((char)x));
    (struct foo)((int)((char)x));

    (int*)x;
    (char*)x;
    (struct foo*)x;
    (void*)x;
    
    (int)((int*)x);
    x = x - (int)-x;
}