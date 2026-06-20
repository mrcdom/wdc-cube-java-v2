package br.com.wdc.shopping.view.remote.shell.cn1.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Acesso seguro a campos de um estado JSON (Map vindo do bridge). */
public final class Json {

    private Json() {
        // NOOP
    }

    public static String str(Map<String, Object> m, String key) {
        Object o = m != null ? m.get(key) : null;
        return o != null ? o.toString() : "";
    }

    public static int intOf(Map<String, Object> m, String key) {
        Object o = m != null ? m.get(key) : null;
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return o != null ? Integer.parseInt(o.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static long longOf(Map<String, Object> m, String key) {
        Object o = m != null ? m.get(key) : null;
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return o != null ? Long.parseLong(o.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static double doubleOf(Map<String, Object> m, String key) {
        Object o = m != null ? m.get(key) : null;
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        try {
            return o != null ? Double.parseDouble(o.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean boolOf(Map<String, Object> m, String key) {
        Object o = m != null ? m.get(key) : null;
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        return o != null && "true".equals(o.toString());
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object o) {
        return o instanceof List ? (List<Object>) o : Collections.emptyList();
    }
}
