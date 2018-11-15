package priv.thinkam.gomoku.client.model;

import priv.thinkam.gomoku.common.Drawable;

import java.awt.*;

/**
 * 棋子
 *
 * @author thinkam
 * @date 2018/11/14
 */
public class Chessman implements Drawable {
	public static final int DIAMETER = 50;

	private int coordinateX;
	private int coordinateY;
	private boolean white;

	public Chessman(int coordinateX, int coordinateY, boolean white) {
		this.coordinateX = coordinateX;
		this.coordinateY = coordinateY;
		this.white = white;
	}

	@Override
	public void draw(Graphics graphics) {
		Color color = graphics.getColor();
		graphics.setColor(white ? Color.white : Color.black);
		graphics.fillOval(coordinateX - DIAMETER / 2, coordinateY - DIAMETER / 2, DIAMETER, DIAMETER);
		graphics.setColor(color);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Chessman)) {
			return false;
		}
		Chessman c = (Chessman) o;
		return coordinateX == c.coordinateX && coordinateY == c.coordinateY;
	}

	@Override
	public int hashCode() {
		return coordinateX * coordinateY;
	}
}
