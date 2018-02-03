package dataType;

import java.io.Serializable;
import java.util.HashMap;

public class SecurityInfo implements Serializable {
HashMap<Integer,String> lookupTable;
KeyPair kp;
public void setLookupTable(HashMap<Integer, String> lookupTable) {
	this.lookupTable = lookupTable;
}
public void setKp(KeyPair kp) {
	this.kp = kp;
}
public HashMap<Integer, String> getLookupTable() {
	return lookupTable;
}
public KeyPair getKp() {
	return kp;
}
}
