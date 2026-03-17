package org.actus.time.calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public class MondayToFridayWithHolidaysCalendar implements BusinessDayCalendarProvider {
	private final Set<LocalDate> holidays;

	public MondayToFridayWithHolidaysCalendar(Set<LocalDate> holidays) {
		this.holidays = holidays;
	}

	@Override
	public boolean isBusinessDay(LocalDateTime dateTime) {
		LocalDate date = dateTime.toLocalDate();
		int dayOfWeek = date.getDayOfWeek().getValue(); // 1 = Monday, ..., 7 = Sunday
		return dayOfWeek < 6 && !holidays.contains(date);
	}

}
