package br.com.wdc.framework.commons.serialization;

import java.io.IOException;

public interface ExtensibleObjectInput {

	/**
	 * Consumes the next token from the JSON stream and asserts that it is the beginning of a new array.
	 */
	void beginArray() throws IOException;

	/**
	 * Consumes the next token from the JSON stream and asserts that it is the end of the current array.
	 */
	void endArray() throws IOException;

	/**
	 * Consumes the next token from the JSON stream and asserts that it is the beginning of a new object.
	 */
	void beginObject() throws IOException;

	/**
	 * Consumes the next token from the JSON stream and asserts that it is the end of the current array.
	 */
	void endObject() throws IOException;

	/**
	 * Returns true if the current array or object has another element.
	 */
	boolean hasNext() throws IOException;

	/**
	 * Returns the type of the next token without consuming it.
	 */
	SerializationToken peek() throws IOException;

	/**
	 * Returns the next token, a {@link SerializationToken#NAME property name}, and consumes it.
	 *
	 * @throws IOException if the next token in the stream is not a property name.
	 */
	String nextName() throws IOException;

	/**
	 * Returns the {@link SerializationToken#STRING string} value of the next token, consuming it. If the next token is a number, this method will return its
	 * string form.
	 *
	 * @throws IllegalStateException if the next token is not a string or if this reader is closed.
	 */
	String nextString() throws IOException;

	/**
	 * Returns the {@link SerializationToken#BOOLEAN boolean} value of the next token, consuming it.
	 *
	 * @throws IllegalStateException if the next token is not a boolean or if this reader is closed.
	 */
	boolean nextBoolean() throws IOException;

	/**
	 * Consumes the next token from the JSON stream and asserts that it is a literal null.
	 *
	 * @throws IllegalStateException if the next token is not null or if this reader is closed.
	 */
	<T> T nextNull() throws IOException;

	/**
	 * Returns the {@link SerializationToken#NUMBER double} value of the next token, consuming it. If the next token is a string, this method will attempt to
	 * parse it as a double using {@link Double#parseDouble(String)}.
	 *
	 * @throws IllegalStateException if the next token is not a literal value.
	 */
	double nextDouble() throws IOException;

	/**
	 * Returns the {@link SerializationToken#NUMBER double} value of the next token, consuming it. If the next token is a string, this method will attempt to
	 * parse it as a number using.
	 *
	 * @throws IllegalStateException if the next token is not a literal value.
	 */
	Number nextNumber() throws IOException;

	/**
	 * Returns the {@link SerializationToken#NUMBER long} value of the next token, consuming it. If the next token is a string, this method will attempt to
	 * parse it as a long. If the next token's numeric value cannot be exactly represented by a Java {@code long}, this method throws.
	 *
	 * @throws IllegalStateException if the next token is not a literal value.
	 * @throws NumberFormatException if the next literal value cannot be parsed as a number, or exactly represented as a long.
	 */
	long nextLong() throws IOException;

	/**
	 * Returns the {@link SerializationToken#NUMBER int} value of the next token, consuming it. If the next token is a string, this method will attempt to parse
	 * it as an int. If the next token's numeric value cannot be exactly represented by a Java {@code int}, this method throws.
	 *
	 * @throws IllegalStateException if the next token is not a literal value.
	 * @throws NumberFormatException if the next literal value cannot be parsed as a number, or exactly represented as an int.
	 */
	int nextInt() throws IOException;

	/**
	 * Skips the next value recursively. If it is an object or array, all nested elements are skipped. This method is intended for use when the JSON token
	 * stream contains unrecognized or unhandled values.
	 */
	void skipValue() throws IOException;

}
