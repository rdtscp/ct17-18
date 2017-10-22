package sem;

import ast.*;

public class Procedure extends Symbol {
	
	public String name;
	public Type type;
	
	public Procedure(String name, Type type) {
        super(name);
		this.name = name;
		this.type = type;
	}
}
