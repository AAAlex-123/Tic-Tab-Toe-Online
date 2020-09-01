import java.util.ArrayList;
import java.util.HashMap;

// search #print to see where stuff gets printed
// search #rounding to see where stuff gets rounded

public class Employee implements ColInterface {
	private static int st_code;
	private final int CODE;
	private final String LNAME;
	private String FNAME;
	private double wage, percIns;
	// they never change during runtime
	// but it's possible that in future expansions they will be required to change so they are non-final

	private ArrayList<Action> actions = new ArrayList<Action> ();

	public Employee(String fname, String lname, double wage, double percent_insurance) {
		this.FNAME = fname;
		this.LNAME = lname;
		this.wage = wage;
		this.percIns = percent_insurance / 100;
		this.CODE = st_code;
		st_code++;
	}	

	public Employee(HashMap<String,String> dataset) {

		String tempfname = "", templname = "";
		int tempcode = 0;
		double tempwage = 0, temppercIns = 0;

		try {
			tempwage = Double.parseDouble(dataset.get("salary"));
			temppercIns = Double.parseDouble(dataset.get("fund_coef")) / 100;
			templname = dataset.get("surname");
			tempfname = dataset.get("firstname");
			tempcode = st_code;

		} catch (NumberFormatException | NullPointerException exc) {
			tempfname = dataset.get("INVALID");
			templname = "";
			tempwage = 0;
			temppercIns = 0;
			tempcode = -1;
			st_code--;
		} // should NOT happen, leaving it as a failsafe

		this.wage = tempwage;
		this.percIns = temppercIns;
		this.LNAME = templname;
		this.FNAME = tempfname;
		this.CODE = tempcode;
		st_code++;
	}

	// General method that calls other sub-methods
	// parameter print to control whether or not partial results will be printed
	// true for question 6, false for question 7
	public double calcPay(boolean print) {
			// #print
		if (print) System.out.println("\nCalculating for employee: " + FNAME + " " + LNAME + "\n");
		double pa, ta, ea, total, ins_pa,  ins_ta, ins_ea, total_ins, pi_ta, pi_ea, tax, to_pay;
		double[] earnings_arr = new double[3];
		double[] insurance_arr = new double[3];
		
		// 1) earnings
		earnings_arr = calcEarnings(print);
		pa = earnings_arr[0];
		ta = earnings_arr[1];
		ea = earnings_arr[2];
		total = pa + ta + ea;

			// #rounding
		total = Utility.round(total, Utility.getDecimals());
			// #print
		if (print) System.out.println("Grand Total: " + total + "\n\n");

		// 2) insurance
		insurance_arr = calcIns(pa, ta, ea, print);
		ins_pa = insurance_arr[0];
		ins_ta = insurance_arr[1];
		ins_ea = insurance_arr[2];
		total_ins = ins_pa + ins_ta + ins_ea;

		// 3) tax
		pi_ta = ta - ins_ta;			//pi = post insurance, earnings - insurance
		pi_ea = ea - ins_ea;
		tax = calcTax(pi_ta , pi_ea, print);

		// 4) result
			// #rounding
		to_pay = total - (total_ins + tax);
		return Utility.round(to_pay, Utility.getDecimals());
	}

	// calculate all earnings {PA, TA, EA}
	public double[] calcEarnings(boolean print) {
		double pa = 0;
		double ta = 0;
		double ea = 0;
		double amt;
		// for each action, add to running total and print information
		// different computation according to its type
		for (Action act : actions) {
			switch(act.getData().getClass().getName()) {
				case "PA":
					amt = act.getData().getAmount();
					pa += amt;
						// #print
					if (print) System.out.println(act + "\nValue: " + amt + "\n\n");
					break;
			
				case "TA":
					amt = (wage/25) * act.getData().getAmount() * act.getData().getPerc();
					ta += amt;

						// #print
					if (print) System.out.println(act + " /// wage/day: " + wage/25 + "\nValue: " + amt + "\n\n");
					break;
			
				case "EA": 
					amt = (wage/(25*8)) * act.getData().getAmount() * act.getData().getPerc();
					ea += amt;
						// #print
					if (print) System.out.println(act + " /// wage/hour: " + wage/(25*8) + "\nValue: " + amt + "\n\n");
					break;
			} // switch
		} // for

			// #rounding
		pa = Utility.round(pa, Utility.getDecimals());
		ta = Utility.round(ta, Utility.getDecimals());
		ea = Utility.round(ea, Utility.getDecimals());

			// #print
		if (print) System.out.println("Total TA: " + ta + " /// Total EA: " + ea +" /// Total PA: " + pa + "\n\n");
		
		double[] arr = {pa, ta, ea};
		return arr;
	}

	// calculate all insurances {ins_pa, ins_ta, ins_ea}
	// return as array because they are needed separately to calculate taxes
	public double[] calcIns(double pa, double ta, double ea, boolean print) {
		double ins_pa = (pa > 0 ? pa*percIns : 0);
		double ins_ta = (ta > 0 ? ta*percIns : 0);
		double ins_ea = (ea > 0 ? ea*percIns : 0);

		double total = ins_pa + ins_ta + ins_ea;

			// #rounding
		ins_pa = Utility.round(ins_pa, Utility.getDecimals());
		ins_ta = Utility.round(ins_ta, Utility.getDecimals());
		ins_ea = Utility.round(ins_ea, Utility.getDecimals());
		total = Utility.round(total, Utility.getDecimals());

			// #print 
		if (print) System.out.println("TA: " + ta + " /// EA: " + ea + " /// PA: " + pa + " /// Percent Insurance: "+ percIns
									+ "\nTA_ins: " + ins_ta + " /// EA_ins: " + ins_ea + " /// PA_ins: " + ins_pa
									+ "\nTotal Insurance: " + total + "\n\n");

		double[] arr = {ins_pa, ins_ta, ins_ea};
		return arr;
	}

	// calculate total tax
	// return total
	public double calcTax(double ta, double ea, boolean print) {
		return calcPa(print) + calcTaEa(ta, ea, print);
	}	

	// calculate tax from PA
	// return total
	public double calcPa(boolean print) {
			// #print
		if (!hasData("PA")) {
			if (print) System.out.println("\nNo PAs found for this Employee;\nTax for PAs = 0");
			return 0;
		} else {
			if (print) System.out.println("\n\nCalculating tax for PAs:\n");
			double pa, perc, taxed_pa, tax_pa;
			double total_pa_tax = 0;

			for (Action act : actions) {
				// we go through all Actions because the percentage of PA is a property of this object
				// we can't access it through the employee
				if (act.getData().getClass().getName().equals("PA")) {
					pa = act.getData().getAmount();
					perc = (act.getData().getPerc() > 0 ? act.getData().getPerc() : 0);

					taxed_pa = pa - (pa*percIns);
					tax_pa = taxed_pa * perc;

					total_pa_tax += tax_pa;

						// #rounding
					taxed_pa = Utility.round(taxed_pa, Utility.getDecimals());
					tax_pa = Utility.round(tax_pa, Utility.getDecimals());
					total_pa_tax = Utility.round(total_pa_tax, Utility.getDecimals());

						// #print
					if (print) System.out.println("PA: " + act
									+ "\nTaxed PA: " + taxed_pa
									+ "\nTax PA: " + tax_pa + "\n\n");
				}
			}
			return total_pa_tax;
		}
	}

	// calculate tax from TA, EA
	// return total
	public double calcTaEa(double ta, double ea, boolean print) {
			// #print
		if (!hasData("TA") && !hasData("EA")) {
			if (print) System.out.println("\nNo TAs or EAs found for this Employee;\nTax for TAs EAs = 0");
			return 0;
		} else {
			if (print) System.out.println("\n\nCalculating tax for TAs and EAs:\n");
			double perc;

			double to_be_taxed_yearly = ((ta + ea) > 0 ? ta + ea : 0) * 14;

			// find out percentage
			if (to_be_taxed_yearly < 10000) perc = 0.1;
			else if (to_be_taxed_yearly < 30000) perc = 0.2;
			else perc = 0.3;
			
			// yearly and monthly
			double tax_yearly = (to_be_taxed_yearly > 0 ? to_be_taxed_yearly * perc : 0);
			double tax_monthly = tax_yearly / 14;

				// #rounding
			ta = Utility.round(ta, Utility.getDecimals());
			ea = Utility.round(ea, Utility.getDecimals());
			to_be_taxed_yearly = Utility.round(to_be_taxed_yearly, Utility.getDecimals());
			tax_yearly = Utility.round(tax_yearly, Utility.getDecimals());
			tax_monthly = Utility.round(tax_monthly, Utility.getDecimals());

				// #print
			if (print) System.out.println("Taxed TA, EA: " + (ta + ea) + ((ta + ea) < 0 ? ", Negative so no tax" : "")
							+ "\nYearly taxed TA, EA: "+ to_be_taxed_yearly + " => Percentage: " + perc
							+ "\nYearly tax TA, EA: "+ tax_yearly
							+ "\nMontly tax TA, EA: " + tax_monthly + "\n\n");
			return tax_monthly;
		}
	}

	// methods related to Action[] array
	public void add_action(Action act) {
		actions.add(act);
	}

	public void print_act() {
		if (actions.isEmpty()) System.out.println("No actions for this employee :(");
		else {
			System.out.println("\n\nShowing Actions for Employee: " + LNAME + "\n");
			for (Action act : actions) System.out.println("\n" + act);
		}
	}

	// getters
	public String getFname() {return FNAME;}
	public String getLname() {return LNAME;}
	public double getWage() {return wage;}
	public double getPercIns() {return percIns;}
	public int getCode() {return CODE;}
	public ArrayList<Action> getActions() {return actions;}
	
	public void setActions(ArrayList<Action> actions) {this.actions = actions;}
	
	// toString
	public String toString() {
		return "Employee No "+CODE+": "+FNAME+" "+LNAME+", "+wage+"$, "+(percIns*100)+"%";
	}
	
	// check if employee has any Action associated with Data of type Type
	private boolean hasData(String type) {
		for (Action act : actions)
			if (act.getData().getClass().getName().equals(type)) return true;
		return false;
	}
}