package br.com.wdc.framework.commons.serialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

@SuppressWarnings({"unchecked", "java:S5976"})
public class MapOrListOutputTest {

	// ── Default constructor ────────────────────────────────────────────

	@Test
	public void defaultConstructor_getValue_returnsNullInitially() {
		var output = new MapOrListOutput();
		assertNull(output.getValue());
	}

	// ── Flat object ────────────────────────────────────────────────────

	@Test
	public void flatObject_allPrimitiveTypes() {
		var output = new MapOrListOutput();

		output.beginObject()
				.name("name").value("Alice")
				.name("age").value(30L)
				.name("score").value(99.5)
				.name("active").value(true)
				.name("note").nullValue()
				.name("count").value((Number) 7)
				.endObject();

		var map = (Map<String, Object>) output.getValue();

		assertEquals("Alice", map.get("name"));
		assertEquals(30L, map.get("age"));
		assertEquals(99.5, (double) map.get("score"), 0.001);
		assertEquals(true, map.get("active"));
		assertNull(map.get("note"));
		assertTrue(map.containsKey("note"));
		assertEquals(7, map.get("count"));
	}

	// ── Flat array ─────────────────────────────────────────────────────

	@Test
	public void flatArray_stringElements() {
		var output = new MapOrListOutput();

		output.beginArray()
				.value("a")
				.value("b")
				.value("c")
				.endArray();

		var list = (List<Object>) output.getValue();

		assertEquals(3, list.size());
		assertEquals("a", list.get(0));
		assertEquals("b", list.get(1));
		assertEquals("c", list.get(2));
	}

	@Test
	public void flatArray_mixedTypes() {
		var output = new MapOrListOutput();

		output.beginArray()
				.value("hello")
				.value(42L)
				.value(true)
				.value(3.14)
				.nullValue()
				.endArray();

		var list = (List<Object>) output.getValue();

		assertEquals(5, list.size());
		assertEquals("hello", list.get(0));
		assertEquals(42L, list.get(1));
		assertEquals(true, list.get(2));
		assertEquals(3.14, (double) list.get(3), 0.001);
		assertNull(list.get(4));
	}

	// ── Nested structures ──────────────────────────────────────────────

	@Test
	public void nestedObject_insideObject() {
		var output = new MapOrListOutput();

		output.beginObject()
				.name("id").value(1L)
				.name("address").beginObject()
					.name("city").value("São Paulo")
					.name("zip").value("01000-000")
				.endObject()
				.endObject();

		var root = (Map<String, Object>) output.getValue();
		assertEquals(1L, root.get("id"));

		var address = (Map<String, Object>) root.get("address");
		assertEquals("São Paulo", address.get("city"));
		assertEquals("01000-000", address.get("zip"));
	}

	@Test
	public void nestedArray_insideObject() {
		var output = new MapOrListOutput();

		output.beginObject()
				.name("tags").beginArray()
					.value("java")
					.value("test")
				.endArray()
				.endObject();

		var root = (Map<String, Object>) output.getValue();
		var tags = (List<Object>) root.get("tags");

		assertEquals(2, tags.size());
		assertEquals("java", tags.get(0));
		assertEquals("test", tags.get(1));
	}

	@Test
	public void nestedObject_insideArray() {
		var output = new MapOrListOutput();

		output.beginArray()
				.beginObject()
					.name("name").value("A")
				.endObject()
				.beginObject()
					.name("name").value("B")
				.endObject()
				.endArray();

		var list = (List<Object>) output.getValue();
		assertEquals(2, list.size());

		var item1 = (Map<String, Object>) list.get(0);
		assertEquals("A", item1.get("name"));

		var item2 = (Map<String, Object>) list.get(1);
		assertEquals("B", item2.get("name"));
	}

	@Test
	public void deeplyNestedStructure() {
		var output = new MapOrListOutput();

		output.beginObject()
				.name("level1").beginObject()
					.name("level2").beginObject()
						.name("value").value(42L)
					.endObject()
				.endObject()
				.endObject();

		var root = (Map<String, Object>) output.getValue();
		var level1 = (Map<String, Object>) root.get("level1");
		var level2 = (Map<String, Object>) level1.get("level2");
		assertEquals(42L, level2.get("value"));
	}

	// ── Empty structures ───────────────────────────────────────────────

	@Test
	public void emptyObject() {
		var output = new MapOrListOutput();
		output.beginObject().endObject();

		var map = (Map<String, Object>) output.getValue();
		assertTrue(map.isEmpty());
	}

	@Test
	public void emptyArray() {
		var output = new MapOrListOutput();
		output.beginArray().endArray();

		var list = (List<Object>) output.getValue();
		assertTrue(list.isEmpty());
	}

	// ── name(int, String) ──────────────────────────────────────────────

	@Test
	public void nameWithId_usesNameWhenPresent() {
		var output = new MapOrListOutput();

		output.beginObject()
				.name(1, "field").value("val")
				.endObject();

		var map = (Map<String, Object>) output.getValue();
		assertEquals("val", map.get("field"));
	}

	@Test
	public void nameWithId_fallsBackToIdWhenNameNull() {
		var output = new MapOrListOutput();

		output.beginObject()
				.name(5, null).value("val")
				.endObject();

		var map = (Map<String, Object>) output.getValue();
		assertEquals("val", map.get("5"));
	}

	@Test
	public void nameWithId_fallsBackToIdWhenNameBlank() {
		var output = new MapOrListOutput();

		output.beginObject()
				.name(7, "  ").value("val")
				.endObject();

		var map = (Map<String, Object>) output.getValue();
		assertEquals("val", map.get("7"));
	}

	// ── byte[] value ───────────────────────────────────────────────────

	@Test
	public void byteArrayValue_storedInObject() {
		byte[] data = {1, 2, 3};
		var output = new MapOrListOutput();

		output.beginObject()
				.name("data").value(data)
				.endObject();

		var map = (Map<String, Object>) output.getValue();
		assertEquals(data, map.get("data"));
	}

	@Test
	public void byteArrayValue_storedInArray() {
		byte[] data = {10, 20};
		var output = new MapOrListOutput();

		output.beginArray()
				.value(data)
				.endArray();

		var list = (List<Object>) output.getValue();
		assertEquals(data, list.get(0));
	}

	// ── Constructor with existing Map ──────────────────────────────────

	@Test
	public void mapConstructor_writesIntoExistingMap() {
		var existing = new LinkedHashMap<String, Object>();
		existing.put("existing", "yes");

		var output = new MapOrListOutput(existing);
		output.name("added").value("new");

		var map = (Map<String, Object>) output.getValue();
		assertEquals("yes", map.get("existing"));
		assertEquals("new", map.get("added"));
	}

	@Test
	public void mapConstructor_nestedStructure() {
		var existing = new LinkedHashMap<String, Object>();

		var output = new MapOrListOutput(existing);
		output.name("items").beginArray()
				.value("one")
				.value("two")
				.endArray();

		var map = (Map<String, Object>) output.getValue();
		var items = (List<Object>) map.get("items");
		assertEquals(2, items.size());
		assertEquals("one", items.get(0));
		assertEquals("two", items.get(1));
	}

	// ── Constructor with existing List ─────────────────────────────────

	@Test
	public void listConstructor_writesIntoExistingList() {
		var existing = new ArrayList<>();
		existing.add("pre-existing");

		var output = new MapOrListOutput(existing);
		output.value("appended");

		var list = (List<Object>) output.getValue();
		assertEquals(2, list.size());
		assertEquals("pre-existing", list.get(0));
		assertEquals("appended", list.get(1));
	}

	@Test
	public void listConstructor_nestedObjectInList() {
		var existing = new ArrayList<>();

		var output = new MapOrListOutput(existing);
		output.beginObject()
				.name("key").value("val")
				.endObject();

		var list = (List<Object>) output.getValue();
		assertEquals(1, list.size());

		var obj = (Map<String, Object>) list.get(0);
		assertEquals("val", obj.get("key"));
	}

	// ── Fluent API returns this ────────────────────────────────────────

	@Test
	public void fluentApi_chainsReturnSameInstance() {
		var output = new MapOrListOutput();

		var result = output.beginObject()
				.name("x").value("y")
				.name("n").nullValue()
				.name("b").value(true)
				.name("d").value(1.0)
				.name("l").value(1L)
				.name("num").value((Number) 1)
				.name("bytes").value(new byte[]{})
				.endObject();

		assertTrue(result instanceof MapOrListOutput);
	}

	// ── Complex real-world-like structure ──────────────────────────────

	@Test
	public void complexStructure_productsWithNestedArrays() {
		var output = new MapOrListOutput();

		output.beginObject()
				.name("count").value(2L)
				.name("products").beginArray()
					.beginObject()
						.name("id").value(1L)
						.name("name").value("Widget")
						.name("prices").beginArray()
							.value(9.99)
							.value(10.50)
							.value(11.0)
						.endArray()
					.endObject()
					.beginObject()
						.name("id").value(2L)
						.name("name").value("Gadget")
						.name("prices").beginArray()
							.value(29.99)
						.endArray()
					.endObject()
				.endArray()
				.endObject();

		var root = (Map<String, Object>) output.getValue();
		assertEquals(2L, root.get("count"));

		var products = (List<Object>) root.get("products");
		assertEquals(2, products.size());

		var p1 = (Map<String, Object>) products.get(0);
		assertEquals(1L, p1.get("id"));
		assertEquals("Widget", p1.get("name"));
		var prices1 = (List<Object>) p1.get("prices");
		assertEquals(3, prices1.size());
		assertEquals(9.99, (double) prices1.get(0), 0.001);
		assertEquals(10.50, (double) prices1.get(1), 0.001);
		assertEquals(11.0, (double) prices1.get(2), 0.001);

		var p2 = (Map<String, Object>) products.get(1);
		assertEquals(2L, p2.get("id"));
		assertEquals("Gadget", p2.get("name"));
		var prices2 = (List<Object>) p2.get("prices");
		assertEquals(1, prices2.size());
		assertEquals(29.99, (double) prices2.get(0), 0.001);
	}

	// ── Roundtrip with MapOrListInput ──────────────────────────────────

	@Test
	public void roundtrip_writeAndReadBack() throws Exception {
		var writer = new MapOrListOutput();

		writer.beginObject()
				.name("title").value("Test")
				.name("qty").value(5L)
				.name("price").value(12.5)
				.name("enabled").value(true)
				.name("tags").beginArray()
					.value("alpha")
					.value("beta")
				.endArray()
				.name("empty").nullValue()
				.endObject();

		var map = (Map<String, Object>) writer.getValue();

		var reader = new MapOrListInput(map);
		reader.beginObject();

		assertEquals("title", reader.nextName());
		assertEquals("Test", reader.nextString());

		assertEquals("qty", reader.nextName());
		assertEquals(5L, reader.nextLong());

		assertEquals("price", reader.nextName());
		assertEquals(12.5, reader.nextDouble(), 0.001);

		assertEquals("enabled", reader.nextName());
		assertTrue(reader.nextBoolean());

		assertEquals("tags", reader.nextName());
		reader.beginArray();
		assertEquals("alpha", reader.nextString());
		assertEquals("beta", reader.nextString());
		reader.endArray();

		assertEquals("empty", reader.nextName());
		assertNull(reader.nextNull());

		reader.endObject();
	}
}
