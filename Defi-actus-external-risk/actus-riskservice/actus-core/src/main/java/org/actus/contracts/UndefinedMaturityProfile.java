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
import org.actus.functions.pam.*;
import org.actus.states.StateSpace;
import org.actus.events.EventFactory;
import org.actus.time.ScheduleFactory;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.types.EventType;
import org.actus.util.CommonUtils;
import org.actus.functions.clm.POF_IED_CLM;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents the Undefined Maturity Profile payoff algorithm
 *
 * @see <a href="https://www.actusfrf.org"></a>
 */
public final class UndefinedMaturityProfile {

    // forward projection of the entire lifecycle of the contract
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<ContractEvent>();

        // initial exchange
        events.add(EventFactory.createEvent(
                model.getAs("initialExchangeDate"),
                EventType.IED,
                model.getAs("currency"),
                new POF_IED_CLM(),
                new STF_IED_PAM(),
                model.getAs("contractID"))
        );
        // interest payment capitalization
        events.addAll(EventFactory.createEvents(
                ScheduleFactory.createSchedule(
                        model.getAs("cycleAnchorDateOfInterestPayment"),
                        to,
                        model.getAs("cycleOfInterestPayment"),
                        model.getAs("endOfMonthConvention"),
                        false
                ),
                EventType.IPCI,
                model.getAs("currency"),
                new POF_IPCI_PAM(),
                new STF_IPCI_PAM(),
                model.getAs("businessDayConvention"),
                model.getAs("contractID"))
        );
        // rate reset
        Set<ContractEvent> rateResetEvents = EventFactory.createEvents(
                ScheduleFactory.createSchedule(
                        model.<LocalDateTime>getAs("cycleAnchorDateOfRateReset"),
                        to,
                        model.getAs("cycleOfRateReset"),
                        model.getAs("endOfMonthConvention"),
                        false
                ),
                EventType.RR,
                model.getAs("currency"),
                new POF_RR_PAM(),
                new STF_RR_PAM(),
                model.getAs("businessDayConvention"),
                model.getAs("contractID")
        );

        // adapt fixed rate reset event
        if(!CommonUtils.isNull(model.getAs("nextResetRate"))) {
            ContractEvent fixedEvent = rateResetEvents.stream().sorted().filter(e -> e.compareTo(EventFactory.createEvent(model.getAs("statusDate"), EventType.AD, model.getAs("currency"), null, null, model.getAs("contractID"))) == 1).findFirst().get();
            fixedEvent.fStateTrans(new STF_RRF_PAM());
            fixedEvent.eventType(EventType.RRF);
            rateResetEvents.add(fixedEvent);
        }
        events.addAll(rateResetEvents);

        // fees (if specified)
        if (!CommonUtils.isNull(model.getAs("cycleOfFee"))) {
            events.addAll(EventFactory.createEvents(
                    ScheduleFactory.createSchedule(
                            model.getAs("cycleAnchorDateOfFee"),
                            to,
                            model.getAs("cycleOfFee"),
                            model.getAs("endOfMonthConvention"),
                            false
                    ),
                    EventType.FP,
                    model.getAs("currency"),
                    new POF_FP_PAM(),
                    new STF_FP_PAM(),
                    model.getAs("businessDayConvention"),
                    model.getAs("contractID"))
            );
        }

        // termination
        if (!CommonUtils.isNull(model.getAs("terminationDate"))) {
            ContractEvent termination =
                    EventFactory.createEvent(
                            model.getAs("terminationDate"),
                            EventType.TD, model.getAs("currency"),
                            new POF_TD_PAM(),
                            new STF_TD_PAM(),
                            model.getAs("contractID")
                    );
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
        events.forEach(e -> e.eval(states, model, observer, model.getAs("dayCountConvention"), model.getAs("businessDayConvention")));

        // return evaluated events
        return events;
    }

    private static StateSpace initStateSpace(ContractModelProvider model) throws AttributeConversionException {
        StateSpace states = new StateSpace();
        states.notionalScalingMultiplier = 1;
        states.interestScalingMultiplier = 1;
        
        states.statusDate = model.getAs("statusDate");
        if (!model.<LocalDateTime>getAs("initialExchangeDate").isAfter(model.getAs("statusDate"))) {
            states.notionalPrincipal = ContractRoleConvention.roleSign(model.getAs("contractRole"))*model.<Double>getAs("notionalPrincipal");
            states.nominalInterestRate = model.getAs("nominalInterestRate");
            states.accruedInterest = ContractRoleConvention.roleSign(model.getAs("contractRole"))*model.<Double>getAs("accruedInterest");
            states.feeAccrued = model.getAs("feeAccrued");
        }

        // return the initialized state space
        return states;
    }

}
