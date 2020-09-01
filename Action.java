public class Action implements ColInterface{
	private static int st_code = 0;

	private final int CODE;
	private Employee emp;
	private final Data DATA;
	
	public Action(Employee emp, Data data) {
		this.emp = emp;
		this.DATA = data;

		this.CODE = st_code;
		st_code++;
	}
	
	// getters
	public Employee getEmployee() {return emp;}
	public Data getData() {return DATA;}
	public int getCode() {return CODE;}

	public void setEmployee(Employee emp){this.emp = emp;}

	// toString
	public String toString() {
		return "Event code: " + CODE + " for " + emp + "\n" + DATA;
	}
}