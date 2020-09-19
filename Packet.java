package ttt_online;

import java.io.Serializable;

public class Packet<T> implements Serializable {

	private static final long serialVersionUID = -2755557613017784530L;

	public final String attribute;
	public final T value;

	public Packet(String attribute, T value) {
		this.attribute = attribute;
		this.value = value;
	}
}
