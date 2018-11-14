package priv.thinkam.gomoku.common;

import java.awt.*;

/**
 * Drawable接口应由那些打算被画出来的类来实现。
 * 类必须定义一个称为 Drawable的方法。
 *
 * @author thinkam
 * @date 2018/02/16
 */
public interface Drawable {
	/**
	 * 画图
	 *
	 * @param g 画笔
	 */
	void draw(Graphics g);
}
