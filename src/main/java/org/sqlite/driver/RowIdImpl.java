package org.sqlite.driver;

import java.sql.RowId;

public class RowIdImpl implements RowId {
  final long value;

  public RowIdImpl(long value) {
    this.value = value;
  }

  @Override
  public byte[] getBytes() {
    return toString().getBytes();
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RowIdImpl rowId = (RowIdImpl) o;

    if (value != rowId.value) return false;

    return true;
  }
  @Override
  public int hashCode() {
    return (int) (value ^ (value >>> 32));
  }
}
