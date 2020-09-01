public class TA extends Data {

	public TA(String descr, float amount, double percent) {
		super(descr, amount, percent);
	}

	@Override
	public String toString() {
		return super.toString() + " /// Days: " + amount;
	}
}