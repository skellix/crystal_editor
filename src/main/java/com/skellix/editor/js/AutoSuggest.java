package com.skellix.editor.js;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class AutoSuggest {
	
//	public static void main(String[] args) {
//		ByteBuffer buffer = ByteBuffer.wrap("var foo = {bar: {baz: 5}};\nfoo.bar.".getBytes());
//		AutoSuggest.getOptionsForCompletingFieldOrMethod(buffer, buffer.limit() - 1);
//	}

	public static ArrayList<Suggestion> getOptionsForCompletingFieldOrMethod(ByteBuffer buffer, int offset) {
		ArrayList<Suggestion> out = new ArrayList<Suggestion>();
		int end = offset;
		int start = end;
		for (; start >= 0 ; start --) {
			buffer.position(start);
			char c = (char) buffer.get();
			if (c == ')') {
				AtomicInteger index = new AtomicInteger(start);
				ContextSearch.backwardsIgnoreBlock(buffer, index);
				start = index.get();
			} else if (!(Character.isJavaIdentifierPart(c) || c == '.')) {
				start ++;
				break;
			}
		}
		int length = (end - start) + 1;
		buffer.position(start);
		byte[] data = new byte[length];
		buffer.get(data);
		final String hoverStr = new String(data);
		if (hoverStr.indexOf('.') >= 0) {
			String firstPart = hoverStr.substring(0, hoverStr.indexOf('.'));
			if (firstPart.indexOf('(') >= 0) {
				firstPart = firstPart.substring(0, firstPart.indexOf('('));
			}
			ContextSearch contextSearch = new ContextSearch(buffer, start - 1);
			contextSearch.addMatchCase(RegexCommon.regexBlankBefore + firstPart + "\\s*=\\s*", new ContextSearchCallback() {
				
				@Override
				public void onMatchFound(ByteBuffer buffer, int start, int end) {
					int length = end - start;
					byte[] data = new byte[length];
					buffer.position(start);
					buffer.get(data);
					String str = new String(data);
//					System.out.printf("str: %s%n", str);
					buffer.position(end);
					char c = (char) buffer.get();
					if (c == '{') {
//						System.out.println("is object");
						ContextSearch contextSearch = new ContextSearch(buffer, end);
						contextSearch.recursiveJSObjectMatch(hoverStr, new ContextSearchCallback() {
							
							@Override
							public void onMatchFound(ByteBuffer buffer, int start, int end) {
								int length = end - start;
								byte[] data = new byte[length];
								buffer.position(start);
								buffer.get(data);
								String str = new String(data);
								out.add(new Suggestion(str));
							}
						});
					} else if (c == 'J') {
						String lookFor = "Java.type";
						int tryEnd = end + lookFor.length();
						if (tryEnd < buffer.limit()) {
							byte[] data1 = new byte[lookFor.length()];
							buffer.position(end);
							buffer.get(data1);
							if (new String(data1).equals(lookFor)) {
								int i = tryEnd;
								for (; i < buffer.limit() ; i ++) {
									buffer.position(i);
									char c1 = (char) buffer.get();
									if (c1 == '(') {
										for (i ++ ; i < buffer.limit() ; i ++) {
											buffer.position(i);
											c1 = (char) buffer.get();
											if (c1 == '"' || c1 == '\'') {
												String found = ContextSearch.getString(buffer, new AtomicInteger(i));
												if (found != null) {
													found = found.substring(1, found.length() - 1);
//													System.out.printf("java type: %s%n", found);
													try {
														ContextSearch.recursiveJavaObjectMatch(hoverStr, Class.forName(found), new ContextSearchMoreCallback() {
															
															@Override
															public void onMatchFound(String text, String more) {
//																System.out.printf("auto: %s | %s%n", text, more);
																out.add(new Suggestion(text, more));
															}
														});
													} catch (ClassNotFoundException e) {
														e.printStackTrace();
													}
													return;
												}
											}
										}
									}
								}
							}
						}
					}
				}
			});
			contextSearch.runToStartOfDocument();
			System.out.print("");
		}
		return out;
	}

	public static ArrayList<Suggestion> getOptionsForCompletingVarName(ByteBuffer buffer, int offset) {
		ArrayList<Suggestion> out = new ArrayList<Suggestion>();
		int end = offset;
		int start = end;
		for (; start >= 0 ; start --) {
			buffer.position(start);
			char c = (char) buffer.get();
			if (!(Character.isJavaIdentifierPart(c) || c == '.')) {
				start ++;
				break;
			}
		}
		int length = (end - start) + 1;
		buffer.position(start);
		byte[] data = new byte[length];
		buffer.get(data);
		final String hoverStr = new String(data);
		ContextSearch contextSearch = new ContextSearch(buffer, start - 1);
		contextSearch.addMatchCase(RegexCommon.regexBlankBefore + "var\\s+" + hoverStr + RegexCommon.isJavaIdentifierPart + "*", new ContextSearchCallback() {
			
			@Override
			public void onMatchFound(ByteBuffer buffer, int start, int end) {
				int length = end - start;
				byte[] data = new byte[length];
				buffer.position(start);
				buffer.get(data);
				String str = new String(data).replaceFirst("var\\s+", "");
//				System.out.printf("auto new: %s%n", str);
				out.add(new Suggestion(str));
			}
		});
		contextSearch.runToStartOfDocument();
		return out;
	}

	public static ArrayList<Suggestion> getOptionsForCompletingJavaClass(ByteBuffer buffer, final int offset) {

		Object o = System.getProperties();
		File JAVA_HOME = new File(System.getenv("JAVA_HOME"));
		ArrayList<Suggestion> out = new ArrayList<Suggestion>();
		
		final int start = getStartOfStringBeforeCursor(buffer, offset);
		
		final String stringBeforeCursor = getString(buffer, start, offset);
		
		String[] parts = stringBeforeCursor.split("\\.");
		
		for (String str : System.getProperty("java.class.path").split(File.pathSeparator)) {
			if (str.endsWith(".jar")) {
				File jar = new File(str);
				try {
					JarFile jarFile = new JarFile(jar);
					Enumeration<JarEntry> entries = jarFile.entries();
					while (entries.hasMoreElements()) {
						JarEntry entry = entries.nextElement();
						String name = entry.getName();
						if (!name.endsWith("/")) {
							name = name.replaceAll("/", ".").replaceAll("\\.class$", "");
							if (name.replaceAll("[^\\.]+\\.", "").startsWith(stringBeforeCursor)) {
								out.add(new Suggestion(name));
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		for (File jar : recursiveFindJar(JAVA_HOME)) {
//			System.out.println(jar.getAbsolutePath());
			try {
				JarFile jarFile = new JarFile(jar);
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String name = entry.getName();
					if (!name.endsWith("/")) {
						name = name.replaceAll("/", ".").replaceAll("\\.class$", "");
						if (name.replaceAll("[^\\.]+\\.", "").startsWith(stringBeforeCursor)) {
							out.add(new Suggestion(name));
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return out;
	}
	
	private static ArrayList<File> recursiveFindJar(File file) {
		ArrayList<File> out = new ArrayList<File>();
		for (File child : file.listFiles()) {
			if (child.isDirectory()) {
				out.addAll(recursiveFindJar(child));
			} else if (child.getName().endsWith(".jar")) {
				out.add(child);
			}
		}
		return out;
	}

	public static String getString(ByteBuffer buffer, int start, int end) {
		int length = (end - start) + 1;
		buffer.position(start);
		byte[] data = new byte[length];
		buffer.get(data);
		final String hoverStr = new String(data);
		return hoverStr;
	}

	public static String getStringBeforeCursor(ByteBuffer buffer, int end) {
		int start = getStartOfStringBeforeCursor(buffer, end);
		int length = (end - start) + 1;
		buffer.position(start);
		byte[] data = new byte[length];
		buffer.get(data);
		final String hoverStr = new String(data);
		return hoverStr;
	}

	public static int getStartOfStringBeforeCursor(ByteBuffer buffer, int offset) {
		int end = offset;
		int start = end;
		for (; start >= 0 ; start --) {
			buffer.position(start);
			char c = (char) buffer.get();
			if (!Character.isJavaIdentifierPart(c) || c == '.') {
				start ++;
				break;
			}
		}
		return start;
	}

	public static Collection<? extends Suggestion> getOptionsForCompletingImport(ByteBuffer buffer, int offset) {
		ArrayList<Suggestion> out = new ArrayList<Suggestion>();
		
		int start = getStartOfStringBeforeCursor(buffer, offset);
		
		final String stringBeforeCursor = getString(buffer, start, offset);
		
		Package[] packages = Package.getPackages();
		for (Package pack : packages) {
			String str = pack.getName();
			if (str.startsWith(stringBeforeCursor)) {
				out.add(new Suggestion(str));
			}
		}
		return out;
	}

}
