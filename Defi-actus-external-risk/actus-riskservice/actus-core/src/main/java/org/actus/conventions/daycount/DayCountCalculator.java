/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 *
 * MODIFIED: Added AA365S/AA365N sub-day DCC cases (Feb 2026)
 * MODIFIED: Bypass toFullHours() rounding for sub-day conventions
 */
package org.actus.conventions.daycount;

import org.actus.time.TimeAdjuster;
import org.actus.time.calendar.BusinessDayCalendarProvider;
import org.actus.util.StringUtils;

import java.time.LocalDateTime;

public class DayCountCalculator {
    private DayCountConventionProvider convention;

    public DayCountCalculator(DayCountConventionProvider convention) {
        this.convention = convention;
    }

    public DayCountCalculator(String convention, BusinessDayCalendarProvider calendar) {
        switch (convention) {
            case StringUtils.DayCountConvention_30E360:
                this.convention = new ThirtyEThreeSixty();
                break;
            case StringUtils.DayCountConvention_30E360ISDA:
                this.convention = new ThirtyEThreeSixtyISDA();
                break;
            case StringUtils.DayCountConvention_A360:
                this.convention = new ActualThreeSixty();
                break;
            case StringUtils.DayCountConvention_A365:
                this.convention = new ActualThreeSixtyFiveFixed();
                break;
            case StringUtils.DayCountConvention_AAISDA:
                this.convention = new ActualActualISDA();
                break;
            case StringUtils.DayCountConvention_B252:
                this.convention = new BusinessTwoFiftyTwo();
                // TODO: ((BusinessTwoFiftyTwo) this.convention).setCalendar(calendar);
                break;
            case StringUtils.DayCountConvention_A336:
            	this.convention = new ActualThreeThirtySix();
                break;
            case StringUtils.DayCountConvention_28336:
            	this.convention = new TwentyEightThreeThirtySix();
            	break;

            // ========================== NEW CASES ==========================
            case StringUtils.DayCountConvention_AA365S:
                this.convention = new ActualThreeSixtyFiveFixedSubDay();
                break;
            case StringUtils.DayCountConvention_AA365N:
                this.convention = new ActualThreeSixtyFiveFixedNano();
                break;
            // ===============================================================
        }
    }

    /**
     * Compute day count fraction between two time-instances.
     * <p>
     * For existing day+ conventions: timestamps are rounded to "full hour" first
     * (minutes 0-29 floored, 30-59 ceiled).
     * <p>
     * For sub-day conventions (AA365S, AA365N): toFullHours() is BYPASSED to
     * preserve minute/second/nanosecond precision.
     */
    public double dayCountFraction(LocalDateTime startTime, LocalDateTime endTime) {
        // NEW: Bypass toFullHours() for sub-day conventions
        if (convention instanceof ActualThreeSixtyFiveFixedSubDay
                || convention instanceof ActualThreeSixtyFiveFixedNano) {
            return convention.dayCountFraction(startTime, endTime);
        }
        // Existing behavior: round to full hours first
        return convention.dayCountFraction(TimeAdjuster.toFullHours(startTime), TimeAdjuster.toFullHours(endTime));
    }
}
