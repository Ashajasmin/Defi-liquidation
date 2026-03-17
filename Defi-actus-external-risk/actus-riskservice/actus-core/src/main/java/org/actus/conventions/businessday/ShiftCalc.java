/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.conventions.businessday;

import java.time.LocalDateTime;

/**
 * Component that represents the Shift-first-Calculate-Second convention
 * <p>
 * This convention assumes that when shifting the {@link ContractEvent}-time according
 * to a {@link BusinessDayConvention}, the time is shifted first and calculations
 * are performed thereafter. Hence, calculations in {@link PayOffFunction} and 
 * {@link StateTransitionFunction} are based on the shifted time as well.
 */
public final class ShiftCalc implements ShiftCalcConvention {
    
    /**
     * Returns argument {@code time} shifted according to the respective {@link BusinessDayConvention}
     * 
     * @param time the time to be shifted
     * @return the shifted time
     */
    public LocalDateTime shift(LocalDateTime time, BusinessDayConvention convention) {
        return convention.shift(time);    
    }
}
