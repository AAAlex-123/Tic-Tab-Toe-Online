package ttt_online;

/**
 * An interface for <code>log</code> and <code>logerr</code> methods because
 * both GameEngine and Server are using it and there can't be a superclass of
 * these.
 *
 */
public interface Logging {

	/**
	 * Same as <code>System.out.printf(text, args)</code>
	 * 
	 * @param text String, text to send
	 * @param args Object[], arguments
	 */
	default void log(String text, Object... args) {
		System.out.printf(text + "\n", args);
	}

	/**
	 * Same as <code>System.err.printf(text, args)</code>
	 * 
	 * @param text String, text to send
	 * @param args Object[], arguments
	 */
	default void logerr(String text, Object... args) {
		System.err.printf(text + "\n", args);
	}
}
