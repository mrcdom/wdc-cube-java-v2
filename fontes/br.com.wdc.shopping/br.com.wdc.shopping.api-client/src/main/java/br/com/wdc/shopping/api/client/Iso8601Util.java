package br.com.wdc.shopping.api.client;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

/**
 * Utilitário para parsing de datas ISO 8601 sem usar {@code java.time.format.DateTimeFormatter}.
 * <p>
 * Necessário para compatibilidade com RoboVM, onde {@code DateTimeFormatter} e
 * {@code AbstractChronology} falham devido a lambdas serializáveis não suportadas pelo AOT.
 * <p>
 * Usa {@link SimpleDateFormat} (disponível no RoboVM/Android runtime) para parsing
 * e constrói os objetos {@code java.time} via factory methods que não dependem de formatters.
 */
final class Iso8601Util {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private Iso8601Util() {}

    /**
     * Parses an ISO 8601 instant string (e.g., "2024-01-15T10:30:00Z" or "2024-01-15T10:30:00.123Z")
     * and returns epoch seconds.
     */
    static long parseEpochSecond(String iso) {
        return parseMillis(iso) / 1000L;
    }

    /**
     * Parses an ISO 8601 instant string and returns a {@link java.time.Instant}.
     * Uses {@code Instant.ofEpochSecond()} which does NOT trigger DateTimeFormatter initialization.
     */
    static Instant parseInstant(String iso) {
        long millis = parseMillis(iso);
        long seconds = millis / 1000L;
        int nanos = (int) ((millis % 1000L) * 1_000_000L);
        if (nanos < 0) {
            seconds--;
            nanos += 1_000_000_000;
        }
        return Instant.ofEpochSecond(seconds, nanos);
    }

    /**
     * Parses an ISO 8601 offset date-time string (e.g., "2024-01-15T10:30:00+03:00" or "2024-01-15T10:30:00Z")
     * and returns an {@link OffsetDateTime}.
     * Uses {@code OffsetDateTime.ofInstant()} which does NOT trigger DateTimeFormatter.
     */
    static OffsetDateTime parseOffsetDateTime(String iso) {
        // Parse offset from the string
        ZoneOffset offset = extractOffset(iso);
        // Parse the instant part
        long millis = parseMillis(iso);
        long seconds = millis / 1000L;
        int nanos = (int) ((millis % 1000L) * 1_000_000L);
        if (nanos < 0) {
            seconds--;
            nanos += 1_000_000_000;
        }
        Instant instant = Instant.ofEpochSecond(seconds, nanos);
        return OffsetDateTime.ofInstant(instant, offset);
    }

    private static long parseMillis(String iso) {
        String cleaned = iso;
        // Remove offset suffix for SimpleDateFormat (it only handles UTC well)
        if (cleaned.endsWith("Z")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        } else {
            // Remove +HH:MM or -HH:MM offset
            int lastPlus = cleaned.lastIndexOf('+');
            int lastMinus = cleaned.lastIndexOf('-');
            int offsetStart = Math.max(lastPlus, lastMinus);
            // Make sure it's an offset, not the date minus (position > 10 means it's past the date part)
            if (offsetStart > 10) {
                cleaned = cleaned.substring(0, offsetStart);
            }
        }

        // Handle fractional seconds
        int dotIdx = cleaned.indexOf('.');
        int fractionalMillis = 0;
        if (dotIdx > 0) {
            String fracStr = cleaned.substring(dotIdx + 1);
            cleaned = cleaned.substring(0, dotIdx);
            // Pad or truncate to 3 digits for milliseconds
            if (fracStr.length() > 3) {
                fracStr = fracStr.substring(0, 3);
            }
            while (fracStr.length() < 3) {
                fracStr = fracStr + "0";
            }
            fractionalMillis = Integer.parseInt(fracStr);
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(UTC);
            long baseMillis = sdf.parse(cleaned).getTime();

            // If original had an offset, we need to adjust
            int offsetMinutes = extractOffsetMinutes(iso);
            return baseMillis + fractionalMillis - (offsetMinutes * 60L * 1000L);
        } catch (ParseException e) {
            throw new RuntimeException("Cannot parse ISO 8601 date: " + iso, e);
        }
    }

    private static int extractOffsetMinutes(String iso) {
        if (iso.endsWith("Z")) {
            return 0;
        }
        int lastPlus = iso.lastIndexOf('+');
        int lastMinus = iso.lastIndexOf('-');
        int offsetStart = Math.max(lastPlus, lastMinus);
        if (offsetStart <= 10) {
            return 0; // No offset found (the minus is part of the date)
        }
        String offsetStr = iso.substring(offsetStart);
        int sign = offsetStr.charAt(0) == '+' ? 1 : -1;
        offsetStr = offsetStr.substring(1);
        String[] parts = offsetStr.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return sign * (hours * 60 + minutes);
    }

    private static ZoneOffset extractOffset(String iso) {
        if (iso.endsWith("Z")) {
            return ZoneOffset.UTC;
        }
        int minutes = extractOffsetMinutes(iso);
        return ZoneOffset.ofTotalSeconds(minutes * 60);
    }
}
