package se.albin.jbinary;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * An abstract bit input stream, that reads data from a resource. This class and some of its methods can be overridden,
 * to create a customized stream.
 */
@SuppressWarnings({"SameParameterValue", "unused", "UnusedReturnValue", "WeakerAccess"})
public abstract class BitInputStream implements Closeable, AutoCloseable
{
	protected ByteOrder defaultByteOrder = ByteOrder.BIG_ENDIAN;
	protected BitOrder defaultBitOrder = BitOrder.MOST_SIGNIFICANT_BIT;
	
	private long byteIndex;
	private int subByteIndex;
	
	/**
	 * Reads from the data in a range of 0 to 64 bits, and returns as a long.<br><br>
	 * <p>
	 * Subclasses only have to implement this method for reading data. The rest (like readAsInt, readAsString etc.) is
	 * handled by BitInputStream. However, it is possible to override those methods if you absolutely have to.
	 *
	 * @param bits      Amount of bits to read.
	 * @param byteOrder Desired byte order.
	 * @param bitOrder  Desired bit order.
	 * @return The data that was read.
	 */
	private long read(int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		if(!hasEnded() && 64 >= bits)
		{
			if(bits <= 0)
				return 0;
			
			if(getRemainingBits() < bits)
				bits = (int)getRemainingBits();
			
			int remainingBits = 8 - subByteIndex;
			
			if(bits <= 8)
			{
				short out = -1;
				
				if(subByteIndex == 0)
				{
					if(bitOrder == BitOrder.MOST_SIGNIFICANT_BIT)
						out = (short)(Byte.toUnsignedInt(currentByte(byteIndex, subByteIndex)) >> (8 - bits));
					else if(bitOrder == BitOrder.LEAST_SIGNIFICANT_BIT)
						out = (short)(currentByte(byteIndex, subByteIndex) & BitUtil.getBitMask(bits));
				}
				else if(bits == remainingBits)
				{
					if(bitOrder == BitOrder.MOST_SIGNIFICANT_BIT)
						out = (short)(currentByte(byteIndex, subByteIndex) & BitUtil.getBitMask(bits));
					else if(bitOrder == BitOrder.LEAST_SIGNIFICANT_BIT)
						out = (short)(Byte.toUnsignedInt(currentByte(byteIndex, subByteIndex)) >> (8 - bits));
				}
				else if(bits < remainingBits)
				{
					if(bitOrder == BitOrder.MOST_SIGNIFICANT_BIT)
						out = (short)((currentByte(byteIndex,
						                           subByteIndex) >> (remainingBits - bits)) & BitUtil.getBitMask(bits));
					else if(bitOrder == BitOrder.LEAST_SIGNIFICANT_BIT)
						out = (short)((currentByte(byteIndex, subByteIndex) >> subByteIndex) & BitUtil.getBitMask(
							bits));
				}
				else
				{
					int secondBits = bits - remainingBits;
					
					if(byteOrder == ByteOrder.BIG_ENDIAN)
						return (short)((read(remainingBits, byteOrder, bitOrder) << secondBits) | read(secondBits,
						                                                                               byteOrder,
						                                                                               bitOrder));
					else if(byteOrder == ByteOrder.LITTLE_ENDIAN)
						return (short)(read(remainingBits, byteOrder, bitOrder) | (read(secondBits, byteOrder,
						                                                                bitOrder) << remainingBits));
				}
				
				byteIndex += (subByteIndex + bits) >> 3;
				subByteIndex = (subByteIndex + bits) & 7;
				
				afterJump(byteIndex, subByteIndex);
				
				return out;
			}
			else if(bits <= 64)
			{
				int bytes = (bits - remainingBits) >> 3;
				int lastByte = bits - ((bytes << 3) + remainingBits);
				
				long out = read(remainingBits, byteOrder, bitOrder);
				
				if(byteOrder == ByteOrder.BIG_ENDIAN)
					out <<= bits - remainingBits;
				
				for(int i = 0; i < bytes; i++)
					if(byteOrder == ByteOrder.BIG_ENDIAN)
						out |= read(8, byteOrder, bitOrder) << (bits - remainingBits - ((i + 1) << 3));
					else
						out |= read(8, byteOrder, bitOrder) << (remainingBits + (i << 3));
				
				if(lastByte > 0)
					if(byteOrder == ByteOrder.BIG_ENDIAN)
						out |= read(lastByte, byteOrder, bitOrder);
					else
						out |= read(lastByte, byteOrder, bitOrder) << (bits - lastByte);
				
				return out;
			}
		}
		
		return -1;
	}
	
	protected abstract byte currentByte(long byteIndex, int subByteIndex);
	
	protected void afterJump(long byteIndex, int subByteIndex) {}
	
	protected boolean skip(long bytes) { return true; }
	
	/**
	 * Jumps to a byte address in the data. The method returns true if the jump succeeded, or false if not. When jumping
	 * to an out-of-bounds address, the method should return false.<br><br>
	 * <p>
	 * Note that this method should jump to the start of a byte, not in the middle of one. Otherwise, goToBit is
	 * required.<br><br>
	 * <p>
	 * It is recommended to handle the output if you are not sure about, for example, how long the data is.
	 *
	 * @param byteIndex The address to jump to.
	 * @return True if the jump succeeded.
	 * @see #goToBit(long)
	 */
	public boolean goToByte(long byteIndex)
	{
		if(byteIndex >= 0)
		{
			long skips = byteIndex - this.byteIndex;
			
			if(skips == 0)
			{
				subByteIndex = 0;
				return true;
			}
			else if(skip(skips))
			{
				this.byteIndex = byteIndex;
				subByteIndex = 0;
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Jumps to a bit address in the data. The method returns true if the jump succeeded, or false if not. When jumping
	 * to an out-of-bounds address, the method should return false.<br><br>
	 * <p>
	 * Note that this method should only be used for cases when you want to start reading in the middle of a byte.
	 * Otherwise, goToByte is required.<br><br>
	 * <p>
	 * It is recommended to handle the output if you are not sure about, for example, how long the data is.
	 *
	 * @param bitIndex The bit address to jump to.
	 * @return True if the jump succeeded.
	 * @see #goToByte(long)
	 */
	public boolean goToBit(long bitIndex)
	{
		long byteIndex = bitIndex >> 3;
		
		if(goToByte(byteIndex))
		{
			this.byteIndex = byteIndex;
			this.subByteIndex = (int)(bitIndex & 7);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Skips a number of bytes ahead in the data. This returns true if it succeeded, or false is not. When skipping out
	 * of bounds, the method should return false.<br><br>
	 * <p>
	 * Note that this method skips bytes, and should end up on the start of a byte, not in the middle of one. Otherwise,
	 * skipBits is required.<br><br>
	 * <p>
	 * It is recommended to handle the output if you are not sure about, for example, how long the data is.
	 *
	 * @param bytes Amount of bytes to skip.
	 * @return True if the skip succeeded.
	 * @see #skipBits(long)
	 */
	public boolean skipBytes(long bytes)
	{
		return goToByte(byteIndex + bytes);
	}
	
	/**
	 * Skips a number of bits ahead in the data. This returns true if it succeeded, or false if not. When skipping out
	 * of bounds, the method should return false.<br><br>
	 * <p>
	 * Not that this method skips bits, and may end up in the middle of a byte, not in the start of one. Otherwise,
	 * skipBytes is required.<br><br>
	 * <p>
	 * It is recommended to handle the output if you are not sure about, for example, how long the data is.
	 *
	 * @param bits Amount of bits to skip.
	 * @return True if the skip succeeded.
	 * @see #skipBytes(long)
	 */
	public boolean skipBits(long bits)
	{
		return goToBit(getBitIndex() + bits);
	}
	
	/**
	 * Gets the current byte position in the data.
	 *
	 * @return Current index in bytes.
	 */
	public final long getByteIndex()
	{
		return byteIndex;
	}
	
	/**
	 * Gets the current bit position in the data.
	 *
	 * @return Current index in bits.
	 */
	public final long getBitIndex()
	{
		return (byteIndex << 3) | subByteIndex;
	}
	
	/**
	 * Gets the amount of bytes in the data.
	 *
	 * @return Data length in bytes.
	 */
	public abstract long getByteSize();
	
	/**
	 * Gets the amount of bits in the data.
	 *
	 * @return Data length in bits.
	 */
	public long getBitSize()
	{
		return getByteSize() << 3;
	}
	
	/**
	 * Gets the remaining amount of bytes to read. This is be the same as the byte size minus the byte index. This means
	 * that the current byte is always included in the amount of remaining bytes, regardless if it is in the middle of
	 * that byte.
	 *
	 * @return Remaining data length in bytes.
	 */
	public long getRemainingBytes()
	{
		return getByteSize() - byteIndex;
	}
	
	/**
	 * Gets the remaining amount of bits to read. This is the same as the bit size minus the bit index.
	 *
	 * @return Remaining data length in bits.
	 */
	public long getRemainingBits()
	{
		return getBitSize() - getBitIndex();
	}
	
	/**
	 * Returns whether or not there is any data to read. This is the same as {@code getRemainingBits() == 0}.
	 *
	 * @return True if the data has ended.
	 */
	public final boolean hasEnded()
	{
		return hasEnded(byteIndex, subByteIndex);
	}
	
	protected abstract boolean hasEnded(long byteIndex, int subByteIndex);
	
	/**
	 * Reads from the data and returns a byte.
	 *
	 * @return A byte (between 0 and 8 bits), or -1 if bits is out of range (not between 0 and 8).
	 */
	public byte readAsByte() { return readAsByte(8, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns a byte.
	 *
	 * @param bitOrder Bit order to use.
	 * @return A byte (between 0 and 8 bits), or -1 if bits is out of range (not between 0 and 8).
	 */
	public byte readAsByte(BitOrder bitOrder) { return readAsByte(8, defaultByteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns a byte.
	 *
	 * @param byteOrder Byte order to use.
	 * @return A byte (between 0 and 8 bits), or -1 if bits is out of range (not between 0 and 8).
	 */
	public byte readAsByte(ByteOrder byteOrder) { return readAsByte(8, byteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns a byte.
	 *
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return A byte (between 0 and 8 bits), or -1 if bits is out of range (not between 0 and 8).
	 */
	public byte readAsByte(ByteOrder byteOrder, BitOrder bitOrder) { return readAsByte(8, byteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns a byte.
	 *
	 * @param bits Bits to read. 0 bits will always return 0.
	 * @return A byte (between 0 and 8 bits), or -1 if bits is out of range (not between 0 and 8).
	 */
	public byte readAsByte(int bits) { return readAsByte(bits, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns a byte.
	 *
	 * @param bits     Bits to read. 0 bits will always return 0.
	 * @param bitOrder Bit order to use.
	 * @return A byte (between 0 and 8 bits), or -1 if bits is out of range (not between 0 and 8).
	 */
	public byte readAsByte(int bits, BitOrder bitOrder) { return readAsByte(bits, defaultByteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns a byte.
	 *
	 * @param bits      Bits to read. 0 bits will always return 0.
	 * @param byteOrder Byte order to use.
	 * @return A byte (between 0 and 8 bits), or -1 if bits is out of range (not between 0 and 8).
	 */
	public byte readAsByte(int bits, ByteOrder byteOrder) { return readAsByte(bits, byteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns a byte.
	 *
	 * @param bits      Bits to read. 0 bits will always return 0.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return A byte (between 0 and 8 bits), or -1 if bits is out of range (not between 0 and 8).
	 */
	public byte readAsByte(int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return (byte)read(bits, byteOrder, bitOrder);
	}
	
	/**
	 * Reads from the data and returns a short.
	 *
	 * @return A short (16 bits).
	 */
	public short readAsShort() { return readAsShort(16, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns a short.
	 *
	 * @param bitOrder Bit order to use.
	 * @return A short (16 bits).
	 */
	public short readAsShort(BitOrder bitOrder) { return readAsShort(16, defaultByteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns a short.
	 *
	 * @param byteOrder Byte order to use.
	 * @return A short (16 bits).
	 */
	public short readAsShort(ByteOrder byteOrder) { return readAsShort(16, byteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns a short.
	 *
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return A short (16 bits).
	 */
	public short readAsShort(ByteOrder byteOrder, BitOrder bitOrder) { return readAsShort(16, byteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns a short.
	 *
	 * @param bits Bits to read. 0 bits will always return 0.
	 * @return A short (between 0 and 16 bits), or -1 if bits is out of range (not between 0 and 16).
	 */
	public short readAsShort(int bits) { return readAsShort(bits, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns a short.
	 *
	 * @param bits     Bits to read. 0 bits will always return 0.
	 * @param bitOrder Bit order to use.
	 * @return A short (between 0 and 16 bits), or -1 if bits is out of range (not between 0 and 16).
	 */
	public short readAsShort(int bits, BitOrder bitOrder) { return readAsShort(bits, defaultByteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns a short.
	 *
	 * @param bits      Bits to read. 0 bits will always return 0.
	 * @param byteOrder Byte order to use.
	 * @return A short (between 0 and 16 bits), or -1 if bits is out of range (not between 0 and 16).
	 */
	public short readAsShort(int bits, ByteOrder byteOrder) { return readAsShort(bits, byteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns a short.
	 *
	 * @param bits      Bits to read. 0 bits will always return 0.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return A short (between 0 and 16 bits), or -1 if bits is out of range (not between 0 and 16).
	 */
	public short readAsShort(int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return (short)read(bits, byteOrder, bitOrder);
	}
	
	/**
	 * Reads from the data and returns an int.
	 *
	 * @return An int (32 bits).
	 */
	public int readAsInt() { return readAsInt(32, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns an int.
	 *
	 * @param bitOrder Bit order to use.
	 * @return An int (32 bits).
	 */
	public int readAsInt(BitOrder bitOrder) { return readAsInt(32, defaultByteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns an int.
	 *
	 * @param byteOrder Byte order to use.
	 * @return An int (32 bits).
	 */
	public int readAsInt(ByteOrder byteOrder) { return readAsInt(32, byteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns an int.
	 *
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return An int (32 bits).
	 */
	public int readAsInt(ByteOrder byteOrder, BitOrder bitOrder) { return readAsInt(32, byteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns an int.
	 *
	 * @param bits Bits to read. 0 bits will always return 0.
	 * @return An int (between 0 and 32 bits), or -1 if bits is out of range (not between 0 and 32).
	 */
	public int readAsInt(int bits) { return readAsInt(bits, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns an int.
	 *
	 * @param bits     Bits to read. 0 bits will always return 0.
	 * @param bitOrder Bit order to use.
	 * @return An int (between 0 and 32 bits), or -1 if bits is out of range (not between 0 and 32).
	 */
	public int readAsInt(int bits, BitOrder bitOrder) { return readAsInt(bits, defaultByteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns an int.
	 *
	 * @param bits      Bits to read. 0 bits will always return 0.
	 * @param byteOrder Byte order to use.
	 * @return An int (between 0 and 32 bits), or -1 if bits is out of range (not between 0 and 32).
	 */
	public int readAsInt(int bits, ByteOrder byteOrder) { return readAsInt(bits, byteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns an int.
	 *
	 * @param bits      Bits to read. 0 bits will always return 0.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return An int (between 0 and 32 bits), or -1 if bits is out of range (not between 0 and 32).
	 */
	public int readAsInt(int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return (int)read(bits, byteOrder, bitOrder);
	}
	
	/**
	 * Reads from the data and returns a long.
	 *
	 * @return A long (64 bits).
	 */
	public long readAsLong() { return readAsLong(64, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns a long.
	 *
	 * @param bitOrder Bit order to use.
	 * @return A long (64 bits).
	 */
	public long readAsLong(BitOrder bitOrder) { return readAsLong(64, defaultByteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns a long.
	 *
	 * @param byteOrder Byte order to use.
	 * @return A long (64 bits).
	 */
	public long readAsLong(ByteOrder byteOrder) { return readAsLong(64, byteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns a long.
	 *
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return A long (64 bits).
	 */
	public long readAsLong(ByteOrder byteOrder, BitOrder bitOrder) { return readAsLong(64, byteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns a long.
	 *
	 * @param bits Bits to read. 0 bits will always return 0.
	 * @return A long (between 0 and 64 bits), or -1 if bits is out of range (not between 0 and 64).
	 */
	public long readAsLong(int bits) { return readAsLong(bits, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns a long.
	 *
	 * @param bits     Bits to read. 0 bits will always return 0.
	 * @param bitOrder Bit order to use.
	 * @return A long (between 0 and 64 bits), or -1 if bits is out of range (not between 0 and 64).
	 */
	public long readAsLong(int bits, BitOrder bitOrder) { return readAsLong(bits, defaultByteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns a long.
	 *
	 * @param bits      Bits to read. 0 bits will always return 0.
	 * @param byteOrder Byte order to use.
	 * @return A long (between 0 and 64 bits), or -1 if bits is out of range (not between 0 and 64).
	 */
	public long readAsLong(int bits, ByteOrder byteOrder) { return readAsLong(bits, byteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns a long.
	 *
	 * @param bits      Bits to read. 0 bits will always return 0.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return A long (between 0 and 64 bits), or -1 if bits is out of range (not between 0 and 64).
	 */
	public long readAsLong(int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return read(bits, byteOrder, bitOrder);
	}
	
	/**
	 * Reads a float from the data.
	 *
	 * @return A float from the data.
	 */
	public float readAsFloat() { return readAsFloat(defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Reads a float from the data.
	 *
	 * @param bitOrder Bit order to use.
	 * @return A float from the data.
	 */
	public float readAsFloat(BitOrder bitOrder) { return readAsFloat(defaultByteOrder, bitOrder); }
	
	/**
	 * Reads a float from the data.
	 *
	 * @param byteOrder Byte order to use.
	 * @return A float from the data.
	 */
	public float readAsFloat(ByteOrder byteOrder) { return readAsFloat(byteOrder, defaultBitOrder); }
	
	/**
	 * Reads a float from the data.
	 *
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return A float from the data.
	 */
	public float readAsFloat(ByteOrder byteOrder, BitOrder bitOrder)
	{
		return Float.intBitsToFloat(readAsInt(32, byteOrder, bitOrder));
	}
	
	/**
	 * Reads a double from the data.
	 *
	 * @return A double from the data.
	 */
	public double readAsDouble() { return readAsDouble(defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Reads a double from the data.
	 *
	 * @param bitOrder Bit order to use.
	 * @return A double from the data.
	 */
	public double readAsDouble(BitOrder bitOrder) { return readAsDouble(defaultByteOrder, bitOrder); }
	
	/**
	 * Reads a double from the data.
	 *
	 * @param byteOrder Byte order to use.
	 * @return A double from the data.
	 */
	public double readAsDouble(ByteOrder byteOrder) { return readAsDouble(byteOrder, defaultBitOrder); }
	
	/**
	 * Reads a double from the data.
	 *
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return A double from the data.
	 */
	public double readAsDouble(ByteOrder byteOrder, BitOrder bitOrder)
	{
		return Double.longBitsToDouble(readAsLong(64, byteOrder, bitOrder));
	}
	
	/**
	 * Reads one bit as a boolean from the data.
	 *
	 * @return A boolean from the data.
	 */
	public boolean readAsBoolean() { return readAsBoolean(defaultBitOrder); }
	
	/**
	 * Reads one bit as a boolean from the data.
	 *
	 * @param bitOrder Bit order to use.
	 * @return A boolean from the data.
	 */
	public boolean readAsBoolean(BitOrder bitOrder)
	{
		return readAsByte(1, bitOrder) == 1;
	}
	
	/**
	 * Reads an array of bytes from the data.
	 *
	 * @param length Amount of bytes to read.
	 * @return A byte array from the data.
	 */
	public byte[] readAsBytes(int length) { return readAsBytes(length, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Reads an array of bytes from the data.
	 *
	 * @param length   Amount of bytes to read.
	 * @param bitOrder Bit order to use.
	 * @return A byte array from the data.
	 */
	public byte[] readAsBytes(int length, BitOrder bitOrder)
	{
		return readAsBytes(length, defaultByteOrder, bitOrder);
	}
	
	/**
	 * Reads an array of bytes from the data.
	 *
	 * @param length    Amount of bytes to read.
	 * @param byteOrder Byte order to use.
	 * @return A byte array from the data.
	 */
	public byte[] readAsBytes(int length, ByteOrder byteOrder)
	{
		return readAsBytes(length, byteOrder, defaultBitOrder);
	}
	
	/**
	 * Reads an array of bytes from the data.
	 *
	 * @param length    Amount of bytes to read.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return A byte array from the data.
	 */
	public byte[] readAsBytes(int length, ByteOrder byteOrder, BitOrder bitOrder)
	{
		byte[] out = new byte[length];
		
		for(int i = 0; i < length; i++)
			out[i] = readAsByte(8, byteOrder, bitOrder);
		
		return out;
	}
	
	/**
	 * Reads an array of bytes until it reads a null byte (zero). The null byte is excluded.
	 *
	 * @return A null terminated byte array from the data.
	 */
	public byte[] readAsBytesUntilNull()
	{
		return readAsBytesUntilNull(defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Reads an array of bytes until it reads a null byte (zero). The null byte is excluded.
	 *
	 * @param bitOrder Bit order to use.
	 * @return A null terminated byte array from the data.
	 */
	public byte[] readAsBytesUntilNull(BitOrder bitOrder)
	{
		return readAsBytesUntilNull(defaultByteOrder, bitOrder);
	}
	
	/**
	 * Reads an array of bytes until it reads a null byte (zero). The null byte is excluded.
	 *
	 * @param byteOrder Byte order to use.
	 * @return A null terminated byte array from the data.
	 */
	public byte[] readAsBytesUntilNull(ByteOrder byteOrder)
	{
		return readAsBytesUntilNull(byteOrder, defaultBitOrder);
	}
	
	/**
	 * Reads an array of bytes until it reads a null byte (zero). The null byte is excluded.
	 *
	 * @param byteOrder Byte order to use.
	 * @param bitOrder Bit order to use.
	 * @return A null terminated byte array from the data.
	 */
	public byte[] readAsBytesUntilNull(ByteOrder byteOrder, BitOrder bitOrder)
	{
		try(ByteArrayOutputStream stream = new ByteArrayOutputStream())
		{
			byte in;
			while((in = readAsByte(8, byteOrder, bitOrder)) != 0)
				stream.write(in);
			
			return stream.toByteArray();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Reads a string from the data.
	 *
	 * @return A string from the data.
	 */
	public String readAsString()
	{
		return readAsString(Charset.defaultCharset(), defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Reads a string from the data.
	 *
	 * @param bitOrder  Bit order to use.
	 * @return A string from the data.
	 */
	public String readAsString(BitOrder bitOrder)
	{
		return readAsString(Charset.defaultCharset(), defaultByteOrder, bitOrder);
	}
	
	/**
	 * Reads a string from the data.
	 *
	 * @param byteOrder Byte order to use.
	 * @return A string from the data.
	 */
	public String readAsString(ByteOrder byteOrder)
	{
		return readAsString(Charset.defaultCharset(), byteOrder, defaultBitOrder);
	}
	
	/**
	 * Reads a string from the data.
	 *
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return A string from the data.
	 */
	public String readAsString(ByteOrder byteOrder, BitOrder bitOrder)
	{
		return readAsString(Charset.defaultCharset(), byteOrder, bitOrder);
	}
	
	/**
	 * Reads a string from the data.
	 *
	 * @param charset   Charset to use.
	 * @return A string from the data.
	 */
	public String readAsString(Charset charset)
	{
		return readAsString(charset, defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Reads a string from the data.
	 *
	 * @param charset   Charset to use.
	 * @param bitOrder  Bit order to use.
	 * @return A string from the data.
	 */
	public String readAsString(Charset charset, BitOrder bitOrder)
	{
		return readAsString(charset, defaultByteOrder, bitOrder);
	}
	
	/**
	 * Reads a string from the data.
	 *
	 * @param charset   Charset to use.
	 * @param byteOrder Byte order to use.
	 * @return A string from the data.
	 */
	public String readAsString(Charset charset, ByteOrder byteOrder)
	{
		return readAsString(charset, byteOrder, defaultBitOrder);
	}
	
	/**
	 * Reads a string from the data.
	 *
	 * @param charset   Charset to use.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return A string from the data.
	 */
	public String readAsString(Charset charset, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return new String(readAsBytesUntilNull(byteOrder, bitOrder), charset);
	}
	
	/**
	 * Sets the default byte order. In reading methods it is optional to pass in a byte order as one of the arguments.
	 * The default byte order is used when no byte order is specified.
	 *
	 * @param byteOrder Byte order to set as default.
	 */
	public final void setDefaultByteOrder(ByteOrder byteOrder)
	{
		defaultByteOrder = byteOrder;
	}
	
	/**
	 * Sets the default bit order. In reading methods it is optional to pass in a bit order as one of the arguments.
	 * The default bit order is used when no bit order is specified.
	 *
	 * @param bitOrder Bit order to set as default.
	 */
	public final void setDefaultBitOrder(BitOrder bitOrder)
	{
		defaultBitOrder = bitOrder;
	}
	
	/**
	 * Gets the default byte order.
	 *
	 * @return The default byte order.
	 */
	public final ByteOrder getDefaultByteOrder()
	{
		return defaultByteOrder;
	}
	
	/**
	 * Gets the default bit order.
	 *
	 * @return The default bit order.
	 */
	public final BitOrder getDefaultBitOrder()
	{
		return defaultBitOrder;
	}
}
