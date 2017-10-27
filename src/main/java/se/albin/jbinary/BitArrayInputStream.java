package se.albin.jbinary;

import java.io.IOException;
import java.nio.ByteOrder;

/**
 * A bit input stream that takes in a byte array as data. Not recommended for large heaps of data, use the file input
 * stream instead.
 *
 * @see BitFileInputStream
 */
@SuppressWarnings("unused")
public final class BitArrayInputStream extends BitInputStream
{
	private byte[] data;
	
	/**
	 * Creates a new bit reader using the byte array as data.<br>
	 * Default byte order is big endian, and default bit order is most significant.
	 * @param data Data to read from.
	 * @see ByteOrder#BIG_ENDIAN
	 * @see BitOrder#MOST_SIGNIFICANT_BIT
	 */
	public BitArrayInputStream(byte[] data)
	{
		this(data, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	/**
	 * Creates a new bit reader using the byte array as data.<br>
	 * Default byte order is big endian.
	 * @param data Data to read from.
	 * @param defaultBitOrder Default bit order to use.
	 */
	public BitArrayInputStream(byte[] data, BitOrder defaultBitOrder)
	{
		this(data, ByteOrder.BIG_ENDIAN, defaultBitOrder);
	}
	
	/**
	 * Creates a new bit reader using the byte array as data.<br>
	 * Default bit order is most significant.
	 * @param data Data to read from.
	 * @param defaultByteOrder Default byte order to use.
	 */
	public BitArrayInputStream(byte[] data, ByteOrder defaultByteOrder)
	{
		this(data, defaultByteOrder, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	/**
	 * Creates a new bit reader using the byte array as data.
	 * @param data Data to read from.
	 * @param defaultByteOrder Default byte order to use.
	 * @param defaultBitOrder Default bit order to use.
	 */
	public BitArrayInputStream(byte[] data, ByteOrder defaultByteOrder, BitOrder defaultBitOrder)
	{
		this.data = data;
		
		this.defaultByteOrder = defaultByteOrder;
		this.defaultBitOrder = defaultBitOrder;
	}
	
	@Override
	protected byte currentByte(long byteIndex, int subByteIndex)
	{
		return data[(int)byteIndex];
	}
	
	@Override
	public long getByteSize() { return data.length; }
	
	@Override
	public boolean hasEnded(long byteIndex, int subByteIndex) { return byteIndex >= data.length; }
	
	@Override
	public void close() throws IOException
	{
	
	}
	
	public void setData(byte[] data)
	{
		this.data = data;
	}
}
