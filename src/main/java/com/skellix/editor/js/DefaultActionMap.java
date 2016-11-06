package com.skellix.editor.js;

import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JFileChooser;

public class DefaultActionMap extends ActionMap {

	private Editor main;

	public DefaultActionMap(Editor main) {
		this.main = main;
	}

	{
		put("right", new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int startColumn = main.viewer.cursorColumn;
				main.viewer.cursorColumn ++;
				if (main.viewer.cursorColumn >= main.getLineLength(main.viewer.cursorLine)) {
					main.viewer.cursorColumn = 0;
					main.viewer.cursorLine ++;
				}
				int numLines = main.getNumLines();
				if (main.viewer.cursorLine >= numLines) {
					main.viewer.cursorLine --;
					main.viewer.cursorColumn = main.getLineLength(main.viewer.cursorLine);
				}
				main.showSuggestions.set(false);
				main.contentPane.revalidate();
				main.contentPane.repaint();
			}
		});
		put("left", new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int startColumn = main.viewer.cursorColumn;
				main.viewer.cursorColumn --;
				if (main.viewer.cursorColumn < 0 && main.viewer.cursorLine > 0) {
					main.viewer.cursorLine --;
					main.viewer.cursorColumn = main.getLineLength(main.viewer.cursorLine) - 1;
				}
				main.showSuggestions.set(false);
				main.contentPane.revalidate();
				main.contentPane.repaint();
			}
		});
		put("up", new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (main.showSuggestions.get()) {
					main.viewer.sugestionsCursor --;
					if (main.viewer.sugestionsCursor < 0) {
						main.viewer.sugestionsCursor = main.suggestions.size() + main.viewer.sugestionsCursor;
					}
				} else {
					int startColumn = main.viewer.cursorColumn;
					int startLine = main.viewer.cursorLine;
					if (main.viewer.cursorLine > 0) {
						int startTabs = main.getTabsOnLineBefore(main.viewer.cursorLine, main.viewer.cursorColumn);
						int startTabOffset = startTabs * 3;
						main.viewer.cursorLine --;
						int endTabs = main.getTabsOnLineBefore(main.viewer.cursorLine, main.viewer.cursorColumn);
						int endTabOffset = endTabs * 3;
						int lineLength = main.getLineLength(main.viewer.cursorLine) - 1;
						main.viewer.cursorColumn += startTabOffset;
						main.viewer.cursorColumn -= endTabOffset;
						if (lineLength < main.viewer.cursorColumn) {
							main.viewer.cursorColumn = lineLength;
						}
					}
				}
				main.contentPane.revalidate();
				main.contentPane.repaint();
			}
		});
		put("down", new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (main.showSuggestions.get()) {
					main.viewer.sugestionsCursor = (++ main.viewer.sugestionsCursor) % main.suggestions.size();
				} else {
					int startColumn = main.viewer.cursorColumn;
					int startLine = main.viewer.cursorLine;
					if (main.viewer.cursorLine < main.getNumLines() - 1) {
						int startTabs = main.getTabsOnLineBefore(main.viewer.cursorLine, main.viewer.cursorColumn);
						int startTabOffset = startTabs * 3;
						main.viewer.cursorLine ++;
						int endTabs = main.getTabsOnLineBefore(main.viewer.cursorLine, main.viewer.cursorColumn);
						int endTabOffset = endTabs * 3;
						main.viewer.cursorColumn += startTabOffset;
						main.viewer.cursorColumn -= endTabOffset;
						int lineLength = main.getLineLength(main.viewer.cursorLine) - 1;
						if (lineLength < main.viewer.cursorColumn) {
							main.viewer.cursorColumn = lineLength;
						}
					}
				}
				main.contentPane.revalidate();
				main.contentPane.repaint();
			}
		});
		put("end", new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				main.viewer.cursorColumn = main.getLineLength(main.viewer.cursorLine) - 1;
				main.showSuggestions.set(false);
				main.contentPane.revalidate();
				main.contentPane.repaint();
			}
		});
		put("home", new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int offset = main.getStartOfLineOffset(main.viewer.cursorLine, 0);
				int i = offset;
				int column = 0;
				for (; i < main.buffer.limit() ; i ++) {
					main.buffer.position(i);
					char c = (char) main.buffer.get();
//					if (c == '\t') {
//						column += 4;
//					} else {
						column ++;
//					}
					if (!Character.isWhitespace(c)) {
						column --;
						if (main.viewer.cursorColumn == column) {
							main.viewer.cursorColumn = 0;
						} else {
							main.viewer.cursorColumn = column;
						}
						main.showSuggestions.set(false);
						main.contentPane.revalidate();
						main.contentPane.repaint();
						return;
					}
				}
				main.viewer.cursorColumn = i;
				main.showSuggestions.set(false);
				main.contentPane.revalidate();
				main.contentPane.repaint();
			}
		});
		for (char c = ' ' ; c <= '~' ; c ++) {
			String str = Character.toString(c);
			final char ch = c;
			put(str, new AbstractAction() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					main.insertCharacterAtCursorAndIncrementCursor(ch);
					main.showSuggestions.set(false);
					main.contentPane.revalidate();
					main.contentPane.repaint();
				}
			});
		}
		for (char c : "\n\t".toCharArray()) {
			String str = Character.toString(c);
			final char ch = c;
			put(str, new AbstractAction() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (main.showSuggestions.get() && ch == '\n') {
						for (Runnable runnable : main.onSelect) runnable.run();
					} else {
						int offset = main.getOffset(main.viewer.cursorLine, main.viewer.cursorColumn);
						ByteBuffer next = ByteBuffer.allocate(main.buffer.limit() + 1);
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						WritableByteChannel channel = Channels.newChannel(out);
						main.buffer.position(0);
						try {
							byte[] before = new byte[offset];
							main.buffer.get(before);
							channel.write(ByteBuffer.wrap(before));
							channel.write(ByteBuffer.wrap(new byte[]{(byte) ch}));
							byte[] after = new byte[main.buffer.remaining()];
							main.buffer.get(after);
							channel.write(ByteBuffer.wrap(after));
							channel.close();
							if (ch == '\n') {
								main.viewer.cursorLine ++;
								main.viewer.cursorColumn = 0;
							} else {
								main.viewer.cursorColumn ++;
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						main.buffer = ByteBuffer.wrap(out.toByteArray());
						main.modified.put(main.currentBuffer, new AtomicBoolean(true));
					}
					main.showSuggestions.set(false);
					main.contentPane.revalidate();
					main.contentPane.repaint();
				}
			});
		}
		put("bs", new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int offset = main.getOffset(main.viewer.cursorLine, main.viewer.cursorColumn);
				if (offset > 0) {
					ByteBuffer next = ByteBuffer.allocate(main.buffer.limit() + 1);
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					WritableByteChannel channel = Channels.newChannel(out);
					main.buffer.position(0);
					try {
						byte[] before = new byte[offset - 1];
						main.buffer.get(before);
						main.buffer.get();
						channel.write(ByteBuffer.wrap(before));
						byte[] after = new byte[main.buffer.remaining()];
						main.buffer.get(after);
						channel.write(ByteBuffer.wrap(after));
						channel.close();
						main.viewer.cursorColumn --;
						if (main.viewer.cursorColumn < 0) {
							if (main.viewer.cursorLine > 0) {
								main.viewer.cursorLine --;
								main.viewer.cursorColumn = main.getLineLength(main.viewer.cursorLine) - 1;
							} else {
								main.viewer.cursorColumn = 0;
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					main.buffer = ByteBuffer.wrap(out.toByteArray());
					main.modified.put(main.currentBuffer, new AtomicBoolean(true));
					main.showSuggestions.set(false);
					main.contentPane.revalidate();
					main.contentPane.repaint();
				}
			}
		});
		put("auto-suggest", new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				main.onSelect.clear();
				int offset = main.getOffset(main.viewer.cursorLine, main.viewer.cursorColumn) - 1;
				ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();
				ArrayList<Suggestion> l1 = AutoSuggest.getOptionsForCompletingFieldOrMethod(main.buffer, offset);
				l1.sort((a,b) -> a.text.compareTo(b.text));
				l1.sort((a,b) -> Boolean.compare(a.text.indexOf('(') >= 0, b.text.indexOf('(') >= 0));
				suggestions.addAll(l1);
				suggestions.addAll(AutoSuggest.getOptionsForCompletingVarName(main.buffer, offset));
				suggestions.addAll(AutoSuggest.getOptionsForCompletingImport(main.buffer, offset));
				suggestions.addAll(AutoSuggest.getOptionsForCompletingJavaClass(main.buffer, offset));
				ArrayList<Suggestion> suggestionsOut = new ArrayList<Suggestion>();
				LinkedHashSet<String> lhs = new LinkedHashSet<String>();
				for (Suggestion suggestion : suggestions) {
					String str = suggestion.text + suggestion.more;
					if (!lhs.contains(str)) {
						lhs.add(str);
						suggestionsOut.add(suggestion);
					}
				}
				main.addOnSelect(new Runnable() {
					@Override
					public void run() {
						int start = AutoSuggest.getStartOfStringBeforeCursor(main.buffer, offset);
						try {
							ByteArrayOutputStream out = new ByteArrayOutputStream();
							byte[] data = new byte[start];
							main.buffer.rewind();
							main.buffer.get(data);
							out.write(data);
							String replacement = main.suggestions.get(main.viewer.sugestionsCursor).text;
							out.write(replacement.getBytes());
							int diff = (offset - start) + 1;
							main.buffer.get(new byte[diff]);
							int length = main.buffer.remaining();
							data = new byte[length];
							main.buffer.get(data);
							out.write(data);
							main.buffer = ByteBuffer.wrap(out.toByteArray());
							main.viewer.cursorColumn -= diff;
							main.viewer.cursorColumn += replacement.length();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				main.showSuggestions(suggestionsOut);
				main.contentPane.revalidate();
				main.contentPane.repaint();
			}
		});
		put("new buffer", new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				for (int i = 0 ; i < 1000 ; i ++) {
					String name = String.format("new%d.js", i);
					if (!main.buffers.containsKey(name)) {
						main.buffers.put(new File(name), ByteBuffer.allocate(0));
						break;
					}
				}
				main.contentPane.revalidate();
				main.contentPane.repaint();
			}
		});
		put("open file", new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser();
				if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					try (FileInputStream in = new FileInputStream(file)) {
						ByteBuffer buffer = ByteBuffer.allocate((int) file.length());
						ReadableByteChannel ch = Channels.newChannel(in);
						ch.read(buffer);
						ch.close();
						main.buffers.put(file, buffer);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				main.contentPane.revalidate();
				main.contentPane.repaint();
			}
		});
		put("save", new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				File out = main.saveBuffer(main.currentBuffer, main.buffer);
				if (out != null) {
					main.currentBuffer = out;
				}
				main.contentPane.revalidate();
				main.contentPane.repaint();
			}
		});
		put("run-buffer", new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ScriptEngineManager engineManager = new ScriptEngineManager();
				ScriptEngine se = engineManager.getEngineByName("js");
				try {
					String code = new String(main.buffer.array());
					se.eval(code);
				} catch (ScriptException e) {
					e.printStackTrace();
				}
				main.contentPane.revalidate();
				main.contentPane.repaint();
			}
		});
	}
}
