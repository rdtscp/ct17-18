package sem;

import java.util.Map;
import java.util.HashMap;

public class Scope {
	public Scope outer;
	private Map<String, Symbol> symbolTable;
	
	public Scope(Scope outer) { 
		this.outer = outer; 
		symbolTable = new HashMap<String, Symbol>();
	}
	
	public Scope() { this(null); }
	
	public Symbol lookup(String name) {
		Scope currScope 	= this;
		Symbol currSymbol 	= null;
		while(currScope != null) {
			currSymbol = currScope.lookupCurrent(name);
			if (currSymbol != null) return currSymbol;
			currScope = currScope.outer;
		}
		return null;
	}
	
	public Symbol lookupCurrent(String name) {
		if (symbolTable.containsKey(name)) {
			return symbolTable.get(name);
		}
		return null;
	}
	
	public void put(Symbol sym) {
		symbolTable.put(sym.name, sym);
	}
}
