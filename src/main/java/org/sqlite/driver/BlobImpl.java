package org.sqlite.driver;

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
    // TODO Check length
    final byte[] bytes = new byte[length];
    blob.setReadOffset((int) (pos - 1));
    final int n = blob.read(ByteBuffer.wrap(bytes)); // read may be incomplete (n < length)...
    // TODO Check length == n
    return bytes;
  }
  @Override
  public InputStream getBinaryStream() throws SQLException {
    checkOpen();
    return blob.getInputStream();
  }
  @Override
  public long position(byte[] pattern, long start) throws SQLException {
    return 0; // FIXME
  }
  @Override
  public long position(Blob pattern, long start) throws SQLException {
    return 0; // FIXME
  }
  @Override
  public int setBytes(long pos, byte[] bytes) throws SQLException {
    return setBytes(pos, bytes, 0, bytes.length);
  }
  @Override
  public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
    return 0; // FIXME
  }
  @Override
  public OutputStream setBinaryStream(long pos) throws SQLException {
    checkOpen();
    blob.setWriteOffset((int) (pos - 1));
    return blob.getOutputStream();
  }
  @Override
  public void truncate(long len) throws SQLException {
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
    checkOpen();
    blob.setReadOffset((int) (pos - 1));
    // TODO Check length ?
    return getBinaryStream();
  }

  private void checkOpen() throws SQLException {
    if (blob == null) {
      throw new SQLException("Blob closed");
    } else {
      blob.checkOpen();
    }
  }
}
