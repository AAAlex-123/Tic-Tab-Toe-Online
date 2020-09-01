public class PA extends Data {
	
	public PA(String descr, float amount, double percent) {
		super(descr, amount, Math.abs(percent));
	}				// amount can only be negative in TA Data
					// protects user from inputing negative amounts where they are positive

	@Override
	public String toString() {
		return super.toString() + " /// Amount: " + amount + "$";
	}
}