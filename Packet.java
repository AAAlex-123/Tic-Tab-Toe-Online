package ttt_online;

import java.io.Serializable;

public class Packet implements Serializable {
	private static final long serialVersionUID = 2136986143180317127L;
	
	public final String attribute;
	public final String value;
	
	public Packet(String attribute, String value) {
		this.attribute = attribute;
		this.value = value;
	}
}
