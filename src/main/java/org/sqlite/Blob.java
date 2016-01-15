/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import com.sun.jna.Pointer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static org.sqlite.SQLite.*;

/**
 * A Handle To An Open BLOB
 * <a href="https://www.sqlite.org/c3ref/blob.html">sqlite3_blob</a>
 */
public class Blob {
	private final Conn c;
	private Pointer pBlob;
	private int readOffset;
	private int writeOffset;
	private int size = -1;

	Blob(Conn c, Pointer pBlob) {
		assert c != null;
		this.c = c;
		this.pBlob = pBlob;
	}

	/**
	 * @return the size of an opened BLOB
	 * <a href="https://www.sqlite.org/c3ref/blob_bytes.html">sqlite3_blob_bytes</a>
	 */
	public int getBytes() throws SQLiteException {
		checkOpen();
		if (size < 0) {
			size = sqlite3_blob_bytes(pBlob);
		}
		return size;
	}

	/**
	 * Read data from this BLOB incrementally
	 * @param b buffer to read into
	 * @return number of bytes read
	 * <a href="https://www.sqlite.org/c3ref/blob_read.html">sqlite3_blob_read</a>
	 */
	public int read(ByteBuffer b) throws SQLiteException {
		if (b == null) {
			throw new NullPointerException();
		}
		checkOpen();
		final int n = b.remaining();
		final int res = sqlite3_blob_read(pBlob, b, n, readOffset);
		if (res != SQLITE_OK) {
			throw new SQLiteException(c, "error while reading blob", res);
		}
		readOffset += n;
		return n;
	}

	/**
	 * Write data into this BLOB incrementally
	 * @param b bytes to write
	 * @return number of bytes written
	 * <a href="https://www.sqlite.org/c3ref/blob_write.html">sqlite3_blob_write</a>
	 */
	public int write(ByteBuffer b) throws SQLiteException {
		if (b == null) {
			throw new NullPointerException();
		}
		checkOpen();
		final int n = b.remaining();
		final int res = sqlite3_blob_write(pBlob, b, n, writeOffset);
		if (res != SQLITE_OK) {
			throw new SQLiteException(c, "error while writing blob", res);
		}
		writeOffset += n;
		return n;
	}

	/**
	 * Move this BLOB handle to a new row
	 * @param iRow new row id
	 * @throws SQLiteException if this BLOB is closed
	 * <a href="https://www.sqlite.org/c3ref/blob_reopen.html">sqlite3_blob_reopen</a>
	 */
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
		if (pBlob != null) {
			sqlite3_log(-1, "dangling SQLite blob.");
			close();
		}
		super.finalize();
	}

	/**
	 * Close this BLOB handle
	 * @return result code (No exception is thrown).
	 * <a href="https://www.sqlite.org/c3ref/blob_close.html">sqlite3_blob_close</a>
	 */
	public int close() {
		if (pBlob == null) {
			return SQLITE_OK;
		}
		final int res = sqlite3_blob_close(pBlob); // must be called only once
		pBlob = null;
		return res;
	}
	/**
	 * Close this BLOB and throw an exception if an error occured.
	 */
	public void closeAndCheck() throws SQLiteException {
		final int res = close();
		if (res != ErrCodes.SQLITE_OK) {
			throw new SQLiteException(c, "error while closing Blob", res);
		}
	}
	/**
	 * @return whether or not this BLOB is closed
	 */
	public boolean isClosed() {
		return pBlob == null;
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
				return Blob.this.read(ByteBuffer.wrap(b, off, len));
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
				Blob.this.write(ByteBuffer.wrap(b, off, len));
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
