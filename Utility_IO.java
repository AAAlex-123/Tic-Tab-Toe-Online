import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.SecurityException;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.Scanner;
import java.util.Arrays;

public abstract class Utility_IO {


	//------------Create Data Objects------------//
	public static ArrayList<Data> createDataCol(ArrayList<HashMap <String,String>> datalist) {
		ArrayList<Data> datals = new ArrayList<Data>();
		for (HashMap<String, String> map : datalist) {
			Data d = createData1(map);
			if (d != null) datals.add(d);
		}
		System.out.println("\n\nSUCCESSFULLY READ FILE AND CREATED DATA ELEMENTS\n\n\n");
		return datals;
	}

	public static Data createData1(HashMap<String, String> dataset) {
		Data d = null;

		String descr = dataset.get("descr");
		float amt = Float.parseFloat(dataset.get("amt"));
		double perc = Double.parseDouble(dataset.get("perc"));

		boolean ok = true;
		try {
			switch (dataset.get("element_type").toUpperCase()) {
				case "TA":
					d = new TA(descr, amt, perc);
					break;
				case "EA":
					d = new EA(descr, amt, perc);
					break;
				case "PA":
					d = new PA(descr, amt, perc);
					break;
				default:
					System.out.println("Invalid element type for element. Skipping...");
					ok = false;
					break;
			}
		} catch (NumberFormatException exc) {
			System.out.println("Element contains illegal data. Skipping...");
			ok = false;
		}
		if (ok) {
			d.setCode(Integer.parseInt(dataset.get("code")));
			return d;
		} else return null;	
	}

	//------------Done Create Data Objects------------//



	private static Scanner input;
	private static int line_number = 0;
	private static final String DEFAULT_VALUE = "0";

	// try to open file, set input a Scanner of this file
	private static void openFile(String fileName) {
		try {
			input = new Scanner(Paths.get(fileName)); 
			System.out.println("\n\nFile >> " + fileName + " << successfully opened\n");
		}
		catch (SecurityException secExc) {
			System.out.println("No clearance to read this file. Terminating...");
			System.exit(1);
		}
		catch (FileNotFoundException fileNF) {
			System.out.println("No such file found. Terminating...");
			System.exit(1);
		}
		catch (IOException | InvalidPathException ioExc) {
			System.out.println("Error opening file. Terminating...");
			System.exit(1);
		}

	}

	//---------------SYNTACTIC CHECK---------------//
	// check if Data file has correct syntax
	public static boolean checkDataSyntax(String fileName) {
		openFile(fileName);
		System.out.println("\nSyntactically analysing file: " + fileName);
		
		int counter = 0;
		int line_number = 0;
		String s;
		String[] target;
		String[] forbidden;

		String[][] targets = {
			{"element_list"}, 
			{"{"},
			{"element", "}"},
			{"{"},
			{"}"},
			{""},
		};
		// Data Tags: "element_list", "element", "element_type", "code", "descr", "amt", "perc", "{", "}"
		String[][] forbiddens = {
			{					"element", 	"element_type", "code", "descr", "amt", "perc", 	"{", 	"}"	},
			{"element_list",	"element", 	"element_type", "code", "descr", "amt", "perc", 			"}"	},
			{"element_list", 				"element_type", "code", "descr", "amt", "perc", 	"{"			},
			{"element_list", 	"element", 	"element_type", "code", "descr", "amt", "perc", 			"}"	},
			{"element_list", 	"element", 													 	"{"  		},
			{"element_list", 	"element", 	"element_type", "code", "descr", "amt", "perc", 	"{", 	"}"	},
		};

		while (input.hasNext()) {

			// update target and forbidden
			target = targets[counter];
			forbidden = forbiddens[counter];

			// get new line and new line_number
			s = input.nextLine().strip().toLowerCase().split(" ")[0];
			line_number++;

			if (!Arrays.asList(target).contains(s)) {
				if (Arrays.asList(forbidden).contains(s)) {
					System.out.println("\nCRASH: INVALID STRING >> " + s + " <<\nAT LINE: " + line_number);
					closeFile();
					return false;
				}

			// if found change state according to these rules
			} else {
				if (counter == 2 && s.equals("}")) counter = 5;
				else if (counter == 4) counter = 2;
				else counter++;
			}

			// if reached EOF in wrong state (not all { are closed)
			if (!input.hasNext() && counter != 5) {
				System.out.println("\nCRASH: UNEXPECTED EOF\nAT LINE: " + line_number);
				closeFile();
				return false;
			// if reached EOF in correct state (all { are closed)
			} else if (!input.hasNext() && counter == 5) {
				System.out.println("\nEOF REACHED; SYNTACTIC ANALYSIS SUCCESSFUL");
				closeFile();
				return true;
			}
		}
		closeFile();
		System.out.println("\nIf you see this message something has gone horribly wrong");
		return true;	// never reached in practice
	}

	// check if Employee-Action file has correct syntax
	public static boolean checkEmpActSyntax(String fileName) {
		openFile(fileName);
		System.out.println("\nSyntactically analysing file: " + fileName);
		
		int counter = 0;
		int line_number = 0;
		String s;
		String[] target;
		String[] forbidden;

		String[][] targets = {
			{"employee_list"},
			{"{"},
			{"employee", "}"},
			{"{"},
			{"trns", "}"},
			{"{"},
			{"trn", "}"},
			{"{"},
			{"}"},
			{"}"},
			{""},
		};
		// Emp-Act Tags: "employee_list", "employee", "code", "surname", "firstname", "salary", "fund_coef", "trns", "trn", "element_type", "code", "value", "{", "}"
		// code is duplicate, so when one is missing, the other is missing too
		String[][] forbiddens = {
			{					"employee",	"code",	"surname",	"firstname",	"salary",	"fund_coef",	"trns",	"trn",	"element_type",	"code",	"value",	"{",	"}"	},
			{"employee_list",	"employee",	"code",	"surname",	"firstname",	"salary",	"fund_coef",	"trns",	"trn",	"element_type",	"code",	"value",			"}"	},
			{"employee_list",				"code",	"surname",	"firstname",	"salary",	"fund_coef",	"trns",	"trn",	"element_type",	"code",	"value",	"{",		},
			{"employee_list",	"employee",	"code",	"surname",	"firstname",	"salary",	"fund_coef",	"trns",	"trn",	"element_type",	"code",	"value",			"}"	},
			{"employee_list",	"employee",																			"trn",	"element_type",			"value",	"{",		},
			{"employee_list",	"employee",	"code",	"surname",	"firstname",	"salary",	"fund_coef",	"trns",	"trn",	"element_type",	"code",	"value",			"}"	},
			{"employee_list",	"employee",	"code",	"surname",	"firstname",	"salary",	"fund_coef",	"trns",			"element_type",	"code",	"value",	"{",		},
			{"employee_list",	"employee",	"code",	"surname",	"firstname",	"salary",	"fund_coef",	"trns",	"trn",	"element_type",	"code",	"value",			"}"	},
			{"employee_list",	"employee",			"surname",	"firstname",	"salary",	"fund_coef",	"trns",	"trn",										"{",		},
			{"employee_list",	"employee",																	"trns",	"trn",	"element_type",			"value",	"{",		},
			{"employee_list",	"employee",	"code",	"surname",	"firstname",	"salary",	"fund_coef",	"trns",	"trn",	"element_type",	"code",	"value",	"{",	"}"	},
		};

		while (input.hasNext()) {

			// update target and forbidden
			target = targets[counter];
			forbidden = forbiddens[counter];

			// get new line and new line_number
			s = input.nextLine().strip().toLowerCase().split(" ")[0];
			line_number++;

			// if not found
			if (!Arrays.asList(target).contains(s)) {
				if (Arrays.asList(forbidden).contains(s)) {
					System.out.println("\nERROR: INVALID STRING >> " + s + " <<\nAT LINE: " + line_number);
					closeFile();
					return false;
				}

			// if found change state according to these rules
			} else {
				if (counter == 2 && s.equals("emp")) counter = 3;
				else if (counter == 2 && s.equals("}")) counter = 10;
				else if (counter == 4 && s.equals("}")) counter = 2;
				else if (counter == 6 && s.equals("trn")) counter = 7;
				else if (counter == 6 && s.equals("}")) counter = 9;
				else if (counter == 8) counter = 6;
				else if (counter == 9) counter = 2;
				else counter++;
			}

			// if reached EOF in wrong state (not all { are closed)
			if (!input.hasNext() && counter != 10) {
				System.out.println("\nERROR: UNEXPECTED EOF\nAT LINE: " + line_number);
				closeFile();
				return false;
			// if reached EOF in correct state (all { are closed)
			} else if (!input.hasNext() && counter == 10) {
				System.out.println("\nEOF REACHED; SYNTACTIC ANALYSIS SUCCESSFUL");
				closeFile();
				return true;
			}
		}
		closeFile();
		System.out.println("\nIf you see this message something has gone horribly wrong");
		return true;	// never reached in practice
	}
	//---------------DONE SYNTACTIC CHECK---------------//


	//------------------------------FILE PARSING------------------------------//

	//---------------DATA FILE PARSING---------------//
	public static ArrayList<HashMap<String, String>> parseElementList(String file_name) {
		String[] allowed = {"element_type", "code", "descr", "amt", "perc"};
		String line;

		HashMap<String, String> temp = null;

		ArrayList<HashMap<String, String>> element_list = new ArrayList<HashMap<String, String>> ();
		
		if (file_name.equals("")) openFile("data_out.txt");
		else openFile(file_name);
		System.out.println("\nReading file >> " + file_name + " <<");

		while (input.hasNext()) {
			line = input.nextLine().strip().toLowerCase();
			line_number++;

			// repeat until next "element"
			if (!line.equals("element")) continue;

			// only add if not null
			temp = parseElement(allowed);
			if (!(temp == null)) element_list.add(temp);
		}
		closeFile();
		return element_list;
	}

	private static HashMap <String,String> parseElement(String[] allowed) {
		System.out.println("\n\nNOW PARSING ELEMENT, AT LINE: " + line_number);
		boolean valid = true;
		String parts[];

		HashMap <String, String> tag_map = new HashMap<String, String>();
		for (String s : allowed) tag_map.put(s, "#");
		String tag, value;

		String line = input.nextLine().strip().toLowerCase();
		line_number++;

		// add all tags
		while (!line.equals("}")) {
			
			parts = line.split(" ");
			tag = parts[0];
			value = (String.join(" ", Arrays.copyOfRange(parts, 1, parts.length))).replaceAll("\"", "");

			// if empty (empty meaning invalid) put # for easy check and print clarity
			if (Arrays.asList(allowed).contains(tag) && !value.equals("")) tag_map.put(tag, value);

			line = input.nextLine().strip().toLowerCase();
			line_number++;
		}

		// insert default values in element
		for (String key : tag_map.keySet()) {
			if (tag_map.get(key).equals("#")) {
				switch (key) {
					case "code":
					case "descr":
					case "element_type":
						System.out.println("\nERROR: No >> " + key.toUpperCase() + " << field found; Invalid element");
						valid = false;
						break;
					case "amt":
					case "perc":
						System.out.println("\nWARNING: No >> " + key.toUpperCase() + " << field found; Element OK");
						tag_map.put(key, DEFAULT_VALUE);
						break;
				}
			}
		}
		if (valid) {
			System.out.println("\nNo Errors found; Element accepted\n");
			return tag_map;
		} else {
			System.out.println("\nElement dismissed due to above errors\n");
			return null;
		}
	}
	//---------------DONE DATA FILE PARSING---------------//

	//---------------EMPLOYEE FILE PARSING---------------//
	public static void parseEmpAct(String fileName) {

		if (fileName.equals("")) openFile("employee_action_out.txt");
		else openFile(fileName);
		System.out.println("\nReading file >> " + fileName + " <<");

		line_number = 0;
		String line = input.nextLine().strip().toLowerCase();
		line_number++;

		while(!line.equals("employee_list")) {
			line = input.nextLine().strip().toLowerCase();
			line_number++;
			continue;
		}

		while(!line.equals("}") && input.hasNext()) {
			if (line.equals("employee")) parseEmployee();

			line = input.nextLine().strip().toLowerCase();
			line_number++;
		}
		System.out.println("\n\nSUCCESSFULLY READ FILE AND CREATED EMPLOYEE AND ACTION ELEMENTS\n\n\n");
		closeFile();
	}

	private static void parseEmployee() {
		System.out.println("\n\n\nNOW PARSING EPMLOYEE, AT LINE: " + line_number);

		String [] empAllowed = {"code", "firstname", "surname", "salary", "fund_coef"};
		String tag, value, line = "";
		String[] parts;
		boolean valid = true;

		HashMap<String, String> empdetails = new HashMap<String, String>();
		for (String s : empAllowed) empdetails.put(s, "#");
		
		ArrayList<Action> trnLs = new ArrayList<Action>();

		while(!line.equals("}") && input.hasNext()) {

			line = input.nextLine().strip().toLowerCase();
			line_number++;

			if (line.equals("trns")) {

				while (!line.equals("}")) {
					if (line.equals("trn")){
						Action new_act = createAction();
						if (new_act!=null) trnLs.add(new_act);
					} 
					line = input.nextLine().strip().toLowerCase();
					line_number++;
				}

				line = input.nextLine().strip().toLowerCase();
				line_number++;
				continue;
			}

			parts = line.split(" ");
			tag = parts[0];
			value = (String.join(" ", Arrays.copyOfRange(parts, 1, parts.length))).replaceAll("\"", "");

			// if empty (empty meaning invalid) put # for easy check and print
			if (Arrays.asList(empAllowed).contains(tag) && !value.equals("")) empdetails.put(tag, value);
		} // while


		// insert default values in element
		for (String key : empdetails.keySet()) {
			if (empdetails.get(key).equals("#")) {
				switch (key) {
					case "code":
					case "surname":
					case "firstname":
						System.out.println("\nERROR: No >> " + key.toUpperCase() + " << field found; Invalid Employee");
						valid = false;
						break;
					case "salary":
					case "fund_coef":
						System.out.println("\nWARNING: No >> " + key.toUpperCase() + " << field found; Employee OK");
						empdetails.put(key, DEFAULT_VALUE);
						break;
				}
			}
		}
		try {
			Double.parseDouble(empdetails.get("salary"));
			Double.parseDouble(empdetails.get("fund_coef"));
		} catch (NumberFormatException exc) {
			System.out.println("\nERROR: >> SALARY or INSURANCE << fields contain invalid data; Invalid Employee");
			valid = false;
		}

		if (valid) {
			System.out.println("\nNo Errors found; Employee accepted\n");
			Employee emp = new Employee(empdetails);
			for (Action trn : trnLs)
				if (trn != null) {
					trn.setEmployee(emp);
					mainApp.getActCol().add_object(trn);
				}
			mainApp.getEmpCol().add_object(emp);
			emp.setActions(trnLs);
		} else {
			System.out.println("Employee dismissed due to above Errors");
		}
	} // parseEmployee

	private static HashMap <String, String> parseTrn(String[] allowed) {
		System.out.println("\n\n\nNOW PARSING TRN, AT LINE: " + line_number);
		boolean valid = true;
		String[] parts;

		HashMap <String, String> tag_map = new HashMap<String, String>();
		String tag, value;
		String line = input.nextLine().strip().toLowerCase();
		line_number++;

		// add all tags
		while (!line.equals("}")) {
			parts = line.split(" ");
			tag = parts[0];
			value = (String.join(" ", Arrays.copyOfRange(parts, 1, parts.length))).replaceAll("\"", "");

			// if empty (empty meaning invalid) put # for easy check and print clarity
			if (Arrays.asList(allowed).contains(tag) && !value.equals("")) tag_map.put(tag, value);
			if (Arrays.asList(allowed).contains(tag) && value.equals("")) tag_map.put(tag, "#");

			line = input.nextLine().strip().toLowerCase();
			line_number++;
			
		}
		for (String key : tag_map.keySet()) {
			if (tag_map.get(key).equals("#")) {
				System.out.println("\nERROR: No >> " + key.toUpperCase() + " << field found; Invalid TRN");
				valid = false;
			}
			if (key.equals("element_type") && !(tag_map.get(key).toUpperCase()).matches("^(PA|TA|EA)$")) {
				System.out.println("\nERROR: >> " + key.toUpperCase() + " << field found contains invalid data; Invalid TRN");
				valid = false;
			}
		}
				
		try {
			Double.parseDouble(tag_map.get("value"));
		} catch (NumberFormatException exc) {
			System.out.println("\nERROR: >> VALUE << field contains invalid data; Invalid TRN");
			valid = false;
		}
		
		if (valid) {
			System.out.println("\nNo Errors found; TRN accepted\n");
			return tag_map;
		} else {
			System.out.println("\nTRN dismissed due to above errors\n");
			return null;
		}
	}
	
	private static Action createAction() {
		String [] allowed = {"element_type", "code", "value"};
		Action new_act = new Action(null, findData(createData(parseTrn(allowed))));
		if (new_act.getData() != null) {
			return new_act;
		} else return null;
	}

	private static Data findData(Data d1) {
		if (d1 == null) return null;
		for (Data d : mainApp.getDatals())
			if (d1.equals(d)) {
				System.out.println("Successfully found matching Data to TRN. TRN accepted");
				return d;
			}
		System.out.println("ERROR: Can't find matching Data to TRN. Invalid TRN");
		return null;
	}

	public static Data createData(HashMap<String, String> dataset) {
		Data d = null;

		boolean ok = true;
		try {
			String descr = "";
			float amt = Float.parseFloat(dataset.get("value"));
			double perc = 0;
			switch (dataset.get("element_type").toUpperCase()) {
				case "TA":
					d = new TA(descr, amt, perc);
					break;
				case "EA":
					d = new EA(descr, amt, perc);
					break;
				case "PA":
					d = new PA(descr, amt, perc);
					break;
				default:
					System.out.println("Invalid element type for element. Skipping...");
					ok = false;
					break;
			}
		} catch (NumberFormatException|NullPointerException exc) {
			System.out.println("Element contains illegal data. Skipping...");
			ok = false;
		}
		if (ok) {
			d.setCode(Integer.parseInt(dataset.get("code")));
			return d;
		} else {
			return null;
		}
	}
		//---------------DONE EMPLOYEE FILE PARSING---------------//

		


	//---------------Create Files---------------//
	public static void updateFiles(Collection<Employee> empCol, Collection<Action> actCol, Collection<Data> dataCol) throws IOException {
		Utility_IO.updateDataFile(dataCol);
		Utility_IO.updateEmployeeActionFile(empCol, actCol);
	}

	public static void updateDataFile(Collection<Data> dataCol) throws IOException {
		BufferedWriter data = new BufferedWriter(new FileWriter("data_out.txt"));

		String s = "";

		s += "ELEMENT_LIST\n{\n";

		for (Data d : dataCol.getls()) {
			s += "\tELEMENT\n\t{\n"
				+ "\t\tELEMENT_TYPE " + d.getClass().getName() + "\n"
				+ "\t\tCODE " + d.getCode() + "\n"
				+ "\t\tDESCR \"" + d.getDescr() + "\"\n"
				+ "\t\tAMT " + d.getAmount() + "\n"
				+ "\t\tPERC " + d.getPerc()*100 + "\n"
				+ "\t}\n";
		}
		s += "}";

		// final
		data.write(s);
		data.close();
	}

	public static void updateEmployeeActionFile(Collection<Employee> empCol, Collection<Action> actCol) throws IOException {
		BufferedWriter emp_act = new BufferedWriter(new FileWriter("employee_action_out.txt"));

		String s = "";

		s += "EMPLOYEE_LIST\n{\n";

		// Employee data
		for (Employee e : empCol.getls()) {
			s += "\tEMPLOYEE\n\t{\n"
				+ "\t\tCODE " + e.getCode() + "\n"
				+ "\t\tSURNAME \"" + e.getLname() + "\"\n"
				+ "\t\tFIRSTNAME \"" + e.getFname() + "\"\n"
				+ "\t\tSALARY " + e.getWage() + "\n"
				+ "\t\tFUND_COEF " + e.getPercIns()*100 + "\n"

				+ "\t\tTRNS\n\t\t{\n";

			// Transactions of each Employee
			for (Action act : e.getActions()) {
				s += "\t\t\tTRN\n\t\t\t{\n"
					+ "\t\t\t\tELEMENT_TYPE " + act.getData().getClass().getName() + "\n"
					+ "\t\t\t\tCODE " + act.getData().getCode() + "\n"
					+ "\t\t\t\tVALUE " + act.getData().getAmount() + "\n"
					+ "\t\t\t}\n";
			}
			s += "\t\t}\n"
				+ "\t}\n";
		}
		s += "}";

		// final
		emp_act.write(s);
		emp_act.close();
	}
	//------------------------------DONE FILE PARSING------------------------------//
	private static void closeFile() {
		if (input != null) input.close();
	}

}