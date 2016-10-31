package com.skellix.editor.js;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHilights {
	
	public static Color COLOR_KEYWORD = Color.MAGENTA;
	public static Color COLOR_CONSTANT = Color.RED;
	public static Color COLOR_COMMENT = Color.BLUE;

	public static ArrayList<SyntaxHilight> getHilights(ByteBuffer buffer) {
		ArrayList<SyntaxHilight> hilights = new ArrayList<SyntaxHilight>();
		{
			Matcher matcher = Pattern.compile(RegexCommon.regexBlankBefore + "(break|do|instanceof|typeof|case|else|new|var|catch|finally|return|void|continue|for|switch|while|debugger|function|this|with|default|if|throw|delete|in|try|abstract|export|interface|static|boolean|extends|long|super|byte|final|native|synchronized|char|float|package|throws|class|goto|private|transient|const|implements|protected|volatile|double|import|public|enum|int|short|null|true|false)" + RegexCommon.regexBlankAfter).matcher(new BufferCharSequence(buffer));
			while (matcher.find()) {
				hilights.add(new SyntaxHilight(matcher.start(), COLOR_KEYWORD));
				hilights.add(new SyntaxHilight(matcher.end(), Color.BLACK));
			}
		}
		{
			Matcher matcher = Pattern.compile(RegexCommon.regexBlankBefore + "((?:\\+\\-)?\\d+(:?\\.\\d+)?)" + RegexCommon.regexBlankAfter).matcher(new BufferCharSequence(buffer));
			while (matcher.find()) {
				hilights.add(new SyntaxHilight(matcher.start(), COLOR_CONSTANT));
				hilights.add(new SyntaxHilight(matcher.end(), Color.BLACK));
			}
		}
		{
			Matcher matcher = Pattern.compile(RegexCommon.regexBlankBefore + "(0x\\p{XDigit}+)" + RegexCommon.regexBlankAfter).matcher(new BufferCharSequence(buffer));
			while (matcher.find()) {
				hilights.add(new SyntaxHilight(matcher.start(), COLOR_CONSTANT));
				hilights.add(new SyntaxHilight(matcher.end(), Color.BLACK));
			}
		}
		{
			Matcher matcher = Pattern.compile(RegexCommon.regexBlankBefore + "//[^\n]*").matcher(new BufferCharSequence(buffer));
			while (matcher.find()) {
				hilights.add(new SyntaxHilight(matcher.start(), COLOR_COMMENT));
				hilights.add(new SyntaxHilight(matcher.end(), Color.BLACK));
			}
		}
		buffer.position(0);
		boolean escape = false;
		while (buffer.remaining() > 0) {
			byte c = buffer.get();
			if (c == (byte) '"') {
				int start = buffer.position() - 1;
				while (buffer.remaining() > 0) {
					c = buffer.get();
					if (escape) {
						escape = false;
					} else if (c == (byte) '\\') {
						escape = true;
					} else if (c == (byte) '"') {
						int end = buffer.position();
						hilights.add(new SyntaxHilight(start, COLOR_CONSTANT));
						hilights.add(new SyntaxHilight(end, Color.BLACK));
						break;
					}
				}
			}
		}
		buffer.position(0);
		escape = false;
		while (buffer.remaining() > 0) {
			byte c = buffer.get();
			if (c == (byte) '\'') {
				int start = buffer.position() - 1;
				while (buffer.remaining() > 0) {
					c = buffer.get();
					if (escape) {
						escape = false;
					} else if (c == (byte) '\\') {
						escape = true;
					} else if (c == (byte) '\'') {
						int end = buffer.position();
						hilights.add(new SyntaxHilight(start, COLOR_CONSTANT));
						hilights.add(new SyntaxHilight(end, Color.BLACK));
						break;
					}
				}
			}
		}
		{
			Matcher matcher = Pattern.compile(RegexCommon.regexBlankBefore + "/\\*(?:.|\n)*\\*/").matcher(new BufferCharSequence(buffer));
			while (matcher.find()) {
				
				for (Iterator<SyntaxHilight> it = hilights.iterator() ; it.hasNext() ;) {
					SyntaxHilight next = it.next();
					if (next.start >= matcher.start() && next.start <= matcher.end()) {
						it.remove();
					}
				}
				hilights.add(new SyntaxHilight(matcher.start(), COLOR_COMMENT));
				hilights.add(new SyntaxHilight(matcher.end(), Color.BLACK));
			}
		}
		hilights.sort((a,b) -> {
			return Integer.compare(a.start, b.start);
		});
		return hilights;
	}

}
