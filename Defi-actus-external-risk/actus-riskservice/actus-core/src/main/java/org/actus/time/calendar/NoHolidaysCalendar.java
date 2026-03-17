/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.time.calendar;

import java.time.LocalDateTime;

/**
 * {@link BusinessDayCalendarProvider} with no holidays
 * <p>
 * This {@link BusinessDayCalendarProvider} defines all dates as business
 * days. Thus, {@code isBusinessDay} returns {@code true} for every {@code date}
 * provided as argument.
 */
public class NoHolidaysCalendar implements BusinessDayCalendarProvider {
    
	@Override
	public boolean isBusinessDay(LocalDateTime date) {
		return true;
	}

}

