package br.com.wdc.shopping.domain.utils;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@SuppressWarnings({ "java:S6548", "java:S1170" })
public class ProjectionValues {

    public static final ProjectionValues INSTANCE = new ProjectionValues() {
    };

    private ProjectionValues() {
        super();
    }

    public final Boolean bool = Boolean.TRUE;
    public final Byte i8 = Byte.valueOf((byte) 1);
    public final Short i16 = Short.valueOf((short) 1);
    public final Integer i32 = Integer.valueOf(1);
    public final Long i64 = Long.valueOf(1L);
    public final Float f32 = Float.valueOf(1f);
    public final Double f64 = Double.valueOf(1d);
    public final Character chr = Character.valueOf('A');
    public final String str = "dummy";
    public final byte[] bin = new byte[0];

    public final java.math.BigInteger bInt = java.math.BigInteger.ONE;
    public final java.math.BigDecimal bDec = java.math.BigDecimal.ONE;
    public final java.util.Date date = new java.util.Date(115754400000L);
    public final LocalDate localDate = LocalDate.MIN;
    public final OffsetDateTime offsetDateTime = OffsetDateTime.MIN;

    public <T> ProjectionList<T> singletonList(T bean, Object criteria) {
        return new ProjectionList<>(bean, criteria);
    }

}
