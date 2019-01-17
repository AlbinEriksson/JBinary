package se.albin.jbinary.bitfield;

import se.albin.jbinary.BitArrayInputStream;
import se.albin.jbinary.BitArrayOutputStream;
import se.albin.jbinary.BitFileInputStream;
import se.albin.jbinary.BitFileOutputStream;
import se.albin.jbinary.BitInputStream;
import se.albin.jbinary.BitOrder;
import se.albin.jbinary.BitOutputStream;
import se.albin.jbinary.BitUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteOrder;

public interface BitField
{
	default void read(byte[] bytes)
	{
		read(bytes, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	default void read(byte[] bytes, BitOrder bitOrder)
	{
		read(bytes, ByteOrder.BIG_ENDIAN, bitOrder);
	}
	
	default void read(byte[] bytes, ByteOrder byteOrder)
	{
		read(bytes, byteOrder, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	default void read(byte[] bytes, ByteOrder byteOrder, BitOrder bitOrder)
	{
		try(BitInputStream in = new BitArrayInputStream(bytes, byteOrder, bitOrder))
		{
			read(in);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	default void read(File file)
	{
		read(file, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	default void read(File file, BitOrder bitOrder)
	{
		read(file, ByteOrder.BIG_ENDIAN, bitOrder);
	}
	
	default void read(File file, ByteOrder byteOrder)
	{
		read(file, byteOrder, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	default void read(File file, ByteOrder byteOrder, BitOrder bitOrder)
	{
		try(BitInputStream in = new BitFileInputStream(file, byteOrder, bitOrder))
		{
			read(in);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	default void read(String filePath)
	{
		read(filePath, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	default void read(String filePath, BitOrder bitOrder)
	{
		read(filePath, ByteOrder.BIG_ENDIAN, bitOrder);
	}
	
	default void read(String filePath, ByteOrder byteOrder)
	{
		read(filePath, byteOrder, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	default void read(String filePath, ByteOrder byteOrder, BitOrder bitOrder)
	{
		try(BitInputStream in = new BitFileInputStream(filePath, byteOrder, bitOrder))
		{
			read(in);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	default byte[] toBytes()
	{
		return toBytes(ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	default byte[] toBytes(BitOrder bitOrder)
	{
		return toBytes(ByteOrder.BIG_ENDIAN, bitOrder);
	}
	
	default byte[] toBytes(ByteOrder byteOrder)
	{
		return toBytes(byteOrder, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	default byte[] toBytes(ByteOrder byteOrder, BitOrder bitOrder)
	{
		try(BitArrayOutputStream out = new BitArrayOutputStream(byteOrder, bitOrder))
		{
			write(out);
			return out.getData();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return new byte[0];
		}
	}
	
	default void write(File file)
	{
		write(file, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	default void write(File file, BitOrder bitOrder)
	{
		write(file, ByteOrder.BIG_ENDIAN, bitOrder);
	}
	
	default void write(File file, ByteOrder byteOrder)
	{
		write(file, byteOrder, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	default void write(File file, ByteOrder byteOrder, BitOrder bitOrder)
	{
		try(BitOutputStream out = new BitFileOutputStream(file, byteOrder, bitOrder))
		{
			write(out);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	default void write(String filePath)
	{
		write(filePath, ByteOrder.BIG_ENDIAN, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	default void write(String filePath, BitOrder bitOrder)
	{
		write(filePath, ByteOrder.BIG_ENDIAN, bitOrder);
	}
	
	default void write(String filePath, ByteOrder byteOrder)
	{
		write(filePath, byteOrder, BitOrder.MOST_SIGNIFICANT_BIT);
	}
	
	default void write(String filePath, ByteOrder byteOrder, BitOrder bitOrder)
	{
		try(BitOutputStream out = new BitFileOutputStream(filePath, byteOrder, bitOrder))
		{
			write(out);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	default void read(BitInputStream in)
	{
		try
		{
			for(Field field : getClass().getFields())
			{
				int modifiers = field.getModifiers();
				// Only accessible, non-static and non-transient members are serialized
				if(field.canAccess(this) && (modifiers & (Modifier.TRANSIENT | Modifier.STATIC)) == 0)
				{
					Class<?> type = field.getType();
					
					if(type.isPrimitive())
						field.set(this, readPrimitive(field, type, in));
					else if(type.isArray())
					{
						Class<?> componentType = field.getType().getComponentType();
						if(componentType.isPrimitive())
							setPrimitiveArrayField(field, readPrimitiveArray(field, in));
					}
				}
			}
		}
		catch(IllegalAccessException e)
		{
			e.printStackTrace();
		}
	}
	
	default void write(BitOutputStream out)
	{
		try
		{
			for(Field field : getClass().getFields())
			{
				int modifiers = field.getModifiers();
				// Only accessible, non-static and non-transient members are serialized
				if(field.canAccess(this) && (modifiers & (Modifier.TRANSIENT | Modifier.STATIC)) == 0)
				{
					Class<?> type = field.getType();
					
					if(type.isPrimitive())
						writePrimitive(field, field.get(this), out);
					else if(type.isArray())
					{
						Class<?> componentType = field.getType().getComponentType();
						if(componentType.isPrimitive())
							writePrimitiveArray(field, out);
					}
				}
			}
		}
		catch(IllegalAccessException e)
		{
			e.printStackTrace();
		}
	}
	
	private Object readPrimitive(Field field, Class<?> type, BitInputStream in)
	{
		if(type == Boolean.TYPE)
			return in.readAsBoolean();
		else if(type == Character.TYPE)
			return in.readAsChar();
		else if(type == Float.TYPE)
			return in.readAsFloat();
		else if(type == Double.TYPE)
			return in.readAsDouble();
		else
		{
			long min = 0, max = Long.MAX_VALUE;
			int bits = BitUtil.bitSizeOf(type);
			if(field.isAnnotationPresent(NumberRange.class))
			{
				NumberRange range = field.getAnnotation(NumberRange.class);
				min = range.min();
				max = range.max();
				bits -= Long.numberOfLeadingZeros(max - min) - (64 - bits);
			}
			
			long number = 0;
			if(bits > 0)
				number = min + in.readAsLong(bits);
			if(number > max)
				number = max;
			
			if(type == Byte.TYPE)
				return (byte)number;
			else if(type == Short.TYPE)
				return (short)number;
			else if(type == Integer.TYPE)
				return (int)number;
			else
				return number;
		}
	}
	
	private Object readPrimitiveArray(Field field, BitInputStream in) throws IllegalAccessException
	{
		int length;
		
		if(field.isAnnotationPresent(DynamicArray.class))
			length = in.readAsInt();
		else
			length = Array.getLength(field.get(this));
		
		Object[] out = new Object[length];
		for(int i = 0; i < length; i++)
			out[i] = readPrimitive(field, field.getType().getComponentType(), in);
		
		return out;
	}
	
	private boolean writePrimitive(Field field, Object value, BitOutputStream out)
	{
		Class<?> type = value.getClass();
		
		if(type == Boolean.TYPE)
			return out.writeBoolean((boolean)value);
		else if(type == Character.TYPE)
			return out.writeChar((char)value);
		else if(type == Float.TYPE)
			return out.writeFloat((float)value);
		else if(type == Double.TYPE)
			return out.writeDouble((double)value);
		else
		{
			long min = 0;
			int bits = BitUtil.bitSizeOf(type);
			if(field.isAnnotationPresent(NumberRange.class))
			{
				NumberRange range = field.getAnnotation(NumberRange.class);
				min = range.min();
				bits -= Long.numberOfLeadingZeros(range.max() - min) - (64 - bits);
			}
			
			if(bits > 0)
			{
				if(type == Byte.TYPE)
					return out.writeByte((byte)((byte)value - min), bits);
				else if(type == Short.TYPE)
					return out.writeShort((short)((short)value - min), bits);
				else if(type == Integer.TYPE)
					return out.writeInt((int)((int)value - min), bits);
				else if(type == Long.TYPE)
					return out.writeLong((long)value - min, bits);
			}
			return true;
		}
	}
	
	private boolean writePrimitiveArray(Field field, BitOutputStream out) throws IllegalAccessException
	{
		Object array = field.get(this);
		int length = Array.getLength(array);
		
		if(field.isAnnotationPresent(DynamicArray.class))
			out.writeInt(length);
		
		for(int i = 0; i < length; i++)
			if(!writePrimitive(field, Array.get(array, i), out))
				return false;
		return true;
	}
	
	@SuppressWarnings("RedundantCast")
	private void setPrimitiveArrayField(Field field, Object source) throws IllegalAccessException
	{
		Object destination = field.get(this);
		int length = Array.getLength(destination);
		Class<?> componentType = field.getType().getComponentType();
		
		for(int i = 0; i < length; i++)
			if(componentType == Boolean.TYPE)
				Array.set(destination, i, (Boolean)Array.get(source, i));
			else if(componentType == Character.TYPE)
				Array.set(destination, i, (Character)Array.get(source, i));
			else if(componentType == Float.TYPE)
				Array.set(destination, i, (Float)Array.get(source, i));
			else if(componentType == Double.TYPE)
				Array.set(destination, i, (Double)Array.get(source, i));
			else if(componentType == Byte.TYPE)
				Array.set(destination, i, (Byte)Array.get(source, i));
			else if(componentType == Short.TYPE)
				Array.set(destination, i, (Short)Array.get(source, i));
			else if(componentType == Integer.TYPE)
				Array.set(destination, i, (Integer)Array.get(source, i));
			else if(componentType == Long.TYPE)
				Array.set(destination, i, (Long)Array.get(source, i));
	}
}
