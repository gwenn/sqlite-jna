package org.sqlite;

import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;

public abstract class Destructor extends FunctionPointer {
	@SuppressWarnings("unused")
	public abstract void call(Pointer p);
}
