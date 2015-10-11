/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.sqlite.SQLite.*;

/*
 blob   0         offset    size
 byte[] 0   offset        length
 */
public class Blob {
	private final Conn c;
	private long pBlob;
	private int readOffset;
	private int writeOffset;
	private int size = -1;

	Blob(Conn c, long pBlob) {
		assert c != null;
		this.c = c;
		this.pBlob = pBlob;
	}

	/**
	 * @return the size of an opened BLOB
	 */
	public int getBytes() throws SQLiteException {
		checkOpen();
		if (size < 0) {
			size = sqlite3_blob_bytes(pBlob);
		}
		return size;
	}

	public int read(byte[] b, int off, int len) throws SQLiteException {
		if (b == null) {
			throw new NullPointerException();
		}
		checkOpen();
		final int n = len;
		final int res = sqlite3_blob_read(pBlob, b, off, len, readOffset);
		if (res != SQLITE_OK) {
			throw new SQLiteException(c, "error while reading blob", res);
		}
		readOffset += n;
		return n;
	}

	public int write(byte[] b, int off, int len) throws SQLiteException {
		if (b == null) {
			throw new NullPointerException();
		}
		checkOpen();
		final int n = len;
		final int res = sqlite3_blob_write(pBlob, b, off, n, writeOffset);
		if (res != SQLITE_OK) {
			throw new SQLiteException(c, "error while writing blob", res);
		}
		writeOffset += n;
		return n;
	}

	public void reopen(long iRow) throws SQLiteException {
		checkOpen();
		final int res = sqlite3_blob_reopen(pBlob, iRow);
		if (res != SQLITE_OK) {
			throw new SQLiteException(c, "error while reopening blob", res);
		}
		readOffset = 0;
		writeOffset = 0;
		size = -1;
	}

	@Override
	protected void finalize() throws Throwable {
		if (pBlob != 0) {
			sqlite3_log(-1, "dangling SQLite blob.");
			close();
		}
		super.finalize();
	}

	public int close() {
		if (pBlob == 0) {
			return SQLITE_OK;
		}
		final int res = sqlite3_blob_close(pBlob); // must be called only once
		pBlob = 0;
		return res;
	}

	public void closeAndCheck() throws SQLiteException {
		final int res = close();
		if (res != ErrCodes.SQLITE_OK) {
			throw new SQLiteException(c, "error while closing Blob", res);
		}
	}

	public boolean isClosed() {
		return pBlob == 0;
	}

	public void checkOpen() throws SQLiteException {
		if (isClosed()) {
			throw new SQLiteException(c, "blob already closed", ErrCodes.WRAPPER_SPECIFIC);
		}
	}

	public OutputStream getOutputStream() {
		return new BlobOutputStream();
	}

	public void setWriteOffset(int writeOffset) throws SQLiteException {
		if (writeOffset < 0) {
			throw new SQLiteException(String.format("invalid write offset: %d < 0", writeOffset), ErrCodes.WRAPPER_SPECIFIC);
		} else if (writeOffset > getBytes()) {
			throw new SQLiteException(String.format("invalid write offset: %d > %d", writeOffset, getBytes()), ErrCodes.WRAPPER_SPECIFIC);
		}
		this.writeOffset = writeOffset;
	}

	public InputStream getInputStream() {
		return new BlobInputStream();
	}

	public void setReadOffset(int readOffset) throws SQLiteException {
		if (readOffset < 0) {
			throw new SQLiteException(String.format("invalid read offset: %d < 0", readOffset), ErrCodes.WRAPPER_SPECIFIC);
		} else if (readOffset > getBytes()) {
			throw new SQLiteException(String.format("invalid read offset: %d > %d", readOffset, getBytes()), ErrCodes.WRAPPER_SPECIFIC);
		}
		this.readOffset = readOffset;
	}

	private class BlobInputStream extends InputStream {
		private int mark;

		@Override
		public int read() throws IOException {
			if (isEOF()) {
				return -1;
			}
			final byte[] b = new byte[1];
			final int i = read(b);
			if (i < 0) {
				return i;
			}
			return b[0];
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (b == null) {
				throw new NullPointerException();
			} else if (off < 0 || len < 0 || len > b.length - off) {
				throw new IndexOutOfBoundsException();
			}
			if (isEOF()) {
				return -1;
			}
			final int avail = available();
			if (len > avail) {
				len = avail;
			}
			if (len <= 0) {
				return 0;
			}
			try {
				return Blob.this.read(b, off, len);
			} catch (SQLiteException e) {
				throw new IOException(e);
			}
		}

		@Override
		public long skip(long n) throws IOException {
			try {
				long k = getBytes() - readOffset;
				if (n < k) {
					k = n < 0L ? 0L : n;
				}

				readOffset += k;
				return k;
			} catch (SQLiteException e) {
				throw new IOException(e);
			}
		}

		@Override
		public int available() throws IOException {
			try {
				return getBytes() - readOffset;
			} catch (SQLiteException e) {
				throw new IOException(e);
			}
		}

		@Override
		public void close() throws IOException {
			try {
				closeAndCheck();
			} catch (SQLiteException e) {
				throw new IOException(e);
			}
		}

		private boolean isEOF() throws IOException {
			try {
				return readOffset >= getBytes();
			} catch (SQLiteException e) {
				throw new IOException(e);
			}
		}

		@Override
		public boolean markSupported() {
			return true;
		}

		@Override
		public synchronized void mark(int _) {
			mark = readOffset;
		}

		@Override
		public synchronized void reset() throws IOException {
			readOffset = mark;
		}
	}

	private class BlobOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
			write(new byte[]{(byte) b});
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			try {
				Blob.this.write(b, off, len);
			} catch (SQLiteException e) {
				throw new IOException(e);
			}
		}

		@Override
		public void close() throws IOException {
			try {
				closeAndCheck();
			} catch (SQLiteException e) {
				throw new IOException(e);
			}
		}
	}

	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
	public static int copy(InputStream input, OutputStream output, int length) throws IOException {
		final byte[] buffer = new byte[Math.min(length, DEFAULT_BUFFER_SIZE)];
		int count = 0;
		int n = buffer.length;
		while (length > 0 && (n = input.read(buffer, 0, n)) >= 0) {
			output.write(buffer, 0, n);
			count += n;
			length -= n;
			n = Math.min(length, DEFAULT_BUFFER_SIZE);
		}
		return count;
	}
}
