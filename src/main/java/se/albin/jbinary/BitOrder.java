package se.albin.jbinary;

/**
 * Bit order (or bit numbering) is an order which indicates from what direction a byte will be read from. The two
 * types are MSB (most significant bit) and LSB (least significant bit).<br><br>
 *
 * Read more about it in <a href="https://en.wikipedia.org/wiki/Bit_numbering">this Wikipedia article</a>.
 */
public enum BitOrder
{
	/**
	 * Most significant bit first (MSB).
	 */
	MOST_SIGNIFICANT_BIT,
	
	/**
	 * Least significant bit first (LSB).
	 */
	LEAST_SIGNIFICANT_BIT
}
