package br.com.wdc.framework.commons.serialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

@SuppressWarnings({ "java:S5961", "java:S125" })
public class MapOrListInputTest {

	// ── Flat Object ────────────────────────────────────────────────────

	@Test
	public void flatObject_allPrimitiveTypes() throws IOException {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("name", "Alice");
		map.put("age", 30);
		map.put("score", 99.5);
		map.put("active", true);
		map.put("note", null);
		map.put("bigNumber", 9_000_000_000L);

		var input = new MapOrListInput(map);

		assertEquals(SerializationToken.BEGIN_OBJECT, input.peek());
		input.beginObject();

		assertTrue(input.hasNext());
		assertEquals("name", input.nextName());
		assertEquals(SerializationToken.STRING, input.peek());
		assertEquals("Alice", input.nextString());

		assertTrue(input.hasNext());
		assertEquals("age", input.nextName());
		assertEquals(SerializationToken.NUMBER, input.peek());
		assertEquals(30, input.nextInt());

		assertTrue(input.hasNext());
		assertEquals("score", input.nextName());
		assertEquals(SerializationToken.NUMBER, input.peek());
		assertEquals(99.5, input.nextDouble(), 0.001);

		assertTrue(input.hasNext());
		assertEquals("active", input.nextName());
		assertEquals(SerializationToken.BOOLEAN, input.peek());
		assertTrue(input.nextBoolean());

		assertTrue(input.hasNext());
		assertEquals("note", input.nextName());
		assertEquals(SerializationToken.NULL, input.peek());
		assertNull(input.nextNull());

		assertTrue(input.hasNext());
		assertEquals("bigNumber", input.nextName());
		assertEquals(SerializationToken.NUMBER, input.peek());
		assertEquals(9_000_000_000L, input.nextLong());

		assertFalse(input.hasNext());
		assertEquals(SerializationToken.END_OBJECT, input.peek());
		input.endObject();
	}

	// ── Flat Array ─────────────────────────────────────────────────────

	@Test
	public void flatArray_stringElements() throws IOException {
		var input = new MapOrListInput(List.of("a", "b", "c"));

		assertEquals(SerializationToken.BEGIN_ARRAY, input.peek());
		input.beginArray();

		assertTrue(input.hasNext());
		assertEquals("a", input.nextString());

		assertTrue(input.hasNext());
		assertEquals("b", input.nextString());

		assertTrue(input.hasNext());
		assertEquals("c", input.nextString());

		assertFalse(input.hasNext());
		assertEquals(SerializationToken.END_ARRAY, input.peek());
		input.endArray();
	}

	@Test
	public void flatArray_mixedTypes() throws IOException {
		var input = new MapOrListInput(List.of("hello", 42, true));

		input.beginArray();

		assertEquals(SerializationToken.STRING, input.peek());
		assertEquals("hello", input.nextString());

		assertEquals(SerializationToken.NUMBER, input.peek());
		assertEquals(42, input.nextInt());

		assertEquals(SerializationToken.BOOLEAN, input.peek());
		assertTrue(input.nextBoolean());

		assertFalse(input.hasNext());
		input.endArray();
	}

	// ── Nested Structures ──────────────────────────────────────────────

	@Test
	public void nestedObject_insideObject() throws IOException {
		Map<String, Object> address = new LinkedHashMap<>();
		address.put("city", "São Paulo");
		address.put("zip", "01000-000");

		Map<String, Object> root = new LinkedHashMap<>();
		root.put("id", 1);
		root.put("address", address);

		var input = new MapOrListInput(root);
		input.beginObject();

		assertEquals("id", input.nextName());
		assertEquals(1, input.nextInt());

		assertTrue(input.hasNext());
		assertEquals("address", input.nextName());
		assertEquals(SerializationToken.BEGIN_OBJECT, input.peek());
		input.beginObject();

		assertEquals("city", input.nextName());
		assertEquals("São Paulo", input.nextString());

		assertEquals("zip", input.nextName());
		assertEquals("01000-000", input.nextString());

		assertFalse(input.hasNext());
		input.endObject();

		assertFalse(input.hasNext());
		input.endObject();
	}

	@Test
	public void nestedArray_insideObject() throws IOException {
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("tags", List.of("java", "test"));

		var input = new MapOrListInput(root);
		input.beginObject();

		assertEquals("tags", input.nextName());
		assertEquals(SerializationToken.BEGIN_ARRAY, input.peek());
		input.beginArray();

		assertEquals("java", input.nextString());
		assertEquals("test", input.nextString());

		assertFalse(input.hasNext());
		input.endArray();

		assertFalse(input.hasNext());
		input.endObject();
	}

	@Test
	public void nestedObject_insideArray() throws IOException {
		Map<String, Object> item1 = new LinkedHashMap<>();
		item1.put("name", "A");

		Map<String, Object> item2 = new LinkedHashMap<>();
		item2.put("name", "B");

		var input = new MapOrListInput(List.of(item1, item2));
		input.beginArray();

		assertEquals(SerializationToken.BEGIN_OBJECT, input.peek());
		input.beginObject();
		assertEquals("name", input.nextName());
		assertEquals("A", input.nextString());
		assertFalse(input.hasNext());
		input.endObject();

		assertTrue(input.hasNext());
		assertEquals(SerializationToken.BEGIN_OBJECT, input.peek());
		input.beginObject();
		assertEquals("name", input.nextName());
		assertEquals("B", input.nextString());
		assertFalse(input.hasNext());
		input.endObject();

		assertFalse(input.hasNext());
		input.endArray();
	}

	@Test
	public void deeplyNestedStructure() throws IOException {
		// { "level1": { "level2": { "value": 42 } } }
		Map<String, Object> level2 = new LinkedHashMap<>();
		level2.put("value", 42);
		Map<String, Object> level1 = new LinkedHashMap<>();
		level1.put("level2", level2);
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("level1", level1);

		var input = new MapOrListInput(root);
		input.beginObject();

		assertEquals("level1", input.nextName());
		input.beginObject();

		assertEquals("level2", input.nextName());
		input.beginObject();

		assertEquals("value", input.nextName());
		assertEquals(42, input.nextInt());

		assertFalse(input.hasNext());
		input.endObject();

		assertFalse(input.hasNext());
		input.endObject();

		assertFalse(input.hasNext());
		input.endObject();
	}

	// ── Number Coercions ───────────────────────────────────────────────

	@Test
	public void nextNumber_returnsNumberInstance() throws IOException {
		var input = new MapOrListInput(List.of(3.14));
		input.beginArray();

		Number result = input.nextNumber();
		assertEquals(3.14, result.doubleValue(), 0.001);

		input.endArray();
	}

	@Test
	public void nextLong_fromIntegerValue() throws IOException {
		var input = new MapOrListInput(List.of(100));
		input.beginArray();
		assertEquals(100L, input.nextLong());
		input.endArray();
	}

	@Test
	public void nextDouble_fromIntegerValue() throws IOException {
		var input = new MapOrListInput(List.of(7));
		input.beginArray();
		assertEquals(7.0, input.nextDouble(), 0.001);
		input.endArray();
	}

	@Test
	public void nextInt_fromLongValue() throws IOException {
		var input = new MapOrListInput(List.of(50L));
		input.beginArray();
		assertEquals(50, input.nextInt());
		input.endArray();
	}

	// ── Null handling ──────────────────────────────────────────────────

	@Test
	public void nextNull_succeedsForNullValue() throws IOException {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("key", null);

		var input = new MapOrListInput(map);
		input.beginObject();

		assertEquals("key", input.nextName());
		assertEquals(SerializationToken.NULL, input.peek());
		assertNull(input.nextNull());

		input.endObject();
	}

	@Test(expected = IOException.class)
	public void nextNull_failsForNonNullValue() throws IOException {
		var input = new MapOrListInput(List.of("not null"));
		input.beginArray();
		input.nextNull();
	}

	// ── skipValue ──────────────────────────────────────────────────────

	@Test
	public void skipValue_skipsCurrentAndAdvances() throws IOException {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("skip_me", "ignored");
		map.put("keep", "important");

		var input = new MapOrListInput(map);
		input.beginObject();

		assertEquals("skip_me", input.nextName());
		input.skipValue();

		assertTrue(input.hasNext());
		assertEquals("keep", input.nextName());
		assertEquals("important", input.nextString());

		assertFalse(input.hasNext());
		input.endObject();
	}

	@Test
	public void skipValue_inArray() throws IOException {
		var input = new MapOrListInput(List.of("first", "second", "third"));
		input.beginArray();

		input.skipValue();

		assertTrue(input.hasNext());
		assertEquals("second", input.nextString());

		input.skipValue();

		assertFalse(input.hasNext());
		input.endArray();
	}

	// ── Character as STRING ────────────────────────────────────────────

	@Test
	public void characterValue_resolvedAsString() throws IOException {
		var input = new MapOrListInput(List.of('X'));
		input.beginArray();

		assertEquals(SerializationToken.STRING, input.peek());
		assertEquals("X", input.nextString());

		input.endArray();
	}

	// ── Empty structures ───────────────────────────────────────────────

	@Test
	public void emptyObject() throws IOException {
		var input = new MapOrListInput(Map.of());
		input.beginObject();

		assertFalse(input.hasNext());
		assertEquals(SerializationToken.END_OBJECT, input.peek());
		input.endObject();
	}

	@Test
	public void emptyArray() throws IOException {
		var input = new MapOrListInput(List.of());
		input.beginArray();

		assertFalse(input.hasNext());
		assertEquals(SerializationToken.END_ARRAY, input.peek());
		input.endArray();
	}

	// ── Error cases ────────────────────────────────────────────────────

	@Test(expected = IOException.class)
	public void beginObject_failsWhenTokenIsNotBeginObject() throws IOException {
		var input = new MapOrListInput(List.of("text"));
		input.beginObject();
	}

	@Test(expected = IOException.class)
	public void beginArray_failsWhenTokenIsNotBeginArray() throws IOException {
		var input = new MapOrListInput(Map.of("key", "val"));
		input.beginArray();
	}

	@Test(expected = IOException.class)
	public void endObject_failsWhenNoParent() throws IOException {
		var input = new MapOrListInput(Map.of());
		input.endObject();
	}

	@Test(expected = IOException.class)
	public void endArray_failsWhenNoParent() throws IOException {
		var input = new MapOrListInput(List.of());
		input.endArray();
	}

	@Test(expected = IOException.class)
	public void endObject_failsWhenMismatchedWithArray() throws IOException {
		var input = new MapOrListInput(List.of());
		input.beginArray();
		input.endObject();
	}

	@Test(expected = IOException.class)
	public void endArray_failsWhenMismatchedWithObject() throws IOException {
		var input = new MapOrListInput(Map.of());
		input.beginObject();
		input.endArray();
	}

	// ── nextString coerces numbers ─────────────────────────────────────

	@Test
	public void nextString_coercesNumberToString() throws IOException {
		var input = new MapOrListInput(List.of(42));
		input.beginArray();
		assertEquals("42", input.nextString());
		input.endArray();
	}

	// ── Boolean coercion from number ───────────────────────────────────

	@Test
	public void nextBoolean_defaultsToFalseForNull() throws IOException {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("flag", null);

		var input = new MapOrListInput(map);
		input.beginObject();
		input.nextName();
		assertFalse(input.nextBoolean());
		input.endObject();
	}

	// ── Complex real-world-like structure ──────────────────────────────

	@Test
	public void complexStructure_productsWithNestedArrays() throws IOException {
		Map<String, Object> product1 = new LinkedHashMap<>();
		product1.put("id", 1);
		product1.put("name", "Widget");
		product1.put("prices", List.of(9.99, 10.50, 11.0));

		Map<String, Object> product2 = new LinkedHashMap<>();
		product2.put("id", 2);
		product2.put("name", "Gadget");
		product2.put("prices", List.of(29.99));

		Map<String, Object> root = new LinkedHashMap<>();
		root.put("count", 2);
		root.put("products", List.of(product1, product2));

		var input = new MapOrListInput(root);
		input.beginObject();

		// count
		assertEquals("count", input.nextName());
		assertEquals(2, input.nextInt());

		// products array
		assertEquals("products", input.nextName());
		input.beginArray();

		// product1
		input.beginObject();
		assertEquals("id", input.nextName());
		assertEquals(1, input.nextInt());
		assertEquals("name", input.nextName());
		assertEquals("Widget", input.nextString());
		assertEquals("prices", input.nextName());
		input.beginArray();
		assertEquals(9.99, input.nextDouble(), 0.001);
		assertEquals(10.50, input.nextDouble(), 0.001);
		assertEquals(11.0, input.nextDouble(), 0.001);
		assertFalse(input.hasNext());
		input.endArray();
		assertFalse(input.hasNext());
		input.endObject();

		// product2
		assertTrue(input.hasNext());
		input.beginObject();
		assertEquals("id", input.nextName());
		assertEquals(2, input.nextInt());
		assertEquals("name", input.nextName());
		assertEquals("Gadget", input.nextString());
		assertEquals("prices", input.nextName());
		input.beginArray();
		assertEquals(29.99, input.nextDouble(), 0.001);
		assertFalse(input.hasNext());
		input.endArray();
		assertFalse(input.hasNext());
		input.endObject();

		assertFalse(input.hasNext());
		input.endArray();

		assertFalse(input.hasNext());
		input.endObject();
	}

	// ── Peek does not consume ──────────────────────────────────────────

	@Test
	public void peek_doesNotConsumeToken() throws IOException {
		var input = new MapOrListInput(List.of("x"));
		input.beginArray();

		assertEquals(SerializationToken.STRING, input.peek());
		assertEquals(SerializationToken.STRING, input.peek());
		assertEquals("x", input.nextString());

		input.endArray();
	}

	// ── nextName returns null for array elements ───────────────────────

	@Test
	public void nextName_returnsNullForArrayElements() throws IOException {
		var input = new MapOrListInput(List.of("val"));
		input.beginArray();

		assertNull(input.nextName());
		input.nextString();

		input.endArray();
	}
}
