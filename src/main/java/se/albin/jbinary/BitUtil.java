package se.albin.jbinary;

public final class BitUtil
{
	public static long getBitMask(int bits)
	{
		if(bits <= 0)
			return 0;
		else if(bits <= 63)
			return (1 << bits) - 1;
		else
			return -1;
	}
	
	public static int bitSizeOf(Class<?> type)
	{
		if(type.isPrimitive())
		{
			if(type == Boolean.TYPE)
				return 1;
			if(type == Byte.TYPE)
				return 8;
			if(type == Short.TYPE)
				return 16;
			if(type == Integer.TYPE)
				return 32;
			if(type == Long.TYPE)
				return 64;
			if(type == Float.TYPE)
				return 32;
			if(type == Double.TYPE)
				return 64;
		}
		
		return -1;
	}
}
