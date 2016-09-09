package se.albin.jbinary;

import java.nio.ByteOrder;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class BitReader
{
	private final byte[] data;
	
	private int bitIndex;
	private ByteOrder defaultByteOrder;
	private BitOrder defaultBitOrder;
	
	/**
	 * Creates a new bit reader using the byte array as data.<br>
	 * Default byte order is big endian, and default bit order is most significant.
	 * @param data Data to read from.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see BitOrder#MOST_SIGNIFICANT
	 */
	public BitReader(byte[] data)
	{
		this(data, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT);
	}
	
	/**
	 * Creates a new bit reader using the byte array as data.<br>
	 * Default byte order is big endian.
	 * @param data Data to read from.
	 * @param defaultBitOrder Default bit order to use.
	 */
	public BitReader(byte[] data, BitOrder defaultBitOrder)
	{
		this(data, ByteOrder.BIG_ENDIAN, defaultBitOrder);
	}
	
	/**
	 * Creates a new bit reader using the byte array as data.<br>
	 * Default bit order is most significant.
	 * @param data Data to read from.
	 * @param defaultByteOrder Default byte order to use.
	 */
	public BitReader(byte[] data, ByteOrder defaultByteOrder)
	{
		this(data, defaultByteOrder, BitOrder.MOST_SIGNIFICANT);
	}
	
	/**
	 * Creates a new bit reader using the byte array as data.
	 * @param data Data to read from.
	 * @param defaultByteOrder Default byte order to use.
	 * @param defaultBitOrder Default bit order to use.
	 */
	public BitReader(byte[] data, ByteOrder defaultByteOrder, BitOrder defaultBitOrder)
	{
		this.data = data;
		
		this.defaultByteOrder = defaultByteOrder;
		this.defaultBitOrder = defaultBitOrder;
	}
	
	/**
	 * Reads from the data and returns it as a byte. Uses the default bit and byte order.
	 * @param bits How many bits to read. Between 1 and 8.
	 * @return A byte from the data, or -1 if the bits are not between 1 and 8, or there is no more data to read.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see ByteOrder#LITTLE_ENDIAN
	 * @see BitOrder
	 */
	public byte getNextByte(int bits) { return getNextByte(bits, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns it as a byte. Uses the default byte order.
	 * @param bits How many bits to read. Between 1 and 8.
	 * @param bitOrder The bit order.
	 * @return A byte from the data, or -1, if the bits are not between 1 and 8, or there is no more data to read.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see ByteOrder#LITTLE_ENDIAN
	 */
	public byte getNextByte(int bits, BitOrder bitOrder) { return getNextByte(bits, defaultByteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns it as a byte. Uses the default bit order.<br>
	 * @param bits How many bits to read. Between 1 and 8.
	 * @param byteOrder The byte order.
	 * @return A byte from the data, or -1, if the bits are not between 1 and 8, or there is no more data to read.
	 * @see BitOrder
	 */
	public byte getNextByte(int bits, ByteOrder byteOrder) { return getNextByte(bits, byteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns it as a byte.
	 * @param bits How many bits to read. Between 1 and 8.
	 * @param byteOrder The byte order.
	 * @param bitOrder The bit order.
	 * @return A byte from the data, or -1, if the bits are not between 1 and 8, or there is no more data to read.
	 */
	public byte getNextByte(int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		if(!hasEnded() &&
			8 >= bits && bits >= 1)
		{
			if(bitIndex + bits > getBitSize())
				bits = data.length - bitIndex;
			
			byte out = -1;
			int byteIndex = bitIndex >> 3;
			int subIndex = bitIndex & 7;
			int remainingBits = 8 - subIndex;
			
			if(subIndex == 0)
			{
				if(bitOrder == BitOrder.MOST_SIGNIFICANT)
					out = (byte)(Byte.toUnsignedInt(data[byteIndex]) >> (8 - bits));
				else if(bitOrder == BitOrder.LEAST_SIGNIFICANT)
					out = (byte)(data[byteIndex] & getMask(bits));
			}
			else if(bits == remainingBits)
			{
				if(bitOrder == BitOrder.MOST_SIGNIFICANT)
					out = (byte)(data[byteIndex] & getMask(bits));
				else if(bitOrder == BitOrder.LEAST_SIGNIFICANT)
					out = (byte)(Byte.toUnsignedInt(data[byteIndex]) >> (8 - bits));
			}
			else if(bits < remainingBits)
			{
				if(bitOrder == BitOrder.MOST_SIGNIFICANT)
					out = (byte)((data[byteIndex] >> (remainingBits - bits)) & getMask(bits));
				else if(bitOrder == BitOrder.LEAST_SIGNIFICANT)
					out = (byte)((data[byteIndex] >> subIndex) & getMask(bits));
			}
			else
			{
				int secondBits = bits - remainingBits;
				
				if(byteOrder == ByteOrder.BIG_ENDIAN)
					return (byte)(
						(getNextByte(remainingBits, bitOrder) << secondBits) | getNextByte(secondBits, bitOrder));
				else
					return (byte)(
						getNextByte(remainingBits, bitOrder) | (getNextByte(secondBits, bitOrder) << remainingBits));
			}
			
			bitIndex += bits;
			
			return out;
		}
		else
			return -1;
	}
	
	/**
	 * Reads from the data and returns it as a short. Uses the default bit and byte order.
	 * @param bits How many bits to read. Between 1 and 16.
	 * @return A short from the data, or -1, if the bits are not between 1 and 16, or there is no more data to read.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see ByteOrder#LITTLE_ENDIAN
	 * @see BitOrder
	 */
	public short getNextShort(int bits) { return getNextShort(bits, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns it as a short. Uses the default byte order.
	 * @param bits How many bits to read. Between 1 and 16.
	 * @param bitOrder The bit order.
	 * @return A short from the data, or -1, if the bits are not between 1 and 16, or there is no more data to read.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see ByteOrder#LITTLE_ENDIAN
	 */
	public short getNextShort(int bits, BitOrder bitOrder) { return getNextShort(bits, defaultByteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns it as a a short. Uses the default bit order.
	 * @param bits How many bits to read. Between 1 and 16.
	 * @param byteOrder The byte order.
	 * @return A short from the data, or -1, if the bits are not between 1 and 16, or there is no more data to read.
	 * @see BitOrder
	 */
	public short getNextShort(int bits, ByteOrder byteOrder) { return getNextShort(bits, byteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns it as a short.
	 * @param bits How many bits to read. Between 1 and 16.
	 * @param byteOrder The byte order.
	 * @param bitOrder The bit order.
	 * @return A short from the data, or -1, if the bits are not between 1 and 16, or there is no more data to read.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see ByteOrder#LITTLE_ENDIAN
	 * @see BitOrder
	 */
	public short getNextShort(int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		if(16 >= bits && bits >= 1)
			return (short)getNextLong(bits, byteOrder, bitOrder);
		else
			return -1;
	}
	
	/**
	 * Reads from the data and returns it as an integer. Uses the default bit and byte order.
	 * @param bits How many bits to read. Between 1 and 32.
	 * @return An integer from the data, or -1, if the bits are not between 1 and 32, or there is no more data to read.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see ByteOrder#LITTLE_ENDIAN
	 * @see BitOrder
	 */
	public int getNextInt(int bits) { return getNextInt(bits, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns it as an integer. Uses the default byte order.
	 * @param bits How many bits to read. Between 1 and 32.
	 * @param bitOrder The bit order.
	 * @return An integer from the data, or -1, if the bits are not between 1 and 32, or there is no more data to read.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see ByteOrder#LITTLE_ENDIAN
	 */
	public int getNextInt(int bits, BitOrder bitOrder) { return getNextInt(bits, defaultByteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns it as an integer. Uses the default bit order.
	 * @param bits How many bits to read. Between 1 and 32.
	 * @param byteOrder The byte order.
	 * @return An integer from the data, or -1, if the bits are not between 1 and 32, or there is no more data to read.
	 * @see BitOrder
	 */
	public int getNextInt(int bits, ByteOrder byteOrder) { return getNextByte(bits, byteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns it as an integer.
	 * @param bits How many bits to read. Between 1 and 32.
	 * @param byteOrder The byte order.
	 * @param bitOrder The bit order.
	 * @return An integer from the data, or -1, if the bits are not between 1 and 32, or there is no more data to read.
	 */
	public int getNextInt(int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		if(32 >= bits && bits >= 1)
			return (int)getNextLong(bits, byteOrder, bitOrder);
		else
			return -1;
	}
	
	/**
	 * Reads from the data and returns it as a long. Uses the default bit and byte order.
	 * @param bits How many bits to read. Between 1 and 64.
	 * @return A long from the data. or -1, if the bits are not between 1 and 64, or there is no more data to read.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see ByteOrder#LITTLE_ENDIAN
	 * @see BitOrder
	 */
	public long getNextLong(int bits) { return getNextLong(bits, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns it as a long. Uses the default byte order.
	 * @param bits How many bits to read. Between 1 and 64.
	 * @param bitOrder The bit order.
	 * @return A long from the data, or -1, if the bits are not between 1 and 64, or there is no more data to read.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see ByteOrder#LITTLE_ENDIAN
	 */
	public long getNextLong(int bits, BitOrder bitOrder) { return getNextLong(bits, defaultByteOrder, bitOrder); }
	
	/**
	 * Reads from the data and returns it as a long. Uses the default bit order.
	 * @param bits How many bits to read. Between 1 and 64.
	 * @param byteOrder The byte order.
	 * @return A long from the data, or -1, if the bits are not between 1 and 64, or there is no more data to read.
	 * @see BitOrder
	 */
	public long getNextLong(int bits, ByteOrder byteOrder) { return getNextLong(bits, byteOrder, defaultBitOrder); }
	
	/**
	 * Reads from the data and returns it as a long.
	 * @param bits How many bits to read. Between 1 and 64.
	 * @param byteOrder The byte order.
	 * @param bitOrder The bit order.
	 * @return A long from the data, or -1, if the bits are not between 1 and 64, or there is no more data to read.
	 */
	public long getNextLong(int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		if(!hasEnded() &&
			64 >= bits && bits >= 1)
		{
			if(bitIndex + bits > getBitSize())
				bits = data.length - bitIndex;
			
			if(bits <= 8)
				return Byte.toUnsignedLong(getNextByte(bits, bitOrder));
			else
			{
				int firstByte = 8 - (bitIndex & 7);
				int bytes = bits >> 3;
				int byteBits = ((bytes - 1) << 3) + firstByte;
				int lastByte = bits - byteBits;
				
				long out = Byte.toUnsignedLong(getNextByte(firstByte, bitOrder));
				
				if(byteOrder == ByteOrder.BIG_ENDIAN)
					out <<= bits - firstByte;
				
				for(int i = 1; i < bytes; i++)
					if(byteOrder == ByteOrder.BIG_ENDIAN)
						out |= Byte.toUnsignedLong(getNextByte(8, bitOrder)) << (bits - firstByte - (i << 3));
					else
						out |= Byte.toUnsignedLong(getNextByte(8, bitOrder)) << (firstByte - ((i - 1) << 3));
				
				if(lastByte > 0)
					if(byteOrder == ByteOrder.BIG_ENDIAN)
						out |= Byte.toUnsignedLong(getNextByte(lastByte, bitOrder));
					else if(firstByte == 8)
						out |= Byte.toUnsignedLong(getNextByte(lastByte, bitOrder)) << (bytes << 3);
					else
						out |= Byte.toUnsignedLong(getNextByte(lastByte, bitOrder)) << ((bytes << 3) - lastByte);
				
				return out;
			}
		}
		
		return -1;
	}
	
	/**
	 * Reads from the data and returns it as a boolean. Uses the default bit order.
	 * @return True if the bit was 1, or false if the bit was 0, or the end was hit.
	 * @see BitOrder
	 */
	public boolean getNextBoolean() { return getNextBoolean(defaultBitOrder); }
	
	/**
	 * Reads from the data and returns it as a boolean.
	 * @param bitOrder The bit order.
	 * @return True if the bit was 1, or false if the bit was 0, or the end was hit.
	 */
	public boolean getNextBoolean(BitOrder bitOrder) { return !hasEnded() && getNextByte(1, bitOrder) == 1; }
	
	/**
	 * Goes to the desired byte in the data, at the first bit.
	 * @param byteIndex The pointer to go to.
	 * @throws IndexOutOfBoundsException If the pointed byte is out of bounds.
	 */
	public void setByteIndex(int byteIndex)
	{
		if(0 <= byteIndex && byteIndex < getByteSize())
			bitIndex = byteIndex << 3;
		else
			throw new IndexOutOfBoundsException("Setting byte index out of bounds!");
	}
	
	/**
	 * Goes to the desired bit in the data. Bit index 8 means the same thing was byte index 1.
	 * @param bitIndex The bit pointer to go to.
	 * @throws IndexOutOfBoundsException If the pointed bit is out of bounds.
	 */
	public void setBitIndex(int bitIndex)
	{
		if(0 <= bitIndex && bitIndex < getBitSize())
			this.bitIndex = bitIndex;
		else
			throw new IndexOutOfBoundsException("Setting bit index out of bounds!");
	}
	
	/**
	 * Adds to the current byte index. Goes to first bit.
	 * @param bytes How many bytes to jump.
	 * @throws IndexOutOfBoundsException If the jump will go out of bounds.
	 */
	public void addByteIndex(int bytes)
	{
		int nextIndex = bitIndex & 0xFFFFFFF8 + bytes << 3;
		
		if(0 <= nextIndex && nextIndex < getBitSize())
			bitIndex = nextIndex;
		else
			throw new IndexOutOfBoundsException("Adding byte index out of bounds!");
	}
	
	/**
	 * Adds to the current bit index.
	 * @param bits How many bits to jump.
	 * @throws IndexOutOfBoundsException If the jump will go out of bounds.
	 */
	public void addBitIndex(int bits)
	{
		int nextIndex = bitIndex + bits;
		
		if(0 <= nextIndex && nextIndex < getBitSize())
			bitIndex = nextIndex;
		else
			throw new IndexOutOfBoundsException("Adding bit index out of bounds!");
	}
	
	/**
	 * Gets the current byte index
	 * @return The byte index.
	 */
	public int getByteIndex() { return bitIndex >> 3; }
	
	/**
	 * Gets the current bit index.
	 * @return The bit index.
	 */
	public int getBitIndex() { return bitIndex; }
	
	/**
	 * Gets the size of the data in bytes.
	 * @return The size in bytes.
	 */
	public int getByteSize() { return data.length; }
	
	/**
	 * Gets the size of the data in bits.
	 * @return The size in bits.
	 */
	public int getBitSize() { return data.length << 3; }
	
	/**
	 * Checks to see if the bit reader has hit the end of the data.
	 * @return True if the end has been hit.
	 */
	public boolean hasEnded() { return (bitIndex >> 3) >= data.length; }
	
	/**
	 * Sets the default byte order. When reading data without telling what byte order to use, it will use the byte order
	 * specified here.
	 * @param byteOrder The byte order to use when not specified. Nothing happens if null.
	 */
	public void setDefaultByteOrder(ByteOrder byteOrder)
	{
		if(byteOrder != null)
			defaultByteOrder = byteOrder;
	}
	
	/**
	 * Sets the default bit order. When reading data without telling what bit order to use, it will use the bit order
	 * specified here.
	 * @param bitOrder The bit order to use when not specified. Nothing happens if null.
	 */
	public void setDefaultBitOrder(BitOrder bitOrder)
	{
		if(bitOrder != null)
			defaultBitOrder = bitOrder;
	}
	
	/**
	 * Returns the current default byte order.
	 * @return The default byte order.
	 */
	public ByteOrder getDefaultByteOrder() { return defaultByteOrder; }
	
	/**
	 * Returns the current default bit order.
	 * @return The default bit order.
	 */
	public BitOrder getDefaultBitOrder() { return defaultBitOrder; }
	
	private long getMask(int bits)
	{
		if(64 >= bits && bits >= 0)
		{
			byte mask = 0;
			for(int i = 0; i < bits; i++)
				mask |= 1 << i;
			return mask;
		}
		else
			return -1;
	}
}
