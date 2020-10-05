import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Scanner;

//package ttt_online;

/**
 * An interface for <code>log</code> and <code>logerr</code> methods providing an easily changeable print and error stream.
 * Default methods implement the System.out.println and System.err.println streams. 
 *
 */
public interface Logging {

	/**
	 * Same as <code>System.out.printf(text, args)</code>
	 * 
	 * @param text String, text to send
	 * @param args Object[], arguments
	 */
	default void log(String text) {
		System.out.println(text);
	}

	/**
	 * Prints the error message to an external "error_log.txt" file.
	 * Includes information about the date/time and class where the error occurred.
	 * 
	 * @param error_msg String, a summary of the error
	 * @param e Exception
	 * @param printFull, whether or not to print the full error message
	 */
	default void logerr(String error_msg, Exception e, boolean printFull) {
		
		File file = new File("error_log.txt");
		try {
			if (!file.exists()) file.createNewFile();
		}catch(IOException ex) {
			ex.printStackTrace();
			return;
		}
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		
		synchronized(file){
			Scanner in;
			StringBuffer old_data = new StringBuffer("");
			try {
				 in = new Scanner(file);
			}catch(IOException ex) {
				ex.printStackTrace();
				return;
			}
			while (in.hasNext()) {
				old_data.append(in.nextLine()+"\n");
			}
			in.close();
			
			BufferedWriter writer;
			LocalDateTime now = LocalDateTime.now();
			String dateTime = String.format("%d/%d/%d %d:%d:%d",now.getDayOfMonth(),now.getMonthValue(),now.getYear(),now.getHour(),now.getMinute(),now.getSecond());
			try {
				writer = new BufferedWriter(new FileWriter("error_log.txt"));
				writer.write(old_data+String.format("Crash at %s\nClass %s\nSummary:%s %s\n#########################################\n",dateTime,this.getClass().getName(),
						error_msg, printFull? "Full report:\n"+sw.toString() :""));
				writer.close();
			}catch (IOException ex) {
				System.err.println(ex);
				return;
			}
		}
	}

	
}
