int main() {
    ( int ) x ;
    ( char ) x ;
    ( void ) x ;
    ( struct foo ) x ;
    ( int ) 1 ;
    ( int ) "string" ; 
    ( int ) - 1 ;
    ( int ) x ( ) ;
    ( int * ) ( * x ) ;
    ( struct foo * ) sizeof ( struct foo * );
    ( int * ) x = ( int * ) x ;
    ( int * ) x.field ;
    ( int * ) x [ field ] ;
    ( int * ) x [ ( int ) x ] ;
    ( int * ) ( 5 != 4 ) = ( int * ) ( 5 != 4 ) ;
    return (int*)x;
}