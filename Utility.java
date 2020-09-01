import java.util.Scanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public abstract class Utility {

	private final static int DECIMALS = 2;
	
	private static Scanner in = new Scanner(System.in);

	public static Data CreateData() {
		// inputs checked [valid values]
		// pa, ta, ea [choose from list (1-3)]
		// description [non-empty string]
		// amount [any non-zero double]
		// percent [any double 0-100]

		String regex;

		String type;        // for menu selection
		String desc;		// data description
		String amt_r;		// string version
		String percent_r;	// string version

		float amount;		// correct type
		double percent;		// correct type

		Data data;

		// -----Type-----
		System.out.print("\nChoose type of data according to the following list (1-3):\n1: PA\n2: TA\n3: EA\n> ");
		type = in.nextLine();

		regex = "^[123]$";
		
			// #input
		while (!type.matches(regex)) {
			System.out.print("Invalid type! " + type + " is not an option!"
						 + "\n\nChoose type of data according to the following list (1-3):\n1: PA\n2: TA\n3: EA\n> ");
			type = in.nextLine();
		}

		// -----Description-----
		System.out.print("\nGive Description of data (any non-empty string)\n> ");
		desc = in.nextLine();

		regex = ".+";

			// #input
		while (!desc.matches(regex)) {
			System.out.print("Invalid description! Remember, a description can't be empty!"
						 + "\n\nGive Description of data (any non-empty string)\n> ");
			desc = in.nextLine();
		}

		// -----Amount-----
		System.out.print("\nGive Amount related to this data (strictly positive, no trailing zeros)\n> ");
		amt_r = in.nextLine();

		regex = "^([1-9][0-9]*)$|^(0\\.[0-9]*[1-9])$|^([1-9][0-9]*\\.[0-9]*[1-9])$";
		
			// #input
		while (!amt_r.matches(regex)) {
			System.out.print("Invalid amount! Amount must a non-zero number!"
						 + "\n\nGive Amount related to this data (strictly positive, no trailing zeros)\n> ");
			amt_r = in.nextLine();
		}

		// -----Percentage-----
		System.out.print("\nGive Percentage related to this data \n(negative for negative amount) (any number, no trailing zeros) (e.g. give 15 for 0.15=15%, 170 for 1.7=170%\n> ");
		percent_r = in.nextLine();

		regex = "^(0)$|^(-?[1-9][0-9]*)$|^(-?0\\.[0-9]*[1-9])$|^(-?[1-9][0-9]*\\.[0-9]*[1-9])$";

			// #input
		while (!percent_r.matches(regex)) {
			System.out.print("Invalid percentage! Percentage must be any number between -100 and 100 without any trailing zeros"
						 + "\n\nGive Percentage related to this data (negative for negative amount)\n(any number 0-100, no trailing zeros)\n> ");
			percent_r = in.nextLine();
		}
		if (percent_r.matches("^-")) System.out.println("\nNegative will only have an effect on TA Data. Other types will have the positive value.");

		// -----String to float/double-----
		amount = Float.parseFloat(amt_r);
		percent = Double.parseDouble(percent_r);

		// -----Creating the Data Object-----
		if (type.equals("1")) data = new PA(desc, amount, percent);
		else if (type.equals("2")) data = new TA(desc, amount, percent);
		else if (type.equals("3")) data = new EA(desc, amount, percent);
		else data = null;	// Data object must always be created

		System.out.println("\n\nData creation succesful:\n" + data);
		return data;
	}

	public static Employee CreateEmployee() {
		// inputs checked [valid values]
		// fname [any non-empty string, only latin chars]
		// lname [any non-empty string, only latin chars]
		// wage [any strictly positive double]
		// percent_insurance [any double 0-100]

		String regex;

		String fname, lname;
		String wage_r, perc_ins_r; // String versions

		double wage, perc_ins; // double versions

		// -----LName-----
		System.out.print("\nGive the Last Name of the Employee (only latin characters)\n> ");
		lname = in.nextLine();
		
		regex = "^[a-zA-Z]+$";

			// #input
		while (!lname.matches(regex)) {
			System.out.print("Invalid Last Name! Remember, it can only contain latin characters and can't be empty!"
						 + "\n\nGive the Last Name of the Employee (only latin characters)\n> ");
			lname = in.nextLine();
		}

		// -----FName-----
		System.out.print("\nGive the First Name of the Employee (only latin characters)\n> ");
		fname = in.nextLine();
		
		regex = "^[a-zA-Z]+$";

			// #input
		while (!fname.matches(regex)) {
			System.out.print("Invalid First Name! Remember, it can only contain latin characters!"
						 + "\n\nGive the First Name of the Employee (only latin characters)\n> ");
			fname = in.nextLine();
		}

		// -----Wage-----
		System.out.print("\nGive the Wage of the Employee (any strictly positive number)\n> ");
		wage_r = in.nextLine();
		
		regex = "^([1-9][0-9]*)$|^(0\\.[0-9]*[1-9])$|^([1-9][0-9]*\\.[0-9]*[1-9])$";

			// #input
		while (!wage_r.matches(regex)) {
			System.out.print("Invalid Wage! Remember, wage is any strictly positive number!"
						 + "\n\nGive the Wage of the Employee (any strictly positive number, no trailing 0)\n> ");
			wage_r = in.nextLine();
		}

		// -----Percent Insurance-----
		System.out.print("\nGive the Insurance Percentage of the Employee (any number 0-100, no trailing 0)\n> ");
		perc_ins_r = in.nextLine();
		
		regex = "^(100)$|^(0)$|^([1-9][0-9]?)$|^(0\\.[0-9]*[1-9])$|^([1-9][0-9]?\\.[0-9]*[1-9])$";

			// #input
		while (!perc_ins_r.matches(regex)) {
			System.out.print("Invalid Insurane Percentage! Remember, insurance percentage is an integer between 0 and 100 inclusive!"
						 + "\n\nGive the Insurance Percentage of the Employee (any number 0-100, no trailing 0)\n> ");
			perc_ins_r = in.nextLine();
		}

		// -----String to float/double-----
		wage = Double.parseDouble(wage_r);
		perc_ins = Double.parseDouble(perc_ins_r);

		// -----Creating the Employee Object-----
		Employee emp = new Employee(fname, lname, wage, perc_ins);

		System.out.println("\n\nEmployee succesfully registered:\n" + emp);
		return emp;
	}

	public static int chooseData(Collection<Data> dataCol) {
		// inputs checked [valid values]
		// index [positive int]
		// index [inside list of data codes]

		String regex;

		String index_r;	// String version

		int index = -1; // int version

		// -----Index-----
		dataCol.print();
		
		regex = "^(0)$|^([1-9][0-9]*)$";

			// #input
		boolean found = false;
		while (!found) {
			System.out.print("\nChoose data by code:\n> ");
			index_r = in.nextLine();

			// make sure code is so it can be converted to int
			while (!index_r.matches(regex)) {
				System.out.print("Invalid Code! Remember, code is a positive integer!"
							+ "\n\nChoose data by code:\n> ");
				index_r = in.nextLine();
			}

			index = Integer.parseInt(index_r);

			// search Data Collection for data with this code
			if (dataCol.hasob(index)) found = true;
			else System.out.println("No data with code " + index + " exists!");
		}
		return index;
	}

	public static int chooseEmployee(Collection<Employee> empCol) {
		// inputs checked [valid values]
		// code [positive int]
		// code [inside list of employee codes]

		String regex;

		String code_r;	// String version

		int code = -1;	// int version

		// -----Code-----
		empCol.print();
		
		regex = "^(0)$|^([1-9][0-9]*)$";

			// #input
		boolean found = false;
		while (!found) {
			System.out.print("\nChoose employee by code:\n> ");
			code_r = in.nextLine();

			// make sure code is so it can be converted to int
			while (!code_r.matches(regex)) {
				System.out.print("Invalid Code! Remember, code is a positive integer!"
							+ "\n\nChoose employee by code:\n> ");
				code_r = in.nextLine();
			}

			code = Integer.parseInt(code_r);

			// search Employee Collection for employee with this code
			if (empCol.hasob(code)) found = true;
			else System.out.println("No employee with code " + code + " exists!");
		}
		return code;
	}

	public static HashMap<String, ArrayList<? extends ColInterface>> random_generation() {

		HashMap <String, ArrayList<? extends ColInterface> > hm = new HashMap <String, ArrayList<? extends ColInterface> >();

		ArrayList<Action> actls  = new ArrayList<Action>();
		ArrayList<Data> datals = new ArrayList<Data>();
		ArrayList<Employee> empls  = new ArrayList<Employee>();

		hm.put("Action", actls);
		hm.put("Data", datals);
		hm.put("Employee", empls);

		// Example Employee
		Object[] arr = Utility.example();
		
		for (int i = 0; i < 11; i++) {
			if (i == 0) empls.add((Employee) arr[i]);
			else if (i < 6) datals.add((Data) arr[i]);
			else if (i < 11) {
				actls.add((Action) arr[i]);
				((Employee) arr[0]).add_action((Action) arr[i]);
			}
		}


		// Random Employee / Data / Action Objects
		String[] fname_ls = {"Sakidis", "Santouitsidios", "Ptoma", "Pasxalinos"};
		String[] lname_ls = {"Andreas", "Anastasios", "Augos", "Anaximandros"};
		String[] TAdescr = {"*Adeia", "Ergasia", "*As8eneia"};
		String[] EAdescr = {"Yperories", "Nixterini Ergasia", "Pros8etes Ypiresies", "erxetai pio nwris to prwi"};
		String[] PAdescr = {"Bonus", "Promi8ia poliseon", "Brabeio"};

		Random r = new Random();

		// create Employees
		// S fname, S lname, d wage, d percent_insurance
		for (int i=0; i<4; i++) {
			empls.add(new Employee(fname_ls[i], lname_ls[i], r.nextInt(250)+10, r.nextInt(80)));
		}
		
		// create Data
		// S descr, f amount, d percent
		for (int i=0; i<3; i++){
			datals.add(new PA(PAdescr[i], r.nextInt(250)+10, r.nextInt(80)));
		}
		for (int i=0; i<3; i++){
			int wage = r.nextInt(50)+10;
			int perc = r.nextInt(80);
			if (TAdescr[i].contains("*")) perc = - perc;
			else wage = wage*3;;
			datals.add(new TA(TAdescr[i], wage, perc));
		}
		for (int i=0; i<4; i++){
			datals.add(new EA(EAdescr[i], r.nextInt(250)+10, r.nextInt(80)));
		}

		// create Actions
		// Employee emp, Data data
		for (Employee emp : empls) {
			if (emp.getCode() != 0) {		// don't add more actions on example employee
				for (int i=0; i<4; i++) {
					Action act = new Action(emp, datals.get(r.nextInt(10)));
					actls.add(act);
					emp.add_action(act);
				}
			}
		}
		return hm;
	}

	public static Object[] example() {
		Employee ex_emp = new Employee("Example_Fname", "ERGAZOMENOS A", 1000, 35);

		TA data1 = new TA(	"Days of Work", 	20, 	100);
		TA data2 = new TA(	"Sick Days", 		5, 		50);
		EA data3 = new EA(	"Overtime", 		5, 		170);
		EA data4 = new EA(	"Yperergasia", 		5, 		140);
		PA data5 = new PA(	"Bonus", 			500, 	20);

		Action act1 = new Action(ex_emp, data1);
		Action act2 = new Action(ex_emp, data2);
		Action act3 = new Action(ex_emp, data3);
		Action act4 = new Action(ex_emp, data4);
		Action act5 = new Action(ex_emp, data5);

		Object[] arr = {ex_emp, data1, data2, data3, data4, data5, act1, act2, act3, act4, act5};
		return arr;
	}
	
	public static double round(double number, int digits) {
		return number = Math.round(number * Math.pow(10, digits)) / Math.pow(10, digits);
	}
	
	public static int getDecimals() {return DECIMALS;}
}