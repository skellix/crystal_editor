package com.skellix.editor.js;

import java.nio.ByteBuffer;

public class BufferCharSequence implements CharSequence {

	private ByteBuffer buffer;

	public BufferCharSequence(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public char charAt(int arg0) {
		return (char) buffer.get(arg0);
	}

	@Override
	public int length() {
		return buffer.limit();
	}

	@Override
	public CharSequence subSequence(int arg0, int arg1) {
		buffer.position(arg0);
		return new BufferCharSequence(buffer.get(new byte[arg1 - arg0]));
	}

}
