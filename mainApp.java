import java.util.Scanner;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.*;

public class mainApp {
	// ----------------------------------------
	/* Omada ----------
	AM: ----------		Full Name: ----------
	AM: ----------		Full Name: ----------

	Notes:
		- Search "#input" to see where user input is checked.
		- For the example employee, their code can't be set to "1111".
				All employees get integer code starting from 0, incrementing with each new employee.
		- All instructions are in English in order for this to operate in any encoding/any compiler.
		- Insurance for negative earnings (negative TA) is 0.
		- Tax for negative earnings (negative TA) is also 0.
		- Total to-pay amount can be negative, if all the earnings sum up to a negative number.
		- Amounts are rounded to the second decimal place (variable Utility.getDecimals() below).
		- Utility.java contains methods used in this file (mainApp.java)

	More notes (IO):
		- Default files for input are data_out.txt and employee_action.txt,
				the user will be prompted to give the name of a file they created.
		- Default files for input are same as above, but they are hardcoded,
				meaning these files will always be created / overwritten when saving data.
		- If no file is found with the above given file name, an error will be raised and the program will stop.
		- Syntactic analysis checks for:
				-- proper { and } placement
				-- tags (reserved words like firstname, salary, value) at the right block everything else is accepted as correct syntax.
				(e.g. duplicate tags, extra whitespaces, random non-tag words)
				view Ex_dat.txt, Ex_emp_act.txt for what is considered valid and invalid syntax
		- Data Parsing:
				-- start parsing
				-- when "element", call parseData
					-- create hashmap with tag-value pairs
					-- check hashmap for missing values and show error messages accordingly
					-- determine if element is valid or not and return it accordingly
		- Employee-Action Parsing:
				-- start parsing
				-- when "employee", call parseEmployee
					-- when "trns"
						-- when "trn"
							-- create Action with data collected by parsing trn (similar to Element parsing above)
							-- check if matches against other Data
							-- if so, add it to Employee and Action Collection
					-- create hashmap with tag-value pairs
					-- check hashmap for missing values and show error messages accordingly
					-- determine if employee is valid or not and add it to Employee collection accordingly
		- Creating Files:
				-- go through Collections and simply build the file with data found there
		- Utility_IO.java contains methods used for IO
		
	*/
	// ---------------------------------------
		
		private static Scanner in = new Scanner(System.in);

		// Utility.getDecimals(): how many digits after the decimal point the any number will be rounded to
		// ex: Utility.getDecimals() = 3  =>  round(123.4567, Utility.getDecimals()) = 123.456
		private static Collection<Action> actCol  = new Collection<Action>();
		private static Collection<Data> dataCol = new Collection<Data>();
		private static Collection<Employee> empCol  = new Collection<Employee>();

		public static ArrayList<Data> getDatals() {return mainApp.datals;}
		public static Collection<Action> getActCol() {return mainApp.actCol;}
		public static Collection<Employee> getEmpCol() {return mainApp.empCol;}

		private static ArrayList<Data> datals;
		private static ArrayList<Action> actls;
		private static ArrayList<Employee> empls;

		public static void main(String[] args) throws IOException  {

			// -------------------- Initialising Data -------------------- //
		System.out.print("\n\nWelcome. Please choose the way you would like to initialise your data"
							+ "\n1. Read from files"
							+ "\n2. Randomly generate 5 Employees"
							+ "\n0. Don't create anything"
							+ "\n> ");
		String choice = in.nextLine();

		while (!choice.matches("^[012]$")) {
			System.out.println("Invalid input! Please choose 1 or 2 according to the following options"
								+ "\n\nPlease choose the way you would like to initialise your data"
								+ "\n1. Read from files"
								+ "\n2. Randomly generate 5 Employees"
								+ "\n0. Don't create anything"
								+ "\n> ");
			choice = in.nextLine();
		}

		// read files
		if (choice.equals("1")) {
			// get file names
			// check for correct syntax
			// parse file, read and create data
			String data_file, emp_act_file;

			System.out.print("\nPlease type the name of the Data file:\n> ");
			data_file = in.nextLine();
			Utility_IO.checkDataSyntax(data_file);
			datals = Utility_IO.createDataCol(Utility_IO.parseElementList(data_file));
			
			System.out.print("\nPlease type the name of the Employee-Action file:\n> ");
			emp_act_file = in.nextLine();
			Utility_IO.checkEmpActSyntax(emp_act_file);
			Utility_IO.parseEmpAct(emp_act_file);

			actls = new ArrayList<Action>();
			empls = new ArrayList<Employee>();

			System.out.println("Successfully initialised data from file");
		}

		// randomly generated
		else if (choice.equals("2")) {
			// call this method that returns a map, then un-pack it
			HashMap<String, ArrayList<? extends ColInterface>> hm = Utility.random_generation();

			datals = (ArrayList<Data>) hm.get("Data");
			actls = (ArrayList<Action>) hm.get("Action");
			empls = (ArrayList<Employee>) hm.get("Employee");
			System.out.println("Successfully randomly initialised data");

		} else if (choice.equals("0")) {
			// do nothing
			System.out.println("Nothing created successfully");
			actls = new ArrayList<Action>();
			datals = new ArrayList<Data>();
			empls = new ArrayList<Employee>();
			
		}

		for (Action act : actls) actCol.add_object(act);
		for (Data data : datals) dataCol.add_object(data);
		for (Employee emp : empls) empCol.add_object(emp);

		// -------------------- Done Initialising Data -------------------- //

		// used before switch
		String menu = "\n\n\n1. Add new payroll."
						+ "\n2. Register new employee"
						+ "\n3. Register new employee event."
						+ "\n4. Show all payroll data."
						+ "\n5. Show all events for an employee."
						+ "\n6. Calculate employee salary."
						+ "\n7. Calculate total expenses."
						+ "\n8. *NEW* Update Files."
						+ "\n0. End session."
						+ "\n> ";

		String user_input = "";
		boolean stop = false;

		// used in cases 3, 5, 6
		Employee target_emp;
		Data target_data;
		Action new_act;

		// used in case 7
		double total_insurance, total_taxes, tax;
		double[] earns, ins_arr;

		while (!stop) {
			System.out.print(menu);
			user_input = in.nextLine();

			switch (user_input) {
				case "debuglol":
					break;
				case "0": // end session
					stop = true;

					System.out.print("\nWould you like to update files with current Data? (y/n)\n> ");
					choice = in.nextLine().toLowerCase();

					while (!choice.matches("^(y|n)$")) {
						System.out.print("Invalid input! Please answer with y (yes) or n (no)"
											+ "\n\nWould you like to update files with current Data? (y/n)\n> ");
						choice = in.nextLine().toLowerCase();
					}

					if (choice.equals("y")) {
						Utility_IO.updateFiles(empCol, actCol, dataCol);
						System.out.println("Files updated");
					}

					System.out.println("\nSession ended");
					break;
				case "1": // new data
					dataCol.add_object(Utility.CreateData());
					break;
				case "2": // new employee
					empCol.add_object(Utility.CreateEmployee());
					break;
				case "3": // new action
					target_emp = empCol.getob(Utility.chooseEmployee(empCol));
					target_data = dataCol.getob(Utility.chooseData(dataCol));

					new_act = new Action(target_emp, target_data);

					target_emp.add_action(new_act);
					actCol.add_object(new_act);

					System.out.println("\n\nAction succefully registered:\n" + new_act);
					break;
				case "4": //show data
					if (dataCol.getls().isEmpty()) {System.out.println("\n\nERROR: No Data found!");}
					else {
						System.out.println("\n\nShowing all Payroll Data:\n");
						dataCol.print();
					}
					break;
				case "5": //show employee actions
					if (empCol.getls().isEmpty()) System.out.println("\n\nERROR: No Employees found!");
					else {
						target_emp = empCol.getob(Utility.chooseEmployee(empCol));
						target_emp.print_act();
					}
					break;
				case "6": //show employee pay				
					if (empCol.getls().isEmpty()) System.out.println("\n\nERROR: No Employees found!");
					else {				
						target_emp = empCol.getob(Utility.chooseEmployee(empCol));
						System.out.println("\n\nTotal to pay: " + target_emp.calcPay(true));
					}													// true => print all results
					break;
				case "7": //show insurance / tax of all employees and show totals
					total_insurance = 0;
					total_taxes = 0;

					for (Employee e : empCol.getls()) {
						earns = e.calcEarnings(false);
										// false => print only final results

						ins_arr = e.calcIns(earns[0], earns[1], earns[2], false);
						tax = e.calcTax(earns[1], earns[2], false);

						System.out.println("\n" + e
										+ "\nInsurance: " + Utility.round(ins_arr[0] + ins_arr[1] + ins_arr[2], Utility.getDecimals())
										+ "\nTax: " + Utility.round(tax, Utility.getDecimals()) + "\n");

						total_insurance += (ins_arr[0] + ins_arr[1] + ins_arr[2]);
						total_taxes += tax;
					}
					System.out.println("\nTotal Insurance: " + Utility.round(total_insurance, Utility.getDecimals())
									+ "\nTotal Taxes: " + Utility.round(total_taxes, Utility.getDecimals()));
					break;
				case "8":
					Utility_IO.updateFiles(empCol, actCol, dataCol);
					System.out.println("Files succesfully updated");
					break;
				default:
					System.out.println("\nInvalid input :(");
					break;
			} //switch
		} // while
		in.close(); //avoid resource leak		
	} // main		
} // class

