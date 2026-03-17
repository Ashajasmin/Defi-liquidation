/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */

package org.actus.contracts;

import org.actus.AttributeConversionException;
import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.events.ContractEvent;
import org.actus.events.EventFactory;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.optns.*;
import org.actus.functions.stk.STF_PRD_STK;
import org.actus.functions.stk.STF_TD_STK;
import org.actus.states.StateSpace;
import org.actus.types.EventType;
import org.actus.util.CommonUtils;
import org.actus.util.CycleUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents the Option contract algorithm
 *
 * @see <a https://www.actusfrf.org"></a>
 */

public final class Option {
    // forward projection of the entire lifecycle of the contract
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<>();
        // purchase
        if (!CommonUtils.isNull(model.getAs("purchaseDate"))) {
            events.add(EventFactory.createEvent(model.getAs("purchaseDate"), EventType.PRD, model.getAs("currency"), new POF_PRD_OPTNS(), new STF_PRD_STK(), model.getAs("contractID")));
        }

        //exercise & settlement
        if(!CommonUtils.isNull(model.getAs("exerciseDate"))){
            events.add(EventFactory.createEvent(model.getAs("exerciseDate"), EventType.XD,model.getAs("currency"), new POF_XD_OPTNS(), new STF_XD_OPTNS(), model.getAs("contractID")));
            events.add(EventFactory.createEvent(model.<BusinessDayAdjuster>getAs("businessDayConvention").shiftEventTime(model.<LocalDateTime>getAs("exerciseDate").plus(CycleUtils.parseTemporalAmount(model.getAs("settlementPeriod")))), EventType.STD, model.getAs("currency"), new POF_STD_OPTNS(), new STF_STD_OPTNS(),model.getAs("contractID")));
        } else{
            events.add(EventFactory.createEvent(model.getAs("maturityDate"), EventType.XD,model.getAs("currency"), new POF_XD_OPTNS(), new STF_XD_OPTNS(), model.getAs("contractID")));
            events.add(EventFactory.createEvent(model.<BusinessDayAdjuster>getAs("businessDayConvention").shiftEventTime(model.<LocalDateTime>getAs("maturityDate").plus(CycleUtils.parseTemporalAmount(model.getAs("settlementPeriod")))), EventType.STD, model.getAs("currency"), new POF_STD_OPTNS(), new STF_STD_OPTNS(),model.getAs("contractID")));
        }
        events.add(EventFactory.createEvent(model.getAs("maturityDate"), EventType.MD, model.getAs("currency"), new POF_MD_OPTNS(), new STF_MD_OPTNS(), model.getAs("contractID")));

        // termination
        if (!CommonUtils.isNull(model.getAs("terminationDate"))) {
            ContractEvent termination = EventFactory.createEvent(
                    model.getAs("terminationDate"),
                    EventType.TD,
                    model.getAs("currency"),
                    new POF_TD_OPTNS(),
                    new STF_TD_STK(),
                    model.getAs("contractID")
            );
            events.removeIf(e -> e.compareTo(termination) == 1); // remove all post-termination events
            events.add(termination);
        }

        // remove all pre-status date events
        events.removeIf(e -> e.compareTo(EventFactory.createEvent(model.getAs("statusDate"), EventType.AD, model.getAs("currency"), null,null, model.getAs("contractID"))) == -1);

        return events;
    }

    // apply a set of events to the current state of a contract and return the post events state
    public static ArrayList<ContractEvent> apply(ArrayList<ContractEvent> events,
                                                 ContractModelProvider model,
                                                 RiskFactorModelProvider observer) throws AttributeConversionException {
        //Add external XD-event
        events.addAll(observer.events(model));

        // initialize state space per status date
        StateSpace states = initStateSpace(model);

        // sort the events according to their time sequence
        Collections.sort(events);

        // apply events according to their time sequence to current state
        events.forEach(e -> e.eval(states, model, observer, model.getAs("dayCountConvention"), model.getAs("businessDayConvention")));

        // remove pre-purchase events if purchase date set
        if(!CommonUtils.isNull(model.getAs("purchaseDate"))) {
            events.removeIf(e -> !e.eventType().equals(EventType.AD) && e.compareTo(EventFactory.createEvent(model.getAs("purchaseDate"), EventType.PRD, model.getAs("currency"), null, null, model.getAs("contractID"))) == -1);
        }

        // return post events states
        return events;
    }

    private static StateSpace initStateSpace(ContractModelProvider model) throws AttributeConversionException {
        StateSpace states = new StateSpace();

        // initialize state variables
        states.statusDate = model.getAs("statusDate");
        states.exerciseAmount = model.getAs("exerciseAmount");
        states.exerciseDate = model.getAs("exerciseDate");
        states.contractPerformance = model.getAs("contractPerformance");

        // return the initialized state space
        return states;
    }
}

