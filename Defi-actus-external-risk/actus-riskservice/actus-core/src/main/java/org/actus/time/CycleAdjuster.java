/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */

package org.actus.time;

import java.time.LocalDateTime;

import org.actus.util.CycleUtils;

/**
 * A temporal adjuster that supports cycle adjustments
 * <p>
 *
 *
 */
public class CycleAdjuster {
    private CycleAdjusterProvider adjuster;

    public CycleAdjuster(String cycle) {
        if(CycleUtils.isPeriod(cycle)) {
            adjuster = new PeriodCycleAdjuster(cycle);
        } else {
            adjuster = new WeekdayCycleAdjuster(cycle);
        }
    }

    /**
     * Cycle adjuster
     * <p>
     * This adjuster adds a full cycle to a given time depending on the cycle definition.
     */
    public LocalDateTime plusCycle(LocalDateTime time) {
        return adjuster.plusCycle(time);
    }

    /**
     * Cycle adjuster
     * <p>
     * This adjuster deducts a full cycle to a given time depending on the cycle definition.
     */
    public LocalDateTime minusCycle(LocalDateTime time) {
        return adjuster.minusCycle(time);
    }

}