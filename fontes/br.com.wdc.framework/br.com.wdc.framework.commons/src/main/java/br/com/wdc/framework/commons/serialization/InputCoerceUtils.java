package br.com.wdc.framework.commons.serialization;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import br.com.wdc.framework.commons.lang.CoerceUtils;

/**
 * Utilitários para leitura coercitiva de valores a partir de um {@link ExtensibleObjectInput}.
 * <p>
 * Cada método inspeciona o token atual ({@code peek()}) e tenta converter o valor lido
 * para o tipo desejado, retornando o {@code defaultValue} quando o token é {@code NULL}.
 */
public final class InputCoerceUtils {

	private InputCoerceUtils() {
	}

	// :: String

	public static String asString(ExtensibleObjectInput input) {
		return asString(input, null);
	}

	public static String asString(ExtensibleObjectInput input, String defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case STRING -> input.nextString();
			case BOOLEAN -> CoerceUtils.asString(input.nextBoolean(), defaultValue);
			case NUMBER -> CoerceUtils.asString(input.nextNumber(), defaultValue);
			default -> throw invalidToken(input);
		};
	}

	public static String asTrimmedString(ExtensibleObjectInput input) {
		return asTrimmedString(input, null);
	}

	public static String asTrimmedString(ExtensibleObjectInput input, String defaultValue) {
		var s = asString(input, defaultValue);
		return s != null ? s.trim() : null;
	}

	public static String asLowerCaseString(ExtensibleObjectInput input) {
		return asLowerCaseString(input, null);
	}

	public static String asLowerCaseString(ExtensibleObjectInput input, String defaultValue) {
		var s = asString(input, defaultValue);
		return s != null ? s.toLowerCase() : null;
	}

	public static String asUpperCaseString(ExtensibleObjectInput input) {
		return asUpperCaseString(input, null);
	}

	public static String asUpperCaseString(ExtensibleObjectInput input, String defaultValue) {
		var s = asString(input, defaultValue);
		return s != null ? s.toUpperCase() : null;
	}

	// :: Boolean

	public static Boolean asBoolean(ExtensibleObjectInput input) {
		return asBoolean(input, null);
	}

	public static Boolean asBoolean(ExtensibleObjectInput input, Boolean defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case BOOLEAN -> input.nextBoolean();
			case STRING -> CoerceUtils.asBoolean(input.nextString());
			case NUMBER -> CoerceUtils.asBoolean(input.nextNumber(), defaultValue);
			default -> throw invalidToken(input);
		};
	}

	// :: Byte

	public static Byte asByte(ExtensibleObjectInput input) {
		return asByte(input, null);
	}

	public static Byte asByte(ExtensibleObjectInput input, Byte defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case NUMBER -> (byte) input.nextInt();
			case BOOLEAN -> CoerceUtils.asByte(input.nextBoolean());
			case STRING -> CoerceUtils.asByte(input.nextString());
			default -> throw invalidToken(input);
		};
	}

	// :: Short

	public static Short asShort(ExtensibleObjectInput input) {
		return asShort(input, null);
	}

	public static Short asShort(ExtensibleObjectInput input, Short defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case NUMBER -> (short) input.nextInt();
			case BOOLEAN -> CoerceUtils.asShort(input.nextBoolean());
			case STRING -> CoerceUtils.asShort(input.nextString());
			default -> throw invalidToken(input);
		};
	}

	// :: Integer

	public static Integer asInteger(ExtensibleObjectInput input) {
		return asInteger(input, null);
	}

	public static Integer asInteger(ExtensibleObjectInput input, Integer defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case NUMBER -> input.nextInt();
			case BOOLEAN -> CoerceUtils.asInteger(input.nextBoolean());
			case STRING -> CoerceUtils.asInteger(input.nextString());
			default -> throw invalidToken(input);
		};
	}

	// :: Character

	public static Character asCharacter(ExtensibleObjectInput input) {
		return asCharacter(input, null);
	}

	public static Character asCharacter(ExtensibleObjectInput input, Character defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case STRING -> CoerceUtils.asCharacter(input.nextString());
			case NUMBER -> CoerceUtils.asCharacter(input.nextInt());
			case BOOLEAN -> CoerceUtils.asCharacter(input.nextBoolean());
			default -> throw invalidToken(input);
		};
	}

	// :: Long

	public static Long asLong(ExtensibleObjectInput input) {
		return asLong(input, null);
	}

	public static Long asLong(ExtensibleObjectInput input, Long defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case NUMBER -> input.nextLong();
			case BOOLEAN -> CoerceUtils.asLong(input.nextBoolean());
			case STRING -> CoerceUtils.asLong(input.nextString());
			default -> throw invalidToken(input);
		};
	}

	// :: BigInteger

	public static BigInteger asBigInteger(ExtensibleObjectInput input) {
		return asBigInteger(input, null);
	}

	public static BigInteger asBigInteger(ExtensibleObjectInput input, BigInteger defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case NUMBER -> CoerceUtils.asBigInteger(input.nextLong());
			case BOOLEAN -> CoerceUtils.asBigInteger(input.nextBoolean());
			case STRING -> CoerceUtils.asBigInteger(input.nextString());
			default -> throw invalidToken(input);
		};
	}

	// :: Float

	public static Float asFloat(ExtensibleObjectInput input) {
		return asFloat(input, null);
	}

	public static Float asFloat(ExtensibleObjectInput input, Float defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case NUMBER -> (float) input.nextDouble();
			case BOOLEAN -> CoerceUtils.asFloat(input.nextBoolean());
			case STRING -> CoerceUtils.asFloat(input.nextString());
			default -> throw invalidToken(input);
		};
	}

	// :: Double

	public static Double asDouble(ExtensibleObjectInput input) {
		return asDouble(input, null);
	}

	public static Double asDouble(ExtensibleObjectInput input, Double defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case NUMBER -> input.nextDouble();
			case BOOLEAN -> CoerceUtils.asDouble(input.nextBoolean());
			case STRING -> CoerceUtils.asDouble(input.nextString());
			default -> throw invalidToken(input);
		};
	}

	// :: BigDecimal

	public static BigDecimal asBigDecimal(ExtensibleObjectInput input) {
		return asBigDecimal(input, null);
	}

	public static BigDecimal asBigDecimal(ExtensibleObjectInput input, BigDecimal defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case NUMBER -> CoerceUtils.asBigDecimal(input.nextDouble());
			case BOOLEAN -> CoerceUtils.asBigDecimal(input.nextBoolean());
			case STRING -> CoerceUtils.asBigDecimal(input.nextString());
			default -> throw invalidToken(input);
		};
	}

	// :: java.util.Date

	public static java.util.Date asDate(ExtensibleObjectInput input) {
		return asDate(input, null);
	}

	public static java.util.Date asDate(ExtensibleObjectInput input, java.util.Date defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case NUMBER -> CoerceUtils.asDate(input.nextDouble());
			case STRING -> CoerceUtils.asDate(input.nextString());
			default -> throw invalidToken(input);
		};
	}

	// :: java.sql.Timestamp

	public static java.sql.Timestamp asTimestamp(ExtensibleObjectInput input) {
		return asTimestamp(input, null);
	}

	public static java.sql.Timestamp asTimestamp(ExtensibleObjectInput input, java.sql.Timestamp defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case NUMBER -> CoerceUtils.asTimestamp(input.nextDouble());
			case STRING -> CoerceUtils.asTimestamp(input.nextString());
			default -> throw invalidToken(input);
		};
	}

	// :: java.sql.Date

	public static java.sql.Date asSqlDate(ExtensibleObjectInput input) {
		return asSqlDate(input, null);
	}

	public static java.sql.Date asSqlDate(ExtensibleObjectInput input, java.sql.Date defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case NUMBER -> CoerceUtils.asSqlDate(input.nextDouble());
			case STRING -> CoerceUtils.asSqlDate(input.nextString());
			default -> throw invalidToken(input);
		};
	}

	// :: LocalDate

	public static LocalDate asLocalDate(ExtensibleObjectInput input) {
		return asLocalDate(input, null);
	}

	public static LocalDate asLocalDate(ExtensibleObjectInput input, LocalDate defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case NUMBER -> CoerceUtils.asLocalDate(input.nextDouble());
			case STRING -> CoerceUtils.asLocalDate(input.nextString());
			default -> throw invalidToken(input);
		};
	}

	// :: LocalDateTime

	public static LocalDateTime asLocalDateTime(ExtensibleObjectInput input) {
		return asLocalDateTime(input, null);
	}

	public static LocalDateTime asLocalDateTime(ExtensibleObjectInput input, LocalDateTime defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case NUMBER -> CoerceUtils.asLocalDateTime(input.nextDouble());
			case STRING -> CoerceUtils.asLocalDateTime(input.nextString());
			default -> throw invalidToken(input);
		};
	}

	// :: OffsetDateTime

	public static OffsetDateTime asOffsetDateTime(ExtensibleObjectInput input) {
		return asOffsetDateTime(input, null);
	}

	public static OffsetDateTime asOffsetDateTime(ExtensibleObjectInput input, OffsetDateTime defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case NUMBER -> CoerceUtils.asOffsetDateTime(input.nextDouble());
			case STRING -> CoerceUtils.asOffsetDateTime(input.nextString());
			default -> throw invalidToken(input);
		};
	}

	// :: ZonedDateTime

	public static ZonedDateTime asZonedDateTime(ExtensibleObjectInput input) {
		return asZonedDateTime(input, null);
	}

	public static ZonedDateTime asZonedDateTime(ExtensibleObjectInput input, ZonedDateTime defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case NUMBER -> CoerceUtils.asZonedDateTime(input.nextDouble());
			case STRING -> CoerceUtils.asZonedDateTime(input.nextString());
			default -> throw invalidToken(input);
		};
	}

	// :: byte[] (Base64)

	public static byte[] asByteArray(ExtensibleObjectInput input) {
		return asByteArray(input, null);
	}

	public static byte[] asByteArray(ExtensibleObjectInput input, byte[] defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case STRING -> CoerceUtils.asByteArray(input.nextString());
			case BOOLEAN -> CoerceUtils.asByteArray(input.nextBoolean(), defaultValue);
			case NUMBER -> CoerceUtils.asByteArray(input.nextNumber(), defaultValue);
			default -> throw invalidToken(input);
		};
	}

	// :: byte[] (Hex)

	public static byte[] asByteArrayFromHex(ExtensibleObjectInput input) {
		return asByteArrayFromHex(input, null);
	}

	public static byte[] asByteArrayFromHex(ExtensibleObjectInput input, byte[] defaultValue) {
		return switch (input.peek()) {
			case NULL -> { input.nextNull(); yield defaultValue; }
			case STRING -> CoerceUtils.asByteArrayFromHex(input.nextString());
			case BOOLEAN -> CoerceUtils.asByteArrayFromHex(input.nextBoolean(), defaultValue);
			case NUMBER -> CoerceUtils.asByteArrayFromHex(input.nextNumber(), defaultValue);
			default -> throw invalidToken(input);
		};
	}

	// :: Internal

	private static IllegalStateException invalidToken(ExtensibleObjectInput input) {
		return new IllegalStateException("No valid value found. peek() = " + input.peek());
	}
}
