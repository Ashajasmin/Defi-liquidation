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
import org.actus.time.ScheduleFactory;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.types.EventType;
import org.actus.util.CommonUtils;
import org.actus.util.Constants;
import org.actus.functions.stk.POF_PRD_STK;
import org.actus.functions.stk.STF_PRD_STK;
import org.actus.functions.stk.POF_TD_STK;
import org.actus.functions.stk.STF_TD_STK;
import org.actus.functions.stk.POF_DV_STK;
import org.actus.functions.stk.STF_DV_STK;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents the Stock payoff algorithm
 * 
 * @see <a href="https://www.actusfrf.org"></a>
 */
public final class Stock {

    // compute next n non-contingent events
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<ContractEvent>();

        // purchase
        if (!CommonUtils.isNull(model.getAs("purchaseDate"))) {
            events.add(EventFactory.createEvent(model.getAs("purchaseDate"), EventType.PRD, model.getAs("currency"), new POF_PRD_STK(), new STF_PRD_STK(), model.getAs("contractID")));
        }
        // dividend payment related
        if (!CommonUtils.isNull(model.getAs("cycleOfDividendPayment"))) {
            if(CommonUtils.isNull(model.getAs("terminationDate"))) {
                events.addAll(EventFactory.createEvents(
                        ScheduleFactory.createSchedule(
                                model.getAs("cycleAnchorDateOfDividendPayment"),
                                model.<LocalDateTime>getAs("cycleAnchorDateOfDividendPayment").plus(Constants.MAX_LIFETIME_STK),
                                model.getAs("cycleOfDividendPayment"),
                                model.getAs("endOfMonthConvention")
                        ),
                        EventType.DV,
                        model.getAs("currency"),
                        new POF_DV_STK(),
                        new STF_DV_STK(),
                        model.getAs("businessDayConvention"),
                        model.getAs("contractID"))
                );
            } else {
                events.addAll(EventFactory.createEvents(ScheduleFactory.createSchedule(model.getAs("cycleAnchorDateOfDividendPayment"),
                        model.getAs("terminationDate"),
                        model.getAs("cycleOfDividendPayment"),
                        model.getAs("endOfMonthConvention")),
                        EventType.DV, model.getAs("currency"), new POF_DV_STK(), new STF_DV_STK(), model.getAs("businessDayConvention"), model.getAs("contractID")));

            }
        } else if(!CommonUtils.isNull(model.getAs("cycleAnchorDateOfDividendPayment"))) { // Add a fallback DV event on the anchor date if no cycleOfDividendPayment exists.
        	events.add(EventFactory.createEvent(model.getAs("cycleAnchorDateOfDividendPayment"), EventType.DV, model.getAs("currency"), new POF_DV_STK(), new STF_DV_STK(), model.getAs("businessDayConvention"), model.getAs("contractID")));
        	
        }
        // termination
        if (!CommonUtils.isNull(model.getAs("terminationDate"))) {
            ContractEvent termination =
                    EventFactory.createEvent(model.getAs("terminationDate"), EventType.TD, model.getAs("currency"), new POF_TD_STK(), new STF_TD_STK(), model.getAs("contractID"));
            events.removeIf(e -> e.compareTo(termination) == 1); // remove all post-termination events
            events.add(termination);
        }

        // remove all pre-status date events
        events.removeIf(e -> e.compareTo(EventFactory.createEvent(model.getAs("statusDate"), EventType.AD, model.getAs("currency"), null,null, model.getAs("contractID"))) == -1);
        
        // remove all post to-date events
        events.removeIf(e -> e.compareTo(EventFactory.createEvent(to, EventType.AD, model.getAs("currency"), null,null, model.getAs("contractID"))) == 1);

        // sort the events in the payoff-list according to their time of occurence
        Collections.sort(events);

        return events;
    }

    // apply a set of events to the current state of a contract and return the post events state
    public static ArrayList<ContractEvent> apply(ArrayList<ContractEvent> events,
                                                 ContractModelProvider model,
                                                 RiskFactorModelProvider observer) throws AttributeConversionException {

        // initialize state space per status date
        StateSpace states = initStateSpace(model);

        // sort the events according to their time sequence
        Collections.sort(events);

        // apply events according to their time sequence to current state
        events.forEach(e -> e.eval(states, model, observer, new DayCountCalculator("30E360", null), model.getAs("businessDayConvention")));

        // return post events states
        return events;
    }

    private static StateSpace initStateSpace(ContractModelProvider model) throws AttributeConversionException {
        StateSpace states = new StateSpace();

        // initialize state variables
        states.statusDate = model.getAs("statusDate");
        
        // return the initialized state space
        return states;
    }

}
