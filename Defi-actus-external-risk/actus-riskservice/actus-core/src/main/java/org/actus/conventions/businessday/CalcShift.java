/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.conventions.businessday;

import java.time.LocalDateTime;

/**
 * Component that represents the Calculate-first-Shift-Second convention
 * <p>
 * This convention assumes that when shifting the {@link ContractEvent}-time according
 * to a {@link BusinessDayConvention}, calculations are performed first and the 
 * event-time is shifted thereafter. Hence, calculations in {@link PayOffFunction} and
 * {@link StateTransitionFunction} are based on the unshifted time as well.
 */
public class CalcShift implements ShiftCalcConvention {
    
    /**
     * Returns argument {@code time} unshifted
     * 
     * @param time the time to be shifted
     * @return the shifted time (actually unshifted)
     */
    public LocalDateTime shift(LocalDateTime time, BusinessDayConvention convention) {
        return time;    
    }
}
