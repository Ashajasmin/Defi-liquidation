/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */

package org.actus.time;

import java.time.LocalDateTime;

/**
 * A temporal adjuster that supports cycle adjustments
 * <p>
 *
 *
 */
public abstract interface CycleAdjusterProvider {

    /**
     * Cycle adjuster
     * <p>
     * This adjuster adds a full cycle to a given time depending on the cycle definition.
     */
    public LocalDateTime plusCycle(LocalDateTime time);

    /**
     * Cycle adjuster
     * <p>
     * This adjuster deducts a full cycle to a given time depending on the cycle definition.
     */
    public LocalDateTime minusCycle(LocalDateTime time);

}
