package se.albin.jbinary;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * A bit input stream that reads from a file.
 */
@SuppressWarnings("unused")
public final class BitFileInputStream extends BitInputStream implements Closeable, AutoCloseable
{
	private final BufferedInputStream stream;
	private final long size;
	
	private byte data;
	
	public BitFileInputStream(String filePath) throws FileNotFoundException
	{
		this(filePath, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	public BitFileInputStream(String filePath, BitOrder defaultBitOrder) throws FileNotFoundException
	{
		this(filePath, ByteOrder.BIG_ENDIAN, defaultBitOrder);
	}
	
	public BitFileInputStream(String filePath, ByteOrder defaultByteOrder) throws FileNotFoundException
	{
		this(filePath, defaultByteOrder, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	public BitFileInputStream(String filePath, ByteOrder defaultByteOrder, BitOrder defaultBitOrder) throws FileNotFoundException
	{
		this(new File(filePath), defaultByteOrder, defaultBitOrder);
	}
	
	public BitFileInputStream(File file) throws FileNotFoundException
	{
		this(file, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	public BitFileInputStream(File file, BitOrder defaultBitOrder) throws FileNotFoundException
	{
		this(file, ByteOrder.BIG_ENDIAN, defaultBitOrder);
	}
	
	public BitFileInputStream(File file, ByteOrder defaultByteOrder) throws FileNotFoundException
	{
		this(file, defaultByteOrder, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	public BitFileInputStream(File file, ByteOrder defaultByteOrder, BitOrder defaultBitOrder) throws FileNotFoundException
	{
		stream = new BufferedInputStream(new FileInputStream(file));
		
		size = file.length();
		
		this.defaultByteOrder = defaultByteOrder;
		this.defaultBitOrder = defaultBitOrder;
		
		read();
	}
	
	private void read()
	{
		try
		{
			data = (byte)stream.read();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	protected byte currentByte(long byteIndex, int subByteIndex)
	{
		return data;
	}
	
	@Override
	protected void afterJump(long byteIndex, int subByteIndex)
	{
		if(subByteIndex == 0)
			read();
	}
	
	@Override
	protected boolean skip(long bytes)
	{
		if(bytes == 0)
			return true;
		else if(bytes > 0)
		{
			try
			{
				if(bytes > 1)
				{
					long skip = bytes - 1;
					long actual = stream.skip(skip);
					
					if(skip != actual)
						throw new IOException(String.format("Unexpected EOF, skipped only %d of %d bytes", actual, skip));
				}
				
				read();
				return true;
			}
			catch(IOException e)
			{
				e.printStackTrace();
				return false;
			}
		}
		
		return false;
	}
	
	@Override
	public long getByteSize()
	{
		return size;
	}

	@Override
	public boolean hasEnded(long byteIndex, int subByteIndex)
	{
		return byteIndex >= size;
	}
	
	@Override
	public void close() throws IOException
	{
		stream.close();
	}
}
