int foo()
{
	int x = 2;       // (1)
	int y = 3;       // (2)	
 	x = x + y - y;   // (3)
	
	return x;        // (4)
}