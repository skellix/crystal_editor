package com.skellix.editor.js;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;
import javax.swing.JFrame;

public class Editor {

	public static void main(String[] args) {
		Editor main = new Editor();
	}
	
	File cacheFile = new File(".editor.js.cache");
	
	HashMap<File, ByteBuffer> buffers = new HashMap<File, ByteBuffer>();
	HashMap<File, AtomicBoolean> modified = new HashMap<File, AtomicBoolean>();
	
	File currentBuffer = new File("new01.js");
	ByteBuffer buffer = ByteBuffer.allocate(0);
	{
		if (cacheFile.exists()) {
			try (Scanner scanner = new Scanner(new FileInputStream(cacheFile))) {
				while (scanner.hasNextLine()) {
					File file = new File(scanner.nextLine());
					if (file.exists()) {
						try (FileInputStream in = new FileInputStream(file)) {
							ByteBuffer buffer = ByteBuffer.allocate((int) file.length());
							ReadableByteChannel ch = Channels.newChannel(in);
							ch.read(buffer);
							ch.close();
							buffers.put(file, buffer);
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						System.err.printf("Unable to find file '%s'%n", file.getAbsolutePath());
						System.exit(-1);
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			if (!buffers.isEmpty()) {
				currentBuffer = buffers.keySet().toArray(new File[0])[0];
				buffer = buffers.get(currentBuffer);
			} else {
				buffers.put(currentBuffer, buffer);
			}
		} else {
//			buffers.put(currentBuffer, buffer);
		}
	}
	public Viewer viewer = new Viewer();
	{
		viewer.cursorLine = 6;
		viewer.cursorColumn = 4;
	}
	JFrame frame = new JFrame("Crystal JS Editor");
	Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
	
	AtomicBoolean showSuggestions = new AtomicBoolean(false);
	ArrayList<Suggestion> suggestions = null;
	
	JComponent contentPane = new JComponent() {
		protected void paintComponent(java.awt.Graphics g0) {
			Graphics2D g = (Graphics2D) g0;
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
			g.setFont(font);
			
			g.setTransform(new AffineTransform());
			drawTabbs(g, getWidth());
			drawBufferWithLineNumbers(g, buffer, viewer, getWidth(), getHeight() - g.getFontMetrics().getHeight());
		}
	};
	
	
	{
		try {
			final URL iconUrl = Thread.currentThread().getContextClassLoader().getResource("icon.png");
			frame.setIconImage(Toolkit.getDefaultToolkit().getImage(iconUrl));
		} catch (Exception e) {
			// TODO: handle exception
		}
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(contentPane);
		frame.setSize(900, 600);
		frame.addWindowListener(new WindowListener() {
			
			@Override
			public void windowOpened(WindowEvent e) {}
			
			@Override
			public void windowIconified(WindowEvent e) {}
			
			@Override
			public void windowDeiconified(WindowEvent e) {}
			
			@Override
			public void windowDeactivated(WindowEvent e) {}
			
			@Override
			public void windowClosing(WindowEvent e) {
				if (!cacheFile.exists()) {
					try {
						cacheFile.createNewFile();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				try (PrintWriter writer = new PrintWriter(new FileWriter(cacheFile))) {
					for (File key : buffers.keySet()) {
						writer.println(key.getAbsolutePath());
					}
					writer.flush();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			
			@Override
			public void windowClosed(WindowEvent e) {}
			
			@Override
			public void windowActivated(WindowEvent e) {}
		});
		contentPane.setActionMap(new DefaultActionMap(this));
		contentPane.setInputMap(JComponent.WHEN_FOCUSED, new DefaultInputMap());
		contentPane.setFocusTraversalKeysEnabled(false);
		contentPane.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent arg0) {}
			
			@Override
			public void mousePressed(MouseEvent arg0) {
				Graphics2D g = (Graphics2D) contentPane.getGraphics();
				if (arg0.getY() <= g.getFontMetrics().getHeight()) {
					AffineTransform origin = g.getTransform();
					g.setTransform(new AffineTransform());
					for (File key : buffers.keySet()) {
						g.translate(2, 0);
						int tabbWidth = (int) g.getFontMetrics().getStringBounds(key.getName(), g).getWidth() + 2;
						Point2D p = g.getTransform().transform(new Point(0, 0), null);
						if (arg0.getX() >= p.getX() && arg0.getX() <= p.getX() + tabbWidth + 4) {
							buffers.put(currentBuffer, buffer);
							currentBuffer = key;
							buffer = buffers.get(key);
//							System.out.printf("tab '%s' selected%n", key);
							break;
						}
						g.translate(tabbWidth + 2, 0);
					}
					g.setTransform(origin);
				} else {
					moveCursorTo(arg0.getPoint());
//					int marginSize = (int) g.getFontMetrics().getStringBounds(Integer.toString(viewer.endLine), g).getWidth() + 2;
//					int y = (int) (viewer.startLine + ((arg0.getY() - (g.getFontMetrics().getHeight() / 2.0)) / g.getFontMetrics().getHeight()) - 1);
//					if (y > viewer.endLine) {
//						y = viewer.endLine;
//					}
//					if (y < viewer.startLine) {
//						y = viewer.startLine;
//					}
//					AffineTransform t = new AffineTransform();
//					t.translate(marginSize, arg0.getY());
//					int x = (arg0.getX() - marginSize) / g.getFontMetrics().charWidth(' ');
//					int length = getLineLength(y);
//					int start = getOffset(y, 0);
//					int x0 = 0;
//					double minDist = Double.MAX_VALUE;
//					int minX = 0;
//					for (int i = start ; i < start + length ; i ++) {
//						buffer.position(i);
//						char c = (char) buffer.get();
//						x0 ++;
//						if (c == '\t') {
//							t.translate(g.getFontMetrics().charWidth(' ') * 4, 0);
//						} else {
//							t.translate(g.getFontMetrics().charWidth(c), 0);
//						}
//						Point2D p = t.transform(new Point(0, 0), null);
//						double dist = p.distance(new Point(arg0.getX(), arg0.getY()));
//						if (dist < minDist) {
//							minDist = dist;
//							minX = x0;
//						}
//					}
					viewer.cursorLine = getNumLines() - 1;
					viewer.cursorColumn = getLineLength(viewer.cursorLine) - 1;
				}
				contentPane.revalidate();
				contentPane.repaint();
			}
			
			@Override
			public void mouseExited(MouseEvent arg0) {}
			
			@Override
			public void mouseEntered(MouseEvent arg0) {}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {}
		});
	}
	
	public Editor() {
		frame.setVisible(true);
	}
	
	AtomicBoolean moveCursorTo = new AtomicBoolean(false);
	Point2D moveCursorToPoint;
	
	protected void moveCursorTo(Point2D point) {
		moveCursorTo.set(true);
		this.moveCursorToPoint = point;
	}

	protected void drawTabbs(Graphics2D g, int width) {
		AffineTransform origin = g.getTransform();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, g.getFontMetrics().getHeight());
		g.translate(0, g.getFontMetrics().getHeight() - g.getFontMetrics().getMaxDescent());
//		g.setColor(Color.GRAY);
		for (File key : buffers.keySet()) {
			if (modified.containsKey(key) && modified.get(key).get()) {
				g.setColor(Color.RED);
			} else {
				g.setColor(Color.GRAY);
			}
			g.drawString(key.getName(), 2, 0);
			int tabbWidth = (int) g.getFontMetrics().getStringBounds(key.getName(), g).getWidth() + 2;
			if (currentBuffer != key) {
				g.setXORMode(Color.WHITE);
				g.fillRect(0, -(g.getFontMetrics().getHeight() - g.getFontMetrics().getMaxDescent()), tabbWidth, g.getFontMetrics().getHeight() - 1);
				g.setPaintMode();
			} else {
				g.drawRect(0, -(g.getFontMetrics().getHeight() - g.getFontMetrics().getMaxDescent()), tabbWidth, g.getFontMetrics().getHeight() + 1);
			}
			g.translate(tabbWidth + 2, 0);
		}
//		g.drawLine(0, g.getFontMetrics().getMaxDescent() - 1, width, g.getFontMetrics().getMaxDescent() - 1);
		g.setTransform(origin);
	}
	
	protected void drawBufferWithLineNumbers(Graphics2D g, ByteBuffer buffer, Viewer viewer, int width, int height) {
		AffineTransform origin = g.getTransform();
		g.translate(0, g.getFontMetrics().getHeight());
		if (viewer.cursorLine < viewer.startLine) {
			viewer.startLine = viewer.cursorLine;
		}
		if (viewer.cursorLine > viewer.endLine) {
			viewer.startLine = Math.max(0, viewer.cursorLine - (height / g.getFontMetrics().getHeight()));
		}
		viewer.endLine = Math.min(viewer.startLine + (height / g.getFontMetrics().getHeight()), getNumLines() - 1);
		int marginSize = (int) g.getFontMetrics().getStringBounds(Integer.toString(viewer.endLine), g).getWidth() + 2;
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, marginSize, height);
		g.setColor(Color.LIGHT_GRAY);
//		g.drawLine(marginSize - 1, 0, marginSize - 1, height);
//		g.setColor(Color.LIGHT_GRAY);
//		g.fillRect(0, 0, marginSize, getHeight());
		g.setColor(Color.GRAY);
		for (int i = 0 ; i <= viewer.endLine - viewer.startLine ; i ++) {
			g.translate(0, g.getFontMetrics().getHeight());
			g.drawString(Integer.toString(viewer.startLine + i), 0, 0);
		}
		
		g.setTransform(origin);
		g.translate(marginSize, g.getFontMetrics().getHeight());
		drawBuffer(g, buffer, viewer, width - marginSize, height);
		g.setTransform(origin);
	}

	private void drawBuffer(Graphics2D g, ByteBuffer buffer, Viewer viewer, int width, int height) {
		AffineTransform origin = g.getTransform();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		g.setColor(Color.LIGHT_GRAY);
		g.drawRect(0, 0, width, height);
		
		g.translate(0, g.getFontMetrics().getHeight());
		g.setColor(Color.BLACK);
		ArrayList<SyntaxHilight> hilights = SyntaxHilights.getHilights(buffer);
		Iterator<SyntaxHilight> it = hilights.iterator();
		SyntaxHilight next = null;
		if (it.hasNext()) {
			next = it.next();
		}
		int i = 0;
		ByteBuffer cb = buffer;
		cb.position(0);
		int lineNumber = 0;
		for (; i < cb.limit() ; i ++) {
			if (lineNumber >= viewer.startLine) {
				break;
			}
			char c = (char) cb.get();
			if (c == '\n') {
				lineNumber ++;
			}
		}
		double closestDist = Double.MAX_VALUE;
		int closestLine = 0, closestColumn = 0;
		Point2D closestPos = new Point();
		
		AffineTransform lastLine = g.getTransform();
		int column = 0;
		for (; i < cb.limit() ; i ++) {
			while (next != null && next.start <= i) {
				g.setColor(next.color);
				if (it.hasNext()) {
					next = it.next();
				} else {
					next = null;
				}
			}
			if (moveCursorTo.get()) {
				Point2D p = g.getTransform().transform(new Point(), null);
				if (Math.abs(p.getY() - moveCursorToPoint.getY()) < g.getFontMetrics().getHeight()) {
					double dist = p.distance(moveCursorToPoint);
					if (dist < closestDist) {
//						System.out.println("p:" + p);
						closestDist = dist;
						closestColumn = column;
						closestLine = lineNumber;
						closestPos = p;
						viewer.cursorLine = closestLine;
						viewer.cursorColumn = closestColumn;
					}
				}
			}
			char c = (char) cb.get();
			if (!moveCursorTo.get()) {
				if (lineNumber == viewer.cursorLine && column == viewer.cursorColumn) {
					drawCursor(g);
				}
			}
			column ++;
			if (c == '\n') {
				g.setTransform(lastLine);
				g.translate(0, g.getFontMetrics().getHeight());
				lastLine = g.getTransform();
				lineNumber ++;
				column = 0;
			} else if (c == '\t') {
				g.translate(g.getFontMetrics().charWidth(' ') * 4, 0);
			} else {
				g.drawString(Character.toString(c), 0, 0);
				g.translate(g.getFontMetrics().charWidth(c), 0);
			}
			if (lineNumber > viewer.endLine) {
				break;
			}
		}
		if (moveCursorTo.get()) {
			moveCursorTo.set(false);
			g.setTransform(new AffineTransform());
			g.translate(closestPos.getX(), closestPos.getY());
			drawCursor(g);
		} else {
			if (lineNumber == viewer.cursorLine && column == viewer.cursorColumn) {
				drawCursor(g);
			}
		}
		showSuggestions(g);
		g.setTransform(origin);
	}
	
	Point2D lastCursorPos = null;

	private void drawCursor(Graphics2D g) {
		Color color = g.getColor();
		g.setColor(Color.BLACK);
		int cursorHeight = g.getFontMetrics().getHeight() - g.getFontMetrics().getMaxDescent();
		g.fillRect(0, -cursorHeight, 2, cursorHeight + g.getFontMetrics().getMaxDescent());
		g.setColor(color);
		lastCursorPos = g.getTransform().transform(new Point(), null);
//		showSuggestions(g);
	}

	protected void showSuggestions(Graphics2D g) {
		AffineTransform origin = g.getTransform();
		if (lastCursorPos != null) {
			g.setTransform(new AffineTransform());
			g.translate(lastCursorPos.getX(), lastCursorPos.getY());
		}
		if (showSuggestions.get()) {
			if (viewer.sugestionsCursor < viewer.sugestionsPageStart) {
				viewer.sugestionsPageStart = viewer.sugestionsCursor;
			}
			viewer.sugestionsPageEnd = Math.min(viewer.sugestionsPageStart + 10, suggestions.size());
			if (viewer.sugestionsCursor > viewer.sugestionsPageEnd) {
				viewer.sugestionsPageEnd = viewer.sugestionsCursor;
				viewer.sugestionsPageStart = viewer.sugestionsPageEnd - 10;
			}
			viewer.sugestionsPageEnd = Math.min(viewer.sugestionsPageStart + 10, suggestions.size());
			
			Color color = g.getColor();
			AffineTransform trans = g.getTransform();
			
			Suggestion maxString = suggestions.stream().max((a,b) -> Integer.compare(a.text.length() + a.more.length(), b.text.length() + b.more.length())).orElse(new Suggestion("", ""));
			//int maxSuggestionLength = maxString.length()
			int width = (int) g.getFontMetrics().getStringBounds(maxString.text + ' ' + maxString.more, g).getWidth() + 2;
			int height = ((viewer.sugestionsPageEnd - viewer.sugestionsPageStart) + 1) * g.getFontMetrics().getHeight() + g.getFontMetrics().getMaxDescent();
			g.setColor(Color.MAGENTA);
			g.fillRect(0, 0, width, height);
			
			g.translate(1, 0);
			g.setColor(Color.BLACK);
			int i = 0;
			for (Suggestion suggestion : suggestions) {
				if (i >= viewer.sugestionsPageStart && i <= viewer.sugestionsPageEnd) {
					g.translate(0, g.getFontMetrics().getHeight());
					g.setColor(Color.BLACK);
					g.drawString(suggestion.text, 0, 0);
					g.setColor(Color.WHITE);
					g.drawString(suggestion.more, g.getFontMetrics().stringWidth(suggestion.text), 0);
					if (i == viewer.sugestionsCursor) {
						g.setXORMode(Color.CYAN);
						int selectHeight = g.getFontMetrics().getHeight() - g.getFontMetrics().getMaxDescent();
						g.fillRect(-1, -selectHeight, width, selectHeight + g.getFontMetrics().getMaxDescent());
						g.setPaintMode();
					}
				}
				if (i ++ >= viewer.sugestionsPageEnd) {
					break;
				}
			}
			
			g.setTransform(trans);
			g.setColor(color);
		}
		g.setTransform(origin);
	}

	public void showSuggestions(ArrayList<Suggestion> suggestions) {
		this.suggestions = suggestions;
		viewer.sugestionsPageStart = 0;
		showSuggestions.set(true);
	}

	public int getNumLines() {
		int i = 0;
		ByteBuffer cb = buffer;
		cb.position(0);
		int lineNumber = 1;
		for (; i < cb.limit() ; i ++) {
			char c = (char) cb.get();
			if (c == '\n') {
				lineNumber ++;
			}
		}
		return lineNumber;
	}

	public int getLineLength(int line) {
		int i = 0;
		ByteBuffer cb = buffer;
		cb.position(0);
		int lineNumber = 0;
		for (; i < cb.limit() ; i ++) {
			if (lineNumber >= line) {
				break;
			}
			char c = (char) cb.get();
			if (c == '\n') {
				lineNumber ++;
			}
		}
		int column = 0;
		for (; i < cb.limit() ; i ++) {
			char c = (char) cb.get();
			column ++;
			if (c == '\n') {
				break;
			}
		}
//		if (i >= cb.limit()) {
//			column ++;
//		}
		return column;
	}
	
	public int getOffset(int line, int column) {
		int i = 0;
		ByteBuffer cb = buffer;
		cb.position(0);
		int lineNumber = 0;
		int more = 0;
		for (; i < cb.limit() ; i ++) {
			if (lineNumber >= line) {
				break;
			}
			char c = (char) cb.get();
			if (c == '\n') {
				lineNumber ++;
			}
		}
		int col = 0;
		for (; i < cb.limit() ; i ++) {
			if (col >= column) {
				break;
			}
			char c = (char) cb.get();
			col ++;
			if (c == '\n') {
				break;
			}
		}
		return i + more;
	}
	
	public int getStartOfLineOffset(int line, int column) {
		int i = 0;
		ByteBuffer cb = buffer;
		cb.position(0);
		int lineNumber = 0;
		for (; i < cb.limit() ; i ++) {
			if (lineNumber >= line) {
				break;
			}
			char c = (char) cb.get();
			if (c == '\n') {
				lineNumber ++;
			}
		}
		return i;
	}
	
	public int getTabsOnLineBefore(int line, int column) {
		int i = 0;
		ByteBuffer cb = buffer;
		cb.position(0);
		int lineNumber = 0;
		int more = 0;
		for (; i < cb.limit() ; i ++) {
			if (lineNumber >= line) {
				break;
			}
			char c = (char) cb.get();
			if (c == '\n') {
				lineNumber ++;
			}
		}
		int col = 0;
		int count = 0;
		for (; i < cb.limit() ; i ++) {
			if (col >= column) {
				break;
			}
			char c = (char) cb.get();
			col ++;
			if (c == '\n') {
				break;
			}
			if (c == '\t') {
				count ++;
			}
		}
		return count;
	}

	public void insertCharacterAtCursorAndIncrementCursor(char c) {
		int offset = getOffset(viewer.cursorLine, viewer.cursorColumn);
		ByteBuffer next = ByteBuffer.allocate(buffer.limit() + 1);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		WritableByteChannel channel = Channels.newChannel(out);
		buffer.position(0);
		try {
			byte[] before = new byte[offset];
			buffer.get(before);
			channel.write(ByteBuffer.wrap(before));
			channel.write(ByteBuffer.wrap(new byte[]{(byte) c}));
			byte[] after = new byte[buffer.remaining()];
			buffer.get(after);
			channel.write(ByteBuffer.wrap(after));
			channel.close();
			viewer.cursorColumn ++;
		} catch (IOException e) {
			e.printStackTrace();
		}
		buffer = ByteBuffer.wrap(out.toByteArray());
		modified.put(currentBuffer, new AtomicBoolean(true));
	}

	public void insertStringAtCursorAndIncrementCursor(String str) {
		int offset = getOffset(viewer.cursorLine, viewer.cursorColumn);
		ByteBuffer next = ByteBuffer.allocate(buffer.limit() + 1);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		WritableByteChannel channel = Channels.newChannel(out);
		buffer.position(0);
		try {
			byte[] before = new byte[offset];
			buffer.get(before);
			channel.write(ByteBuffer.wrap(before));
			channel.write(ByteBuffer.wrap(str.getBytes()));
			byte[] after = new byte[buffer.remaining()];
			buffer.get(after);
			channel.write(ByteBuffer.wrap(after));
			channel.close();
			viewer.cursorColumn += str.length();
		} catch (IOException e) {
			e.printStackTrace();
		}
		buffer = ByteBuffer.wrap(out.toByteArray());
		modified.put(currentBuffer, new AtomicBoolean(true));
	}
	
	public static Editor editor; { editor = this; }
	
	public static ArrayList<Runnable> onSelect = null;
	
	{
		onSelect = new ArrayList<Runnable>();
	}

	public static void addOnSelect(Runnable runnable) {
		onSelect.add(runnable);
	}

}
