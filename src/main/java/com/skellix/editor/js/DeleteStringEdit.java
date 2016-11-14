package com.skellix.editor.js;

public class DeleteStringEdit extends Edit {

	public String string;

	public DeleteStringEdit(int offset, String str) {
		super(offset);
		this.string = str;
	}

}
