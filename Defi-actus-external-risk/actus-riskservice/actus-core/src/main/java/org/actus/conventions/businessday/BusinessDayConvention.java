/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.conventions.businessday;

import java.time.LocalDateTime;

/**
 * Component that represents a certain business day convention
 * <p>
 * Various conventions of how to handle non-business days when generating schedules of
 * {@link ContractEvent}s exist. E.g. if a {@link ContractEvent} falls on a non-business
 * day it can be shifted or not, if shifted it is possible that it is shifted to the next
 * or previous valid business days, etc. This component represents a specific convention
 * and provides respective shift method.
 * <p>
 * Make sure, new conventions implement this interface when adding to the library.
 */
public interface BusinessDayConvention {

    /**
     * Shift a date to the closest business day according to a specific convention
     * 
     * @param date the date to be shifted
     * @return the shifted date
     */
	public LocalDateTime shift(LocalDateTime date);

}
