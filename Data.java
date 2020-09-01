import java.util.ArrayList;

public abstract class Data implements ColInterface{
	protected static int st_code = 0;
	protected static ArrayList<Integer> taken = new ArrayList<Integer>();//needed for Employee constructor


	protected int code; 
	protected final String DESCR; 
	protected float amount;
	protected double percent;
	// they never change during runtime
	// but it's possible that in future expansions they will be required to change so they are non-final

	public Data (String descr, float amount, double percent) {
		this.DESCR = descr;
		this.amount = amount;
		this.percent = percent / 100;

		// make sure new code isn't taken, and update taken 
		while (taken.contains(st_code)) {st_code++;}
		this.code = st_code;
		taken.add(code);
	}

	// getters
	public String getDescr() {return DESCR;}
	public float getAmount() {return amount;}
	public double getPerc() {return percent;}
	public int getCode() {return code;}
	
	public void setCode(int code){
		this.code = code;
		st_code = (st_code < 1 ? 0 : st_code--);
	}

	public boolean equals(Data d) {
		if (this.getCode() == d.getCode())
			if (this.getClass().getName() == d.getClass().getName())
				if (this.getAmount() == d.getAmount()) 
					return true;
		return false;
	}

	// toString
	public String toString() {
		return "Operation code: " + code + " /// Description: " + DESCR + " /// Coefficient: " + percent*100 + "% "; 
	}
}