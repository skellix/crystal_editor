package com.skellix.editor.js;

import java.awt.event.KeyEvent;

import javax.swing.InputMap;
import javax.swing.KeyStroke;

public class DefaultInputMap extends InputMap {

	{
		put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right");
		put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left");
		put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
		put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
		put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), "end");
		put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "home");
		put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK), "auto-suggest");
		put(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), "new buffer");
		put(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK), "open file");
		put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "save");
		for (char c = ' ' + 1 ; c <= '~' ; c ++) {
			String str = Character.toString(c);
			put(KeyStroke.getKeyStroke(new Character(c), 0), str);
//			put(KeyStroke.getKeyStroke(c), str);
		}
		put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), Character.toString(' '));
		for (char c : "\n\t".toCharArray()) {
			String str = Character.toString(c);
			put(KeyStroke.getKeyStroke(new Character(c), 0), str);
//			put(KeyStroke.getKeyStroke(c), str);
		}
		put(KeyStroke.getKeyStroke('\b'), "bs");
		put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), "run-buffer");
	}
}
