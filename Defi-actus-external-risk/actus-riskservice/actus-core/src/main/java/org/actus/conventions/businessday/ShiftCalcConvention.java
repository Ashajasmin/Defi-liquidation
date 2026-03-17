/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.conventions.businessday;

import java.time.LocalDateTime;

/**
 * Component that represents a certain shift-calc convention
 * <p>
 * When shifting a {@link ContractEvent}-time according to a {@link BusinessDayConvention}
 * different conventions exist that define whether calculations in {@link PayOffFunction} 
 * and {@link StateTransitionFunction} are based on the initial (unshifted) time or on the
 * shifted time. E.g. if an Interest-Payment schedule-time falls on a non-business day it
 * can be shifted or not. Further, the calculation of accrued interest can be based on the
 * unshifted time or the shifted as well. This component represents a specific convention
 * and provides respective shift method applied when determining the time used for
 * {@link ContractEvent}-calculations.
 * <p>
 * Make sure, new conventions implement this interface when adding to the library.
 */
public interface ShiftCalcConvention {
    
    /**
     * Shifts argument {@code time} according to a specific shift-calc convention and based on a {@link BusinessDayConvention}
     * 
     * @param time the time to be shifted
     * @return the shifted time
     */
    public LocalDateTime shift(LocalDateTime time, BusinessDayConvention convention);
}
