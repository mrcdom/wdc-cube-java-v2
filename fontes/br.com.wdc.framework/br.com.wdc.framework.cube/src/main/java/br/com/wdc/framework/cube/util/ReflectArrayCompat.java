package br.com.wdc.framework.cube.util;

/**
 * Emulação dos métodos de {@code java.lang.reflect.Array} sem uso de reflection.
 * Compatível com TeaVM e outros AOT compilers que não suportam reflection.
 */
public final class ReflectArrayCompat {

    private ReflectArrayCompat() {
    }

    public static int getLength(Object array) {
        if (array instanceof Object[] a) return a.length;
        if (array instanceof int[] a) return a.length;
        if (array instanceof long[] a) return a.length;
        if (array instanceof double[] a) return a.length;
        if (array instanceof float[] a) return a.length;
        if (array instanceof byte[] a) return a.length;
        if (array instanceof short[] a) return a.length;
        if (array instanceof char[] a) return a.length;
        if (array instanceof boolean[] a) return a.length;
        throw new IllegalArgumentException("Not an array: " + array.getClass().getName());
    }

    public static Object get(Object array, int index) {
        if (array instanceof Object[] a) return a[index];
        if (array instanceof int[] a) return a[index];
        if (array instanceof long[] a) return a[index];
        if (array instanceof double[] a) return a[index];
        if (array instanceof float[] a) return a[index];
        if (array instanceof byte[] a) return a[index];
        if (array instanceof short[] a) return a[index];
        if (array instanceof char[] a) return a[index];
        if (array instanceof boolean[] a) return a[index];
        throw new IllegalArgumentException("Not an array: " + array.getClass().getName());
    }

    public static void set(Object array, int index, Object value) {
        if (array instanceof Object[] a) { a[index] = value; return; }
        if (array instanceof int[] a) { a[index] = (int) value; return; }
        if (array instanceof long[] a) { a[index] = (long) value; return; }
        if (array instanceof double[] a) { a[index] = (double) value; return; }
        if (array instanceof float[] a) { a[index] = (float) value; return; }
        if (array instanceof byte[] a) { a[index] = (byte) value; return; }
        if (array instanceof short[] a) { a[index] = (short) value; return; }
        if (array instanceof char[] a) { a[index] = (char) value; return; }
        if (array instanceof boolean[] a) { a[index] = (boolean) value; return; }
        throw new IllegalArgumentException("Not an array: " + array.getClass().getName());
    }

}
