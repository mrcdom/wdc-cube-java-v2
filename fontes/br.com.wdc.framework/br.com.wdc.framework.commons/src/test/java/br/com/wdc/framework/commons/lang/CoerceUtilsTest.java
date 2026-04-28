package br.com.wdc.framework.commons.lang;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Base64;

import org.junit.Test;

import br.com.wdc.framework.commons.convert.DateUtil;

public class CoerceUtilsTest {

	// ── asString ───────────────────────────────────────────────────────

	@Test
	public void asString_null_returnsDefault() {
		assertNull(CoerceUtils.asString(null));
		assertEquals("fallback", CoerceUtils.asString(null, "fallback"));
	}

	@Test
	public void asString_string_returnsItself() {
		assertEquals("hello", CoerceUtils.asString("hello"));
	}

	@Test
	public void asString_number_returnsToString() {
		assertEquals("42", CoerceUtils.asString(42));
		assertEquals("3.14", CoerceUtils.asString(3.14));
	}

	@Test
	public void asString_boolean_returnsToString() {
		assertEquals("true", CoerceUtils.asString(true));
	}

	// ── asTrimmedString ────────────────────────────────────────────────

	@Test
	public void asTrimmedString_null_returnsDefault() {
		assertNull(CoerceUtils.asTrimmedString(null));
		assertEquals("ok", CoerceUtils.asTrimmedString(null, "  ok  "));
	}

	@Test
	public void asTrimmedString_trims() {
		assertEquals("hello", CoerceUtils.asTrimmedString("  hello  "));
	}

	// ── asLowerCaseString ──────────────────────────────────────────────

	@Test
	public void asLowerCaseString_null_returnsDefault() {
		assertNull(CoerceUtils.asLowerCaseString(null));
	}

	@Test
	public void asLowerCaseString_convertsToLower() {
		assertEquals("hello", CoerceUtils.asLowerCaseString("HELLO"));
	}

	// ── asUpperCaseString ──────────────────────────────────────────────

	@Test
	public void asUpperCaseString_null_returnsDefault() {
		assertNull(CoerceUtils.asUpperCaseString(null));
		assertEquals("OK", CoerceUtils.asUpperCaseString(null, "ok"));
	}

	@Test
	public void asUpperCaseString_convertsToUpper() {
		assertEquals("HELLO", CoerceUtils.asUpperCaseString("hello"));
	}

	// ── asBoolean ──────────────────────────────────────────────────────

	@Test
	public void asBoolean_null_returnsDefault() {
		assertNull(CoerceUtils.asBoolean(null));
		assertTrue(CoerceUtils.asBoolean(null, Boolean.TRUE));
	}

	@Test
	public void asBoolean_booleanValue() {
		assertTrue(CoerceUtils.asBoolean(Boolean.TRUE));
		assertFalse(CoerceUtils.asBoolean(Boolean.FALSE));
	}

	@Test
	public void asBoolean_fromNumber() {
		assertTrue(CoerceUtils.asBoolean(1));
		assertFalse(CoerceUtils.asBoolean(0));
		assertTrue(CoerceUtils.asBoolean(-1));
	}

	@Test
	public void asBoolean_fromCharacter() {
		assertTrue(CoerceUtils.asBoolean('s'));
		assertTrue(CoerceUtils.asBoolean('S'));
		assertTrue(CoerceUtils.asBoolean('t'));
		assertTrue(CoerceUtils.asBoolean('T'));
		assertFalse(CoerceUtils.asBoolean('n'));
		assertFalse(CoerceUtils.asBoolean('N'));
		assertFalse(CoerceUtils.asBoolean('f'));
		assertFalse(CoerceUtils.asBoolean('F'));
		assertNull(CoerceUtils.asBoolean('x'));
	}

	@Test
	public void asBoolean_fromSingleCharString() {
		assertTrue(CoerceUtils.asBoolean("s"));
		assertTrue(CoerceUtils.asBoolean("T"));
		assertFalse(CoerceUtils.asBoolean("n"));
		assertFalse(CoerceUtils.asBoolean("F"));
		assertNull(CoerceUtils.asBoolean("x"));
	}

	@Test
	public void asBoolean_fromTextString() {
		assertTrue(CoerceUtils.asBoolean("true"));
		assertTrue(CoerceUtils.asBoolean("TRUE"));
		assertTrue(CoerceUtils.asBoolean("sim"));
		assertTrue(CoerceUtils.asBoolean("SIM"));
		assertFalse(CoerceUtils.asBoolean("false"));
		assertFalse(CoerceUtils.asBoolean("FALSE"));
		assertFalse(CoerceUtils.asBoolean("nao"));
		assertFalse(CoerceUtils.asBoolean("NAO"));
	}

	@Test
	public void asBoolean_emptyString_returnsDefault() {
		assertNull(CoerceUtils.asBoolean(""));
		assertTrue(CoerceUtils.asBoolean("", Boolean.TRUE));
	}

	@Test(expected = IllegalArgumentException.class)
	public void asBoolean_unsupportedType_throws() {
		CoerceUtils.asBoolean(new Object());
	}

	// ── asByte ─────────────────────────────────────────────────────────

	@Test
	public void asByte_null_returnsDefault() {
		assertNull(CoerceUtils.asByte(null));
		assertEquals(Byte.valueOf((byte) 5), CoerceUtils.asByte(null, (byte) 5));
	}

	@Test
	public void asByte_fromByte() {
		assertEquals(Byte.valueOf((byte) 10), CoerceUtils.asByte((byte) 10));
	}

	@Test
	public void asByte_fromNumber() {
		assertEquals(Byte.valueOf((byte) 42), CoerceUtils.asByte(42));
	}

	@Test
	public void asByte_fromString() {
		assertEquals(Byte.valueOf((byte) 7), CoerceUtils.asByte("7"));
	}

	@Test
	public void asByte_emptyString_returnsDefault() {
		assertNull(CoerceUtils.asByte(""));
	}

	@Test
	public void asByte_fromBoolean() {
		assertEquals(Byte.valueOf((byte) 1), CoerceUtils.asByte(true));
		assertEquals(Byte.valueOf((byte) 0), CoerceUtils.asByte(false));
	}

	@Test(expected = NumberFormatException.class)
	public void asByte_unsupportedType_throws() {
		CoerceUtils.asByte(new Object());
	}

	// ── asShort ────────────────────────────────────────────────────────

	@Test
	public void asShort_null_returnsDefault() {
		assertNull(CoerceUtils.asShort(null));
	}

	@Test
	public void asShort_fromShort() {
		assertEquals(Short.valueOf((short) 100), CoerceUtils.asShort((short) 100));
	}

	@Test
	public void asShort_fromNumber() {
		assertEquals(Short.valueOf((short) 42), CoerceUtils.asShort(42));
	}

	@Test
	public void asShort_fromString() {
		assertEquals(Short.valueOf((short) 200), CoerceUtils.asShort("200"));
	}

	@Test
	public void asShort_fromBoolean() {
		assertEquals(Short.valueOf((short) 1), CoerceUtils.asShort(true));
		assertEquals(Short.valueOf((short) 0), CoerceUtils.asShort(false));
	}

	@Test(expected = NumberFormatException.class)
	public void asShort_unsupportedType_throws() {
		CoerceUtils.asShort(new Object());
	}

	// ── asInteger ──────────────────────────────────────────────────────

	@Test
	public void asInteger_null_returnsDefault() {
		assertNull(CoerceUtils.asInteger(null));
		assertEquals(Integer.valueOf(99), CoerceUtils.asInteger(null, 99));
	}

	@Test
	public void asInteger_fromInteger() {
		assertEquals(Integer.valueOf(42), CoerceUtils.asInteger(42));
	}

	@Test
	public void asInteger_fromNumber() {
		assertEquals(Integer.valueOf(3), CoerceUtils.asInteger(3.7));
	}

	@Test
	public void asInteger_fromString() {
		assertEquals(Integer.valueOf(123), CoerceUtils.asInteger("123"));
	}

	@Test
	public void asInteger_emptyString_returnsDefault() {
		assertNull(CoerceUtils.asInteger(""));
	}

	@Test
	public void asInteger_fromBoolean() {
		assertEquals(Integer.valueOf(1), CoerceUtils.asInteger(true));
		assertEquals(Integer.valueOf(0), CoerceUtils.asInteger(false));
	}

	@Test(expected = NumberFormatException.class)
	public void asInteger_unsupportedType_throws() {
		CoerceUtils.asInteger(new Object());
	}

	// ── asCharacter ────────────────────────────────────────────────────

	@Test
	public void asCharacter_null_returnsDefault() {
		assertNull(CoerceUtils.asCharacter(null));
		assertEquals(Character.valueOf('Z'), CoerceUtils.asCharacter(null, 'Z'));
	}

	@Test
	public void asCharacter_fromCharacter() {
		assertEquals(Character.valueOf('A'), CoerceUtils.asCharacter('A'));
	}

	@Test
	public void asCharacter_fromNumber() {
		assertEquals(Character.valueOf('A'), CoerceUtils.asCharacter(65));
	}

	@Test
	public void asCharacter_fromString() {
		assertEquals(Character.valueOf('H'), CoerceUtils.asCharacter("Hello"));
	}

	@Test
	public void asCharacter_emptyString_returnsDefault() {
		assertNull(CoerceUtils.asCharacter(""));
	}

	@Test
	public void asCharacter_fromBoolean() {
		assertEquals(Character.valueOf('T'), CoerceUtils.asCharacter(true));
		assertEquals(Character.valueOf('F'), CoerceUtils.asCharacter(false));
	}

	@Test(expected = NumberFormatException.class)
	public void asCharacter_unsupportedType_throws() {
		CoerceUtils.asCharacter(new Object());
	}

	// ── asLong ─────────────────────────────────────────────────────────

	@Test
	public void asLong_null_returnsDefault() {
		assertNull(CoerceUtils.asLong(null));
	}

	@Test
	public void asLong_fromLong() {
		assertEquals(Long.valueOf(999L), CoerceUtils.asLong(999L));
	}

	@Test
	public void asLong_fromNumber() {
		assertEquals(Long.valueOf(42L), CoerceUtils.asLong(42));
	}

	@Test
	public void asLong_fromString() {
		assertEquals(Long.valueOf(9_000_000_000L), CoerceUtils.asLong("9000000000"));
	}

	@Test
	public void asLong_fromBoolean() {
		assertEquals(Long.valueOf(1L), CoerceUtils.asLong(true));
		assertEquals(Long.valueOf(0L), CoerceUtils.asLong(false));
	}

	@Test(expected = NumberFormatException.class)
	public void asLong_unsupportedType_throws() {
		CoerceUtils.asLong(new Object());
	}

	// ── asBigInteger ───────────────────────────────────────────────────

	@Test
	public void asBigInteger_null_returnsDefault() {
		assertNull(CoerceUtils.asBigInteger(null));
	}

	@Test
	public void asBigInteger_fromBigInteger() {
		BigInteger val = BigInteger.valueOf(123);
		assertEquals(val, CoerceUtils.asBigInteger(val));
	}

	@Test
	public void asBigInteger_fromBigDecimal() {
		assertEquals(BigInteger.valueOf(99), CoerceUtils.asBigInteger(new BigDecimal("99.7")));
	}

	@Test
	public void asBigInteger_fromNumber() {
		assertEquals(BigInteger.valueOf(42), CoerceUtils.asBigInteger(42));
	}

	@Test
	public void asBigInteger_fromString() {
		assertEquals(BigInteger.valueOf(777), CoerceUtils.asBigInteger("777"));
	}

	@Test
	public void asBigInteger_fromBoolean() {
		assertEquals(BigInteger.ONE, CoerceUtils.asBigInteger(true));
		assertEquals(BigInteger.ZERO, CoerceUtils.asBigInteger(false));
	}

	@Test(expected = NumberFormatException.class)
	public void asBigInteger_unsupportedType_throws() {
		CoerceUtils.asBigInteger(new Object());
	}

	// ── asFloat ────────────────────────────────────────────────────────

	@Test
	public void asFloat_null_returnsDefault() {
		assertNull(CoerceUtils.asFloat(null));
	}

	@Test
	public void asFloat_fromFloat() {
		assertEquals(Float.valueOf(1.5f), CoerceUtils.asFloat(1.5f));
	}

	@Test
	public void asFloat_fromNumber() {
		assertEquals(42.0f, CoerceUtils.asFloat(42), 0.001f);
	}

	@Test
	public void asFloat_fromString() {
		assertEquals(3.14f, CoerceUtils.asFloat("3.14"), 0.001f);
	}

	@Test
	public void asFloat_fromBoolean() {
		assertEquals(1.0f, CoerceUtils.asFloat(true), 0.001f);
		assertEquals(0.0f, CoerceUtils.asFloat(false), 0.001f);
	}

	@Test(expected = NumberFormatException.class)
	public void asFloat_unsupportedType_throws() {
		CoerceUtils.asFloat(new Object());
	}

	// ── asDouble ───────────────────────────────────────────────────────

	@Test
	public void asDouble_null_returnsDefault() {
		assertNull(CoerceUtils.asDouble(null));
		assertEquals(Double.valueOf(1.0), CoerceUtils.asDouble(null, 1.0));
	}

	@Test
	public void asDouble_fromDouble() {
		assertEquals(3.14, CoerceUtils.asDouble(3.14), 0.001);
	}

	@Test
	public void asDouble_fromNumber() {
		assertEquals(42.0, CoerceUtils.asDouble(42), 0.001);
	}

	@Test
	public void asDouble_fromString() {
		assertEquals(99.9, CoerceUtils.asDouble("99.9"), 0.001);
	}

	@Test
	public void asDouble_emptyString_returnsDefault() {
		assertNull(CoerceUtils.asDouble(""));
	}

	@Test
	public void asDouble_fromBoolean() {
		assertEquals(1.0, CoerceUtils.asDouble(true), 0.001);
		assertEquals(0.0, CoerceUtils.asDouble(false), 0.001);
	}

	@Test(expected = NumberFormatException.class)
	public void asDouble_unsupportedType_throws() {
		CoerceUtils.asDouble(new Object());
	}

	// ── asBigDecimal ───────────────────────────────────────────────────

	@Test
	public void asBigDecimal_null_returnsDefault() {
		assertNull(CoerceUtils.asBigDecimal(null));
	}

	@Test
	public void asBigDecimal_fromBigDecimal() {
		BigDecimal val = new BigDecimal("123.45");
		assertEquals(val, CoerceUtils.asBigDecimal(val));
	}

	@Test
	public void asBigDecimal_fromBigInteger() {
		assertEquals(new BigDecimal(BigInteger.TEN), CoerceUtils.asBigDecimal(BigInteger.TEN));
	}

	@Test
	public void asBigDecimal_fromLong() {
		assertEquals(BigDecimal.valueOf(100L), CoerceUtils.asBigDecimal(100L));
	}

	@Test
	public void asBigDecimal_fromShort() {
		assertEquals(BigDecimal.valueOf((short) 50), CoerceUtils.asBigDecimal((short) 50));
	}

	@Test
	public void asBigDecimal_fromByte() {
		assertEquals(BigDecimal.valueOf((byte) 10), CoerceUtils.asBigDecimal((byte) 10));
	}

	@Test
	public void asBigDecimal_fromDouble() {
		assertEquals(3.14, CoerceUtils.asBigDecimal(3.14).doubleValue(), 0.001);
	}

	@Test
	public void asBigDecimal_fromString() {
		assertEquals(new BigDecimal("999.99"), CoerceUtils.asBigDecimal("999.99"));
	}

	@Test
	public void asBigDecimal_emptyString_returnsDefault() {
		assertNull(CoerceUtils.asBigDecimal(""));
	}

	@Test
	public void asBigDecimal_fromBoolean() {
		assertEquals(BigDecimal.ONE, CoerceUtils.asBigDecimal(true));
		assertEquals(BigDecimal.ZERO, CoerceUtils.asBigDecimal(false));
	}

	@Test(expected = NumberFormatException.class)
	public void asBigDecimal_unsupportedType_throws() {
		CoerceUtils.asBigDecimal(new Object());
	}

	// ── asNumber ───────────────────────────────────────────────────────

	@Test
	public void asNumber_null_returnsDefault() {
		assertNull(CoerceUtils.asNumber(null));
		assertEquals(Integer.valueOf(5), CoerceUtils.asNumber(null, 5));
	}

	@Test
	public void asNumber_fromNumber_returnsItself() {
		assertEquals(Integer.valueOf(42), CoerceUtils.asNumber(42));
	}

	@Test
	public void asNumber_string_noDefault_returnsBigDecimal() {
		Number result = CoerceUtils.asNumber("123.45");
		assertEquals(new BigDecimal("123.45"), result);
	}

	@Test
	public void asNumber_string_integerDefault_returnsInteger() {
		assertEquals(Integer.valueOf(10), CoerceUtils.asNumber("10", Integer.valueOf(0)));
	}

	@Test
	public void asNumber_string_longDefault_returnsLong() {
		assertEquals(Long.valueOf(10L), CoerceUtils.asNumber("10", Long.valueOf(0L)));
	}

	@Test
	public void asNumber_string_doubleDefault_returnsDouble() {
		assertEquals(10.5, CoerceUtils.asNumber("10.5", Double.valueOf(0.0)).doubleValue(), 0.001);
	}

	@Test
	public void asNumber_string_floatDefault_returnsFloat() {
		assertEquals(1.5f, CoerceUtils.asNumber("1.5", Float.valueOf(0f)).floatValue(), 0.001f);
	}

	@Test
	public void asNumber_string_shortDefault_returnsShort() {
		assertEquals(Short.valueOf((short) 10), CoerceUtils.asNumber("10", Short.valueOf((short) 0)));
	}

	@Test
	public void asNumber_string_byteDefault_returnsByte() {
		assertEquals(Byte.valueOf((byte) 5), CoerceUtils.asNumber("5", Byte.valueOf((byte) 0)));
	}

	@Test
	public void asNumber_string_bigIntegerDefault_returnsBigInteger() {
		assertEquals(BigInteger.valueOf(99), CoerceUtils.asNumber("99", BigInteger.ZERO));
	}

	@Test
	public void asNumber_string_bigDecimalDefault_returnsBigDecimal() {
		assertEquals(new BigDecimal("99.5"), CoerceUtils.asNumber("99.5", BigDecimal.ZERO));
	}

	// ── asDate ─────────────────────────────────────────────────────────

	@Test
	public void asDate_null_returnsDefault() {
		assertNull(CoerceUtils.asDate(null));
	}

	@Test
	public void asDate_fromJavaUtilDate() {
		var now = new java.util.Date();
		assertEquals(now, CoerceUtils.asDate(now));
	}

	@Test
	public void asDate_fromSqlDate() {
		var sqlDate = java.sql.Date.valueOf(LocalDate.of(2025, 1, 15));
		var result = CoerceUtils.asDate(sqlDate);
		assertEquals(sqlDate.getTime(), result.getTime());
	}

	@Test
	public void asDate_fromSqlTimestamp() {
		var ts = java.sql.Timestamp.valueOf(LocalDateTime.of(2025, 6, 15, 10, 30));
		var result = CoerceUtils.asDate(ts);
		assertEquals(ts.getTime(), result.getTime());
	}

	@Test
	public void asDate_fromLocalDate() {
		var ld = LocalDate.of(2025, 3, 20);
		var result = CoerceUtils.asDate(ld);
		var expected = java.util.Date.from(
				LocalDateTime.of(ld, LocalTime.MIN).toInstant(DateUtil.getSysZoneOffset()));
		assertEquals(expected, result);
	}

	@Test
	public void asDate_fromLocalDateTime() {
		var ldt = LocalDateTime.of(2025, 6, 15, 14, 30);
		var result = CoerceUtils.asDate(ldt);
		var expected = java.util.Date.from(ldt.toInstant(DateUtil.getSysZoneOffset()));
		assertEquals(expected, result);
	}

	@Test
	public void asDate_emptyString_returnsDefault() {
		assertNull(CoerceUtils.asDate(""));
	}

	@Test(expected = IllegalArgumentException.class)
	public void asDate_unsupportedType_throws() {
		CoerceUtils.asDate(new Object());
	}

	// ── asTimestamp ────────────────────────────────────────────────────

	@Test
	public void asTimestamp_null_returnsDefault() {
		assertNull(CoerceUtils.asTimestamp(null));
	}

	@Test
	public void asTimestamp_fromTimestamp() {
		var ts = java.sql.Timestamp.valueOf(LocalDateTime.of(2025, 1, 1, 12, 0));
		assertEquals(ts, CoerceUtils.asTimestamp(ts));
	}

	@Test
	public void asTimestamp_fromJavaUtilDate() {
		var date = new java.util.Date();
		var result = CoerceUtils.asTimestamp(date);
		assertEquals(date.getTime(), result.getTime());
	}

	@Test
	public void asTimestamp_fromLocalDate() {
		var ld = LocalDate.of(2025, 5, 10);
		var expected = java.sql.Timestamp.valueOf(LocalDateTime.of(ld, LocalTime.MIN));
		assertEquals(expected, CoerceUtils.asTimestamp(ld));
	}

	@Test
	public void asTimestamp_fromLocalDateTime() {
		var ldt = LocalDateTime.of(2025, 5, 10, 8, 45);
		assertEquals(java.sql.Timestamp.valueOf(ldt), CoerceUtils.asTimestamp(ldt));
	}

	@Test
	public void asTimestamp_emptyString_returnsDefault() {
		assertNull(CoerceUtils.asTimestamp(""));
	}

	@Test(expected = IllegalArgumentException.class)
	public void asTimestamp_unsupportedType_throws() {
		CoerceUtils.asTimestamp(new Object());
	}

	// ── asSqlDate ──────────────────────────────────────────────────────

	@Test
	public void asSqlDate_null_returnsDefault() {
		assertNull(CoerceUtils.asSqlDate(null));
	}

	@Test
	public void asSqlDate_fromSqlDate() {
		var sqlDate = java.sql.Date.valueOf(LocalDate.of(2025, 7, 4));
		assertEquals(sqlDate, CoerceUtils.asSqlDate(sqlDate));
	}

	@Test
	public void asSqlDate_fromLocalDate() {
		var ld = LocalDate.of(2025, 12, 25);
		assertEquals(java.sql.Date.valueOf(ld), CoerceUtils.asSqlDate(ld));
	}

	@Test
	public void asSqlDate_fromLocalDateTime() {
		var ldt = LocalDateTime.of(2025, 3, 15, 10, 0);
		assertEquals(java.sql.Date.valueOf(ldt.toLocalDate()), CoerceUtils.asSqlDate(ldt));
	}

	@Test
	public void asSqlDate_emptyString_returnsDefault() {
		assertNull(CoerceUtils.asSqlDate(""));
	}

	@Test(expected = IllegalArgumentException.class)
	public void asSqlDate_unsupportedType_throws() {
		CoerceUtils.asSqlDate(new Object());
	}

	// ── asLocalDate ────────────────────────────────────────────────────

	@Test
	public void asLocalDate_null_returnsDefault() {
		assertNull(CoerceUtils.asLocalDate(null));
	}

	@Test
	public void asLocalDate_fromLocalDate() {
		var ld = LocalDate.of(2025, 8, 20);
		assertEquals(ld, CoerceUtils.asLocalDate(ld));
	}

	@Test
	public void asLocalDate_fromSqlTimestamp() {
		var ldt = LocalDateTime.of(2025, 1, 10, 15, 30);
		var ts = java.sql.Timestamp.valueOf(ldt);
		assertEquals(ldt.toLocalDate(), CoerceUtils.asLocalDate(ts));
	}

	@Test
	public void asLocalDate_fromSqlDate() {
		var ld = LocalDate.of(2025, 6, 1);
		var sqlDate = java.sql.Date.valueOf(ld);
		assertEquals(ld, CoerceUtils.asLocalDate(sqlDate));
	}

	@Test
	public void asLocalDate_fromLocalDateTime() {
		var ldt = LocalDateTime.of(2025, 9, 5, 12, 0);
		assertEquals(ldt.toLocalDate(), CoerceUtils.asLocalDate(ldt));
	}

	@Test
	public void asLocalDate_fromOffsetDateTime() {
		var odt = OffsetDateTime.of(2025, 4, 10, 8, 0, 0, 0, DateUtil.getSysZoneOffset());
		assertEquals(odt.toLocalDate(), CoerceUtils.asLocalDate(odt));
	}

	@Test
	public void asLocalDate_fromZonedDateTime() {
		var zdt = ZonedDateTime.of(2025, 11, 28, 20, 0, 0, 0, DateUtil.getSysZoneOffset());
		assertEquals(zdt.toLocalDate(), CoerceUtils.asLocalDate(zdt));
	}

	@Test
	public void asLocalDate_emptyString_returnsDefault() {
		assertNull(CoerceUtils.asLocalDate(""));
	}

	@Test(expected = IllegalArgumentException.class)
	public void asLocalDate_unsupportedType_throws() {
		CoerceUtils.asLocalDate(new Object());
	}

	// ── asLocalDateTime ────────────────────────────────────────────────

	@Test
	public void asLocalDateTime_null_returnsDefault() {
		assertNull(CoerceUtils.asLocalDateTime(null));
	}

	@Test
	public void asLocalDateTime_fromLocalDateTime() {
		var ldt = LocalDateTime.of(2025, 2, 14, 9, 30);
		assertEquals(ldt, CoerceUtils.asLocalDateTime(ldt));
	}

	@Test
	public void asLocalDateTime_fromSqlTimestamp() {
		var ldt = LocalDateTime.of(2025, 7, 20, 18, 0);
		var ts = java.sql.Timestamp.valueOf(ldt);
		assertEquals(ldt, CoerceUtils.asLocalDateTime(ts));
	}

	@Test
	public void asLocalDateTime_fromSqlDate() {
		var ld = LocalDate.of(2025, 4, 1);
		var sqlDate = java.sql.Date.valueOf(ld);
		assertEquals(LocalDateTime.of(ld, LocalTime.MIN), CoerceUtils.asLocalDateTime(sqlDate));
	}

	@Test
	public void asLocalDateTime_fromLocalDate() {
		var ld = LocalDate.of(2025, 10, 31);
		assertEquals(ld.atStartOfDay(), CoerceUtils.asLocalDateTime(ld));
	}

	@Test
	public void asLocalDateTime_fromOffsetDateTime() {
		var odt = OffsetDateTime.of(2025, 5, 5, 12, 30, 0, 0, DateUtil.getSysZoneOffset());
		assertEquals(odt.toLocalDateTime(), CoerceUtils.asLocalDateTime(odt));
	}

	@Test
	public void asLocalDateTime_fromZonedDateTime() {
		var zdt = ZonedDateTime.of(2025, 8, 15, 7, 45, 0, 0, DateUtil.getSysZoneOffset());
		assertEquals(zdt.toLocalDateTime(), CoerceUtils.asLocalDateTime(zdt));
	}

	@Test
	public void asLocalDateTime_emptyString_returnsDefault() {
		assertNull(CoerceUtils.asLocalDateTime(""));
	}

	@Test(expected = IllegalArgumentException.class)
	public void asLocalDateTime_unsupportedType_throws() {
		CoerceUtils.asLocalDateTime(new Object());
	}

	// ── asLocalTime ────────────────────────────────────────────────────

	@Test
	public void asLocalTime_null_returnsDefault() {
		assertNull(CoerceUtils.asLocalTime(null));
	}

	@Test
	public void asLocalTime_fromLocalTime() {
		var lt = LocalTime.of(14, 30);
		assertEquals(lt, CoerceUtils.asLocalTime(lt));
	}

	@Test
	public void asLocalTime_fromSqlTimestamp() {
		var ldt = LocalDateTime.of(2025, 1, 1, 10, 15, 30);
		var ts = java.sql.Timestamp.valueOf(ldt);
		assertEquals(ldt.toLocalTime(), CoerceUtils.asLocalTime(ts));
	}

	@Test
	public void asLocalTime_fromOffsetDateTime() {
		var odt = OffsetDateTime.of(2025, 3, 1, 8, 0, 0, 0, DateUtil.getSysZoneOffset());
		assertEquals(odt.toLocalTime(), CoerceUtils.asLocalTime(odt));
	}

	@Test
	public void asLocalTime_fromZonedDateTime() {
		var zdt = ZonedDateTime.of(2025, 6, 1, 23, 59, 0, 0, DateUtil.getSysZoneOffset());
		assertEquals(zdt.toLocalTime(), CoerceUtils.asLocalTime(zdt));
	}

	@Test
	public void asLocalTime_fromString() {
		assertEquals(LocalTime.of(10, 30), CoerceUtils.asLocalTime("10:30"));
	}

	@Test
	public void asLocalTime_emptyString_returnsDefault() {
		assertNull(CoerceUtils.asLocalTime(""));
	}

	@Test(expected = IllegalArgumentException.class)
	public void asLocalTime_unsupportedType_throws() {
		CoerceUtils.asLocalTime(new Object());
	}

	// ── asOffsetDateTime ───────────────────────────────────────────────

	@Test
	public void asOffsetDateTime_null_returnsDefault() {
		assertNull(CoerceUtils.asOffsetDateTime(null));
	}

	@Test
	public void asOffsetDateTime_fromOffsetDateTime() {
		var odt = OffsetDateTime.of(2025, 4, 20, 16, 0, 0, 0, DateUtil.getSysZoneOffset());
		assertEquals(odt, CoerceUtils.asOffsetDateTime(odt));
	}

	@Test
	public void asOffsetDateTime_fromLocalDateTime() {
		var ldt = LocalDateTime.of(2025, 9, 10, 10, 0);
		var expected = ldt.atOffset(DateUtil.getSysZoneOffset());
		assertEquals(expected, CoerceUtils.asOffsetDateTime(ldt));
	}

	@Test
	public void asOffsetDateTime_fromLocalDate() {
		var ld = LocalDate.of(2025, 12, 1);
		var result = CoerceUtils.asOffsetDateTime(ld);
		assertEquals(ld, result.toLocalDate());
	}

	@Test
	public void asOffsetDateTime_fromZonedDateTime() {
		var zdt = ZonedDateTime.of(2025, 7, 4, 12, 0, 0, 0, DateUtil.getSysZoneOffset());
		assertEquals(zdt.toOffsetDateTime(), CoerceUtils.asOffsetDateTime(zdt));
	}

	@Test
	public void asOffsetDateTime_emptyString_returnsDefault() {
		assertNull(CoerceUtils.asOffsetDateTime(""));
	}

	@Test(expected = IllegalArgumentException.class)
	public void asOffsetDateTime_unsupportedType_throws() {
		CoerceUtils.asOffsetDateTime(new Object());
	}

	// ── asZonedDateTime ────────────────────────────────────────────────

	@Test
	public void asZonedDateTime_null_returnsDefault() {
		assertNull(CoerceUtils.asZonedDateTime(null));
	}

	@Test
	public void asZonedDateTime_fromZonedDateTime() {
		var zdt = ZonedDateTime.of(2025, 2, 28, 18, 0, 0, 0, DateUtil.getSysZoneOffset());
		assertEquals(zdt, CoerceUtils.asZonedDateTime(zdt));
	}

	@Test
	public void asZonedDateTime_fromLocalDate() {
		var ld = LocalDate.of(2025, 5, 1);
		var result = CoerceUtils.asZonedDateTime(ld);
		assertEquals(ld, result.toLocalDate());
	}

	@Test
	public void asZonedDateTime_fromLocalDateTime() {
		var ldt = LocalDateTime.of(2025, 10, 15, 9, 0);
		var expected = ldt.atZone(DateUtil.getSysZoneOffset());
		assertEquals(expected, CoerceUtils.asZonedDateTime(ldt));
	}

	@Test
	public void asZonedDateTime_fromOffsetDateTime() {
		var odt = OffsetDateTime.of(2025, 3, 22, 14, 30, 0, 0, DateUtil.getSysZoneOffset());
		assertEquals(odt.toZonedDateTime(), CoerceUtils.asZonedDateTime(odt));
	}

	@Test
	public void asZonedDateTime_emptyString_returnsDefault() {
		assertNull(CoerceUtils.asZonedDateTime(""));
	}

	@Test(expected = IllegalArgumentException.class)
	public void asZonedDateTime_unsupportedType_throws() {
		CoerceUtils.asZonedDateTime(new Object());
	}

	// ── asByteArray ────────────────────────────────────────────────────

	@Test
	public void asByteArray_null_returnsDefault() {
		assertNull(CoerceUtils.asByteArray(null));
		byte[] fallback = {9};
		assertArrayEquals(fallback, CoerceUtils.asByteArray(null, fallback));
	}

	@Test
	public void asByteArray_fromByteArray() {
		byte[] data = {1, 2, 3};
		assertArrayEquals(data, CoerceUtils.asByteArray(data));
	}

	@Test
	public void asByteArray_fromByte() {
		assertArrayEquals(new byte[]{42}, CoerceUtils.asByteArray((byte) 42));
	}

	@Test
	public void asByteArray_fromBase64String() {
		byte[] original = {10, 20, 30};
		String encoded = Base64.getEncoder().encodeToString(original);
		assertArrayEquals(original, CoerceUtils.asByteArray(encoded));
	}

	@Test
	public void asByteArray_emptyString_returnsDefault() {
		assertNull(CoerceUtils.asByteArray(""));
	}

	@Test(expected = IllegalArgumentException.class)
	public void asByteArray_unsupportedType_throws() {
		CoerceUtils.asByteArray(new Object());
	}
}
