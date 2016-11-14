package com.skellix.editor.js;

public class InsertCharacterEdit extends Edit {

	char character;

	public InsertCharacterEdit(int offset, char ch) {
		super(offset);
		this.character = ch;
	}

}
