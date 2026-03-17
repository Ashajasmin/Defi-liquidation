/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */

package org.actus.time;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjuster;

/**
 * A temporal adjuster that provides time rounding
 * <p>
 *
 *
 */
public class TimeAdjuster {

    /**
     * Full hour adjuster
     * <p>
     * This adjuster rounds timestamps to the full hour. Timestamps with temporal units minutes + seconds
     * up to but excluding 30 minutes are floored to the previous full hour while 30 minutes up to 60 minutes
     * are ceiled to the next full hour. temporal units of 0 minutes and 60 minutes remain at the exact
     * same hour.
     */
    public static LocalDateTime toFullHours(LocalDateTime time) {
        return time.withNano(0).withSecond(0).withMinute(0).plusHours(Math.floorDiv(time.getMinute(),30));
    }


}
