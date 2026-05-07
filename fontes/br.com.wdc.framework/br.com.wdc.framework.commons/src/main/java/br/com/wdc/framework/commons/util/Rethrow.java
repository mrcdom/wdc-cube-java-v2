package br.com.wdc.framework.commons.util;

public class Rethrow {

    private Rethrow() {
        // NOOP
    }

    public static <T extends RuntimeException> T asRuntimeException(final Throwable throwable) {
        // claim that the typeErasure invocation throws a RuntimeException
        return Rethrow.<T, RuntimeException>eraseType(throwable);
    }

    /**
     * Claims a Throwable is another Throwable type using type erasure. This hides a checked exception from the Java
     * compiler, allowing a checked exception to be thrown without having the exception in the method's throw clause.
     */
    @SuppressWarnings("unchecked")
    private static <R, T extends Throwable> R eraseType(final Throwable throwable) throws T {
        throw (T) throwable;
    }

}
