package br.com.wdc.framework.domain.security.policy;

/**
 * How a textual field is shown to a role that may see it only partially — a document number with only the last digits,
 * a login with only the first letter.
 *
 * <p>
 * A masked field can be read, but not used in a filter or an ordering (that would reveal what the mask hides), and not
 * written: a {@link DataGrant} that masks a field cannot also write it, so a masked value sent back is refused rather
 * than stored over the real one.
 * </p>
 */
@FunctionalInterface
public interface Mask {

    char FILL = '*';

    String apply(String value);

    /** Keeps the first {@code n} characters and hides the rest ({@code "jdoe"} → {@code "j***"}). */
    static Mask keepFirst(int n) {
        return value -> {
            var kept = Math.min(n, value.length());
            return value.substring(0, kept) + String.valueOf(FILL).repeat(value.length() - kept);
        };
    }

    /** Keeps the last {@code n} characters and hides the rest ({@code "12345678909"} → {@code "*********09"}). */
    static Mask keepLast(int n) {
        return value -> {
            var kept = Math.min(n, value.length());
            return String.valueOf(FILL).repeat(value.length() - kept) + value.substring(value.length() - kept);
        };
    }
}
