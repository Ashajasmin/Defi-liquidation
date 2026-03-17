/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 *
 * NEW FILE: Sub-day-aware A/365-Fixed DCC (Feb 2026)
 * Mirrors actus-core/ActualThreeSixtyFiveFixedSubDay.java
 */
package org.actus.risksrv3.core.conventions.daycount;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ActualThreeSixtyFiveFixedSubDay implements DayCountConventionProvider {

    private static final double SECONDS_PER_YEAR = 365.0 * 24.0 * 3600.0;

    @Override
    public double dayCount(LocalDateTime startTime, LocalDateTime endTime) {
        long seconds = ChronoUnit.SECONDS.between(startTime, endTime);
        return seconds / 86400.0;
    }

    @Override
    public double dayCountFraction(LocalDateTime startTime, LocalDateTime endTime) {
        long seconds = ChronoUnit.SECONDS.between(startTime, endTime);
        return seconds / SECONDS_PER_YEAR;
    }
}
