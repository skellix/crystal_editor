package com.skellix.editor.js;

public class Suggestion {

	String text, more;
	
	public Suggestion(String text) {
		this.text = text;
		this.more = "";
	}
	
	public Suggestion(String text, String more) {
		this.text = text;
		this.more = more;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Suggestion) {
			Suggestion other = (Suggestion) obj;
			return text.equals(other.text) && more.equals(other.more);
		}
		return super.equals(obj);
	}
}
