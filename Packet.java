package ttt_online;

import java.io.Serializable;

public class Packet<T> implements Serializable {
	
	private static final long serialVersionUID = -2755557613017784530L;

	public final int attribute;
	public final T value;

	public Packet(int attribute, T value) {
		this.attribute = attribute;
		this.value = value;
	}
	
	// never actually used but why not
	public String toString() {
		return String.format("Packet: [%d, %s]", attribute, value);
	}
}
