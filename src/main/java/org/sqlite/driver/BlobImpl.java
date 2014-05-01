package org.sqlite.driver;

import org.sqlite.ErrCodes;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.SQLException;

public class BlobImpl implements Blob {
  private final org.sqlite.Blob blob;

  public BlobImpl(org.sqlite.Blob blob) {
    this.blob = blob;
  }

  @Override
  public long length() throws SQLException {
    checkOpen();
    return blob.getBytes();
  }

  @Override
  public byte[] getBytes(long pos, int length) throws SQLException {
    checkOpen();
    if (length < 0) {
      throw new SQLException(String.format("invalid read length: %d < 0", length), null, ErrCodes.WRAPPER_SPECIFIC);
    }
    final int readOffset = checkPosition(pos);
    blob.setReadOffset(readOffset);
    final byte[] bytes = new byte[Math.min(blob.getBytes() - readOffset, length)];
    final int n = blob.read(ByteBuffer.wrap(bytes)); // read may be incomplete (n < length)...
    if (n != bytes.length) {
      throw new SQLException(String.format("short read: %d < %d", n, bytes.length), null, ErrCodes.WRAPPER_SPECIFIC);
    }
    return bytes;
  }

  @Override
  public InputStream getBinaryStream() throws SQLException {
    checkOpen();
    return blob.getInputStream();
  }

  @Override
  public long position(byte[] pattern, long start) throws SQLException {
    //checkPosition(start);
    throw Util.unsupported("*Blob.position"); // FIXME
  }

  @Override
  public long position(Blob pattern, long start) throws SQLException {
    //checkPosition(start);
    throw Util.unsupported("*Blob.position"); // FIXME
  }

  @Override
  public int setBytes(long pos, byte[] bytes) throws SQLException {
    return setBytes(pos, bytes, 0, bytes.length);
  }

  @Override
  public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
    checkOpen();
    blob.setWriteOffset(checkPosition(pos));
    return blob.write(ByteBuffer.wrap(bytes, offset, len));
  }

  @Override
  public OutputStream setBinaryStream(long pos) throws SQLException {
    checkOpen();
    blob.setWriteOffset(checkPosition(pos));
    return blob.getOutputStream();
  }

  @Override
  public void truncate(long len) throws SQLException {
    //checkLength(len);
    throw Util.unsupported("Blob.truncate"); // TODO Validate
  }

  @Override
  public void free() throws SQLException {
    if (blob == null) {
      return;
    }
    blob.closeAndCheck();
  }

  @Override
  public InputStream getBinaryStream(long pos, long length) throws SQLException {
    checkLength(length);
    checkOpen();
    final int readOffset = checkPosition(pos);
    if (length + readOffset > blob.getBytes()) {
      throw new SQLException(String.format("pos + length is greater than the number of bytes in the Blob: %d + %d > %d", pos, length, blob.getBytes()), null, ErrCodes.WRAPPER_SPECIFIC);
    }
    blob.setReadOffset(readOffset);
    return getBinaryStream();
  }

  private void checkOpen() throws SQLException {
    if (blob == null) {
      throw new SQLException("Blob closed");
    } else {
      blob.checkOpen();
    }
  }

  private static int checkPosition(long pos) throws SQLException {
    if (pos < 1) {
      throw new SQLException(String.format("invalid position: %d < 1", pos), null, ErrCodes.WRAPPER_SPECIFIC);
    }
    if (pos > Integer.MAX_VALUE) {
      throw new SQLException(String.format("invalid position: %d > %d", pos, Integer.MAX_VALUE), null, ErrCodes.WRAPPER_SPECIFIC);
    }
    return (int) (pos - 1);
  }

  public static int checkLength(long length) throws SQLException {
    if (length < 0) {
      throw new SQLException(String.format("invalid length: %d < 0", length), null, ErrCodes.WRAPPER_SPECIFIC);
    }
    if (length > Integer.MAX_VALUE) {
      throw new SQLException(String.format("invalid length: %d > %d", length, Integer.MAX_VALUE), null, ErrCodes.WRAPPER_SPECIFIC);
    }
    return (int) length;
  }
}
