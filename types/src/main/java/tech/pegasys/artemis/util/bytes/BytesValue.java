/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.artemis.util.bytes;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;

import java.security.MessageDigest;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;

/**
 * A value made of bytes.
 *
 * <p>
 * This class essentially represents an immutable view (the view is immutable, the underlying value
 * may not be) over an array of bytes, but the backing may not be an array in practice.
 *
 * <p>
 * This interface makes no thread-safety guarantee, and a {@link BytesValue} is generally not thread
 * safe. Specific implementations may be thread-safe however (for instance, the value returned by
 * {@link #copy} is guaranteed to be thread-safe since deeply immutable).
 *
 * @see BytesValues for static methods to create and work with {@link BytesValue}.
 */
public interface BytesValue extends Comparable<BytesValue> {

  /**
   * The empty value (with 0 bytes).
   */
  BytesValue EMPTY = wrap(new byte[0]);

  /**
   * Wraps the provided byte array as a {@link BytesValue}.
   *
   * <p>
   * Note that value is not copied, only wrapped, and thus any future update to {@code value} will
   * be reflected in the returned value.
   *
   * @param value The value to wrap.
   * @return A {@link BytesValue} wrapping {@code value}.
   */
  static BytesValue wrap(byte[] value) {
    return wrap(value, 0, value.length);
  }

  /**
   * Wraps a slice/sub-part of the provided array as a {@link BytesValue}.
   *
   * <p>
   * Note that value is not copied, only wrapped, and thus any future update to {@code value} within
   * the wrapped parts will be reflected in the returned value.
   *
   * @param value The value to wrap.
   * @param offset The index (inclusive) in {@code value} of the first byte exposed by the returned
   *        value. In other words, you will have {@code wrap(value, o, l).get(0) == value[o]}.
   * @param length The length of the resulting value.
   * @return A {@link BytesValue} that expose the bytes of {@code value} from {@code offset}
   *         (inclusive) to {@code offset + length} (exclusive).
   * @throws IndexOutOfBoundsException if {@code offset &lt; 0 || (value.length > 0 && offset >=
   *     value.length)}.
   * @throws IllegalArgumentException if {@code length &lt; 0 || offset + length > value.length}.
   */
  static BytesValue wrap(byte[] value, int offset, int length) {
    return new ArrayWrappingBytesValue(value, offset, length);
  }

  /**
   * Wraps two other value into the new concatenated value.
   *
   * @param v1 The first value to wrap.
   * @param v2 The second value to wrap.
   * @return v1 concatenated with v2.
   */
  static BytesValue wrap(BytesValue v1, BytesValue v2) {
    return v1.concat(v2);
  }

  /**
   * Concatenates two values
   *
   * The resulting value would be a copy of backing bytes
   */
  static BytesValue concat(BytesValue v1, BytesValue v2) {
    byte[] bb = new byte[v1.size() + v2.size()];
    byte[] arr1 = v1.getArrayUnsafe();
    byte[] arr2 = v2.getArrayUnsafe();
    System.arraycopy(arr1, 0, bb, 0, arr1.length);
    System.arraycopy(arr2, 0, bb, arr1.length, arr2.length);
    return wrap(bb);
  }

  static BytesValue concat(List<? extends BytesValue> vals) {
    // TODO optimize this naive implementation
    if (vals.isEmpty()) {
      return BytesValue.EMPTY;
    }
    int size = vals.stream().mapToInt(BytesValue::size).sum();
    byte[] resBytes = new byte[size];
    int pos = 0;
    for (BytesValue val : vals) {
      byte[] srcArr = val.getArrayUnsafe();
      System.arraycopy(srcArr, 0, resBytes, pos, srcArr.length);
      pos += srcArr.length;
    }
    return BytesValue.wrap(resBytes);
  }

  /**
   * Wraps a full Vert.x {@link Buffer} as a {@link BytesValue}.
   *
   * <p>
   * Note that as the buffer is wrapped, any change to the content of that buffer may be reflected
   * in the returned value.
   *
   * @param buffer The buffer to wrap.
   * @return A {@link BytesValue} that exposes the bytes of {@code buffer}.
   */
  static BytesValue wrapBuffer(Buffer buffer) {
    return wrapBuffer(buffer, 0, buffer.length());
  }

  /**
   * Wraps a slice of a Vert.x {@link Buffer} as a {@link BytesValue}.
   *
   * <p>
   * Note that as the buffer is wrapped, any change to the content of that buffer may be reflected
   * in the returned value.
   *
   * @param buffer The buffer to wrap.
   * @param offset The offset in {@code buffer} from which to expose the bytes in the returned
   *        value. That is, {@code wrapBuffer(buffer, i, 1).get(0) == buffer.getByte(i)}.
   * @param size The size of the returned value.
   * @return A {@link BytesValue} that exposes the equivalent of {@code buffer.getBytes(offset,
   * offset + size)} (but without copying said bytes).
   */
  static BytesValue wrapBuffer(Buffer buffer, int offset, int size) {
    return MutableBytesValue.wrapBuffer(buffer, offset, size);
  }

  /**
   * Wraps a full Netty {@link ByteBuf} as a {@link BytesValue}.
   *
   * @param buffer The buffer to wrap.
   * @return A {@link BytesValue} that exposes the bytes of {@code buffer}.
   */
  static BytesValue wrapBuffer(ByteBuf buffer) {
    return wrapBuffer(buffer, buffer.readerIndex(), buffer.readableBytes());
  }

  /**
   * Wraps a slice of a Netty {@link ByteBuf} as a {@link BytesValue}.
   *
   * @param buffer The buffer to wrap.
   * @param offset The offset in {@code buffer} from which to expose the bytes in the returned
   *        value. That is, {@code wrapBuffer(buffer, i, 1).get(0) == buffer.getByte(i)}.
   * @param size The size of the returned value.
   * @return A {@link BytesValue} that exposes the equivalent of {@code buffer.getBytes(offset,
   * offset + size)} (but without copying said bytes).
   */
  static BytesValue wrapBuffer(ByteBuf buffer, int offset, int size) {
    return MutableBytesValue.wrapBuffer(buffer, offset, size);
  }

  /**
   * Creates a newly allocated value that contains the provided bytes in their provided order.
   *
   * @param bytes The bytes that must compose the returned value.
   * @return A newly allocated value whose bytes are the one from {@code bytes}.
   */
  static BytesValue of(byte... bytes) {
    return wrap(bytes);
  }

  /**
   * Creates a newly allocated value that contains the provided bytes in their provided order.
   *
   * @param bytes The bytes that must compose the returned value.
   * @return A newly allocated value whose bytes are the one from {@code bytes}.
   * @throws IllegalArgumentException if any of {@code bytes} would be truncated when storing as a
   *         byte.
   */
  @VisibleForTesting
  static BytesValue of(int... bytes) {
    MutableBytesValue res = MutableBytesValue.create(bytes.length);
    for (int i = 0; i < bytes.length; i++) {
      int b = bytes[i];
      checkArgument(b == (((byte) b) & 0xff), "%sth value %s does not fit a byte", i + 1, b);
      res.set(i, (byte) b);
    }
    return res;
  }

  /**
   * Parse an hexadecimal string into a {@link BytesValue}.
   *
   * <p>
   * This method is lenient in that {@code str} may of an odd length, in which case it will behave
   * exactly as if it had an additional 0 in front.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x".
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to valid hexadecimal
   *         representation.
   */
  static BytesValue fromHexStringLenient(String str) {
    return BytesValues.fromHexString(str, -1, true);
  }

  /**
   * Parse an hexadecimal string into a {@link BytesValue} of the provided size.
   *
   * <p>
   * This method is lenient in that {@code str} may of an odd length, in which case it will behave
   * exactly as if it had an additional 0 in front.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x".
   * @param destinationSize The size of the returned value, which must be big enough to hold the
   *        bytes represented by {@code str}. If it is strictly bigger those bytes from {@code str},
   *        the returned value will be left padded with zeros.
   * @return A value of size {@code destinationSize} corresponding to {@code str} potentially
   *         left-padded.
   * @throws IllegalArgumentException if {@code str} does not correspond to valid hexadecimal
   *         representation, represents more bytes than {@code destinationSize} or
   *         {@code destinationSize &lt; 0}.
   */
  static BytesValue fromHexStringLenient(String str, int destinationSize) {
    checkArgument(destinationSize >= 0, "Invalid negative destination size %s", destinationSize);
    return BytesValues.fromHexString(str, destinationSize, true);
  }

  /**
   * Parse an hexadecimal string into a {@link BytesValue}.
   *
   * <p>
   * This method is strict in that {@code str} must of an even length.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x".
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to valid hexadecimal
   *         representation, or is of an odd length.
   */
  static BytesValue fromHexString(String str) {
    return BytesValues.fromHexString(str, -1, false);
  }

  /**
   * Parse an hexadecimal string into a {@link BytesValue}.
   *
   * <p>
   * This method is strict in that {@code str} must of an even length.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x".
   * @param destinationSize The size of the returned value, which must be big enough to hold the
   *        bytes represented by {@code str}. If it is strictly bigger those bytes from {@code str},
   *        the returned value will be left padded with zeros.
   * @return A value of size {@code destinationSize} corresponding to {@code str} potentially
   *         left-padded.
   * @throws IllegalArgumentException if {@code str} does correspond to valid hexadecimal
   *         representation, or is of an odd length.
   * @throws IllegalArgumentException if {@code str} does not correspond to valid hexadecimal
   *         representation, or is of an odd length, or represents more bytes than
   *         {@code destinationSize} or {@code destinationSize &lt; 0}.
   */
  static BytesValue fromHexString(String str, int destinationSize) {
    checkArgument(destinationSize >= 0, "Invalid negative destination size %s", destinationSize);
    return BytesValues.fromHexString(str, destinationSize, false);
  }

  /** @return The number of bytes this value represents. */
  int size();

  /**
   * Retrieves a byte in this value.
   *
   * @param i The index of the byte to fetch within the value (0-indexed).
   * @return The byte at index {@code i} in this value.
   * @throws IndexOutOfBoundsException if {@code i &lt; 0} or {i &gt;= size()}.
   */
  byte get(int i);

  default boolean getBit(int bitIndex) {
    return ((get(bitIndex / 8) >> (bitIndex % 8)) & 1) == 1;
  }

  /**
   * Retrieves the 4 bytes starting at the provided index in this value as an integer.
   *
   * @param i The index from which to get the int, which must less than or equal to {@code size() -
   *     4}.
   * @return An integer whose value is the 4 bytes from this value starting at index {@code i}.
   * @throws IndexOutOfBoundsException if {@code i &lt; 0} or {i &gt;= size()}.
   * @throws IllegalArgumentException if {@code i &gt; size() - 4}.
   */
  default int getInt(int i) {
    checkElementIndex(i, size());
    checkArgument(i <= size() - 4,
        "Value of size %s has not enough bytes to read a 4 bytes int from index %s", size(), i);

    int value = 0;
    value |= ((int) get(i) & 0xFF) << 24;
    value |= ((int) get(i + 1) & 0xFF) << 16;
    value |= ((int) get(i + 2) & 0xFF) << 8;
    value |= ((int) get(i + 3) & 0xFF);
    return value;
  }

  /**
   * Retrieves the 8 bytes starting at the provided index in this value as a long.
   *
   * @param i The index from which to get the long, which must less than or equal to {@code size() -
   *     8}.
   * @return A long whose value is the 8 bytes from this value starting at index {@code i}.
   * @throws IndexOutOfBoundsException if {@code i &lt; 0} or {i &gt;= size()}.
   * @throws IllegalArgumentException if {@code i &gt; size() - 8}.
   */
  default long getLong(int i) {
    checkElementIndex(i, size());
    checkArgument(i <= size() - 8,
        "Value of size %s has not enough bytes to read a 8 bytes long from index %s", size(), i);

    return (((long) getInt(i)) << 32) | (((long) getInt(i + 4)) & 0xFFFFFFFFL);
  }

  /**
   * Creates a new value representing (a view of) a slice of the bytes of this value.
   *
   * <p>
   * Please note that the resulting slice is only a view and as such maintains a link to the
   * underlying full value. So holding a reference to the returned slice may hold more memory than
   * the slide represents. Use {@link #copy} on the returned slice if that is not what you want.
   *
   * @param index The start index for the slice.
   * @return A new value providing a view over the bytes from index {@code index} (included) to the
   *         end.
   * @throws IndexOutOfBoundsException if {@code index &lt; 0}.
   */
  BytesValue slice(int index);

  /**
   * Creates a new value representing (a view of) a slice of the bytes of this value.
   *
   * <p>
   * Please note that the resulting slice is only a view and as such maintains a link to the
   * underlying full value. So holding a reference to the returned slice may hold more memory than
   * the slide represents. Use {@link #copy} on the returned slice if that is not what you want.
   *
   * @param index The start index for the slice.
   * @param length The length of the resulting value.
   * @return A new value providing a view over the bytes from index {@code index} (included) to
   *         {@code index + length} (excluded).
   * @throws IllegalArgumentException if {@code length &lt; 0}.
   * @throws IndexOutOfBoundsException if {@code index &lt; 0} or {index &gt;= size()} or {index +
   *         length &gt; size()} .
   */
  BytesValue slice(int index, int length);

  /**
   * Returns a value equivalent to this one but that is guaranteed to 1) be deeply immutable (that
   * is, the underlying value will be immutable) and 2) to not retain more bytes than exposed by the
   * value.
   *
   * @return A value, equals to this one, but deeply immutable and that doesn't retain any
   *         "unreachable" bytes. For performance reasons, this is allowed to return this value
   *         however if it already fit those constraints.
   */
  BytesValue copy();

  /**
   * Returns a new mutable value initialized with the content of this value.
   *
   * @return A mutable copy of this value. This will copy bytes, modifying the returned value will
   *         <b>not</b> modify this value.
   */
  MutableBytesValue mutableCopy();

  /**
   * Copy the bytes of this value to the provided mutable one, which must have the same size.
   *
   * @param destination The mutable value to which to copy the bytes to, which must have the same
   *        size as this value. If you want to copy value where size differs, you should use
   *        {@link #slice} and/or {@link MutableBytesValue#mutableSlice} and apply the copy to the
   *        result.
   * @throws IllegalArgumentException if {@code this.size() != destination.size()}.
   */
  void copyTo(MutableBytesValue destination);

  /**
   * Copy the bytes of this value to the provided mutable one from a particular offset.
   *
   * <p>
   * This is a (potentially slightly more efficient) shortcut for {@code
   * copyTo(destination.mutableSlice(destinationOffset, this.size()))}.
   *
   * @param destination The mutable value to which to copy the bytes to, which must have enough
   *        bytes from {@code destinationOffset} for the copied value.
   * @param destinationOffset The offset in {@code destination} at which the copy starts.
   * @throws IllegalArgumentException if the destination doesn't have enough room, that is if {@code
   *     this.size() &gt; (destination.size() - destinationOffset)}.
   */
  void copyTo(MutableBytesValue destination, int destinationOffset);

  /**
   * Appends the bytes of this value to the provided Vert.x {@link Buffer}.
   *
   * <p>
   * Note that since a Vert.x {@link Buffer} will grow as necessary, this method never fails.
   *
   * @param buffer The {@link Buffer} to which to append this value.
   */
  default void appendTo(Buffer buffer) {
    for (int i = 0; i < size(); i++) {
      buffer.appendByte(get(i));
    }
  }

  /**
   * Appends the bytes of this value to the provided Netty {@link ByteBuf}.
   *
   * @param buffer The {@link ByteBuf} to which to append this value.
   */
  default void appendTo(ByteBuf buffer) {
    for (int i = 0; i < size(); i++) {
      buffer.writeByte(get(i));
    }
  }

  /**
   * Return the number of bytes in common between this set of bytes and another.
   *
   * @param other The bytes to compare to.
   * @return The number of common bytes.
   */
  int commonPrefixLength(BytesValue other);

  /**
   * Return a slice over the common prefix between this set of bytes and another.
   *
   * @param other The bytes to compare to.
   * @return A slice covering the common prefix.
   */
  BytesValue commonPrefix(BytesValue other);

  /**
   * Update the provided message digest with the bytes of this value.
   *
   * @param digest The digest to update.
   */
  void update(MessageDigest digest);

  /**
   * Whether this value has only zeroed bytes.
   *
   * @return True if all the bits of this value are zeros.
   */
  boolean isZero();

  /**
   * Whether this value is empty, that is is of zero size.
   *
   * @return {@code true} if {@code size() == 0}, {@code false} otherwise.
   */
  default boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Extracts the bytes of this value into a newly allocated byte array.
   *
   * @return A newly allocated byte array with the same content than this value.
   */
  default byte[] extractArray() {
    int size = size();
    byte[] array = new byte[size];
    for (int i = 0; i < size; i++) {
      array[i] = get(i);
    }
    return array;
  }

  /**
   * Get the bytes represented by this value as byte array.
   *
   * <p>
   * Contrarily to {@link #extractArray()}, this may avoid allocating a new array and directly
   * return the backing array of this value if said value is array backed and doing so is possible.
   * As such, modifications to the returned array may or may not impact this value. As such, this
   * method should be used with care and hence the "unsafe" moniker.
   *
   * @return A byte array with the same content than this value, which may or may not be the direct
   *         backing of this value.
   */
  default byte[] getArrayUnsafe() {
    return extractArray();
  }

  @Override
  default int compareTo(BytesValue other) {
    int minSize = Math.min(size(), other.size());
    for (int i = 0; i < minSize; i++) {
      // Using integer comparison to basically simulate unsigned byte comparison
      int cmp = Integer.compare(get(i) & 0xFF, other.get(i) & 0xFF);
      if (cmp != 0) {
        return cmp;
      }
    }
    return Integer.compare(size(), other.size());
  }

  default BytesValue concat(final BytesValue value) {
    return concat(this, value);
  }

  /**
   * Returns the hexadecimal string representation of this value.
   *
   * @return The hexadecimal representation of this value, starting with "0x".
   */
  @Override
  String toString();
}
