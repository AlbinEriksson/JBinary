package se.albin.jbinary;

import java.nio.ByteOrder;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class BitWriter
{
	private static final int DEFAULT_INITIAL_SIZE = 64;
	private static final int DEFAULT_BUFFER_SIZE = 16;
	
	private byte[] data;
	
	private final int bufferSize;
	
	private ByteOrder defaultByteOrder;
	private BitOrder defaultBitOrder, activeBitOrder;
	
	private int bitIndex;
	private int lastIndex;
	
	/**
	 * Creates a new bit writer with a default initial size of 64 bytes, and buffer size of 16 bytes.<br>
	 * Default byte order is big endian, and default bit order is most significant.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see BitOrder#MOST_SIGNIFICANT
	 */
	public BitWriter() { this(DEFAULT_INITIAL_SIZE, DEFAULT_BUFFER_SIZE, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT); }
	
	/**
	 * Creates a new bit writer with a default initial size of 64 bytes, and buffer size of 16 bytes.<br>
	 * Default byte order is big endian.
	 * @param defaultBitOrder Default bit order to use.
	 * @see ByteOrder#BIG_ENDIAN
	 */
	public BitWriter(BitOrder defaultBitOrder) { this(DEFAULT_INITIAL_SIZE, DEFAULT_BUFFER_SIZE, ByteOrder.BIG_ENDIAN, defaultBitOrder); }
	
	/**
	 * Creates a new bit writer with a default initial size of 64 bytes, and buffer size of 16 bytes.<br>
	 * Default bit order is most significant.
	 * @param defaultByteOrder Default byte order to use.
	 * @see BitOrder#MOST_SIGNIFICANT
	 */
	public BitWriter(ByteOrder defaultByteOrder) { this(DEFAULT_INITIAL_SIZE, DEFAULT_BUFFER_SIZE, defaultByteOrder, BitOrder.MOST_SIGNIFICANT); }
	
	/**
	 * Creates a new bit writer with a default initial size of 64 bytes, and buffer size of 16 bytes.
	 * @param defaultByteOrder Default byte order to use.
	 * @param defaultBitOrder Default bit order to use.
	 */
	public BitWriter(ByteOrder defaultByteOrder, BitOrder defaultBitOrder) { this(DEFAULT_INITIAL_SIZE, DEFAULT_BUFFER_SIZE, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Creates a new bit writer with a specified initial size and buffer size.<br>
	 * Default byte order is big endian, and default bit order is most significant.
	 * @param initialSize Initial size to allocate.
	 * @param bufferSize How many bytes to allocate when you run out of free data.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see BitOrder#MOST_SIGNIFICANT
	 */
	public BitWriter(int initialSize, int bufferSize) { this(initialSize, bufferSize, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT); }
	
	/**
	 * Creates a new bit writer with a specified initial size and buffer size.<br>
	 * Default byte order is big endian.
	 * @param initialSize Initial size to allocate.
	 * @param bufferSize How many bytes to allocate when you run out of free data.
	 * @param defaultBitOrder Default bit order to use.
	 * @see ByteOrder#BIG_ENDIAN
	 */
	public BitWriter(int initialSize, int bufferSize, BitOrder defaultBitOrder) { this(initialSize, bufferSize, ByteOrder.BIG_ENDIAN, defaultBitOrder); }
	
	/**
	 * Creates a new bit writer with a specified initial size and buffer size.<br>
	 * Default bit order is most significant.
	 * @param initialSize Initial size to allocate.
	 * @param bufferSize How many bytes to allocate when you run out of free data.
	 * @param defaultByteOrder Default byte order to use.
	 * @see BitOrder#MOST_SIGNIFICANT
	 */
	public BitWriter(int initialSize, int bufferSize, ByteOrder defaultByteOrder) { this(initialSize, bufferSize, defaultByteOrder, BitOrder.MOST_SIGNIFICANT); }
	
	/**
	 * Creates a new bit writer with a specified initial size and buffer size.
	 * @param initialSize Initial size to allocate.
	 * @param bufferSize How many bytes to allocate when you run out of free data.
	 * @param defaultByteOrder Default byte order to use.
	 * @param defaultBitOrder Default bit order to use.
	 */
	public BitWriter(int initialSize, int bufferSize, ByteOrder defaultByteOrder, BitOrder defaultBitOrder)
	{
		data = new byte[initialSize];
		this.bufferSize = bufferSize;
		
		this.defaultByteOrder = defaultByteOrder;
		this.defaultBitOrder = defaultBitOrder;
	}
	
	/**
	 * Writes one byte of specified bits to the data. Uses the default bit and byte order.
	 * @param b Byte to write.
	 * @param bits How many bits to write. Between 1 and 8.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 8 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putByte(byte b, int bits) { return putByte(b, bits, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Writes one byte of specified bits to the data. Uses the default byte order.
	 * @param b Byte to write.
	 * @param bits How many bits to write. Between 1 and 8.
	 * @param bitOrder The bit order.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 8 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putByte(byte b, int bits, BitOrder bitOrder) { return putByte(b, bits, defaultByteOrder, bitOrder); }
	
	/**
	 * Writes one byte of specified bits to the data. Uses the default bit order.
	 * @param b Byte to write.
	 * @param bits How many bits to write. Between 1 and 8.
	 * @param byteOrder The byte order.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 8 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putByte(byte b, int bits, ByteOrder byteOrder) { return putByte(b, bits, byteOrder, defaultBitOrder); }
	
	/**
	 * Writes one byte of specified bits to the data. Uses the default bit and byte order.
	 * @param b Byte to write.
	 * @param bits How many bits to write. Between 1 and 8.
	 * @param byteOrder The byte order.
	 * @param bitOrder The bit order.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 8 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putByte(byte b, int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		if(1 <= bits && bits <= 8)
		{
			int subIndex = bitIndex & 7;
			int byteIndex = bitIndex >> 3;
			int remainingBits = 8 - subIndex;
			
			if(subIndex != 0 && activeBitOrder != null && bitOrder != activeBitOrder)
				return false;
			
			if(byteIndex >= data.length)
			  expandData(bufferSize);
			
			if(subIndex == 0)
			{
				activeBitOrder = bitOrder;
				
				if(bitOrder == BitOrder.MOST_SIGNIFICANT)
					data[byteIndex] = (byte)((b & getMask(bits)) << (8 - bits));
				else
					data[byteIndex] = (byte)(b & getMask(bits));
			}
			else if(bits <= remainingBits)
			{
				if(bitOrder == BitOrder.MOST_SIGNIFICANT)
					data[byteIndex] |= (b & getMask(bits)) << (remainingBits - bits);
				else
					data[byteIndex] |= (b & getMask(bits)) << (8 - remainingBits);
			}
			else if(bits > remainingBits)
			{
				int secondBits = bits - remainingBits;
				
				if(byteOrder == ByteOrder.BIG_ENDIAN)
				{
					putByte((byte)(b >> secondBits), remainingBits, byteOrder, bitOrder);
					putByte(b, secondBits, byteOrder, bitOrder);
				}
				else
				{
					putByte(b, remainingBits, byteOrder, bitOrder);
					putByte((byte)(b >> remainingBits), secondBits, byteOrder, bitOrder);
				}
				
				return true;
			}
			
			bitIndex += bits;
			if((bitIndex & 7) == 0)
				activeBitOrder = null;
			
			if(bitIndex > lastIndex)
				lastIndex = bitIndex;
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Writes one short of specified bits to the data. Uses the default bit and byte order.
	 * @param s Short to write.
	 * @param bits How many bits to write. Between 1 and 16.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 16 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putShort(short s, int bits) { return putShort(s, bits, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Writes one short of specified bits to the data. Uses the default byte order.
	 * @param s Short to write.
	 * @param bits How many bits to write. Between 1 and 16.
	 * @param bitOrder The bit order.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 16 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putShort(short s, int bits, BitOrder bitOrder) { return putShort(s, bits, defaultByteOrder, bitOrder); }
	
	/**
	 * Writes one short of specified bits to the data. Uses the default bit order.
	 * @param s Short to write.
	 * @param bits How many bits to write. Between 1 and 16.
	 * @param byteOrder The byte order.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 16 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putShort(short s, int bits, ByteOrder byteOrder) { return putShort(s, bits, byteOrder, defaultBitOrder); }
	
	/**
	 * Writes one short of specified bits to the data.
	 * @param s Short to write.
	 * @param bits How many bits to write. Between 1 and 16.
	 * @param byteOrder The byte order.
	 * @param bitOrder The bit order.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 16 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putShort(short s, int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return 1 <= bits && bits <= 16 && putLong(s, bits, byteOrder, bitOrder);
	}
	
	/**
	 * Writes one integer of specified bits to the data. Uses the default bit and byte order.
	 * @param i Integer to write.
	 * @param bits How many bits to write. Between 1 and 32.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 32 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putInt(int i, int bits) { return putInt(i, bits, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Writes one integer of specified bits to the data. Uses the default byte order.
	 * @param i Integer to write.
	 * @param bits How many bits to write. Between 1 and 32.
	 * @param bitOrder The bit order.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 32 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putInt(int i, int bits, BitOrder bitOrder) { return putInt(i, bits, defaultByteOrder, bitOrder); }
	
	/**
	 * Writes one integer of specified bits to the data. Uses the default bit order.
	 * @param i Integer to write.
	 * @param bits How many bits to write. Between 1 and 32.
	 * @param byteOrder The byte order.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 32 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putInt(int i, int bits, ByteOrder byteOrder) { return putInt(i, bits, byteOrder, defaultBitOrder); }
	
	/**
	 * Writes one integer of specified bits to the data.
	 * @param i Integer to write.
	 * @param bits How many bits to write. Between 1 and 32.
	 * @param byteOrder The byte order.
	 * @param bitOrder The bit order.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 32 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putInt(int i, int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return 1 <= bits && bits <= 32 && putLong(i, bits, byteOrder, bitOrder);
	}
	
	/**
	 * Writes one long of specified bits to the data. Uses the default bit and byte order.
	 * @param l Long to write.
	 * @param bits How many bits to write. Between 1 and 64.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 64 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putLong(long l, int bits) { return putLong(l, bits, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Writes one long of specified bits to the data. Uses the default byte order.
	 * @param l Long to write.
	 * @param bits How many bits to write. Between 1 and 64.
	 * @param bitOrder The bit order.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 64 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putLong(long l, int bits, BitOrder bitOrder) { return putLong(l, bits, defaultByteOrder, bitOrder); }
	
	/**
	 * Writes one long of specified bits to the data. Uses the default bit order.
	 * @param l Long to write.
	 * @param bits How many bits to write. Between 1 and 64.
	 * @param byteOrder The byte order.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 64 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putLong(long l, int bits, ByteOrder byteOrder)
	{ return putLong(l, bits, byteOrder, defaultBitOrder); }
	
	/**
	 * Writes one long of specified bits to the data.
	 * @param l Long to write.
	 * @param bits How many bits to write. Between 1 and 64.
	 * @param byteOrder The byte order.
	 * @param bitOrder The bit order.
	 * @return True if the data was written successfully, or false if bits was not between 1 and 64 or if the active bit
	 * order of the current byte is different from the bit order used.
	 */
	public boolean putLong(long l, int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		if(1 <= bits && bits <= 64)
		{
			if(bits <= 8)
				return putByte((byte)l, bits, byteOrder, bitOrder);
			
			int subIndex = bitIndex & 7;
			int remainingBits = 8 - subIndex;
			int fullBytes = bits >> 3;
			int endBits = bits - (fullBytes << 3) + subIndex;
			
			if(bits >= remainingBits)
			{
				if(byteOrder == ByteOrder.BIG_ENDIAN)
				{
					if(!putByte((byte)(l >> (bits - remainingBits)), remainingBits, byteOrder, bitOrder))
						return false;
				}
				else
					if(!putByte((byte)l, remainingBits, byteOrder, bitOrder))
						return false;
			}
			
			for(int i = 1; i < fullBytes; i++)
			{
				if(byteOrder == ByteOrder.BIG_ENDIAN)
					putByte((byte)(l >> (((fullBytes - i - 1) << 3) + endBits)), 8, byteOrder, bitOrder);
				else
					putByte((byte)(l >> (((i - 1) << 3) + remainingBits)), 8, byteOrder, bitOrder);
			}
			
			if(endBits > 0)
			{
				if(byteOrder == ByteOrder.BIG_ENDIAN)
					putByte((byte)l, endBits, byteOrder, bitOrder);
				else
					putByte((byte)(l >> (bits - endBits)), endBits, byteOrder, bitOrder);
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Writes one float to the data (Floats must be 32-bit). Uses the default bit and byte order.
	 * @param f Float to write.
	 * @return True if the data was written successfully, or false if the active bit order of the current byte is
	 * different from the bit order used.
	 */
	public boolean putFloat(float f) { return putFloat(f, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Writes one float to the data (Floats must be 32-bit). Uses the default byte order.
	 * @param f Float to write.
	 * @param bitOrder The bit order.
	 * @return True if the data was written successfully, or false if the active bit order of the current byte is
	 * different from the bit order used.
	 */
	public boolean putFloat(float f, BitOrder bitOrder) { return putFloat(f, defaultByteOrder, bitOrder); }
	
	/**
	 * Writes one float to the data (Floats must be 32-bit). Uses the default bit order.
	 * @param f Float to write.
	 * @param byteOrder The byte order.
	 * @return True if the data was written successfully, or false if the active bit order of the current byte is
	 * different from the bit order used.
	 */
	public boolean putFloat(float f, ByteOrder byteOrder) { return putFloat(f, byteOrder, defaultBitOrder); }
	
	/**
	 * Writes one float to the data (Floats must be 32-bit).
	 * @param f Float to write.
	 * @param byteOrder The byte order.
	 * @param bitOrder The bit order.
	 * @return True if the data was written successfully, or false if the active bit order of the current byte is
	 * different from the bit order used.
	 */
	public boolean putFloat(float f, ByteOrder byteOrder, BitOrder bitOrder)
	{ return putInt(Float.floatToIntBits(f), 32, byteOrder, bitOrder); }
	
	/**
	 * Writes one double to the data (Doubles must be 64-bit). Uses the default bit and byte order.
	 * @param d Double to write.
	 * @return True if the data was written successfully, or false if the active bit order of the current byte is
	 * different from the bit order used.
	 */
	public boolean putDouble(double d) { return putDouble(d, defaultByteOrder, defaultBitOrder); }
	
	/**
	 * Writes one double to the data (Doubles must be 64-bit). Uses the default byte order.
	 * @param d Double to write.
	 * @param bitOrder The bit order.
	 * @return True if the data was written successfully, or false if the active bit order of the current byte is
	 * different from the bit order used.
	 */
	public boolean putDouble(double d, BitOrder bitOrder) { return putDouble(d, defaultByteOrder, bitOrder); }
	
	/**
	 * Writes one double to the data (Doubles must be 64-bit). Uses the default bit order.
	 * @param d Double to write.
	 * @param byteOrder The byte order.
	 * @return True if the data was written successfully, or false if the active bit order of the current byte is
	 * different from the bit order used.
	 */
	public boolean putDouble(double d, ByteOrder byteOrder) { return putDouble(d, byteOrder, defaultBitOrder); }
	
	/**
	 * Writes one double to the data (Doubles must be 64-bit). Uses the default bit and byte order.
	 * @param d Double to write.
	 * @param byteOrder The byte order.
	 * @param bitOrder The bit order.
	 * @return True if the data was written successfully, or false if the active bit order of the current byte is
	 * different from the bit order used.
	 */
	public boolean putDouble(double d, ByteOrder byteOrder, BitOrder bitOrder)
	{ return putLong(Double.doubleToLongBits(d), 64, byteOrder, bitOrder); }
	
	/**
	 * Writes one boolean to the data. Uses the default bit order.
	 * @param b Boolean to write.
	 * @return True if the data was written successfully, or false if the active bit order of the current byte is
	 * different from the bit order used.
	 */
	public boolean putBoolean(boolean b) { return putBoolean(b, defaultBitOrder); }
	
	/**
	 * Writes one boolean to the data.
	 * @param b Boolean to write.
	 * @param bitOrder The bit order.
	 * @return True if the data was written successfully, or false if the active bit order of the current byte is
	 * different from the bit order used.
	 */
	public boolean putBoolean(boolean b, BitOrder bitOrder) { return putByte((byte)(b ? 1 : 0), 1, bitOrder); }
	
	/**
	 * Expands the data by some amount of bytes.
	 * @param buffer How many bytes to expand by.
	 */
	public void expandData(int buffer)
	{
		setDataSize(data.length + buffer);
	}
	
	private void expandDataIfOver()
	{
		int byteIndex = bitIndex >> 3;
		if(byteIndex >= data.length)
			setDataSize(byteIndex + bufferSize);
	}
	
	/**
	 * Sets the data size.
	 * @param size A new size for the data.
	 * @return True if it succeeded, or false if the provided size is less than or equal to the current size.
	 */
	public boolean setDataSize(int size)
	{
		if(size <= data.length)
			return false;
		
		byte[] newData = new byte[size];
		System.arraycopy(data, 0, newData, 0, data.length);
		data = newData;
		
		return true;
	}
	
	/**
	 * Goes to the desired byte in the data, at the first bit. The data will expand if it goes over bounds.
	 * @param byteIndex The pointer to go to.
	 * @return True if it succeeded, or false if the pointed byte was less than 0.
	 */
	public boolean setByteIndex(int byteIndex)
	{
		if(byteIndex < 0)
			return false;
		
		bitIndex = byteIndex << 3;
		expandDataIfOver();
		return true;
	}
	
	/**
	 * Goes to the desired bit in the data. The data will expand if it goes over bounds.
	 * @param bitIndex The pointer to go to.
	 * @return True if it succeeded, or false if the pointed bit was less than 0.
	 */
	public boolean setBitIndex(int bitIndex)
	{
		if(bitIndex < 0)
			return false;
		
		this.bitIndex += bitIndex;
		expandDataIfOver();
		return true;
	}
	
	/**
	 * Adds to the current byte index and goes to the first bit. The data will expand if it goes over bounds.
	 * @param bytes How many bytes to jump.
	 * @return True if it succeeded, or false if it goes below 0.
	 */
	public boolean addByteIndex(int bytes)
	{
		int newBitIndex = bitIndex & 0xFFFFFFF8 + bytes << 3;
		if(newBitIndex < 0)
			return false;
		
		bitIndex = newBitIndex;
		expandDataIfOver();
		return true;
	}
	
	/**
	 * Adds to the current bit index. The data will expand if it goes over bounds.
	 * @param bits How many bits to jump.
	 * @return True if it succeeded, or false if it goes below 0.
	 */
	public boolean addBitIndex(int bits)
	{
		int newBitIndex = bitIndex + bits;
		if(newBitIndex < 0)
			return false;
		
		bitIndex = newBitIndex;
		expandDataIfOver();
		return true;
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
	 * Gets the data that has been written. The data will be cut after the last byte, i.e. you won't have trailing 0
	 * bytes at the end of your data.
	 * @return The data.
	 */
	public byte[] getData()
	{
		byte[] out;
		if((lastIndex & 7) != 0)
			out = new byte[(lastIndex >> 3) + 1];
		else
			out = new byte[lastIndex >> 3];
		System.arraycopy(data, 0, out, 0, out.length);
		return out;
	}
	
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
			long mask = 0;
			for(int i = 0; i < bits; i++)
				mask |= 1 << i;
			return mask;
		}
		else
			return -1;
	}
}
