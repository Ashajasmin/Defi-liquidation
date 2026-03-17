/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */

package org.actus.time;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

import org.actus.util.CycleUtils;

/**
 * A temporal adjuster that supports cycle adjustments
 * <p>
 *
 *
 */
public final class WeekdayCycleAdjuster implements CycleAdjusterProvider{
    private DayOfWeek weekday;
    private int position;
    private char stub;

    /**
     * Public constructor
     * 
     * @param period
     */
    public WeekdayCycleAdjuster(String cycle) {
        this.weekday=CycleUtils.parseWeekday(cycle);
        this.position=CycleUtils.parsePosition(cycle);
        this.stub = CycleUtils.parseStub(cycle);
    }

    /**
     * Cycle adjuster
     * <p>
     * This adjuster adds a full cycle to a given time depending on the cycle definition.
     */
    @Override
    public LocalDateTime plusCycle(LocalDateTime time) {
        return time.plusMonths(1).with(TemporalAdjusters.dayOfWeekInMonth(position, weekday));
    }

    /**
     * Cycle adjuster
     * <p>
     * This adjuster deducts a full cycle to a given time depending on the cycle definition.
     */
    @Override
    public LocalDateTime minusCycle(LocalDateTime time) {
        return time.minusMonths(1).with(TemporalAdjusters.dayOfWeekInMonth(position, weekday));
    }

}
