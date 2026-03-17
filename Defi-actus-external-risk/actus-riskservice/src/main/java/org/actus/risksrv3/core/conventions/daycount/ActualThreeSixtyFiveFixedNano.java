/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 *
 * NEW FILE: Nanosecond-precision A/365-Fixed DCC (Feb 2026)
 * Mirrors actus-core/ActualThreeSixtyFiveFixedNano.java
 */
package org.actus.risksrv3.core.conventions.daycount;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ActualThreeSixtyFiveFixedNano implements DayCountConventionProvider {

    private static final double NANOS_PER_YEAR = 365.0 * 24.0 * 3600.0 * 1_000_000_000.0;

    @Override
    public double dayCount(LocalDateTime startTime, LocalDateTime endTime) {
        long nanos = ChronoUnit.NANOS.between(startTime, endTime);
        return nanos / (86400.0 * 1_000_000_000.0);
    }

    @Override
    public double dayCountFraction(LocalDateTime startTime, LocalDateTime endTime) {
        long nanos = ChronoUnit.NANOS.between(startTime, endTime);
        return nanos / NANOS_PER_YEAR;
    }
}
