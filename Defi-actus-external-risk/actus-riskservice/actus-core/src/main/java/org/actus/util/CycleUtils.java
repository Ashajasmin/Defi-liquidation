/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 *
 * MODIFIED: Sub-day granularity support (Feb 2026)
 * ADDED: isDuration(), parseTemporalAmount()
 * UNCHANGED: All existing methods retained for backward compatibility
 */
package org.actus.util;

import org.actus.AttributeConversionException;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.DayOfWeek;
import static java.util.Locale.forLanguageTag;

public final class CycleUtils {
    
    public static boolean isPeriod(String cycle) {
        return cycle.charAt(0)=='P';
    }

    // ========================== NEW METHODS ==========================

    /**
     * Returns true if the cycle represents a sub-day (Duration-based) period.
     * Detection: "T" after "P" indicates time components (hours/minutes/seconds).
     *
     *   "PT1HL0"  → true   (hourly — T present)
     *   "P1DL0"   → false  (daily — no T)
     *   "P1HL0"   → false  (half-year — no T, H=Half-Year in ACTUS)
     */
    public static boolean isDuration(String cycle) {
        String periodPart = cycle.split("L")[0];
        return periodPart.contains("T");
    }

    /**
     * Parse cycle to TemporalAmount — returns Duration for sub-day, Period for day+.
     *
     *   "PT1HL0"     → Duration.parse("PT1H")     → 1 hour
     *   "PT30ML0"    → Duration.parse("PT30M")    → 30 minutes
     *   "PT1SL0"     → Duration.parse("PT1S")     → 1 second
     *   "PT0.001SL0" → Duration.parse("PT0.001S") → 1 millisecond
     *   "P1DL0"      → Period.parse("P1D")        → 1 day (unchanged)
     *   "P1ML0"      → Period.parse("P1M")        → 1 month (unchanged)
     */
    public static TemporalAmount parseTemporalAmount(String cycle) {
        String periodPart = cycle.split("L")[0];
        try {
            if (periodPart.contains("T")) {
                return Duration.parse(periodPart);
            } else {
                return Period.parse(periodPart);
            }
        } catch (Exception e) {
            throw new AttributeConversionException();
        }
    }

    // ========================== EXISTING METHODS (UNCHANGED) ==========================

    public static Period parsePeriod(String cycle, boolean stub) {
        return parsePeriod(cycle);
    }
    
    public static Period parsePeriod(String cycle) {
        Period period;
        try {
            period = Period.parse(cycle.split("L")[0]);
        } catch (Exception e) {
          throw(new AttributeConversionException());
        }
        return period;
    }
    
    public static int parsePosition(String cycle) {
        int position;
        try {
          position = Integer.parseInt(""+cycle.charAt(0));
        } catch (Exception e) {
          throw(new AttributeConversionException());
        }
        return position; 
    }

    public static DayOfWeek parseWeekday(String cycle) {
        DayOfWeek weekday;
        try {
          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E", forLanguageTag("en"));
          TemporalAccessor accessor = formatter.parse(cycle.split("L")[0].substring(1));
          weekday= DayOfWeek.from(accessor);
        } catch (Exception e) {
          throw(new AttributeConversionException());
        }
        return weekday; 
    }

    public static char parseStub(String cycle) throws AttributeConversionException {
        char stub;
        try {
          stub = cycle.split("L")[1].charAt(0);
          if(!(stub==StringUtils.LongStub || stub==StringUtils.ShortStub)) throw(new AttributeConversionException());
        } catch (Exception e) {
          throw(new AttributeConversionException());
        }
        return stub;
    }
    
}
