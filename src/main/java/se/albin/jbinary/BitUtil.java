package se.albin.jbinary;

@SuppressWarnings("WeakerAccess")
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
}
