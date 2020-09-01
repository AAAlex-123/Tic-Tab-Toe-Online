public class EA extends Data {

	public EA(String descr, float amount, double percent){
		super(descr, amount, Math.abs(percent));
	}				// amount can only be negative in TA Data
					// protects user from inputing negative amounts where they are positive

	@Override
	public String toString() {
		return super.toString() + " /// Hours: " + amount;
	}
}