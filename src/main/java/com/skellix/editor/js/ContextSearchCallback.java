package com.skellix.editor.js;

import java.nio.ByteBuffer;

public interface ContextSearchCallback {
	
	public void onMatchFound(ByteBuffer buffer, int start, int end);

}
