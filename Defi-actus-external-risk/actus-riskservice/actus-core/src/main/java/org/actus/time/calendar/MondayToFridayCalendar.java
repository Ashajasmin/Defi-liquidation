/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.time.calendar;

import java.time.LocalDateTime;

/**
 * {@link BusinessDayCalendarProvider} with Monday through Friday being business days
 * <p>
 * This {@link BusinessDayCalendarProvider} defines all dates as business
 * days which are weekdays from monday through friday. Thus, {@code isBusinessDay} returns 
 * {@code false} only if argument {@code date} is either a saturday or sunday.
 */
public class MondayToFridayCalendar implements BusinessDayCalendarProvider {

	@Override
	public boolean isBusinessDay(LocalDateTime date) {
		if (date.getDayOfWeek().getValue() == 6 || date.getDayOfWeek().getValue() == 7) {
			return false;
		} else {
			return true;
		}
	}
}

