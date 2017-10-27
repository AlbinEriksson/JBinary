package se.albin.jbinary;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * An abstract bit output stream, that writes data to a resource. This class and some of its methods can be overridden,
 * to create a customized stream.
 */
@SuppressWarnings({ "unused", "WeakerAccess", "SameParameterValue" })
public abstract class BitOutputStream implements Closeable, AutoCloseable
{
	protected ByteOrder defaultByteOrder;
	protected BitOrder defaultBitOrder;
	
	private long byteIndex;
	private int subByteIndex;
	private byte data;
	
	private boolean write(long out, int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		if(1 <= bits && bits <= 64)
		{
			int remainingBits = 8 - subByteIndex;
			
			if(bits <= 8)
			{
				if(subByteIndex == 0)
				{
					if(bitOrder == BitOrder.MOST_SIGNIFICANT_BIT)
						data = (byte)((out & BitUtil.getBitMask(bits)) << (8 - bits));
					else
						data = (byte)(out & BitUtil.getBitMask(bits));
				}
				else if(bits <= remainingBits)
				{
					if(bitOrder == BitOrder.MOST_SIGNIFICANT_BIT)
						data |= (out & BitUtil.getBitMask(bits)) << (remainingBits - bits);
					else
						data |= (out & BitUtil.getBitMask(bits)) << (8 - remainingBits);
				}
				else if(bits > remainingBits)
				{
					int secondBits = bits - remainingBits;
					
					if(byteOrder == ByteOrder.BIG_ENDIAN)
					{
						return write(out >> secondBits, remainingBits, byteOrder, bitOrder)
							&& write(out, secondBits, byteOrder, bitOrder);
						
					}
					else
					{
						return write(out, remainingBits, byteOrder, bitOrder)
							&& write(out >> remainingBits, secondBits, byteOrder, bitOrder);
					}
				}
				
				int newSubByteIndex = subByteIndex + bits;
				
				if(newSubByteIndex >= 8)
				{
					write(data, byteIndex, subByteIndex);
					data = 0;
				}
				
				byteIndex += newSubByteIndex >> 3;
				subByteIndex = newSubByteIndex & 7;
				afterJump(byteIndex, subByteIndex);
				
				return true;
			}
			else
			{
				int fullBytes = (bits - remainingBits) >> 3;
				int endBits = bits - (fullBytes << 3) - remainingBits;
				
				if(byteOrder == ByteOrder.BIG_ENDIAN)
				{
					if(!write(out >> (bits - remainingBits), remainingBits, byteOrder, bitOrder))
						return false;
				}
				else if(!write(out, remainingBits, byteOrder, bitOrder))
					return false;
				
				for(int i = 0; i < fullBytes; i++)
				{
					if(byteOrder == ByteOrder.BIG_ENDIAN)
					{
						if(!write(out >> (((fullBytes - i - 1) << 3) + endBits), 8, byteOrder, bitOrder))
							return false;
					}
					else if(!write(out >> ((i << 3) + remainingBits), 8, byteOrder, bitOrder))
						return false;
				}
				
				if(byteOrder == ByteOrder.BIG_ENDIAN)
					return write(out, endBits, byteOrder, bitOrder);
				else
					return write(out >> (bits - endBits), endBits, byteOrder, bitOrder);
			}
		}
		
		return bits == 0;
	}
	
	protected abstract void write(byte b, long byteIndex, int subByteIndex);
	
	protected void afterJump(long byteIndex, int subByteIndex) {}
	
	protected boolean skip(long byteIndex, int subByteIndex, long bytes)
	{
		return true;
	}
	
	/**
	 * Goes to a byte position in the data. The data should be extended if it goes over the current
	 *
	 * @param byteIndex Byte index to go to.
	 * @return True if successful, or false if it failed or {@code byteIndex} is negative.
	 */
	public boolean goToByte(long byteIndex)
	{
		if(byteIndex >= 0)
		{
			long skips = byteIndex - this.byteIndex;
			
			if(skips == 0)
				return true;
			else
			{
				if(subByteIndex > 0)
				{
					write(data, this.byteIndex, subByteIndex);
					data = 0;
					skips--;
				}
				
				if(skip(this.byteIndex, subByteIndex, skips))
				{
					this.byteIndex = byteIndex;
					subByteIndex = 0;
					afterJump(byteIndex, 0);
					
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Goes to a bit position in the data. The data should be extended if it goes over the current
	 *
	 * @param bitIndex Bit index to go to.
	 * @return True if successful, or false if it failed or {@code bitIndex} is negative.
	 */
	public boolean goToBit(long bitIndex)
	{
		long byteIndex = bitIndex >> 3;
		
		if(goToByte(byteIndex))
		{
			this.subByteIndex = (int)(bitIndex & 7);
			afterJump(byteIndex, subByteIndex);
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Skips an amount of bytes in the data. The data should be extended if it goes over the current size. Remember that
	 * the position goes to the start of a byte (bit position in multiples of 8).
	 *
	 * @param bytes Number of bytes to skip.
	 * @return True if successful, or false if it failed or the position would become negative.
	 */
	public boolean skipBytes(long bytes)
	{
		return goToByte(byteIndex + bytes);
	}
	
	/**
	 * Skips an amount of bits in the data. The data should be extended if it goes over the current size.
	 *
	 * @param bits Number of bits to skip.
	 * @return True if successful, or false if it failed or the position would become negative.
	 */
	public boolean skipBits(long bits)
	{
		return goToBit(getBitIndex() + bits);
	}
	
	/**
	 * @return Byte position in the data.
	 */
	public long getByteIndex()
	{
		return byteIndex;
	}
	
	/**
	 * @return Bit position in the data.
	 */
	public long getBitIndex()
	{
		return (byteIndex << 3) | subByteIndex;
	}
	
	/**
	 * @return Size of data in bytes.
	 */
	public abstract long getByteSize();
	
	/**
	 * @return Size of data in bits.
	 */
	public long getBitSize()
	{
		return getByteSize() << 3;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void close() throws IOException
	{
		if(subByteIndex > 0)
			write(data, byteIndex, subByteIndex);
		
		closeStream();
	}
	
	protected abstract void closeStream() throws IOException;
	
	/**
	 * Writes a byte to the data.
	 *
	 * @param data Data to write.
	 * @return True if the write succeeded, or false if it failed.
	 */
	public boolean writeByte(byte data)
	{
		return writeByte(data, 8, defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a byte to the data.
	 *
	 * @param data     Data to write.
	 * @param bitOrder Bit order to use.
	 * @return True if the write succeeded, or false if it failed.
	 */
	public boolean writeByte(byte data, BitOrder bitOrder)
	{
		return writeByte(data, 8, defaultByteOrder, bitOrder);
	}
	
	/**
	 * Writes a byte to the data.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @return True if the write succeeded, or false if it failed.
	 */
	public boolean writeByte(byte data, ByteOrder byteOrder)
	{
		return writeByte(data, 8, byteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a byte to the data.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return True if the write succeeded, or false if it failed.
	 */
	public boolean writeByte(byte data, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return writeByte(data, 8, byteOrder, bitOrder);
	}
	
	/**
	 * Writes a byte to the data.
	 *
	 * @param data Data to write.
	 * @param bits Amount of bits to write.
	 * @return True if the write succeeded, or false if it failed or if bits is out of range (not between 0 and 8).
	 */
	public boolean writeByte(byte data, int bits)
	{
		return writeByte(data, bits, defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a byte to the data.
	 *
	 * @param data     Data to write.
	 * @param bits     Amount of bits to write.
	 * @param bitOrder Bit order to use.
	 * @return True if the write succeeded, or false if if it failed or bits is out of range (not between 0 and 8).
	 */
	public boolean writeByte(byte data, int bits, BitOrder bitOrder)
	{
		return writeByte(data, bits, defaultByteOrder, bitOrder);
	}
	
	/**
	 * Writes a byte to the data.
	 *
	 * @param data      Data to write.
	 * @param bits      Amount of bits to write.
	 * @param byteOrder Byte order to use.
	 * @return True if the write succeeded, or false if it failed or if bits is out of range (not between 0 and 8).
	 */
	public boolean writeByte(byte data, int bits, ByteOrder byteOrder)
	{
		return writeByte(data, bits, byteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a byte to the data.
	 *
	 * @param data      Data to write.
	 * @param bits      Amount of bits to write.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return True if the write succeeded, or false if it failed or if bits is out of range (not between 0 and 8)
	 */
	public boolean writeByte(byte data, int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return write(data, bits, byteOrder, bitOrder);
	}
	
	/**
	 * Writes a short to the data.
	 *
	 * @param data Data to write.
	 * @return True if the write succeeded.
	 */
	public boolean writeShort(short data)
	{
		return writeShort(data, 16, defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a short to the data.
	 *
	 * @param data     Data to write.
	 * @param bitOrder Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeShort(short data, BitOrder bitOrder)
	{
		return writeShort(data, 16, defaultByteOrder, bitOrder);
	}
	
	/**
	 * Writes a short to the data.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeShort(short data, ByteOrder byteOrder)
	{
		return writeShort(data, 16, byteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a short to the data.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeShort(short data, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return writeShort(data, 16, byteOrder, bitOrder);
	}
	
	/**
	 * Writes a short to the data.
	 *
	 * @param data Data to write.
	 * @param bits Amount of bits to write.
	 * @return True if the write succeeded, or false if it failed or if bits is out of range (not between 0 and 16).
	 */
	public boolean writeShort(short data, int bits)
	{
		return writeShort(data, bits, defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a short to the data.
	 *
	 * @param data     Data to write.
	 * @param bits     Amount of bits to write.
	 * @param bitOrder Bit order to use.
	 * @return True if the write succeeded, or false if it failed or if bits is out of range (not between 0 and 16).
	 */
	public boolean writeShort(short data, int bits, BitOrder bitOrder)
	{
		return writeShort(data, bits, defaultByteOrder, bitOrder);
	}
	
	/**
	 * Writes a short to the data.
	 *
	 * @param data      Data to write.
	 * @param bits      Amount of bits to write.
	 * @param byteOrder Byte order to use.
	 * @return True if the write succeeded, or false if it failed or if bits is out of range (not between 0 and 16).
	 */
	public boolean writeShort(short data, int bits, ByteOrder byteOrder)
	{
		return writeShort(data, bits, byteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a short to the data.
	 *
	 * @param data      Data to write.
	 * @param bits      Amount of bits to write.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return True if the write succeeded, or false if it failed or if bits is out of range (not between 0 and 16).
	 */
	public boolean writeShort(short data, int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return write(data, bits, byteOrder, bitOrder);
	}
	
	/**
	 * Writes an int to the data.
	 *
	 * @param data Data to write.
	 * @return True if the write succeeded.
	 */
	public boolean writeInt(int data)
	{
		return writeInt(data, 32, defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes an int to the data.
	 *
	 * @param data     Data to write.
	 * @param bitOrder Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeInt(int data, BitOrder bitOrder)
	{
		return writeInt(data, 32, defaultByteOrder, bitOrder);
	}
	
	/**
	 * Writes an int to the data.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeInt(int data, ByteOrder byteOrder)
	{
		return writeInt(data, 32, byteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes an int to the data.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeInt(int data, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return writeInt(data, 32, byteOrder, bitOrder);
	}
	
	/**
	 * Writes an int to the data.
	 *
	 * @param data Data to write.
	 * @param bits Amount of bits to write.
	 * @return True if the write succeeded, or false if it failed or if bits is out of range (not between 0 and 32).
	 */
	public boolean writeInt(int data, int bits)
	{
		return writeInt(data, bits, defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes an int to the data.
	 *
	 * @param data     Data to write.
	 * @param bits     Amount of bits to write.
	 * @param bitOrder Bit order to use.
	 * @return True if the write succeeded, or false if it failed or if bits is out of range (not between 0 and 32).
	 */
	public boolean writeInt(int data, int bits, BitOrder bitOrder)
	{
		return writeInt(data, bits, defaultByteOrder, bitOrder);
	}
	
	/**
	 * Writes an int to the data.
	 *
	 * @param data      Data to write.
	 * @param bits      Amount of bits to write.
	 * @param byteOrder Byte order to use.
	 * @return True if the write succeeded, or false if it failed or if bits is out of range (not between 0 and 32).
	 */
	public boolean writeInt(int data, int bits, ByteOrder byteOrder)
	{
		return writeInt(data, bits, byteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes an int to the data.
	 *
	 * @param data      Data to write.
	 * @param bits      Amount of bits to write.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return True if the write succeeded, or false if it failed or if bits is out of range (not between 0 and 32).
	 */
	public boolean writeInt(int data, int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return write(data, bits, byteOrder, bitOrder);
	}
	
	/**
	 * Writes a long to the data.
	 *
	 * @param data Data to write.
	 * @return True if the write succeeded.
	 */
	public boolean writeLong(long data)
	{
		return writeLong(data, 64, defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a long to the data.
	 *
	 * @param data     Data to write.
	 * @param bitOrder Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeLong(long data, BitOrder bitOrder)
	{
		return writeLong(data, 64, defaultByteOrder, bitOrder);
	}
	
	/**
	 * Writes a long to the data.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeLong(long data, ByteOrder byteOrder)
	{
		return writeLong(data, 64, byteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a long to the data.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeLong(long data, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return writeLong(data, 64, byteOrder, bitOrder);
	}
	
	/**
	 * Writes a long to the data.
	 *
	 * @param data Data to write.
	 * @param bits Amount of bits to write.
	 * @return True if the write succeeded, or false if it failed or if bits is out of range (not between 0 and 64).
	 */
	public boolean writeLong(long data, int bits)
	{
		return writeLong(data, bits, defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a long to the data.
	 *
	 * @param data     Data to write.
	 * @param bits     Amount of bits to write.
	 * @param bitOrder Bit order to use.
	 * @return True if the write succeeded, or false if it failed or if bits is out of range (not between 0 and 64).
	 */
	public boolean writeLong(long data, int bits, BitOrder bitOrder)
	{
		return writeLong(data, bits, defaultByteOrder, bitOrder);
	}
	
	/**
	 * Writes a long to the data.
	 *
	 * @param data      Data to write.
	 * @param bits      Amount of bits to write.
	 * @param byteOrder Byte order to use.
	 * @return True if the write succeeded, or false if it failed or if bits is out of range (not between 0 and 64).
	 */
	public boolean writeLong(long data, int bits, ByteOrder byteOrder)
	{
		return writeLong(data, bits, byteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a long to the data.
	 *
	 * @param data      Data to write.
	 * @param bits      Amount of bits to write.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return True if the write succeeded, or false if it failed or if bits is out of range (not between 0 and 64).
	 */
	public boolean writeLong(long data, int bits, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return write(data, bits, byteOrder, bitOrder);
	}
	
	/**
	 * Writes a float to the data.
	 *
	 * @param data Data to write.
	 * @return True if the write succeeded.
	 */
	public boolean writeFloat(float data)
	{
		return writeFloat(data, defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a float to the data.
	 *
	 * @param data     Data to write.
	 * @param bitOrder Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeFloat(float data, BitOrder bitOrder)
	{
		return writeFloat(data, defaultByteOrder, bitOrder);
	}
	
	/**
	 * Writes a float to the data.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeFloat(float data, ByteOrder byteOrder)
	{
		return writeFloat(data, byteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a float to the data.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeFloat(float data, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return write(Float.floatToIntBits(data), 32, byteOrder, bitOrder);
	}
	
	/**
	 * Writes a double to the data.
	 *
	 * @param data Data to write.
	 * @return True if the write succeeded.
	 */
	public boolean writeDouble(double data)
	{
		return writeDouble(data, defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a double to the data.
	 *
	 * @param data     Data to write.
	 * @param bitOrder Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeDouble(double data, BitOrder bitOrder)
	{
		return writeDouble(data, defaultByteOrder, bitOrder);
	}
	
	/**
	 * Writes a double to the data.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeDouble(double data, ByteOrder byteOrder)
	{
		return writeDouble(data, byteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a double to the data.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeDouble(double data, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return write(Double.doubleToLongBits(data), 64, byteOrder, bitOrder);
	}
	
	/**
	 * Writes a boolean to the data.
	 *
	 * @param data Data to write.
	 * @return True if the write succeeded.
	 */
	public boolean writeBoolean(boolean data)
	{
		return writeBoolean(data, defaultBitOrder);
	}
	
	/**
	 * Writes a boolean to the data.
	 *
	 * @param data     Data to write.
	 * @param bitOrder Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeBoolean(boolean data, BitOrder bitOrder)
	{
		return write(data ? 1 : 0, 1, defaultByteOrder, bitOrder);
	}
	
	/**
	 * Writes an array of bytes to the data.
	 *
	 * @param data Data to write.
	 * @return True if the write succeeded.
	 */
	public boolean writeBytes(byte[] data)
	{
		return writeBytes(data, defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes an array of bytes to the data.
	 *
	 * @param data     Data to write.
	 * @param bitOrder Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeBytes(byte[] data, BitOrder bitOrder)
	{
		return writeBytes(data, defaultByteOrder, bitOrder);
	}
	
	/**
	 * Writes an array of bytes to the data.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeBytes(byte[] data, ByteOrder byteOrder)
	{
		return writeBytes(data, byteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes an array of bytes to the data.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeBytes(byte[] data, ByteOrder byteOrder, BitOrder bitOrder)
	{
		for(byte b : data)
			if(!write(b, 8, byteOrder, bitOrder))
				return false;
		
		return true;
	}
	
	/**
	 * Writes a null-terminated string to the data, encoded in ASCII.
	 *
	 * @param data Data to write.
	 * @return True if the write succeeded.
	 */
	public boolean writeString(String data)
	{
		return writeString(data, Charset.defaultCharset(), defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a null-terminated string to the data, encoded in ASCII.
	 *
	 * @param data     Data to write.
	 * @param bitOrder Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeString(String data, BitOrder bitOrder)
	{
		return writeString(data, Charset.defaultCharset(), defaultByteOrder, bitOrder);
	}
	
	/**
	 * Writes a null-terminated string to the data, encoded in ASCII.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeString(String data, ByteOrder byteOrder)
	{
		return writeString(data, Charset.defaultCharset(), byteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a null-terminated string to the data, encoded in ASCII.
	 *
	 * @param data      Data to write.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeString(String data, ByteOrder byteOrder, BitOrder bitOrder)
	{
		return writeString(data, Charset.defaultCharset(), byteOrder, bitOrder);
	}
	
	/**
	 * Writes a null-terminated string to the data.
	 *
	 * @param data    Data to write.
	 * @param charset The charset that you want to use to encode the string.
	 * @return True if the write succeeded.
	 */
	public boolean writeString(String data, Charset charset)
	{
		return writeString(data, charset, defaultByteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a null-terminated string to the data.
	 *
	 * @param data     Data to write.
	 * @param charset  The charset that you want to use to encode the string.
	 * @param bitOrder Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeString(String data, Charset charset, BitOrder bitOrder)
	{
		return writeString(data, charset, defaultByteOrder, bitOrder);
	}
	
	/**
	 * Writes a null-terminated string to the data.
	 *
	 * @param data      Data to write.
	 * @param charset   The charset that you want to use to encode the string.
	 * @param byteOrder Byte order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeString(String data, Charset charset, ByteOrder byteOrder)
	{
		return writeString(data, charset, byteOrder, defaultBitOrder);
	}
	
	/**
	 * Writes a string to the data.
	 *
	 * @param data      Data to write.
	 * @param charset   The charset that you want to use to encode the string.
	 * @param byteOrder Byte order to use.
	 * @param bitOrder  Bit order to use.
	 * @return True if the write succeeded.
	 */
	public boolean writeString(String data, Charset charset, ByteOrder byteOrder, BitOrder bitOrder)
	{
		byte[] a = data.getBytes(charset);
		
		// Find last byte which isn't a null byte
		int i = a.length - 1;
		while(i >= 0 && a[i] == 0)
			i--;
		
		return writeBytes(Arrays.copyOf(a, i + 1), byteOrder, bitOrder)
			&& writeByte((byte)0, 8, byteOrder, bitOrder);
	}
	
	/**
	 * Sets the default byte order. It is used in write methods where you don't specify byte order.
	 *
	 * @param defaultByteOrder Byte order to set as default.
	 */
	public final void setDefaultByteOrder(ByteOrder defaultByteOrder) { this.defaultByteOrder = defaultByteOrder; }
	
	/**
	 * Sets the default bit order. It is used in write methods where you don't specify bit order.
	 *
	 * @param defaultBitOrder Bit order to set as default.
	 */
	public final void setDefaultBitOrder(BitOrder defaultBitOrder) { this.defaultBitOrder = defaultBitOrder; }
	
	/**
	 * @return The default byte order.
	 */
	public final ByteOrder getDefaultByteOrder() { return defaultByteOrder; }
	
	/**
	 * @return The default bit order.
	 */
	public final BitOrder getDefaultBitOrder() { return defaultBitOrder; }
}
