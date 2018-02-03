package dataType;

import java.io.Serializable;

public class KeyPair implements Serializable{
  int x;
  int y;
  public KeyPair(int x,int y) {
	  this.x=x;
	  this.y=y;
  }
  public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
}
