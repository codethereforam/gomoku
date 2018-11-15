package priv.thinkam.gomoku.server;

import priv.thinkam.gomoku.common.Constant;
import priv.thinkam.gomoku.net.FixLengthWrapper;
import priv.thinkam.gomoku.util.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Gomoku server
 *
 * @author thinkam
 * @date 2018/11/14
 */
public class GomokuServer {
	private Selector selector;
	private Set<SocketChannel> socketChannelSet = new HashSet<>();
	private int clientId = 1;

	private GomokuServer() {
		try {
			selector = Selector.open();
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.socket().bind(new InetSocketAddress(Constant.SERVER_PORT));
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			e.printStackTrace();
			this.close();
			System.exit(-1);
		}
		System.out.println("----- chat server start -----");
	}

	public static void main(String[] args) {
		new GomokuServer().start();
		System.out.println("server stop...");
	}

	/**
	 * server thread
	 *
	 * @author yanganyu
	 * @date 2018/11/7 15:21
	 */
	private void start() {
		while (true) {
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
				this.close();
				return;
			}
			this.handleSelectedKeys();
		}
	}

	private void handleSelectedKeys() {
		Set<SelectionKey> selectionKeySet = selector.selectedKeys();
		Iterator<SelectionKey> keyIterator = selectionKeySet.iterator();
		SelectionKey selectionKey;
		while (keyIterator.hasNext()) {
			selectionKey = keyIterator.next();
			this.handleSelectionKey(selectionKey);
			keyIterator.remove();
		}
	}

	private void closeSelectionKey(SelectionKey selectionKey) {
		if (selectionKey != null) {
			selectionKey.cancel();
			if (selectionKey.channel() != null) {
				try {
					selectionKey.channel().close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	/**
	 * handle input
	 *
	 * @param selectionKey selectionKey
	 * @author yanganyu
	 * @date 2018/11/7 15:37
	 */
	private void handleSelectionKey(SelectionKey selectionKey) {
		if (selectionKey.isValid()) {
			if (selectionKey.isAcceptable()) {
				this.handleAcceptableKey(selectionKey);
			} else if (selectionKey.isReadable()) {
				this.handleReadableKey(selectionKey);
			} else {
				System.out.println("!!! something wrong !!!");
				System.exit(-1);
			}
		}
	}

	private void handleAcceptableKey(SelectionKey selectionKey) {
		// Accept the new connection
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
		SocketChannel socketChannel = null;
		try {
			socketChannel = serverSocketChannel.accept();
			socketChannel.configureBlocking(false);
			// Add the new connection to the selector
			socketChannel.register(selector, SelectionKey.OP_READ);

		} catch (IOException e) {
			this.closeSelectionKey(selectionKey);
			if (socketChannel != null) {
				try {
					socketChannel.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			e.printStackTrace();
		}
		if (socketChannel == null) {
			return;
		}
		this.sendOnlineInfoToNew(socketChannel);
		this.sendConnectedInfoToAll(socketChannel);
		clientId++;
		socketChannelSet.add(socketChannel);
	}

	private void handleReadableKey(SelectionKey selectionKey) {
		SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
		ByteBuffer byteBuffer = ByteBuffer.allocate(10240);
		int byteCount;
		try {
			byteCount = socketChannel.read(byteBuffer);
		} catch (IOException e) {
			handleClientDisconnect(socketChannel, selectionKey);
			e.printStackTrace();
			return;
		}
		if(byteCount < 0) {
			handleClientDisconnect(socketChannel, selectionKey);
		}

		while (byteCount > 0) {
			byteBuffer.flip();
			while (byteBuffer.remaining() >= FixLengthWrapper.MAX_LENGTH) {
				byte[] data = new byte[FixLengthWrapper.MAX_LENGTH];
				byteBuffer.get(data, 0, FixLengthWrapper.MAX_LENGTH);
				String message = new String(data, StandardCharsets.UTF_8).trim();
				System.out.println(message);
				sendMessageToAll(message, socketChannel);
			}

			try {
				byteCount = socketChannel.read(byteBuffer);
			} catch (IOException e) {
				handleClientDisconnect(socketChannel, selectionKey);
				e.printStackTrace();
				return;
			}
			if(byteCount < 0) {
				handleClientDisconnect(socketChannel, selectionKey);
			}
		}
	}

	private void handleClientDisconnect(SocketChannel socketChannel, SelectionKey selectionKey) {
		this.sendExitInfoToAll(socketChannel);
		// close resource
		selectionKey.cancel();
		try {
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		socketChannelSet.remove(socketChannel);
	}

	/**
	 * send exit server info to all
	 *
	 * @param socketChannel exit socketChannel
	 * @author yanganyu
	 * @date 2018/11/12 14:36
	 */
	private void sendExitInfoToAll(SocketChannel socketChannel) {
		String exitMessage = this.getSocketIpAndPort(socketChannel) + " exited!!!";
		System.out.println(exitMessage);
		sendMessageToAll(exitMessage, socketChannel);
	}

	/**
	 * send connected server info to all
	 *
	 * @param socketChannel socketChannel
	 * @author yanganyu
	 * @date 2018/11/12 14:30
	 */
	private void sendConnectedInfoToAll(SocketChannel socketChannel) {
		sendMessageToAll("4;" + clientId, socketChannel);
	}

	/**
	 * send online server info to new
	 *
	 * @param socketChannel socketChannel
	 * @author yanganyu
	 * @date 2018/11/12 14:28
	 */
	private void sendOnlineInfoToNew(SocketChannel socketChannel) {
		sendMessage(socketChannel, "0;" + clientId);
	}

	/**
	 * get ip:port
	 *
	 * @return java.lang.String
	 * @author yanganyu
	 * @date 11/10/18 8:11 PM
	 */
	private String getSocketIpAndPort(SocketChannel socketChannel) {
		return socketChannel.socket().getInetAddress().getHostAddress() + ":" + socketChannel.socket().getPort();
	}

	/**
	 * send message to all except exceptedSocketChannel
	 *
	 * @param message               message
	 * @param exceptedSocketChannel excepted SocketChannel
	 * @author yanganyu
	 * @date 2018/11/12 14:33
	 */
	private void sendMessageToAll(String message, SocketChannel exceptedSocketChannel) {
		socketChannelSet.forEach(s -> {
			if (s.isOpen() && s != exceptedSocketChannel) {
				sendMessage(s, message);
			}
		});
	}

	/**
	 * send message to client
	 *
	 * @param channel  channel
	 * @param response response
	 * @author yanganyu
	 * @date 2018/11/7 15:39
	 */
	private void sendMessage(SocketChannel channel, String response) {
		if (StringUtils.isNotBlank(response)) {
			try {
				channel.write(ByteBuffer.wrap(new FixLengthWrapper(response).getBytes()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * close resource
	 *
	 * @author yanganyu
	 * @date 2018/11/12 14:44
	 */
	private void close() {
		if (selector != null) {
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
