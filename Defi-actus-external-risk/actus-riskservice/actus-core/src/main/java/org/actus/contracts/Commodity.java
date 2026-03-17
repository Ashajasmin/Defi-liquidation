/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.contracts;

import org.actus.AttributeConversionException;
import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.events.ContractEvent;
import org.actus.states.StateSpace;
import org.actus.events.EventFactory;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.types.EventType;
import org.actus.util.CommonUtils;
import org.actus.functions.stk.POF_PRD_STK;
import org.actus.functions.stk.STF_PRD_STK;
import org.actus.functions.stk.POF_TD_STK;
import org.actus.functions.stk.STF_TD_STK;


import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents the Commodity payoff algorithm
 * 
 * @see <a https://www.actusfrf.org"></a>
 */
public final class Commodity {

    // compute contract schedule
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<ContractEvent>();

        LocalDateTime statusDate = model.getAs("statusDate");
        LocalDateTime purchaseDate = model.getAs("purchaseDate");
        LocalDateTime terminationDate = model.getAs("terminationDate");
        
        // purchase
        if (!CommonUtils.isNull(purchaseDate) && purchaseDate.isAfter(statusDate) && to.isAfter(purchaseDate)) {
            events.add(EventFactory.createEvent(model.getAs("purchaseDate"), EventType.PRD, model.getAs("currency"), new POF_PRD_STK(), new STF_PRD_STK(), model.getAs("contractID")));
        }
        // termination
        if (!CommonUtils.isNull(terminationDate) && terminationDate.isAfter(statusDate) && to.isAfter(terminationDate)) {
            events.add(EventFactory.createEvent(model.getAs("terminationDate"), EventType.TD, model.getAs("currency"), new POF_TD_STK(), new STF_TD_STK(), model.getAs("contractID")));
        }
        return events;
    }

    // apply a set of events to the current state of a contract and return the post events state
    public static ArrayList<ContractEvent> apply(ArrayList<ContractEvent> events,
                                                 ContractModelProvider model,
                                                 RiskFactorModelProvider observer) throws AttributeConversionException {

        // initialize state space per status date
        StateSpace states = new StateSpace();
        states.statusDate = model.getAs("statusDate");

        // sort the events according to their time sequence
        Collections.sort(events);

        // apply events according to their time sequence to current state
        events.forEach(e -> e.eval(states, model, observer, new DayCountCalculator("AA", null), new BusinessDayAdjuster(null, null)));

        // return evaluated events
        return events;
    }
}