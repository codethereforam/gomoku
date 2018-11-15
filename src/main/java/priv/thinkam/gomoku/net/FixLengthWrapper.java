package priv.thinkam.gomoku.net;


import java.nio.ByteBuffer;

/**
 * 封装固定长度的工具类
 *
 * @author thinkam
 * @date 2018/11/15
 */
public class FixLengthWrapper {

	public static final int MAX_LENGTH = 30;
	private byte[] data;

	public FixLengthWrapper(String msg) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(MAX_LENGTH);
		byteBuffer.put(padRight(msg, MAX_LENGTH).getBytes());
		data = byteBuffer.array();
	}

	public byte[] getBytes() {
		return data;
	}

	public static String padRight(String s, int n) {
		return String.format("%1$-" + n + "s", s);
	}
}
