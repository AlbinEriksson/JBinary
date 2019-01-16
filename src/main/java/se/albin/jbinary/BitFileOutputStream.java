package se.albin.jbinary;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * A bit output stream which writes to a file.
 */
@SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
public class BitFileOutputStream extends BitOutputStream implements Closeable, AutoCloseable
{
	private final BufferedOutputStream stream;
	private final File file;
	private long size;
	
	public BitFileOutputStream(String filePath) throws FileNotFoundException
	{
		this(filePath, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	public BitFileOutputStream(String filePath, BitOrder defaultBitOrder) throws FileNotFoundException
	{
		this(filePath, ByteOrder.BIG_ENDIAN, defaultBitOrder);
	}
	
	public BitFileOutputStream(String filePath, ByteOrder defaultByteOrder) throws FileNotFoundException
	{
		this(filePath, defaultByteOrder, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	public BitFileOutputStream(String filePath, ByteOrder defaultByteOrder, BitOrder defaultBitOrder) throws FileNotFoundException
	{
		this(new File(filePath), defaultByteOrder, defaultBitOrder);
	}
	
	public BitFileOutputStream(File file) throws FileNotFoundException
	{
		this(file, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	public BitFileOutputStream(File file, BitOrder defaultBitOrder) throws FileNotFoundException
	{
		this(file, ByteOrder.BIG_ENDIAN, defaultBitOrder);
	}
	
	public BitFileOutputStream(File file, ByteOrder defaultByteOrder) throws FileNotFoundException
	{
		this(file, defaultByteOrder, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	public BitFileOutputStream(File file, ByteOrder defaultByteOrder, BitOrder defaultBitOrder) throws FileNotFoundException
	{
		this.file = file;
		stream = new BufferedOutputStream(new FileOutputStream(file));
		
		this.defaultByteOrder = defaultByteOrder;
		this.defaultBitOrder = defaultBitOrder;
	}
	
	@Override
	protected void write(byte b, long byteIndex, int subByteIndex)
	{
		try
		{
			stream.write(b);
			size++;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public long getByteSize()
	{
		return size;
	}
	
	@Override
	protected boolean skip(long byteIndex, int subByteIndex, long bytes)
	{
		if(bytes < 0)
			return false;
		
		try
		{
			for(long i = 0; i < bytes; i++)
				stream.write(0);
			size += bytes;
			
			return true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		return false;
	}
	
	public File getFile()
	{
		return file;
	}
	
	@Override
	public void closeStream() throws IOException
	{
		if(hasUnwrittenByte())
			write(unwrittenByte(), -1, -1);
		stream.close();
	}
}
