package se.albin.jbinary;

import java.nio.ByteOrder;

/**
 * A bit output stream that writes to a byte array.
 */
@SuppressWarnings({ "WeakerAccess", "unused", "ConstantConditions", "UnusedReturnValue" })
public final class BitArrayOutputStream extends BitOutputStream
{
	private static final int DEFAULT_INITIAL_SIZE = 64;
	private static final int DEFAULT_BUFFER_SIZE = 16;
	
	private byte[] data;
	
	private final int bufferSize;
	
	private long lastIndex;
	
	/**
	 * Creates a new bit writer with a default initial size of 64 bytes, and buffer size of 16 bytes.<br>
	 * Default byte order is big endian, and default bit order is most significant.
	 *
	 * @see ByteOrder#BIG_ENDIAN
	 * @see BitOrder#MOST_SIGNIFICANT_BIT
	 */
	public BitArrayOutputStream()
	{
		this(DEFAULT_INITIAL_SIZE, DEFAULT_BUFFER_SIZE, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	/**
	 * Creates a new bit writer with a default initial size of 64 bytes, and buffer size of 16 bytes.<br>
	 * Default byte order is big endian.
	 *
	 * @param defaultBitOrder Default bit order to use.
	 * @see ByteOrder#BIG_ENDIAN
	 */
	public BitArrayOutputStream(BitOrder defaultBitOrder)
	{
		this(DEFAULT_INITIAL_SIZE, DEFAULT_BUFFER_SIZE, ByteOrder.BIG_ENDIAN, defaultBitOrder);
	}
	
	/**
	 * Creates a new bit writer with a default initial size of 64 bytes, and buffer size of 16 bytes.<br>
	 * Default bit order is most significant.
	 *
	 * @param defaultByteOrder Default byte order to use.
	 * @see BitOrder#MOST_SIGNIFICANT_BIT
	 */
	public BitArrayOutputStream(ByteOrder defaultByteOrder)
	{
		this(DEFAULT_INITIAL_SIZE, DEFAULT_BUFFER_SIZE, defaultByteOrder, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	/**
	 * Creates a new bit writer with a default initial size of 64 bytes, and buffer size of 16 bytes.
	 *
	 * @param defaultByteOrder Default byte order to use.
	 * @param defaultBitOrder  Default bit order to use.
	 */
	public BitArrayOutputStream(ByteOrder defaultByteOrder, BitOrder defaultBitOrder)
	{
		this(DEFAULT_INITIAL_SIZE, DEFAULT_BUFFER_SIZE, defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Creates a new bit writer with a specified initial size and buffer size.<br>
	 * Default byte order is big endian, and default bit order is most significant.
	 *
	 * @param initialSize Initial size to allocate.
	 * @param bufferSize  How many bytes to allocate when you run out of free data.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see BitOrder#MOST_SIGNIFICANT_BIT
	 */
	public BitArrayOutputStream(int initialSize, int bufferSize)
	{
		this(initialSize, bufferSize, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	/**
	 * Creates a new bit writer with a specified initial size and buffer size.<br>
	 * Default byte order is big endian.
	 *
	 * @param initialSize     Initial size to allocate.
	 * @param bufferSize      How many bytes to allocate when you run out of free data.
	 * @param defaultBitOrder Default bit order to use.
	 * @see ByteOrder#BIG_ENDIAN
	 */
	public BitArrayOutputStream(int initialSize, int bufferSize, BitOrder defaultBitOrder)
	{
		this(initialSize, bufferSize, ByteOrder.BIG_ENDIAN, defaultBitOrder);
	}
	
	/**
	 * Creates a new bit writer with a specified initial size and buffer size.<br>
	 * Default bit order is most significant.
	 *
	 * @param initialSize      Initial size to allocate.
	 * @param bufferSize       How many bytes to allocate when you run out of free data.
	 * @param defaultByteOrder Default byte order to use.
	 * @see BitOrder#MOST_SIGNIFICANT_BIT
	 */
	public BitArrayOutputStream(int initialSize, int bufferSize, ByteOrder defaultByteOrder)
	{
		this(initialSize, bufferSize, defaultByteOrder, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	/**
	 * Creates a new bit writer with a specified initial size and buffer size.
	 *
	 * @param initialSize      Initial size to allocate.
	 * @param bufferSize       How many bytes to allocate when you run out of free data.
	 * @param defaultByteOrder Default byte order to use.
	 * @param defaultBitOrder  Default bit order to use.
	 */
	public BitArrayOutputStream(int initialSize, int bufferSize, ByteOrder defaultByteOrder, BitOrder defaultBitOrder)
	{
		data = new byte[initialSize];
		this.bufferSize = bufferSize;
		
		this.defaultByteOrder = defaultByteOrder;
		this.defaultBitOrder = defaultBitOrder;
	}
	
	@Override
	protected void write(byte b, long byteIndex, int subByteIndex)
	{
		data[(int)byteIndex] = b;
	}
	
	@Override
	protected void afterJump(long byteIndex, int subByteIndex)
	{
		long newIndex = (byteIndex << 3) | subByteIndex;
		
		if(newIndex > lastIndex)
			lastIndex = newIndex;
		
		expandDataIfOver(newIndex);
	}
	
	/**
	 * Expands the data by some amount of bytes.
	 *
	 * @param buffer How many bytes to expand by.
	 */
	public void expandData(int buffer)
	{
		setDataSize(data.length + buffer);
	}
	
	private void expandDataIfOver(long newBitIndex)
	{
		int byteIndex = (int)((newBitIndex >> 3) + ((newBitIndex & 7) > 0 ? 1 : 0));
		
		if(byteIndex >= data.length)
			setDataSize(byteIndex + bufferSize);
	}
	
	/**
	 * Sets the data size.
	 *
	 * @param size A new size for the data.
	 * @return True if it succeeded, or false if the provided size is less than or equal to the current size.
	 */
	public boolean setDataSize(int size)
	{
		if(size <= data.length || size < 0)
			return false;
		
		byte[] newData = new byte[size];
		System.arraycopy(data, 0, newData, 0, data.length);
		data = newData;
		
		return true;
	}
	
	@Override
	public long getByteSize() { return (lastIndex >> 3) + (((lastIndex & 7) > 0 ? 1 : 0)); }
	
	@Override
	protected void closeStream()
	{
	
	}
	
	/**
	 * Gets the data that has been written.
	 *
	 * @return The data.
	 */
	public byte[] getData()
	{
		byte[] out = new byte[(int)getByteSize()];
		
		System.arraycopy(data, 0, out, 0, out.length);
		out[out.length - 1] = unwrittenByte();
		
		return out;
	}
}
