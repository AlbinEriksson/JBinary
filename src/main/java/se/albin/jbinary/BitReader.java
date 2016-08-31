package se.albin.jbinary;

import java.nio.ByteOrder;

public final class BitReader
{
	private final byte[] data;
	
	private int bitIndex;
	
	/**
	 * Creates a new bit reader using the byte array as data.
	 * @param data Data to read from.
	 */
	public BitReader(byte[] data)
	{
		this.data = data;
	}
	
	/**
	 * Reads from the data and returns it as a byte. (Big endian, most significant bit order)
	 * @param bits How many bits to read. Between 1 and 8.
	 * @return A byte, with the desired bits, or -1 if the bits are not between 1 and 8, or there is no more data to
	 * read.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see BitOrder#MOST_SIGNIFICANT
	 */
	public byte getNextByte(int bits) { return getNextByte(bits, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT); }
	
	/**
	 * Reads from the data and returns it as a byte. (Big endian)<br>
	 *
	 * @param bits How many bits to read. Between 1 and 8.
	 * @param bitOrder The bit order. Most significant means left ro right, least significant means right to left.
	 * @return A byte, with the desired bits, or -1, if the bits are not between 1 and 8, or there is no more data to
	 * read.
	 * @see ByteOrder#BIG_ENDIAN
	 */
	public byte getNextByte(int bits, BitOrder bitOrder) { return getNextByte(bits, ByteOrder.BIG_ENDIAN, bitOrder); }
	
	/**
	 * Reads from the data and returns it as a byte. (Most significant bit order)<br>
	 * @param bits How many bits to read. Between 1 and 8.
	 * @param byteOrder The byte order. Big endian means first goes first, little endian means reversed.
	 * @return A byte, with the desired bits, or -1, if the bits are not between 1 and 8, or there is no more data to
	 * read.
	 * @see BitOrder#MOST_SIGNIFICANT
	 */
	public byte getNextByte(int bits, ByteOrder byteOrder) { return getNextByte(bits, byteOrder,
	                                                                            BitOrder.MOST_SIGNIFICANT); }
	
	/**
	 * Reads from the data and returns it as a byte.
	 * @param bits How many bits to read. Between 1 and 8.
	 * @param byteOrder The byte order. Big endian means first goes first, little endian means reversed.
	 * @param bitOrder The bit order. Most significant means left ro right, least significant means right to left.
	 * @return A byte, with the desired bits, or -1, if the bits are not between 1 and 8, or there is no more data to
	 * read.
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
	 * Reads from the data and returns it as an integer. (Big endian, most significant bit order)
	 * @param bits How many bits to read. Between 1 and 32.
	 * @return An integer, with the desired bits, or -1, if the bits are not between 1 and 32, or there is no more data
	 * to read.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see BitOrder#MOST_SIGNIFICANT
	 */
	public int getNextInt(int bits) { return getNextInt(bits, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT); }
	
	/**
	 * Reads from the data and returns it as an integer. (Big endian)
	 * @param bits How many bits to read. Between 1 and 32.
	 * @param bitOrder The bit order. Most significant means left ro right, least significant means right to left.
	 * @return An integer, with the desired bits, or -1, if the bits are not between 1 and 32, or there is no more data
	 * to read.
	 * @see ByteOrder#BIG_ENDIAN
	 */
	public int getNextInt(int bits, BitOrder bitOrder) { return getNextInt(bits, ByteOrder.BIG_ENDIAN, bitOrder); }
	
	/**
	 * Reads from the data and returns it as an integer. (Most significant bit order)
	 * @param bits How many bits to read. Between 1 and 32.
	 * @param byteOrder The byte order. Big endian means first goes first, little endian means reversed.
	 * @return An integer, with the desired bits, or -1, if the bits are not between 1 and 32, or there is no more data
	 * to read.
	 * @see BitOrder#MOST_SIGNIFICANT
	 */
	public int getNextInt(int bits, ByteOrder byteOrder) { return getNextByte(bits, byteOrder,
	                                                                          BitOrder.MOST_SIGNIFICANT); }
	
	/**
	 * Reads from the data and returns it as an integer.
	 * @param bits How many bits to read. Between 1 and 32.
	 * @param byteOrder The byte order. Big endian means first goes first, little endian means reversed.
	 * @param bitOrder The bit order. Most significant means left ro right, least significant means right to left.
	 * @return An integer, with the desired bits, or -1, if the bits are not between 1 and 32, or there is no more data
	 * to read.
	 */
	public int getNextInt(int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		if(32 >= bits && bits >= 1)
			return (int)getNextLong(bits, byteOrder, bitOrder);
		else
			return -1;
	}
	
	/**
	 * Reads from the data and returns it as a long. (Big endian, most significant bit order)
	 * @param bits How many bits to read. Between 1 and 64.
	 * @return A long, with the desired bits, or -1, if the bits are not between 1 and 64, or there is no more data to
	 * read.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see BitOrder#MOST_SIGNIFICANT
	 */
	public long getNextLong(int bits) { return getNextLong(bits, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT); }
	
	/**
	 * Reads from the data and returns it as a long. (Big endian)
	 * @param bits How many bits to read. Between 1 and 64.
	 * @param bitOrder The bit order. Most significant means left ro right, least significant means right to left.
	 * @return A long, with the desired bits, or -1, if the bits are not between 1 and 64, or there is no more data to
	 * read.
	 * @see ByteOrder#BIG_ENDIAN
	 */
	public long getNextLong(int bits, BitOrder bitOrder) { return getNextLong(bits, ByteOrder.BIG_ENDIAN, bitOrder); }
	
	/**
	 * Reads from the data and returns it as a long. (Most significant bit order)
	 * @param bits How many bits to read. Between 1 and 64.
	 * @param byteOrder The byte order. Big endian means first goes first, little endian means reversed.
	 * @return A long, with the desired bits, or -1, if the bits are not between 1 and 64, or there is no more data to
	 * read.
	 * @see BitOrder#MOST_SIGNIFICANT
	 */
	public long getNextLong(int bits, ByteOrder byteOrder) { return getNextLong(bits, byteOrder,
	                                                                            BitOrder.MOST_SIGNIFICANT); }
	
	/**
	 * Reads from the data and returns it as a long.
	 * @param bits How many bits to read. Between 1 and 64.
	 * @param byteOrder The byte order. Big endian means first goes first, little endian means reversed.
	 * @param bitOrder The bit order. Most significant means left ro right, least significant means right to left.
	 * @return A long, with the desired bits, or -1, if the bits are not between 1 and 64, or there is no more data to
	 * read.
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
	 * Reads from the data and returns it as a boolean. (Most significant bit order)
	 * @return True if the bit was 1, or false if the end was hit.
	 * @see BitOrder#MOST_SIGNIFICANT
	 */
	public boolean getNextBoolean() { return getNextBoolean(BitOrder.MOST_SIGNIFICANT); }
	
	/**
	 * Reads from the data and returns it as a boolean.
	 * @param bitOrder The bit order. Most significant means left ro right, least significant means right to left.
	 * @return True if the bit was 1, or false if the end was hit.
	 */
	public boolean getNextBoolean(BitOrder bitOrder) { return !hasEnded() && getNextByte(1, bitOrder) == 1; }
	
	/**
	 * Goes to the desired byte in the data.
	 * @param byteIndex The pointer to go to.
	 */
	public void setByteIndex(int byteIndex) { bitIndex = byteIndex << 3; }
	
	/**
	 * Goes to the desired bit in the data. Bit index 8 means the same thing was byte index 1.
	 * @param bitIndex The bit pointer to go to.
	 */
	public void setBitIndex(int bitIndex) { this.bitIndex = bitIndex; }
	
	/**
	 * Adds to the current byte index.
	 * @param bytes How many bytes to jump.
	 */
	public void addByteIndex(int bytes) { bitIndex += bytes << 3; }
	
	/**
	 * Adds to the current bit index.
	 * @param bits How many bits to jump.
	 */
	public void addBitIndex(int bits) { bitIndex += bits; }
	
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
