package com.skellix.editor.js;

public class InsertStringEdit extends Edit {

	public String string;

	public InsertStringEdit(int offset, String str) {
		super(offset);
		this.string = str;
	}

}
