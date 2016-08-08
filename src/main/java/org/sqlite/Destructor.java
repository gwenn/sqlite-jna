package org.sqlite;

import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Pointer;

public abstract class Destructor extends FunctionPointer {
	protected Destructor() {
		allocate();
	}
	private native void allocate();
	@SuppressWarnings("unused")
	public abstract void call(Pointer p);
}
