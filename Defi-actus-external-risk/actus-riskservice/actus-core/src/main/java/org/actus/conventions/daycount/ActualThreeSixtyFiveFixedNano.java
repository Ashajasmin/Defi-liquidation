/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 *
 * NEW FILE: Nanosecond-precision A/365-Fixed day count convention (Feb 2026)
 * For millisecond/microsecond cycle granularity.
 */
package org.actus.conventions.daycount;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Nanosecond-precision A/365-Fixed day count convention (AA365N).
 * <p>
 * Uses ChronoUnit.NANOS as the base unit:
 *   dayCountFraction = nanos_between / (365 * 24 * 3600 * 1,000,000,000)
 * <p>
 * Use this for millisecond or microsecond cycle granularity (PT0.001S, PT0.000001S).
 * For hour/minute/second granularity, AA365S (second-precision) is sufficient.
 */
public class ActualThreeSixtyFiveFixedNano implements DayCountConventionProvider {

    private static final double NANOS_PER_YEAR = 365.0 * 24.0 * 3600.0 * 1_000_000_000.0;

    @Override
    public double dayCount(LocalDateTime startTime, LocalDateTime endTime) {
        long nanos = ChronoUnit.NANOS.between(startTime, endTime);
        return nanos / (86400.0 * 1_000_000_000.0);  // fractional days
    }

    @Override
    public double dayCountFraction(LocalDateTime startTime, LocalDateTime endTime) {
        long nanos = ChronoUnit.NANOS.between(startTime, endTime);
        return nanos / NANOS_PER_YEAR;
    }
}
