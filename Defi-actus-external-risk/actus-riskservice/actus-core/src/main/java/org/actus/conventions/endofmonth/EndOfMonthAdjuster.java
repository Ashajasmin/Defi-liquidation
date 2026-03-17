/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.conventions.endofmonth;

import org.actus.AttributeConversionException;
import org.actus.types.EndOfMonthConventionEnum;
import org.actus.util.CommonUtils;
import org.actus.util.CycleUtils;
import org.actus.conventions.endofmonth.EndOfMonthConvention;
import org.actus.conventions.endofmonth.SameDay;
import org.actus.conventions.endofmonth.EndOfMonth;

import java.time.LocalDateTime;
import java.time.Period;

import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

/**
 * Component for the adjustment of dates according to a {@link EndOfMonthConvention}
 */
public final class EndOfMonthAdjuster {
    private EndOfMonthConvention convention;

    /**
     * Constructor
     * 
     * {@link EndOfMonthConvention}-adjustments are made when creating a schedule of {@link ContractEvent}-dates
     * using the {@link ScheduleFactory}. Whether or not schedule dates are shifted to the end of month is 
     * determined by
     * <ul>
     *      <li> The convention: "SD" = same day and means not adjusting, "EM" = end of month and means adjusting</li>
     *      <li> The start date of the schedule: dates are only shifted if the schedule start date is an end-of-month date</li>
     *      <li> the period of the schedule cycle: dates are only shifted if the schedule cycle is based on an "M" period unit or multiple thereof</li>
     * </ul>
     * 
     * @param convention indicates the {@link EndOfMonthConvention} to be applied
     * @param refDate the schedule start date
     * @param cycle the cycle used in the schedule
     * @throws AttributeConversionException if {@code convention} does not conform with the ACTUS Data Dictionary
     * @return 
     */
    public EndOfMonthAdjuster(EndOfMonthConventionEnum convention, LocalDateTime refDate, String cycle) throws AttributeConversionException {
        if(CommonUtils.isNull(convention)){
            throw new AttributeConversionException();
        }
        switch (convention) {
            case EOM:
                // note, internally, units which are a multiple of "1M" are converted to "XM" why here we only have to check
                // for period-unit M when deciding whether or not to shift a date
                if (refDate.equals(refDate.with(lastDayOfMonth())) && CycleUtils.parsePeriod(cycle).getMonths() > 0) {
                    this.convention = new EndOfMonth();
                } else {
                    this.convention = new SameDay();
                }
                break;
            case SD:
                this.convention = new SameDay();
                break;
            default:
                throw new AttributeConversionException();
        }
    }

    /**
     * Shift a date to the end of the month according to a specific convention
     * 
     * @param date the date to be shifted
     * @return the shifted date
     */
    public LocalDateTime shift(LocalDateTime date) {
        return convention.shift(date);
    }
}
