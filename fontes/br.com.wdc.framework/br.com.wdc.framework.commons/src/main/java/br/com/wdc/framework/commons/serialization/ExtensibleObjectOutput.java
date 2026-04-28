package br.com.wdc.framework.commons.serialization;

public interface ExtensibleObjectOutput {

	/**
	 * Begins encoding a new array. Each call to this method must be paired with a call to {@link #endArray}.
	 * 
	 * @return this writer.
	 */
	ExtensibleObjectOutput beginArray();

	/**
	 * Ends encoding the current array.
	 * 
	 * @return this writer.
	 */
	ExtensibleObjectOutput endArray();

	/**
	 * Begins encoding a new object. Each call to this method must be paired with a call to {@link #endObject}.
	 * 
	 * @return this writer.
	 */
	ExtensibleObjectOutput beginObject();

	/**
	 * Ends encoding the current object.
	 * 
	 * @return this writer.
	 */
	ExtensibleObjectOutput endObject();

	/**
	 * Encodes the property name.
	 * 
	 * @param name the name of the forthcoming value. May not be null.
	 * @return this writer.
	 */
	ExtensibleObjectOutput name(String name);
	
	/**
	 * Encodes the property name.
	 * 
	 * @param id the id of the forthcoming value. Must me greater than zero.
	 * @param name the name of the forthcoming value. May not be null.
	 * @return this writer.
	 */
	ExtensibleObjectOutput name(int id, String name);

	/**
	 * Encodes {@code value}.
	 * 
	 * @param value the literal string value, or null to encode a null literal.
	 * @return this writer.
	 */
	ExtensibleObjectOutput value(final String value);
	
	ExtensibleObjectOutput value(final byte[] value);

	/**
	 * Encodes {@code null}.
	 * 
	 * @return this writer.
	 */
	ExtensibleObjectOutput nullValue();

	/**
	 * Encodes {@code value}.
	 * 
	 * @param value value to be written
	 * 
	 * @return this writer.
	 */
	ExtensibleObjectOutput value(final boolean value);

	/**
	 * Encodes {@code value}.
	 * 
	 * @param value a finite value. May not be {@link Double#isNaN() NaNs} or {@link Double#isInfinite() infinities}
	 *            unless this writer is lenient.
	 * @return this writer.
	 */
	ExtensibleObjectOutput value(final double value);

	/**
	 * Encodes {@code value}.
	 * 
	 * @param value value to be written
	 * 
	 * @return this writer.
	 */
	ExtensibleObjectOutput value(final long value);

	/**
	 * Encodes {@code value}.
	 * 
	 * @param value a finite value. May not be {@link Double#isNaN() NaNs} or {@link Double#isInfinite() infinities}
	 *            unless this writer is lenient.
	 * @return this writer.
	 */
	ExtensibleObjectOutput value(final Number value);

}
