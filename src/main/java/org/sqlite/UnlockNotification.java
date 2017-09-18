package org.sqlite;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class UnlockNotification {
	private boolean fired; // True after unlock event has occurred
	private final Lock mutex = new ReentrantLock(); // Mutex to protect structure
	private final Condition cond = mutex.newCondition();

	void fire() {
		mutex.lock();
		try {
			fired = true;
			cond.signal();
		} finally {
			mutex.unlock();
		}
	}

	void await(Conn c) throws ConnException {
		mutex.lock();
		try {
			if (!fired) {
				cond.await();
			}
		} catch (InterruptedException e) {
			throw new ConnException(c, e.getMessage(), ErrCodes.WRAPPER_SPECIFIC); // TODO Validate
		} finally {
			mutex.unlock();
		}
	}
}
