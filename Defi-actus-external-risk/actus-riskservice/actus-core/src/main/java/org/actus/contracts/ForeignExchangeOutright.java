/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.contracts;

import org.actus.AttributeConversionException;
import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.events.ContractEvent;
import org.actus.functions.fxout.*;
import org.actus.functions.stk.*;
import org.actus.states.StateSpace;
import org.actus.events.EventFactory;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.types.DeliverySettlement;
import org.actus.types.EventType;
import org.actus.util.CommonUtils;
import org.actus.util.CycleUtils;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;

/**
 * Represents the Foreign Exchange Outright payoff algorithm
 * 
 * @see <a href="https://www.actusfrf.org"></a>
 */
public final class ForeignExchangeOutright {

    // compute next n non-contingent events
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<ContractEvent>();
        
        // purchase
        if (!CommonUtils.isNull(model.getAs("purchaseDate"))) {
            events.add(EventFactory.createEvent(model.getAs("purchaseDate"), EventType.PRD, model.getAs("currency"), new POF_PRD_FXOUT(), new STF_PRD_STK(), model.getAs("contractID")));
        }

        // termination
        if (!CommonUtils.isNull(model.getAs("terminationDate"))) {
            events.add(EventFactory.createEvent(model.getAs("terminationDate"), EventType.TD, model.getAs("currency"), new POF_TD_FXOUT(), new STF_TD_STK(), model.getAs("contractID")));
        } else {
            // settlement
            if (CommonUtils.isNull(model.getAs("deliverySettlement")) || model.getAs("deliverySettlement").equals(DeliverySettlement.D)) {
                events.add(EventFactory.createEvent(model.getAs("maturityDate"), EventType.MD, model.getAs("currency"), new POF_MD1_FXOUT(), new STF_MD1_FXOUT(), model.getAs("businessDayConvention"), model.getAs("contractID")));
                events.add(EventFactory.createEvent(model.getAs("maturityDate"), EventType.MD, model.getAs("currency2"), new POF_MD2_FXOUT(), new STF_MD2_FXOUT(), model.getAs("businessDayConvention"), model.getAs("contractID")));
            } else {
                events.add(EventFactory.createEvent(model.<BusinessDayAdjuster>getAs("businessDayConvention").shiftEventTime(model.<LocalDateTime>getAs("maturityDate").plus(CycleUtils.parseTemporalAmount(model.getAs("settlementPeriod")))), EventType.STD, model.getAs("currency"), new POF_STD_FXOUT(), new STF_STD_FXOUT(), model.getAs("businessDayConvention"), model.getAs("contractID")));
            }
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
        events.forEach(e -> e.eval(states, model, observer, new DayCountCalculator("AA", model.getAs("calendar")), model.getAs("businessDayConvention")));

        // return evaluated events
        return events;
    }

    // initialize state space per status date
    private static StateSpace initStateSpace(ContractModelProvider model) {
        StateSpace states = new StateSpace();
        states.statusDate = model.getAs("statusDate");
        return states;
    }
}
