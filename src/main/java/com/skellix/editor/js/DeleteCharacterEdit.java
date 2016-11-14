package com.skellix.editor.js;

public class DeleteCharacterEdit extends Edit {

	public char character;

	public DeleteCharacterEdit(int offset, char ch) {
		super(offset);
		this.character = ch;
	}

}
