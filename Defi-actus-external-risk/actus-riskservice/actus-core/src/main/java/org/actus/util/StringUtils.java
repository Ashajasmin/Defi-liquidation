/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 *
 * MODIFIED: Added AA365S, AA365N day count convention constants (Feb 2026)
 */
package org.actus.util;

public final class StringUtils {

    // cycle stubs
    public final static char LongStub = '0';
    public final static char ShortStub = '1';
  
    // day count conventions (existing)
    public final static String DayCountConvention_AAISDA = "AA";
    public final static String DayCountConvention_A360 = "A360";
    public final static String DayCountConvention_A365 = "A365";
    public final static String DayCountConvention_B252 = "B252";
    public final static String DayCountConvention_30E360 = "30E360";
    public final static String DayCountConvention_30E360ISDA = "30E360ISDA";
    public final static String DayCountConvention_A336 = "A336";
    public final static String DayCountConvention_28336 = "28336";
    
    // day count conventions (NEW — sub-day precision)
    public final static String DayCountConvention_AA365S = "AA365S";   // Second-precision A/365
    public final static String DayCountConvention_AA365N = "AA365N";   // Nanosecond-precision A/365
    
    // calc/shift conventions
    public final static String CalcShiftConvention_CS = "CS";
    public final static String CalcShiftConvention_SC = "SC";

}
