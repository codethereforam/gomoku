package priv.thinkam.gomoku.util;

/**
 * String utility
 *
 * @author yanganyu
 * @date 2018/11/8 13:52
 */
public class StringUtils {
	/**
	 * empty string: ""
	 */
	public static final String EMPTY_STRING = "";

	private StringUtils() {
	}

	/**
	 * whether a string is blank
	 *
	 * @param str str
	 * @return boolean
	 * @author yanganyu
	 * @date 2018/11/8 13:53
	 */
	public static boolean isBlank(String str) {
		if (str == null) {
			return true;
		}
		return str.trim().length() == 0;
	}

	/**
	 * whether a string is not blank
	 *
	 * @param str str
	 * @return boolean
	 * @author yanganyu
	 * @date 2018/11/8 15:03
	 */
	public static boolean isNotBlank(String str) {
		return !isBlank(str);
	}

}
