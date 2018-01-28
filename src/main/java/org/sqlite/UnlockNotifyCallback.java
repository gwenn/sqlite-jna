package org.sqlite;

import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.PointerPointer;

public abstract class UnlockNotifyCallback extends FunctionPointer {
	protected UnlockNotifyCallback() {
		allocate();
	}
	private native void allocate();

	@SuppressWarnings("unused")
	public void call(PointerPointer args, int nArg) {
		if (nArg == 0) {
			return;
		}
		notify(args, nArg);
	}

	protected abstract void notify(PointerPointer args, int nArg);
}
