package br.com.wdc.framework.commons.serialization;

import java.util.Base64;

/**
 * Streaming JSON writer — puro Java, sem dependências externas.
 * <p>
 * Implementa {@link ExtensibleObjectOutput} escrevendo diretamente num StringBuilder
 * sem construir árvore intermediária (Map/List).
 */
public class JsonStreamWriter implements ExtensibleObjectOutput {

	private static final int SCOPE_EMPTY_OBJECT = 0;
	private static final int SCOPE_NONEMPTY_OBJECT = 1;
	private static final int SCOPE_EMPTY_ARRAY = 2;
	private static final int SCOPE_NONEMPTY_ARRAY = 3;

	private final StringBuilder sb = new StringBuilder();
	private int[] scopeStack = new int[16];
	private int stackSize = 0;
	private String pendingName;

	public String result() {
		return sb.toString();
	}

	@Override
	public ExtensibleObjectOutput beginObject() {
		beforeValue();
		sb.append('{');
		push(SCOPE_EMPTY_OBJECT);
		return this;
	}

	@Override
	public ExtensibleObjectOutput endObject() {
		stackSize--;
		sb.append('}');
		return this;
	}

	@Override
	public ExtensibleObjectOutput beginArray() {
		beforeValue();
		sb.append('[');
		push(SCOPE_EMPTY_ARRAY);
		return this;
	}

	@Override
	public ExtensibleObjectOutput endArray() {
		stackSize--;
		sb.append(']');
		return this;
	}

	@Override
	public ExtensibleObjectOutput name(String name) {
		pendingName = name;
		return this;
	}

	@Override
	public ExtensibleObjectOutput name(int id, String name) {
		pendingName = (name == null || name.isBlank()) ? Integer.toString(id) : name;
		return this;
	}

	@Override
	public ExtensibleObjectOutput value(String value) {
		if (value == null) return nullValue();
		beforeValue();
		writeString(value);
		return this;
	}

	@Override
	public ExtensibleObjectOutput value(byte[] value) {
		if (value == null) return nullValue();
		beforeValue();
		writeString(Base64.getEncoder().encodeToString(value));
		return this;
	}

	@Override
	public ExtensibleObjectOutput nullValue() {
		beforeValue();
		sb.append("null");
		return this;
	}

	@Override
	public ExtensibleObjectOutput value(boolean value) {
		beforeValue();
		sb.append(value ? "true" : "false");
		return this;
	}

	@Override
	public ExtensibleObjectOutput value(double value) {
		beforeValue();
		sb.append(value);
		return this;
	}

	@Override
	public ExtensibleObjectOutput value(long value) {
		beforeValue();
		sb.append(value);
		return this;
	}

	@Override
	public ExtensibleObjectOutput value(Number value) {
		if (value == null) return nullValue();
		beforeValue();
		sb.append(value);
		return this;
	}

	// ── Internal ──

	private void beforeValue() {
		if (stackSize > 0) {
			int scope = scopeStack[stackSize - 1];
			switch (scope) {
				case SCOPE_EMPTY_OBJECT:
					scopeStack[stackSize - 1] = SCOPE_NONEMPTY_OBJECT;
					writePendingName();
					break;
				case SCOPE_NONEMPTY_OBJECT:
					sb.append(',');
					writePendingName();
					break;
				case SCOPE_EMPTY_ARRAY:
					scopeStack[stackSize - 1] = SCOPE_NONEMPTY_ARRAY;
					break;
				case SCOPE_NONEMPTY_ARRAY:
					sb.append(',');
					break;
				default:
					break;
			}
		}
	}

	private void writePendingName() {
		if (pendingName == null) {
			throw new IllegalStateException("value() called without name() in object");
		}
		writeString(pendingName);
		sb.append(':');
		pendingName = null;
	}

	private void writeString(String value) {
		sb.append('"');
		for (int i = 0, len = value.length(); i < len; i++) {
			char c = value.charAt(i);
			switch (c) {
				case '"':
					sb.append("\\\"");
					break;
				case '\\':
					sb.append("\\\\");
					break;
				case '\b':
					sb.append("\\b");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '\t':
					sb.append("\\t");
					break;
				default:
					if (c < 0x20) {
						sb.append("\\u");
						sb.append(String.format("%04x", (int) c));
					} else {
						sb.append(c);
					}
					break;
			}
		}
		sb.append('"');
	}

	private void push(int scope) {
		if (stackSize >= scopeStack.length) {
			int[] newStack = new int[scopeStack.length * 2];
			System.arraycopy(scopeStack, 0, newStack, 0, scopeStack.length);
			scopeStack = newStack;
		}
		scopeStack[stackSize++] = scope;
	}
}
