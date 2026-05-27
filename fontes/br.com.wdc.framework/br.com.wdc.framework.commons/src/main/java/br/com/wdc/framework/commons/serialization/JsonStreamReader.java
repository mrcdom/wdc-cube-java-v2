package br.com.wdc.framework.commons.serialization;

/**
 * Streaming JSON reader — puro Java, sem dependências externas.
 * <p>
 * Implementa {@link ExtensibleObjectInput} lendo diretamente da string JSON
 * sem construir árvore intermediária (Map/List).
 */
public class JsonStreamReader implements ExtensibleObjectInput {

	// Scope constants
	private static final int SCOPE_EMPTY_DOCUMENT = 0;
	private static final int SCOPE_NONEMPTY_DOCUMENT = 1;
	private static final int SCOPE_EMPTY_ARRAY = 2;
	private static final int SCOPE_NONEMPTY_ARRAY = 3;
	private static final int SCOPE_EMPTY_OBJECT = 4;
	private static final int SCOPE_NONEMPTY_OBJECT = 5;
	private static final int SCOPE_DANGLING_NAME = 6;

	// Peeked token constants
	private static final int PEEKED_NONE = 0;
	private static final int PEEKED_BEGIN_OBJECT = 1;
	private static final int PEEKED_END_OBJECT = 2;
	private static final int PEEKED_BEGIN_ARRAY = 3;
	private static final int PEEKED_END_ARRAY = 4;
	private static final int PEEKED_TRUE = 5;
	private static final int PEEKED_FALSE = 6;
	private static final int PEEKED_NULL = 7;
	private static final int PEEKED_STRING = 8;
	private static final int PEEKED_NAME = 9;
	private static final int PEEKED_NUMBER = 10;
	private static final int PEEKED_EOF = 11;

	private final String source;
	private int pos = 0;
	private int[] scopeStack = new int[16];
	private int stackSize = 1;
	private int peeked = PEEKED_NONE;

	public JsonStreamReader(String source) {
		this.source = source;
		scopeStack[0] = SCOPE_EMPTY_DOCUMENT;
	}

	// ── ExtensibleObjectInput ──

	@Override
	public void beginObject() {
		int p = ensurePeeked();
		if (p != PEEKED_BEGIN_OBJECT) throw illegalState("BEGIN_OBJECT", p);
		pos++; // consume '{'
		push(SCOPE_EMPTY_OBJECT);
		peeked = PEEKED_NONE;
	}

	@Override
	public void endObject() {
		int p = ensurePeeked();
		if (p != PEEKED_END_OBJECT) throw illegalState("END_OBJECT", p);
		stackSize--;
		peeked = PEEKED_NONE;
	}

	@Override
	public void beginArray() {
		int p = ensurePeeked();
		if (p != PEEKED_BEGIN_ARRAY) throw illegalState("BEGIN_ARRAY", p);
		pos++; // consume '['
		push(SCOPE_EMPTY_ARRAY);
		peeked = PEEKED_NONE;
	}

	@Override
	public void endArray() {
		int p = ensurePeeked();
		if (p != PEEKED_END_ARRAY) throw illegalState("END_ARRAY", p);
		stackSize--;
		peeked = PEEKED_NONE;
	}

	@Override
	public boolean hasNext() {
		int p = ensurePeeked();
		return p != PEEKED_END_OBJECT && p != PEEKED_END_ARRAY && p != PEEKED_EOF;
	}

	@Override
	public SerializationToken peek() {
		return switch (ensurePeeked()) {
			case PEEKED_BEGIN_OBJECT -> SerializationToken.BEGIN_OBJECT;
			case PEEKED_END_OBJECT -> SerializationToken.END_OBJECT;
			case PEEKED_BEGIN_ARRAY -> SerializationToken.BEGIN_ARRAY;
			case PEEKED_END_ARRAY -> SerializationToken.END_ARRAY;
			case PEEKED_TRUE, PEEKED_FALSE -> SerializationToken.BOOLEAN;
			case PEEKED_NULL -> SerializationToken.NULL;
			case PEEKED_STRING -> SerializationToken.STRING;
			case PEEKED_NAME -> SerializationToken.NAME;
			case PEEKED_NUMBER -> SerializationToken.NUMBER;
			case PEEKED_EOF -> SerializationToken.END_DOCUMENT;
			default -> throw new IllegalStateException("Unknown peeked state");
		};
	}

	@Override
	public String nextName() {
		int p = ensurePeeked();
		if (p != PEEKED_NAME) throw illegalState("NAME", p);
		String name = readQuotedString();
		scopeStack[stackSize - 1] = SCOPE_DANGLING_NAME;
		peeked = PEEKED_NONE;
		return name;
	}

	@Override
	public String nextString() {
		int p = ensurePeeked();
		String result = switch (p) {
			case PEEKED_STRING -> readQuotedString();
			case PEEKED_NUMBER -> readRawNumber();
			case PEEKED_TRUE -> { consumeLiteral("true"); yield "true"; }
			case PEEKED_FALSE -> { consumeLiteral("false"); yield "false"; }
			case PEEKED_NULL -> { consumeLiteral("null"); yield "null"; }
			default -> throw illegalState("STRING", p);
		};
		peeked = PEEKED_NONE;
		return result;
	}

	@Override
	public boolean nextBoolean() {
		int p = ensurePeeked();
		return switch (p) {
			case PEEKED_TRUE -> { consumeLiteral("true"); peeked = PEEKED_NONE; yield true; }
			case PEEKED_FALSE -> { consumeLiteral("false"); peeked = PEEKED_NONE; yield false; }
			default -> throw illegalState("BOOLEAN", p);
		};
	}

	@Override
	public <T> T nextNull() {
		int p = ensurePeeked();
		if (p != PEEKED_NULL) throw illegalState("NULL", p);
		consumeLiteral("null");
		peeked = PEEKED_NONE;
		return null;
	}

	@Override
	public double nextDouble() {
		int p = ensurePeeked();
		String raw = switch (p) {
			case PEEKED_NUMBER -> readRawNumber();
			case PEEKED_STRING -> readQuotedString();
			default -> throw illegalState("NUMBER", p);
		};
		peeked = PEEKED_NONE;
		return Double.parseDouble(raw);
	}

	@Override
	public long nextLong() {
		int p = ensurePeeked();
		String raw = switch (p) {
			case PEEKED_NUMBER -> readRawNumber();
			case PEEKED_STRING -> readQuotedString();
			default -> throw illegalState("NUMBER", p);
		};
		peeked = PEEKED_NONE;
		try {
			return Long.parseLong(raw);
		} catch (NumberFormatException e) {
			double d = Double.parseDouble(raw);
			long l = (long) d;
			if (d == (double) l) return l;
			throw new NumberFormatException("Expected long but was " + raw + " at position " + pos);
		}
	}

	@Override
	public int nextInt() {
		int p = ensurePeeked();
		String raw = switch (p) {
			case PEEKED_NUMBER -> readRawNumber();
			case PEEKED_STRING -> readQuotedString();
			default -> throw illegalState("NUMBER", p);
		};
		peeked = PEEKED_NONE;
		try {
			return Integer.parseInt(raw);
		} catch (NumberFormatException e) {
			double d = Double.parseDouble(raw);
			int i = (int) d;
			if (d == (double) i) return i;
			throw new NumberFormatException("Expected int but was " + raw + " at position " + pos);
		}
	}

	@Override
	public Number nextNumber() {
		int p = ensurePeeked();
		if (p == PEEKED_NULL) {
			consumeLiteral("null");
			peeked = PEEKED_NONE;
			return null;
		}
		String raw = switch (p) {
			case PEEKED_NUMBER -> readRawNumber();
			case PEEKED_STRING -> readQuotedString();
			default -> throw illegalState("NUMBER", p);
		};
		peeked = PEEKED_NONE;
		if (raw.indexOf('.') >= 0 || raw.indexOf('e') >= 0 || raw.indexOf('E') >= 0) {
			return Double.parseDouble(raw);
		}
		long longVal = Long.parseLong(raw);
		if (longVal >= Integer.MIN_VALUE && longVal <= Integer.MAX_VALUE) {
			return (int) longVal;
		}
		return longVal;
	}

	@Override
	public void skipValue() {
		int depth = 0;
		do {
			int p = ensurePeeked();
			switch (p) {
				case PEEKED_BEGIN_OBJECT:
					pos++;
					push(SCOPE_EMPTY_OBJECT);
					depth++;
					break;
				case PEEKED_END_OBJECT:
					stackSize--;
					depth--;
					break;
				case PEEKED_BEGIN_ARRAY:
					pos++;
					push(SCOPE_EMPTY_ARRAY);
					depth++;
					break;
				case PEEKED_END_ARRAY:
					stackSize--;
					depth--;
					break;
				case PEEKED_NAME:
					readQuotedString();
					scopeStack[stackSize - 1] = SCOPE_DANGLING_NAME;
					break;
				case PEEKED_STRING:
					readQuotedString();
					break;
				case PEEKED_NUMBER:
					readRawNumber();
					break;
				case PEEKED_TRUE:
					consumeLiteral("true");
					break;
				case PEEKED_FALSE:
					consumeLiteral("false");
					break;
				case PEEKED_NULL:
					consumeLiteral("null");
					break;
				case PEEKED_EOF:
					return;
			}
			peeked = PEEKED_NONE;
		} while (depth > 0);
	}

	// ── State machine ──

	private int ensurePeeked() {
		if (peeked == PEEKED_NONE) {
			peeked = doPeek();
		}
		return peeked;
	}

	private int doPeek() {
		int scope = scopeStack[stackSize - 1];

		switch (scope) {
			case SCOPE_EMPTY_DOCUMENT:
				scopeStack[stackSize - 1] = SCOPE_NONEMPTY_DOCUMENT;
				break;

			case SCOPE_NONEMPTY_DOCUMENT:
				skipWhitespace();
				if (pos >= source.length()) return PEEKED_EOF;
				throw syntaxError("Expected end of document");

			case SCOPE_EMPTY_ARRAY:
				scopeStack[stackSize - 1] = SCOPE_NONEMPTY_ARRAY;
				skipWhitespace();
				if (pos < source.length() && source.charAt(pos) == ']') {
					pos++;
					return PEEKED_END_ARRAY;
				}
				break;

			case SCOPE_NONEMPTY_ARRAY:
				skipWhitespace();
				if (pos >= source.length()) throw syntaxError("Unterminated array");
				switch (source.charAt(pos)) {
					case ']':
						pos++;
						return PEEKED_END_ARRAY;
					case ',':
						pos++;
						break;
					default:
						throw syntaxError("Expected ',' or ']'");
				}
				break;

			case SCOPE_EMPTY_OBJECT:
				scopeStack[stackSize - 1] = SCOPE_NONEMPTY_OBJECT;
				skipWhitespace();
				if (pos < source.length()) {
					char c = source.charAt(pos);
					if (c == '}') {
						pos++;
						return PEEKED_END_OBJECT;
					}
					if (c == '"') return PEEKED_NAME;
				}
				throw syntaxError("Expected '\"' or '}'");

			case SCOPE_NONEMPTY_OBJECT:
				skipWhitespace();
				if (pos >= source.length()) throw syntaxError("Unterminated object");
				switch (source.charAt(pos)) {
					case '}':
						pos++;
						return PEEKED_END_OBJECT;
					case ',':
						pos++;
						skipWhitespace();
						if (pos < source.length() && source.charAt(pos) == '"') {
							return PEEKED_NAME;
						}
						throw syntaxError("Expected name after ','");
					default:
						throw syntaxError("Expected ',' or '}'");
				}

			case SCOPE_DANGLING_NAME:
				scopeStack[stackSize - 1] = SCOPE_NONEMPTY_OBJECT;
				skipWhitespace();
				if (pos >= source.length() || source.charAt(pos) != ':') {
					throw syntaxError("Expected ':'");
				}
				pos++;
				break;
		}

		// Read a value token
		skipWhitespace();
		if (pos >= source.length()) throw syntaxError("Unexpected end of input");

		return switch (source.charAt(pos)) {
			case '{' -> PEEKED_BEGIN_OBJECT;
			case '[' -> PEEKED_BEGIN_ARRAY;
			case '"' -> PEEKED_STRING;
			case 't' -> PEEKED_TRUE;
			case 'f' -> PEEKED_FALSE;
			case 'n' -> PEEKED_NULL;
			case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> PEEKED_NUMBER;
			default -> throw syntaxError("Unexpected character '" + source.charAt(pos) + "'");
		};
	}

	// ── Low-level parsing ──

	private void skipWhitespace() {
		while (pos < source.length()) {
			char c = source.charAt(pos);
			if (c != ' ' && c != '\t' && c != '\n' && c != '\r') return;
			pos++;
		}
	}

	private String readQuotedString() {
		if (pos >= source.length() || source.charAt(pos) != '"') {
			throw syntaxError("Expected '\"'");
		}
		pos++; // consume opening quote

		// Fast path: scan for closing quote without escapes
		int start = pos;
		while (pos < source.length()) {
			char c = source.charAt(pos);
			if (c == '"') {
				String result = source.substring(start, pos);
				pos++; // consume closing quote
				return result;
			}
			if (c == '\\') break; // has escapes, use slow path
			pos++;
		}

		// Slow path: string has escape sequences
		StringBuilder sb = new StringBuilder(source.substring(start, pos));
		while (pos < source.length()) {
			char c = source.charAt(pos++);
			switch (c) {
				case '"':
					return sb.toString();
				case '\\':
					if (pos >= source.length()) throw syntaxError("Unterminated escape sequence");
					char escaped = source.charAt(pos++);
					switch (escaped) {
						case '"': sb.append('"'); break;
						case '\\': sb.append('\\'); break;
						case '/': sb.append('/'); break;
						case 'b': sb.append('\b'); break;
						case 'f': sb.append('\f'); break;
						case 'n': sb.append('\n'); break;
						case 'r': sb.append('\r'); break;
						case 't': sb.append('\t'); break;
						case 'u':
							if (pos + 4 > source.length()) throw syntaxError("Unterminated unicode escape");
							String hex = source.substring(pos, pos + 4);
							pos += 4;
							sb.append((char) Integer.parseInt(hex, 16));
							break;
						default:
							throw syntaxError("Invalid escape: '\\" + escaped + "'");
					}
					break;
				default:
					sb.append(c);
					break;
			}
		}
		throw syntaxError("Unterminated string");
	}

	private String readRawNumber() {
		int start = pos;
		if (pos < source.length() && source.charAt(pos) == '-') pos++;
		if (pos < source.length() && source.charAt(pos) == '0') {
			pos++;
		} else {
			readDigits();
		}
		if (pos < source.length() && source.charAt(pos) == '.') {
			pos++;
			readDigits();
		}
		if (pos < source.length() && (source.charAt(pos) == 'e' || source.charAt(pos) == 'E')) {
			pos++;
			if (pos < source.length() && (source.charAt(pos) == '+' || source.charAt(pos) == '-')) pos++;
			readDigits();
		}
		return source.substring(start, pos);
	}

	private void readDigits() {
		if (pos >= source.length() || source.charAt(pos) < '0' || source.charAt(pos) > '9') {
			throw syntaxError("Expected digit");
		}
		while (pos < source.length() && source.charAt(pos) >= '0' && source.charAt(pos) <= '9') {
			pos++;
		}
	}

	private void consumeLiteral(String expected) {
		for (int i = 0; i < expected.length(); i++) {
			if (pos + i >= source.length() || source.charAt(pos + i) != expected.charAt(i)) {
				throw syntaxError("Expected '" + expected + "'");
			}
		}
		pos += expected.length();
	}

	private void push(int scope) {
		if (stackSize >= scopeStack.length) {
			int[] newStack = new int[scopeStack.length * 2];
			System.arraycopy(scopeStack, 0, newStack, 0, scopeStack.length);
			scopeStack = newStack;
		}
		scopeStack[stackSize++] = scope;
	}

	private IllegalStateException syntaxError(String message) {
		return new IllegalStateException(message + " at position " + pos);
	}

	private IllegalStateException illegalState(String expected, int actual) {
		String actualName = switch (actual) {
			case PEEKED_BEGIN_OBJECT -> "BEGIN_OBJECT";
			case PEEKED_END_OBJECT -> "END_OBJECT";
			case PEEKED_BEGIN_ARRAY -> "BEGIN_ARRAY";
			case PEEKED_END_ARRAY -> "END_ARRAY";
			case PEEKED_TRUE, PEEKED_FALSE -> "BOOLEAN";
			case PEEKED_NULL -> "NULL";
			case PEEKED_STRING -> "STRING";
			case PEEKED_NAME -> "NAME";
			case PEEKED_NUMBER -> "NUMBER";
			case PEEKED_EOF -> "END_DOCUMENT";
			default -> "UNKNOWN";
		};
		return new IllegalStateException("Expected " + expected + " but was " + actualName + " at position " + pos);
	}
}
