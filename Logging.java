package ttt_online;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Scanner;

/**
 * An Interface for {@code log} and {@code logerr} methods providing
 * easily changeable print and error streams.Default log method uses
 * {@code System.out.println()}. Default logerr method writes the errors
 * data in a file.
 */
public interface Logging {

	/**
	 * Same as {@code System.out.println(text)}
	 * 
	 * @param text String, text to send
	 */
	default void log(String text) {
		System.out.println(text);
	}

	/**
	 * Prints the error message to an external "error_log.txt" file. Includes
	 * information about the date/time and class where the error occurred.
	 * 
	 * @param error_msg  String, a summary of the error
	 * @param exc        Exception, the exception that caused the logged error
	 * @param printFull  boolean, whether or not to print the full error message
	 */
	default void logerr(String error_msg, Exception exc, boolean printFull) {

		log("Error logged to file: " + error_msg);

		File file = new File("error_log.txt");
		try {
			if (!file.exists())
				file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// get the Stack Trace as a String
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		exc.printStackTrace(pw);

		// make sure file is synchronized across many different threads
		synchronized (file) {

			// read the old data
			Scanner errorFile;
			StringBuffer oldData = new StringBuffer("");
			try {
				errorFile = new Scanner(file);
			} catch (IOException ex) {
				ex.printStackTrace();
				return;
			}

			while (errorFile.hasNext())
				oldData.append(errorFile.nextLine() + "%n");

			errorFile.close();

			// get date/time and create crash message
			LocalDateTime now = LocalDateTime.now();
			String dateTime = String.format("%d/%d/%d %d:%d:%d", now.getDayOfMonth(), now.getMonthValue(),
					now.getYear(), now.getHour(), now.getMinute(), now.getSecond());
			String crashReport = String.format(
					"Crash at %s || Class %s%nSummary: %s %s%n#########################################%n", dateTime,
					this.getClass().getName(), error_msg, printFull ? "%nFull report:%n" + sw.toString() : "");
			
			System.out.println(System.lineSeparator());

			// write the data
			BufferedWriter writer;
			try {
				writer = new BufferedWriter(new FileWriter("error_log.txt"));
				writer.write(oldData + crashReport);
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
	}
}
