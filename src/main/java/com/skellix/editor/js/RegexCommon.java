package com.skellix.editor.js;

public class RegexCommon {

	public static final String regexBlankBefore = "(?<=^|\\s|\\(|\\{|\\[|\\)|\\}|\\]|,|\\+|\\-|\\*|\\/)";
	public static final String regexBlankAfter = "(?=$|\\s|\\(|\\{|\\[|\\)|\\}|\\]|,|\\+|\\-|\\*|\\/)";
	public static final String isJavaIdentifierStart = "(?:\\p{Alpha}|\\$|_)";
	public static final String isJavaIdentifierPart = "(?:\\p{Alnum}|\\$|_)";
	
}
