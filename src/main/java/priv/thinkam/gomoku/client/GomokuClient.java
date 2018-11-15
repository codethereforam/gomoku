package priv.thinkam.gomoku.client;

import priv.thinkam.gomoku.client.model.Chessman;
import priv.thinkam.gomoku.common.Constant;
import priv.thinkam.gomoku.net.FixLengthWrapper;

import javax.swing.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * Gomoku client
 *
 * @author thinkam
 * @date 2018/11/14
 */
public class GomokuClient {
	private static final String SERVER_IP = "127.0.0.1";
	private Selector selector;
	private SocketChannel socketChannel;
	private volatile boolean running = true;
	private GomokuClientFrame gomokuClientFrame;

	private GomokuClient() {
		try {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		GomokuClient gomokuClient = new GomokuClient();
		GomokuClientFrame gomokuClientFrame = new GomokuClientFrame(gomokuClient);
		gomokuClient.setGomokuClientFrame(gomokuClientFrame);
		gomokuClient.start();
		System.out.println("client disconnected...");
	}

	private void setGomokuClientFrame(GomokuClientFrame gomokuClientFrame) {
		this.gomokuClientFrame = gomokuClientFrame;
	}

	/**
	 * client thread
	 *
	 * @author yanganyu
	 * @date 2018/11/7 15:48
	 */
	private void start() {
		try {
			this.connect();
		} catch (IOException e) {
			e.printStackTrace();
			this.close();
			System.exit(-1);
		}
		while (running) {
			int readyChannelCount;
			try {
				readyChannelCount = selector.select(1000);
			} catch (IOException e) {
				e.printStackTrace();
				this.close();
				return;
			}
			if (readyChannelCount == 0) {
				continue;
			}
			if (!selector.isOpen()) {
				return;
			}
			this.handleSelectedKeys();
		}
	}

	private void handleSelectedKeys() {
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
		SelectionKey selectionKey;
		while (keyIterator.hasNext()) {
			selectionKey = keyIterator.next();
			keyIterator.remove();
			try {
				handleSelectionKey(selectionKey);
			} catch (Exception e) {
				e.printStackTrace();
				this.closeSelectionKey(selectionKey);
			}
		}
	}

	private void handleSelectionKey(SelectionKey selectionKey) {
		if (selectionKey.isValid()) {
			if (selectionKey.isConnectable()) {
				this.handleConnectableKey(selectionKey);
			} else if (selectionKey.isReadable()) {
				this.handleReadableKey(selectionKey);
			}
		}
	}

	private void handleConnectableKey(SelectionKey selectionKey) {
		SocketChannel sc = (SocketChannel) selectionKey.channel();
		boolean finishConnected = false;
		try {
			finishConnected = sc.finishConnect();
		} catch (IOException e) {
			System.out.println("can not connected to server...");
			e.printStackTrace();
		}
		if (finishConnected) {
			try {
				sc.register(selector, SelectionKey.OP_READ);
			} catch (ClosedChannelException e) {
				this.handleServerCrashAndClose(selectionKey);
				e.printStackTrace();
			}
		} else {
			// connect fail
			gomokuClientFrame.handleServerCrash();
			this.close();
		}
	}

	private void handleReadableKey(SelectionKey selectionKey) {
		SocketChannel sc = (SocketChannel) selectionKey.channel();
		ByteBuffer byteBuffer = ByteBuffer.allocate(10240);
		int byteCount;
		try {
			byteCount = sc.read(byteBuffer);
		} catch (IOException e) {
			e.printStackTrace();
			this.handleServerCrashAndClose(selectionKey);
			return;
		}
		if(byteCount < 0) {
			this.handleServerCrashAndClose(selectionKey);
		}

		while (byteCount > 0) {
			byteBuffer.flip();
			while (byteBuffer.remaining() >= FixLengthWrapper.MAX_LENGTH) {
				byte[] data = new byte[FixLengthWrapper.MAX_LENGTH];
				byteBuffer.get(data, 0, FixLengthWrapper.MAX_LENGTH);
				String message = new String(data, StandardCharsets.UTF_8);
				this.process(message.trim());
			}

			try {
				byteCount = sc.read(byteBuffer);
			} catch (IOException e) {
				e.printStackTrace();
				this.handleServerCrashAndClose(selectionKey);
				return;
			}
			if(byteCount < 0) {
				this.handleServerCrashAndClose(selectionKey);
			}
		}
	}

	private void process(String message) {
		String messageTypeStr = message.substring(0, 1);
		int messageType = Integer.parseInt(messageTypeStr);
		String leftMessage = message.substring(2);
		System.out.println("message: " + message);
		switch (messageType) {
			case 0:
				int clientId = Integer.parseInt(leftMessage);
				gomokuClientFrame.setId(clientId);
				// 奇数为白色，先下
				gomokuClientFrame.setWhite(clientId % 2 == 1);
				System.out.println("has been connected to server!!!server gives me an ID:" + clientId);
				break;
			case 1:
				break;
			case 2:
				break;
			case 4:
				gomokuClientFrame.setRivalId(Integer.parseInt(leftMessage));
				String str;
				if (!gomokuClientFrame.isWhite()) {
					str = "我方是黑子\n等待对方下";
				} else {
					str = "我方是白子\n轮到我下了";
					gomokuClientFrame.setAddible(true);
				}
				String info = "对手已上线!!!\n对手ID:" + gomokuClientFrame.getRivalId() + " 我的ID:" + gomokuClientFrame.getId() +
						"\n" + str;
				gomokuClientFrame.setTextAreaText(info);
				this.sendMessageToServer("5;" + gomokuClientFrame.getId());
				break;
			case 5:
				gomokuClientFrame.setRivalId(Integer.parseInt(leftMessage));
				String str1;
				if (!gomokuClientFrame.isWhite()) {
					str1 = "我方是黑子\n等待对方下";
				} else {
					str1 = "我方是白子\n轮到我下了";
					gomokuClientFrame.setAddible(true);
				}
				String info1 =
						"对手已在线!!!\n对手ID:" + gomokuClientFrame.getRivalId() + " 我的ID:" + gomokuClientFrame.getId() +
								"\n" + str1;
				gomokuClientFrame.setTextAreaText(info1);
				break;
			case 6:
				String[] strs = leftMessage.split(";");
				int rivalId = Integer.parseInt(strs[0]);
				int x = Integer.parseInt(strs[1]);
				int y = Integer.parseInt(strs[2]);
				if (rivalId == gomokuClientFrame.getRivalId()) {

					boolean white = rivalId % 2 == 1;
					gomokuClientFrame.getOutline().set(x, y, white);
					gomokuClientFrame.getOutline().setDrawable(true);

					Chessman chessman = new Chessman(x, y, white);
					gomokuClientFrame.addChessman(chessman);
					gomokuClientFrame.repaint();
					if (gomokuClientFrame.isDraw()) {
						return;
					}
					gomokuClientFrame.setAddible(true);
					gomokuClientFrame.setTextAreaText("轮到我下了！！!\n对方刚下的坐标 "
							+ "(" + x / GomokuClientFrame.GRID_LENGTH + "," + y / GomokuClientFrame.GRID_LENGTH + ")");
				}
				break;
			case 7:
				if (Integer.parseInt(leftMessage) == gomokuClientFrame.getRivalId()) {
					gomokuClientFrame.setTextAreaText("游戏已结束！！！");
					JOptionPane.showMessageDialog(gomokuClientFrame, "输了！！！");
					gomokuClientFrame.gameOver();
				}
			default:
		}
	}

	private void handleServerCrashAndClose(SelectionKey selectionKey) {
		gomokuClientFrame.handleServerCrash();
		this.closeSelectionKey(selectionKey);
	}

	private void closeSelectionKey(SelectionKey selectionKey) {
		if (selectionKey != null) {
			selectionKey.cancel();
			if (selectionKey.channel() != null) {
				try {
					selectionKey.channel().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void connect() throws IOException {
		if (!socketChannel.connect(new InetSocketAddress(SERVER_IP, Constant.SERVER_PORT))) {
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
		}
	}

	/**
	 * close clint
	 *
	 * @author yanganyu
	 * @date 2018/11/8 16:07
	 */
	void close() {
		if (running) {
			running = false;
			if (selector != null) {
				try {
					selector.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * send message to server
	 *
	 * @author yanganyu
	 * @date 11/10/18 10:22 AM
	 */
	void sendMessageToServer(String text) {
		try {
			socketChannel.write(ByteBuffer.wrap(new FixLengthWrapper(text).getBytes()));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
