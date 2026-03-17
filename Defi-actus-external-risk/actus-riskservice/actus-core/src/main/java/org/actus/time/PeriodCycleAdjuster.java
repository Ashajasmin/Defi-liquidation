/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */

package org.actus.time;

import java.time.LocalDateTime;
import java.time.Period;

import org.actus.util.CycleUtils;

/**
 * A temporal adjuster that supports cycle adjustments
 * <p>
 *
 *
 */
public final class PeriodCycleAdjuster implements CycleAdjusterProvider {
    private Period period;
    private char stub;
    /**
     * Public constructor
     * 
     * @param period
     */
    public PeriodCycleAdjuster(String cycle) {
        this.period=CycleUtils.parsePeriod(cycle);
        this.stub=CycleUtils.parseStub(cycle);
    }

    /**
     * Cycle adjuster
     * <p>
     * This adjuster adds a full cycle to a given time depending on the cycle definition.
     */
    @Override
    public LocalDateTime plusCycle(LocalDateTime time) {
        return time.plus(period);
    }

    /**
     * Cycle adjuster
     * <p>
     * This adjuster deducts a full cycle to a given time depending on the cycle definition.
     */
    @Override
    public LocalDateTime minusCycle(LocalDateTime time) {
        return time.minus(period);
    }

}
