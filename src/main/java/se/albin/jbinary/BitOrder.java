package se.albin.jbinary;

/**
 * Bit ordering is an order which indicates from what direction a byte will be read from. The two options are most
 * significant (from the bit with the greatest value, the first one), or the least significant (from the bit with the
 * smallest value, the last one). This can be explained with some examples:<br><br>
 * <code>10100111 11001101</code> Say we have these two bytes. We're going to read 8 bits from them.<br>
 * <code>___^^^^^ ^^^_____</code> When reading from the most significant bit, the bytes are read from left to
 * right.<br><br>
 * <code>10100111 11001101</code> With the same bytes, let's try it with least significant bit ordering.<br>
 * <code>^^^^^___ _____^^^</code> Now, the bytes are read from right to left, and give a completely different
 * output.<br><br>
 * Most significant bit order output: <code>00111110</code><br>
 * Least significant bit order output: <code>10100101</code><br>
 * This difference might not seem important, but some binary files are encoded this way, and so this is required.
 */
public enum BitOrder
{
	/**
	 * Most significant bit order. Reads bytes from left to right. First bit to last bit. Most significant bit to least
	 * significant bit.
	 */
	MOST_SIGNIFICANT,
	/**
	 * Least significant bit order. Reads bytes from right to left. Last bit to first bit. Least significant bit to most
	 * significant bit.
	 */
	LEAST_SIGNIFICANT
}
