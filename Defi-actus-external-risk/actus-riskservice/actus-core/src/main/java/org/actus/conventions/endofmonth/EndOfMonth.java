/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.conventions.endofmonth;

import java.time.LocalDateTime;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

/**
 * End-of-Month end of month convention
 * <p>
 * This end of month convention assumes that if the day of month used in the schedule
 * start date is the last day in the respective month, and if the schedule period is 
 * monthly, then all dates in the schedule should fall on the last day in the respective 
 * month. Hence, if {@code d} is an unshifted date in the schedule and {@code d'} a shifted 
 * date, we have for all {@code d} and {@code d'} in the schedule that
 * <ul>
 * <li> {@code d' = d} if {@code d} is the last day in the respective month</li>
 * <li> {@code d' > d} if {@code d} otherwise</li>
 * </ul>
 * <p>
 * C.f. documentation for {@link EndOfMonthConvention} for more information.
 */
public class EndOfMonth implements EndOfMonthConvention {
    
    @Override
	public LocalDateTime shift(LocalDateTime date) {
	    return date.with(lastDayOfMonth());
	}
}
