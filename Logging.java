import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

package ttt_online;

/**
 * An interface for <code>log</code> and <code>logerr</code> methods providing an easily changeable print and error stream.
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
	 * @param error_msg String, the line that will be printed on the file
	 */
	default void logerr(String error_msg) {
		BufferedWriter writer;
		LocalDateTime now = LocalDateTime.now();
		String dateTime = String.format("%d/%d/%d %d:%d:%d",now.getDayOfMonth(),now.getMonthValue(),now.getYear(),now.getHour(),now.getMinute(),now.getSecond());
		try {
			writer = new BufferedWriter(new FileWriter("error_log.txt"));
			writer.write(String.format("Crash at %s\nClass %s\nError:%s\n#########################################\n",dateTime,this.getClass().getName(),error_msg));
			writer.close();
		}catch (IOException e) {
			System.err.println(e);
			return;
		}
		
	}
	
}
