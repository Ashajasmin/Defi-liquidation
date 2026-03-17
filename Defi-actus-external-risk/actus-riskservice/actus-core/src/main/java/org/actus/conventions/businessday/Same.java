/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.conventions.businessday;

import java.time.LocalDateTime;

/**
 * Implementation of the Same business day convention
 * <p>
 * This business day convention assumes that schedule dates are not shifted 
 * if they fall on a non-business day. Hence, if {@code d} is the 
 * initial date and {@code d'} the shifted date we have that 
 * <ul>
 * <li> {@code d' = d} always</li>
 * </ul>
 */
public class Same implements BusinessDayConvention {
 /**
     * Constructor
     * 
     * @param calendar the {@link BusinessDayCalendarProvider} to be used
     */
	public Same() {
		super();
	}

	/**
	 * Returns the non-shifted time (even if a non-business day)
	 * 
	 * @param time the time to be shifted
	 * @return the shifted time (can be a non-business day)
	 */
	@Override
	public LocalDateTime shift(LocalDateTime date) {
		return date;
	}
    
}
