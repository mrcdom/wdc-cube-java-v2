package br.com.wdc.store.view.remote.shell.swt;

import java.util.LinkedHashMap;
import java.util.Map;

import br.com.wdc.framework.cube.remote.bridge.java.model.ScopeSnapshot;
import br.com.wdc.store.i18n.Message;

/** The messages of the host's scopes: each travels as {@code {"key": ..., "args": {...}}}. */
final class WireMessages {

	private WireMessages() {
	}

	/** The message in {@code field} of {@code snapshot}; {@code null} when there is none. */
	static Message messageOf(ScopeSnapshot snapshot, String field) {
		return messageOf(snapshot.values().get(field));
	}

	/** The message of a wire value ({@code {"key", "args"}}); {@code null} when it is not one. */
	static Message messageOf(Object wire) {
		if (!(wire instanceof Map<?, ?> map) || !(map.get("key") instanceof String key) || key.isEmpty()) {
			return null;
		}
		var args = new LinkedHashMap<String, String>();
		if (map.get("args") instanceof Map<?, ?> wireArgs) {
			wireArgs.forEach((name, value) -> args.put(String.valueOf(name), value == null ? "" : String.valueOf(value)));
		}
		return new Message(key, args);
	}
}
