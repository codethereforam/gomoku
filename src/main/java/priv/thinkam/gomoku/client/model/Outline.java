package priv.thinkam.gomoku.client.model;

import priv.thinkam.gomoku.common.Drawable;

import java.awt.*;

/**
 * 对方刚下的棋子外框
 *
 * @author thinkam
 * @date 2018/11/15
 */
public class Outline implements Drawable {
	private int coordinateX;
	private int coordinateY;
	private boolean white;
	private static Outline outline = null;
	/**
	 * 是否被画出来
	 */
	private boolean drawable;

	private Outline() {}

	public static Outline getInstance() {
		if(outline == null) {
			outline = new Outline();
		}
		return outline;
	}

	public void set(int coordinateX, int coordinateY, boolean white) {
		this.coordinateX = coordinateX;
		this.coordinateY = coordinateY;
		this.white = white;
	}

	public boolean isDrawable() {
		return drawable;
	}

	public void setDrawable(boolean drawable) {
		this.drawable = drawable;
	}

	@Override
	public void draw(Graphics g) {
		Color color = g.getColor();
		g.setColor(white ? Color.white : Color.black);
		g.drawRect(coordinateX - Chessman.DIAMETER/2, coordinateY - Chessman.DIAMETER/2, Chessman.DIAMETER, Chessman.DIAMETER);
		g.setColor(color);
	}
}
