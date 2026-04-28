package br.com.wdc.framework.commons.convert;

import java.time.ZoneOffset;
import java.util.TimeZone;

public class DateUtil {
    
    private DateUtil() {
        super();
    }
    
    public static ZoneOffset getSysZoneOffset() {
        return DateUtilsLazyConsts.zoneOffset;
    }

    public static java.util.TimeZone getSysTimeZone() {
        return DateUtilsLazyConsts.timeZone;
    }

    private static class DateUtilsLazyConsts {

        static ZoneOffset zoneOffset = initDefaultZoneOffset();

        static java.util.TimeZone timeZone = initTimeZone();

        static ZoneOffset initDefaultZoneOffset() {
            TimeZone tz = TimeZone.getDefault();
            return ZoneOffset.ofTotalSeconds(tz.getOffset(System.currentTimeMillis()) / 1000);
        }

        static java.util.TimeZone initTimeZone() {
            return java.util.TimeZone.getDefault();
        }

    }
}
