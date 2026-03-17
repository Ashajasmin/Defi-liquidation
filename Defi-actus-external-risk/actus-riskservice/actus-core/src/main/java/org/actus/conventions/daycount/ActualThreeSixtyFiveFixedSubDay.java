/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 *
 * NEW FILE: Sub-day-aware A/365-Fixed day count convention (Feb 2026)
 * Uses SECONDS as base unit for sub-day precision.
 * For day+ intervals, produces identical results to existing A365.
 */
package org.actus.conventions.daycount;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Sub-day-aware A/365-Fixed day count convention (AA365S).
 * <p>
 * Uses ChronoUnit.SECONDS as the base unit:
 *   dayCountFraction = seconds_between / (365 * 24 * 3600)
 * <p>
 * Handles ALL sub-day granularities with a single implementation:
 *   1 hour    → 3600 / 31,536,000   = 0.000114155
 *   1 minute  → 60 / 31,536,000     = 0.000001903
 *   1 second  → 1 / 31,536,000      = 0.0000000317
 * <p>
 * Backward compatible: 1 day = 86400/31536000 = 1/365 (identical to A365).
 */
public class ActualThreeSixtyFiveFixedSubDay implements DayCountConventionProvider {

    private static final double SECONDS_PER_YEAR = 365.0 * 24.0 * 3600.0; // 31,536,000

    @Override
    public double dayCount(LocalDateTime startTime, LocalDateTime endTime) {
        long seconds = ChronoUnit.SECONDS.between(startTime, endTime);
        return seconds / 86400.0;  // fractional days
    }

    @Override
    public double dayCountFraction(LocalDateTime startTime, LocalDateTime endTime) {
        long seconds = ChronoUnit.SECONDS.between(startTime, endTime);
        return seconds / SECONDS_PER_YEAR;
    }
}
