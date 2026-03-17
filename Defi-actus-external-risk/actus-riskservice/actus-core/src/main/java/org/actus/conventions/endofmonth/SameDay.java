/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.conventions.endofmonth;

import java.time.LocalDateTime;

/**
 * Same Day end of month convention
 * <p>
 * This end of month convention assumes that the day of month used in the
 * schedule start date is kept throughout the schedule. Hence, if {@code d} 
 * is the schedule start date and {@code d'} the shifted date, we have that
 * <ul>
 * <li> {@code d' = d} always</li>
 * </ul>
 * <p>
 * C.f. documentation for {@link EndOfMonthConvention} for more information.
 */
public class SameDay implements EndOfMonthConvention {
    
    @Override
	public LocalDateTime shift(LocalDateTime date) {
	    return date;
	}
}
