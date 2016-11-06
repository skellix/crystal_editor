package com.skellix.editor.js;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContextSearch {

	private ByteBuffer buffer;
	private int start;
	
	HashMap<String, ContextSearchCallback> matchCases = new HashMap<String, ContextSearchCallback>();

	public ContextSearch(ByteBuffer buffer, int start) {
		this.buffer = buffer;
		this.start = start;
	}

	public void addMatchCase(String regex, ContextSearchCallback contextSearchCallback) {
		matchCases.put(regex, contextSearchCallback);
	}
	
	public void runToStartOfDocument() {
		ContextSection section = new ContextSection();
		section.end = start;
		AtomicInteger index = new AtomicInteger(start);
		for (; index.get() >= 0 ; index.getAndDecrement()) {
			buffer.position(index.get());
			char c = (char) buffer.get();
			if (c == ')' || c == '}' || c == ']') {
				section.start = index.get() + 1;
				compareCasesWithSection(section);
				backwardsIgnoreBlock(buffer, index);
				section.end = index.get() - 1;
			}
			if (c == '(' || c == '{' || c == '[') {
				section.start = index.get() + 1;
				compareCasesWithSection(section);
				section.end = index.get() - 1;
			}
		}
		section.start = index.get() + 1;
		compareCasesWithSection(section);
	}
	
	public static void backwardsIgnoreBlock(ByteBuffer buffer, AtomicInteger index) {
		int depth = 0;
		for (; index.get() >= 0 ; index.getAndDecrement()) {
			buffer.position(index.get());
			char c = (char) buffer.get();
			if (c == ')' || c == '}' || c == ']') {
				depth ++;
			}
			if (c == '(' || c == '{' || c == '[') {
				depth --;
			}
			if (depth == 0) {
				return;
			}
		}
	}
	
	public void runUntilEndOfBlock() {
		ContextSection section = new ContextSection();
		section.start = start;
		AtomicInteger index = new AtomicInteger(start);
		for (; index.get() < buffer.limit() ; index.getAndIncrement()) {
			buffer.position(index.get());
			char c = (char) buffer.get();
			if (c == '(' || c == '{' || c == '[') {
				section.end = index.get() - 1;
				compareCasesWithSection(section);
				forewardsIgnoreBlock(index);
				section.start = index.get() + 1;
			}
			if (c == ')' || c == '}' || c == ']') {
//				section.start = index.get() + 1;
//				compareCasesWithSection(section);
//				section.end = index.get() - 1;
				break;
			}
		}
		section.end = index.get() - 1;
		compareCasesWithSection(section);
	}
	
	public void matchUntilEndOfContext() {
		matchUntilEndOfContext(new AtomicInteger(start));
	}
	
	public void matchUntilEndOfContext(AtomicInteger index) {
		ContextSection section = new ContextSection();
		section.start = index.get();
		for (; index.get() < buffer.limit() ; index.getAndIncrement()) {
			buffer.position(index.get());
			char c = (char) buffer.get();
			if (c == '(' || c == '{' || c == '[') {
				section.end = index.get() - 1;
				if (section.start < buffer.limit() && section.end < buffer.limit()) {
					compareCasesWithSection(section);
				}
				index.getAndIncrement();
				matchUntilEndOfContext(index);
				index.getAndIncrement();
				section.start = index.get() + 1;
			}
			if (c == ')' || c == '}' || c == ']') {
//				section.start = index.get() + 1;
//				compareCasesWithSection(section);
//				section.end = index.get() - 1;
				break;
			}
		}
		section.end = index.get() - 1;
		if (section.start < buffer.limit() && section.end < buffer.limit()) {
			compareCasesWithSection(section);
		}
	}
	
	private void forewardsIgnoreBlock(AtomicInteger index) {
		int depth = 0;
		for (; index.get() >= 0 ; index.getAndIncrement()) {
			buffer.position(index.get());
			char c = (char) buffer.get();
			if (c == '(' || c == '{' || c == '[') {
				depth ++;
			}
			if (c == ')' || c == '}' || c == ']') {
				depth --;
			}
			if (depth == 0) {
				return;
			}
		}
	}

	private void compareCasesWithSection(ContextSection section) {
		int length = (section.end - section.start) + 1;
		byte[] data = new byte[length];
		buffer.position(section.start);
		buffer.get(data);
		String str = new String(data);
		for (String regex : matchCases.keySet()) {
			Matcher matcher = Pattern.compile(regex).matcher(str);
			while (matcher.find()) {
				matchCases.get(regex).onMatchFound(buffer, section.start + matcher.start(), section.start + matcher.end());
			}
		}
	}
	
	public static String getString(ByteBuffer buffer, AtomicInteger index) {
		buffer.position(index.get());
		boolean escape = false;
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
					int length = end - start;
					byte[] data = new byte[length];
					buffer.position(start);
					buffer.get(data);
					index.set(end);
					return new String(data);
				}
			}
		} else if (c == (byte) '\'') {
			int start = buffer.position() - 1;
			while (buffer.remaining() > 0) {
				c = buffer.get();
				if (escape) {
					escape = false;
				} else if (c == (byte) '\\') {
					escape = true;
				} else if (c == (byte) '\'') {
					int end = buffer.position();
					int length = end - start;
					byte[] data = new byte[length];
					buffer.position(start);
					buffer.get(data);
					index.set(end);
					return new String(data);
				}
			}
		}
		return null;
	}

	/**
	 * {@link #start} should be the index of the opening curly bracket for the object declaration
	 * 
	 * @param chainStr
	 * @param contextSearchCallback
	 */
	public void recursiveJSObjectMatch(String chainStr, ContextSearchCallback contextSearchCallback) {
		String chainNext = chainStr;
		if (chainNext.indexOf('.') >= 0) {
			chainNext = chainNext.substring(chainNext.indexOf('.') + 1);
		}
		final String transferChainNext = chainNext;
		if (chainNext.indexOf('.') >= 0) {
			String firstPart = chainNext.substring(0, chainNext.indexOf('.'));
			ContextSearch contextSearch = new ContextSearch(buffer, start + 1);
			contextSearch.addMatchCase(
					RegexCommon.regexBlankBefore + firstPart + "\\s*:\\s*"
					, new ContextSearchCallback() {
				
				@Override
				public void onMatchFound(ByteBuffer buffer, int start, int end) {
					buffer.position(end);
					char c = (char) buffer.get();
					if (c == '{') {
//						System.out.println("is object");
						ContextSearch contextSearch = new ContextSearch(buffer, end);
						contextSearch.recursiveJSObjectMatch(transferChainNext, contextSearchCallback);
						System.out.print("");
					}
				}
			});
			contextSearch.runUntilEndOfBlock();
		} else {
			ContextSearch contextSearch = new ContextSearch(buffer, start + 1);
			contextSearch.addMatchCase(
					RegexCommon.regexBlankBefore + RegexCommon.isJavaIdentifierStart + RegexCommon.isJavaIdentifierPart + "*\\s*(?=:)"
					, new ContextSearchCallback() {
						
						@Override
						public void onMatchFound(ByteBuffer buffer, int start, int end) {
							int length = end - start;
							byte[] data = new byte[length];
							buffer.position(start);
							buffer.get(data);
							String str = new String(data);
							if (str.startsWith(transferChainNext)) {
								contextSearchCallback.onMatchFound(buffer, start, end);
							}
						}
					});
			contextSearch.runUntilEndOfBlock();
		}
	}

	public static void recursiveJavaObjectMatch(String chainStr, Class<?> clazz, ContextSearchMoreCallback contextSearchCallback) {
		String chainNext = chainStr;
		if (chainNext.indexOf('.') >= 0) {
			chainNext = chainNext.substring(chainNext.indexOf('.') + 1);
		}
		final String transferChainNext = chainNext;
		if (chainNext.indexOf('.') >= 0) {
			String firstPart = chainNext.substring(0, chainNext.indexOf('.'));
			System.out.print("");
		} else {
			for (; clazz != null ; clazz = clazz.getSuperclass()) {
				for (Field field : clazz.getDeclaredFields()) {
					String name = field.getName();
					if (name.startsWith(chainNext)) {
						contextSearchCallback.onMatchFound(name, "");
					}
				}
				for (Field field : clazz.getFields()) {
					String name = field.getName();
					if (name.startsWith(chainNext)) {
						contextSearchCallback.onMatchFound(name, "");
					}
				}
				for (Method method : clazz.getDeclaredMethods()) {
					Matcher matcher = Pattern.compile("^[^\\s]+\\s+([^\\s]+)\\s+[^\\s]+\\.([^\\s]+)(\\([^\\s]+).*$").matcher(method.toGenericString());
					if (matcher.find()) {
						String returnType = matcher.group(1);
						String name = matcher.group(2);
						String params = matcher.group(3);
						StringBuilder jsParam = new StringBuilder();
						for (Parameter parameter : method.getParameters()) {
							jsParam.append(parameter.getName());
							jsParam.append(',');
						}
						if (jsParam.length() > 0) {
							jsParam.setLength(jsParam.length() - 1);
						}
						if (name.startsWith(chainNext)) {
							contextSearchCallback.onMatchFound(name + "(" + jsParam.toString() + ")", params + " " + returnType);
						}
					}
				}
				for (Method method : clazz.getMethods()) {
					Matcher matcher = Pattern.compile("^[^\\s]+\\s+([^\\s]+)\\s+[^\\s]+\\.([^\\s]+)(\\([^\\s]+).*$").matcher(method.toGenericString());
					if (matcher.find()) {
						String returnType = matcher.group(1);
						String name = matcher.group(2);
						String params = matcher.group(3);
						StringBuilder jsParam = new StringBuilder();
						for (Parameter parameter : method.getParameters()) {
							jsParam.append(parameter.getName());
							jsParam.append(',');
						}
						if (jsParam.length() > 0) {
							jsParam.setLength(jsParam.length() - 1);
						}
						if (name.startsWith(chainNext)) {
							contextSearchCallback.onMatchFound(name + "(" + jsParam.toString() + ")", params + " " + returnType);
						}
					}
				}
				System.out.print("");
			}
		}
	}

	private static String getMethodArgsString(Method method) {
		StringBuilder sb = new StringBuilder();
		sb.append('(');
//		for (method.) {
//			//	
//		}
		sb.append(')');
		return null;
	}

}
