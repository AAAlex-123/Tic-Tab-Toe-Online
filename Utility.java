package ttt_online;

import java.util.ArrayList;

/**
 * A class with a set of static utility methods to be used throughout the
 * project.
 */
public class Utility {

	/**
	 * Main method for testing the methods of this class.
	 * 
	 * @param args not used
	 */
	public static void main(String[] args) {
		String s = "";
		char[] chars = new char[]{'\u0000'};
		System.out.println(myStrip(s, chars));
	}

	/**
	 * Returns a string, stripped by <code>chars</code>.<br>
	 * Similar to python's <code>str.strip(string)</code>.
	 * 
	 * @param string String, the string to strip
	 * @param chars  char[], the characters to strip from the string
	 * @return String, the stripped string
	 */
	public static String myStrip(String string, char... chars) {
		if (string.equals(""))
			return "";
		return string.substring(firstIndexOfNonChars(string, chars), lastIndexofNonChars(string, chars) + 1);
	}

	/**
	 * Returns true or false indicating if <code>item</code> is an item of
	 * <code>array</code>.<br>
	 * Same as <code>ArrayList.contains()</code>
	 * 
	 * @param array char[], the array of items
	 * @param item  char, the item to check if it is in the array
	 * @return boolean, whether or not <code>item</code> is in <code>array</code>
	 * 
	 * @see ArrayList#contains(Object)
	 */
	public static boolean myContains(char[] array, char item) {
		for (int i = 0; i < array.length; i++)
			if (array[i] == item)
				return true;
		return false;
	}

	private static int firstIndexOfNonChars(String string, char... chars) {
		for (int i = 0; i < string.length(); i++)
			if (!myContains(chars, string.charAt(i)))
				return i;
		return -1;
	}

	private static int lastIndexofNonChars(String string, char... chars) {
		for (int i = string.length() - 1; i > -1; i--)
			if (!myContains(chars, string.charAt(i)))
				return i;
		return -1;
	}
}
