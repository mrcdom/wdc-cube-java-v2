package br.com.wdc.framework.cube.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import br.com.wdc.framework.cube.CubeIntent;

public class QueryStringParserTest {

	// ── Helper ─────────────────────────────────────────────────────────

	private CubeIntent parseQuery(String queryString) {
		var intent = new CubeIntent();
		QueryStringParser.parse(intent, queryString, StandardCharsets.UTF_8);
		return intent;
	}

	// ── Single key=value ───────────────────────────────────────────────

	@Test
	public void singleParam() {
		var intent = parseQuery("name=Alice");
		assertEquals("Alice", intent.getParameterValue("name"));
	}

	@Test
	public void singleParamNumericValue() {
		var intent = parseQuery("id=42");
		assertEquals("42", intent.getParameterValue("id"));
	}

	// ── Multiple params ────────────────────────────────────────────────

	@Test
	public void multipleParams() {
		var intent = parseQuery("a=1&b=2&c=3");
		assertEquals("1", intent.getParameterValue("a"));
		assertEquals("2", intent.getParameterValue("b"));
		assertEquals("3", intent.getParameterValue("c"));
	}

	// ── Empty value ────────────────────────────────────────────────────

	@Test
	public void emptyValue() {
		var intent = parseQuery("key=");
		assertEquals("", intent.getParameterValue("key"));
	}

	// ── Value containing equals sign ───────────────────────────────────

	@Test
	public void valueContainsEquals() {
		var intent = parseQuery("expr=a=b");
		assertEquals("a=b", intent.getParameterValue("expr"));
	}

	// ── Plus decoded as space ──────────────────────────────────────────

	@Test
	public void plusDecodedAsSpace() {
		var intent = parseQuery("msg=hello+world");
		assertEquals("hello world", intent.getParameterValue("msg"));
	}

	// ── Percent-encoded characters ─────────────────────────────────────

	@Test
	public void percentEncodedSpace() {
		var intent = parseQuery("msg=hello%20world");
		assertEquals("hello world", intent.getParameterValue("msg"));
	}

	@Test
	public void percentEncodedAmpersand() {
		// %26 = '&'
		var intent = parseQuery("val=a%26b");
		assertEquals("a&b", intent.getParameterValue("val"));
	}

	@Test
	public void percentEncodedEquals() {
		// %3D = '='
		var intent = parseQuery("val=a%3Db");
		assertEquals("a=b", intent.getParameterValue("val"));
	}

	@Test
	public void percentEncodedUpperAndLowerHex() {
		// %2f = '/', %2F = '/'
		var intent1 = parseQuery("p=a%2fb");
		assertEquals("a/b", intent1.getParameterValue("p"));

		var intent2 = parseQuery("p=a%2Fb");
		assertEquals("a/b", intent2.getParameterValue("p"));
	}

	// ── Null and empty inputs ──────────────────────────────────────────

	@Test
	public void nullData_noop() {
		var intent = new CubeIntent();
		QueryStringParser.parse(intent, null, StandardCharsets.UTF_8);
		assertNull(intent.getParameterValue("any"));
	}

	@Test
	public void emptyString_noop() {
		var intent = parseQuery("");
		assertNull(intent.getParameterValue("any"));
	}

	@Test
	public void blankString_noop() {
		var intent = parseQuery("   ");
		assertNull(intent.getParameterValue("any"));
	}

	@Test
	public void nullByteArray_noop() {
		var intent = new CubeIntent();
		QueryStringParser.parseParameters(intent, null, StandardCharsets.UTF_8);
		assertNull(intent.getParameterValue("any"));
	}

	@Test
	public void emptyByteArray_noop() {
		var intent = new CubeIntent();
		QueryStringParser.parseParameters(intent, new byte[0], StandardCharsets.UTF_8);
		assertNull(intent.getParameterValue("any"));
	}

	// ── Key without value (no '=') ─────────────────────────────────────

	@Test
	public void keyWithoutEquals_ignored() {
		var intent = parseQuery("lonely");
		assertNull(intent.getParameterValue("lonely"));
	}

	@Test
	public void mixedKeyWithAndWithoutValue() {
		var intent = parseQuery("orphan&real=value");
		assertNull(intent.getParameterValue("orphan"));
		assertEquals("value", intent.getParameterValue("real"));
	}

	// ── Trailing ampersand ─────────────────────────────────────────────

	@Test
	public void trailingAmpersand() {
		var intent = parseQuery("a=1&");
		assertEquals("1", intent.getParameterValue("a"));
	}

	// ── Leading ampersand ──────────────────────────────────────────────

	@Test
	public void leadingAmpersand() {
		var intent = parseQuery("&a=1");
		assertEquals("1", intent.getParameterValue("a"));
	}

	// ── Multiple ampersands ────────────────────────────────────────────

	@Test
	public void consecutiveAmpersands() {
		var intent = parseQuery("a=1&&b=2");
		assertEquals("1", intent.getParameterValue("a"));
		assertEquals("2", intent.getParameterValue("b"));
	}

	// ── Unicode via percent encoding ───────────────────────────────────

	@Test
	public void percentEncodedUtf8_accentedChar() {
		// 'é' in UTF-8 = 0xC3 0xA9 → %C3%A9
		var intent = parseQuery("name=caf%C3%A9");
		assertEquals("café", intent.getParameterValue("name"));
	}

	// ── parseParameters direct call ────────────────────────────────────

	@Test
	public void parseParameters_directWithBytes() {
		var intent = new CubeIntent();
		byte[] data = "x=10&y=20".getBytes(StandardCharsets.UTF_8);
		QueryStringParser.parseParameters(intent, data, StandardCharsets.UTF_8);
		assertEquals("10", intent.getParameterValue("x"));
		assertEquals("20", intent.getParameterValue("y"));
	}

	// ── Special characters in value ────────────────────────────────────

	@Test
	public void valueWithSpecialChars() {
		var intent = parseQuery("q=hello+world%21");
		assertEquals("hello world!", intent.getParameterValue("q"));
	}

	// ── Long value ─────────────────────────────────────────────────────

	@Test
	public void longValue() {
		String longVal = "a".repeat(500);
		var intent = parseQuery("data=" + longVal);
		assertEquals(longVal, intent.getParameterValue("data"));
	}

	// ── Multiple params with same structure ────────────────────────────

	@Test
	public void manyParams() {
		var intent = parseQuery("a=1&b=2&c=3&d=4&e=5");
		assertEquals("1", intent.getParameterValue("a"));
		assertEquals("2", intent.getParameterValue("b"));
		assertEquals("3", intent.getParameterValue("c"));
		assertEquals("4", intent.getParameterValue("d"));
		assertEquals("5", intent.getParameterValue("e"));
	}

	// ── Percent-encoded key ────────────────────────────────────────────

	@Test
	public void percentEncodedKey() {
		// %6B = 'k'
		var intent = parseQuery("%6Bey=val");
		assertEquals("val", intent.getParameterValue("key"));
	}
}
