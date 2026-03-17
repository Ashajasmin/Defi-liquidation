/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 *
 * MODIFIED: Support sub-day cycles via TemporalAmount (Feb 2026)
 * Uses CycleUtils.parseTemporalAmount() instead of CycleUtils.parsePeriod()
 * Duration-based cycles bypass EndOfMonthAdjuster (irrelevant for sub-day)
 */
package org.actus.time;

import org.actus.AttributeConversionException;
import org.actus.types.EndOfMonthConventionEnum;
import org.actus.util.CommonUtils;
import org.actus.util.StringUtils;
import org.actus.util.CycleUtils;
import org.actus.conventions.endofmonth.EndOfMonthAdjuster;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Set;
import java.util.HashSet;

public final class ScheduleFactory {
    
	private ScheduleFactory() {
	}

	public static Set<LocalDateTime> createSchedule(LocalDateTime startTime, LocalDateTime endTime, String cycle, EndOfMonthConventionEnum endOfMonthConvention) throws AttributeConversionException {
		return ScheduleFactory.createSchedule(startTime,endTime,cycle,endOfMonthConvention,true);
	}

	public static Set<LocalDateTime> createSchedule(LocalDateTime startTime, LocalDateTime endTime, String cycle, EndOfMonthConventionEnum endOfMonthConvention, boolean addEndTime) throws AttributeConversionException {
		EndOfMonthAdjuster shifter;
		Set<LocalDateTime> timesSet = new HashSet<LocalDateTime>();
        char stub;

		// if no cycle then only start (if specified) and end dates
		if (CommonUtils.isNull(cycle)) {
		    if (!CommonUtils.isNull(startTime)) {
		      timesSet.add(startTime);
		    }
			if(addEndTime) {
				timesSet.add(endTime);
			} else {
				if(endTime.equals(startTime)) timesSet.remove(startTime);
			}
			return timesSet;
		}

        // parse stub
        stub = CycleUtils.parseStub(cycle);
        
        // ========================== MODIFIED SECTION ==========================
        // Use parseTemporalAmount() to handle both Period (day+) and Duration (sub-day)
        TemporalAmount period = CycleUtils.parseTemporalAmount(cycle);
        boolean isSubDay = (period instanceof Duration);

        // parse end of month convention (not relevant for sub-day but needed for Period path)
        shifter = new EndOfMonthAdjuster(endOfMonthConvention, startTime, cycle);

		LocalDateTime newTime = LocalDateTime.from(startTime);
		int counter = 1;
		
		if (isSubDay) {
		    // SUB-DAY PATH: Use Duration arithmetic, bypass EndOfMonthAdjuster
		    Duration dur = (Duration) period;
		    while (newTime.isBefore(endTime)) {
		        timesSet.add(newTime);
		        newTime = startTime.plus(dur.multipliedBy(counter));
		        counter++;
		    }
		} else {
		    // DAY+ PATH: Existing Period behavior preserved exactly
		    Period per = (Period) period;
		    Period increment;
		    while (newTime.isBefore(endTime)) {
		        timesSet.add(newTime);
		        increment = per.multipliedBy(counter);
		        newTime = shifter.shift(startTime.plus(increment));
		        counter++;
		    }
		}
        // ========================== END MODIFIED SECTION ==========================

		// add (or not) additional time at endTime
		if(addEndTime) {
			timesSet.add(endTime);
		} else {
			if(endTime.equals(startTime)) timesSet.remove(startTime);
		}

        // adjust for the last stub
		if (stub == StringUtils.LongStub && timesSet.size() > 2 && !endTime.equals(newTime)) {
		    if (isSubDay) {
		        // Sub-day: remove second-to-last event
		        Duration dur = (Duration) period;
		        timesSet.remove(startTime.plus(dur.multipliedBy(counter - 2)));
		    } else {
		        // Day+: existing behavior
		        Period per = (Period) period;
		        timesSet.remove(shifter.shift(startTime.plus(per.multipliedBy((counter - 2)))));
		    }
		}
		
		return timesSet;
	}

	public static Set<LocalDateTime> createArraySchedule(LocalDateTime[] startTimes,
			LocalDateTime endTime, String[] cycles, EndOfMonthConventionEnum endOfMonthConvention) {
        Set<LocalDateTime> timesSet = new HashSet<LocalDateTime>();
        
		for (int i = 0; i < startTimes.length - 1; i++) {
		    timesSet.addAll(
					createSchedule(startTimes[i], startTimes[i + 1], (cycles==null)? null : cycles[i], endOfMonthConvention));
		}
		
		timesSet.addAll(
				createSchedule(startTimes[startTimes.length - 1], endTime,
						(cycles==null)? null : cycles[startTimes.length - 1], endOfMonthConvention));
		
		return timesSet;
	}
}
