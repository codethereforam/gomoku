package priv.thinkam.gomoku.client;

import priv.thinkam.gomoku.client.model.Chessman;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Gomoku client frame
 *
 * @author thinkam
 * @date 2018/11/14
 */
public class GomokuClientFrame extends Frame {
	private static final Color BACKGROUND_COLOR = new Color(210, 105, 30);
	private static final int GRID_NUMBER = 15;
	/**
	 * 每个格子的边长与棋子直径相同
	 */
	static final int GRID_LENGTH = Chessman.DIAMETER;
	private static final int LENGTH = (GRID_NUMBER + 1) * GRID_LENGTH;
	private static final int HEIGHT = LENGTH + 100;

	private int id;
	/**
	 * 对手ID
	 */
	private int rivalId;
	private boolean white;
	/**
	 * 装棋子的容器
	 */
	private Set<Chessman> chessmanSet = new HashSet<>();
	private TextArea textArea;
	/**
	 * 记录棋盘中我的棋子的位置
	 */
	private boolean[][] mark = new boolean[GRID_NUMBER][GRID_NUMBER];
	private Image offScreenImage = null;
	/**
	 * 用于双缓冲 负责区分是不是第一次重画
	 */
	private boolean init;
	/**
	 * 是否可以落子
	 */
	private boolean addible;
	private GomokuClient gomokuClient;
	private boolean over;

	GomokuClientFrame(GomokuClient gomokuClient) {
		this.gomokuClient = gomokuClient;
		this.init();
	}

	private GomokuClientFrame() {
		this.init();
	}

	public static void main(String[] args) {
		new GomokuClientFrame();
	}

	private void init() {
		this.initTextArea();
		this.initFrame();
	}

	private void initTextArea() {
		textArea = new TextArea("等待对方上线！！！", 4, 10);
		textArea.setFont(new Font("宋体", Font.BOLD, 18));
		textArea.setBounds(GRID_LENGTH * 5, GRID_LENGTH * (GRID_NUMBER + 1), 250, 100);
		textArea.setEditable(false);
		textArea.setFocusable(false);
		textArea.setBackground(BACKGROUND_COLOR);
	}

	private void initFrame() {
		this.setTitle("Gomoku client");
		this.setBounds(400, 0, LENGTH, HEIGHT);
		this.setBackground(BACKGROUND_COLOR);
		this.setLayout(null);
		this.add(textArea);
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				handleMousePressed(e);
			}
		});
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		this.setResizable(true);
		this.setVisible(true);
	}

	void setId(int id) {
		this.id = id;
	}

	int getId() {
		return this.id;
	}

	void setWhite(boolean white) {
		this.white = white;
	}

	boolean isWhite() {
		return this.white;
	}

	void setAddible(boolean addible) {
		this.addible = addible;
	}

	void setRivalId(int rivalId) {
		this.rivalId = rivalId;
	}

	int getRivalId() {
		return this.rivalId;
	}

	void gameOver() {
		this.over = true;
	}

	private void handleMousePressed(MouseEvent e) {
		if (over) {
			JOptionPane.showMessageDialog(GomokuClientFrame.this, "游戏已结束！！！");
			return;
		}

		if (rivalId == 0) {
			JOptionPane.showMessageDialog(GomokuClientFrame.this, "等待对方上线！！！");
			return;
		}

		if (!addible) {
			JOptionPane.showMessageDialog(GomokuClientFrame.this, "等待对方下！！！");
			return;
		}

		//估计鼠标按下的位置对应网格点
		int x = 0, y = 0;
		for (int i = GRID_LENGTH; i < LENGTH; i += GRID_LENGTH) {
			if (Math.abs(e.getX() - i) <= GRID_LENGTH / 2) {
				x = i;
				break;
			}
		}
		for (int i = GRID_LENGTH; i < LENGTH; i += GRID_LENGTH) {
			if (Math.abs(e.getY() - i) <= GRID_LENGTH / 2) {
				y = i;
				break;
			}
		}

		Chessman chess = null;
		if (x != 0 && y != 0) {
			chess = new Chessman(x, y, white);
		}

		if (chess == null) {
			JOptionPane.showMessageDialog(GomokuClientFrame.this, "位置无效，重下！！！");
		} else if (chessmanSet.contains(chess)) {
			JOptionPane.showMessageDialog(GomokuClientFrame.this, "已有，重下！！！");
		} else {
			chessmanSet.add(chess);
			repaint();

			gomokuClient.sendMessageToServer("6;" + this.id + ";" + x + ";" + y);

			int cx = x / GRID_LENGTH - 1, cy = y / GRID_LENGTH - 1;
			mark[cx][cy] = true;
			if (this.checkWin(cx, cy)) {
				gomokuClient.sendMessageToServer("7;" + this.id);
				textArea.setText("游戏已结束！！！");
				JOptionPane.showMessageDialog(GomokuClientFrame.this, "恭喜你赢了！！！");
				return;
			}

			addible = false;
			textArea.setText("等待对方下");
			isDraw();
		}
	}

	void setTextAreaText(String text) {
		textArea.setText(text);
	}

	void addChessman(Chessman chessman) {
		this.chessmanSet.add(chessman);
	}

	/**
	 * 双缓冲除闪烁
	 */
	@Override
	public void update(Graphics g) {
		if (!init) {
			super.update(g);
			init = true;
		}
		if (offScreenImage == null) {
			offScreenImage = this.createImage(LENGTH, HEIGHT);
		} else {
			Graphics gOffScreen = offScreenImage.getGraphics();

			Color c = gOffScreen.getColor();
			gOffScreen.setColor(BACKGROUND_COLOR);
			gOffScreen.fillRect(0, 0, LENGTH, HEIGHT);
			gOffScreen.setColor(c);

			paint(gOffScreen);
			g.drawImage(offScreenImage, 0, 0, null);
		}
	}

	@Override
	public void paint(Graphics g) {
		drawGrid(g);
		chessmanSet.forEach((c) -> c.draw(g));
	}

	/**
	 * 画出网格
	 */
	private void drawGrid(Graphics g) {
		//画出竖线
		for (int i = GRID_LENGTH; i < LENGTH; i += GRID_LENGTH) {
			g.drawLine(i, GRID_LENGTH, i, LENGTH - GRID_LENGTH);
			g.drawString(i / GRID_LENGTH + "", i - 3, GRID_LENGTH - 18);
		}
		//画出横线
		for (int i = GRID_LENGTH; i < LENGTH; i += GRID_LENGTH) {
			g.drawLine(GRID_LENGTH, i, LENGTH - GRID_LENGTH, i);
			g.drawString(i / GRID_LENGTH + "", 15, i);
		}
	}

	/**
	 * 判断是否是平局并做出响应
	 */
	boolean isDraw() {
		if (chessmanSet.size() == GRID_NUMBER * GRID_NUMBER) {
			JOptionPane.showMessageDialog(this, "平局！！！");
			return true;
		}
		return false;
	}


	private boolean checkWin(int x, int y) {
		boolean flag = false;
		if (checkXCount(x, y) >= 5) {
			System.out.println("win水平线");
			flag = true;
		} else if (checkYCount(x, y) >= 5) {
			System.out.println("win竖直线");
			flag = true;
		} else if (checkLRCount(x, y) >= 5) {
			System.out.println("win左上右下斜线");
			flag = true;
		} else if (checkRLCount(x, y) >= 5) {
			System.out.println("win右上左下斜线");
			flag = true;
		}
		return flag;
	}

	/**
	 * 计算水平线上的个数
	 */
	private int checkXCount(int x, int y) {
		int count = 1;
		int tempX = x + 1;
		while (tempX < GRID_NUMBER) {
			if (mark[tempX][y]) {
				count++;
			} else {
				break;
			}
			tempX++;
		}

		tempX = x - 1;
		while (tempX >= 0) {
			if (mark[tempX][y]) {
				count++;
			} else {
				break;
			}
			tempX--;
		}
		return count;
	}

	/**
	 * 计算竖直线上的个数
	 */
	private int checkYCount(int x, int y) {
		int count = 1;
		int tempY = y + 1;
		while (tempY < GRID_NUMBER) {
			if (mark[x][tempY]) {
				count++;
			} else {
				break;
			}
			tempY++;
		}

		tempY = y - 1;
		while (tempY >= 0) {
			if (mark[x][tempY]) {
				count++;
			} else {
				break;
			}
			tempY--;
		}
		return count;
	}

	/**
	 * 计算左上右下斜线上的个数
	 */
	private int checkLRCount(int x, int y) {
		int count = 1;
		int tempX = x + 1;
		int tempY = y + 1;
		while (tempX < GRID_NUMBER && tempY < GRID_NUMBER) {
			if (mark[tempX][tempY]) {
				count++;
			} else {
				break;
			}
			tempX++;
			tempY++;
		}
		tempX = x - 1;
		tempY = y - 1;
		while (tempX >= 0 && tempY >= 0) {
			if (mark[tempX][tempY]) {
				count++;
			} else {
				break;
			}
			tempX--;
			tempY--;
		}
		return count;
	}

	/**
	 * 计算右上左下斜线上的个数
	 */
	private int checkRLCount(int x, int y) {
		int count = 1;
		//先向右上
		int tempX = x + 1;
		int tempY = y - 1;
		while (tempX < GRID_NUMBER && tempY >= 0) {
			if (mark[tempX][tempY]) {
				count++;
			} else {
				break;
			}
			tempX++;
			tempY--;
		}

		tempX = x - 1;
		tempY = y + 1;
		while (tempX >= 0 && tempY < GRID_NUMBER) {
			if (mark[tempX][tempY]) {
				count++;
			} else {
				break;
			}
			tempX--;
			tempY++;
		}
		return count;
	}

	/**
	 * server crash
	 */
	void handleServerCrash() {
		textArea.setForeground(Color.red);
		textArea.setFont(new Font("Verdana", Font.BOLD, 24));
		textArea.setText("Server crashes......");
	}
}
