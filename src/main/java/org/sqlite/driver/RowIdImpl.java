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

    return value == rowId.value;

  }
  @Override
  public int hashCode() {
    return (int) (value ^ (value >>> 32));
  }

  public static long getValue(RowId rowId) {
    if (rowId instanceof RowIdImpl) {
      return ((RowIdImpl) rowId).value;
    } else {
      return Long.parseLong(rowId.toString());
    }
  }
}
