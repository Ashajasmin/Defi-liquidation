/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */

package org.actus.conventions.businessday;
import org.actus.time.calendar.BusinessDayCalendarProvider;

import java.time.LocalDateTime;

/**
 * Implementation of the Modified Preceeding business day convention
 * <p>
 * This business day convention assumes that if a date falls on a non-business day
 * it is shifted to the next preceeding business day, if this is in the same month, 
 * or to the next following business day otherwise. Hence, if {@code d} is the 
 * initial date and {@code d'} is the shifted date we have that 
 * <ul>
 * <li> {@code d' = d} if {@code d} is a business day</li>
 * <li> {@code d' > d} if {@code d} is a non-business day and the next following business day is in the same month than {@code d} </li>
 * <li> {@code d' < d} if {@code d} is a non-business day and the next following business day is not in the same month than {@code d} </li>
 * </ul>
 */
public class ModifiedPreceeding implements BusinessDayConvention {
    private BusinessDayCalendarProvider calendar;

    /**
     * Constructor
     * 
     * @param calendar the {@link BusinessDayCalendarProvider} to be used 
     */
	public ModifiedPreceeding(BusinessDayCalendarProvider calendar) {
		this.calendar = calendar;
	}

	/**
	 * Shift the input date to the closest business day if it is a non-business day
	 * 
	 * @param date the date to be shifted
	 * @return the shifted date (a business day)
	 */
	@Override
	public LocalDateTime shift(LocalDateTime date) {
		LocalDateTime shiftedDate = LocalDateTime.from(date);
		while (!calendar.isBusinessDay(shiftedDate)) {
			shiftedDate = shiftedDate.minusDays(1);
		}
		if (!shiftedDate.getMonth().equals(date.getMonth())) {
			shiftedDate = LocalDateTime.from(date);
			while (!calendar.isBusinessDay(shiftedDate)) {
				shiftedDate = shiftedDate.plusDays(1);
			}
		}
		return shiftedDate;
	}

}

