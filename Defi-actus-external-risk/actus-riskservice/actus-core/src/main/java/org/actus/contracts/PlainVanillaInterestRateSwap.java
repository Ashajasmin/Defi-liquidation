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
import org.actus.functions.stk.POF_PRD_STK;
import org.actus.states.StateSpace;
import org.actus.events.EventFactory;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.types.DeliverySettlement;
import org.actus.util.CommonUtils;
import org.actus.time.ScheduleFactory;
import org.actus.functions.pam.*;
import org.actus.functions.swppv.*;
import org.actus.functions.fxout.*;
import org.actus.types.EventType;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents the Plain Vanilla Interest Rate Swap payoff algorithm
 * 
 * @see <a href="https://www.actusfrf.org"></a>
 */
public final class PlainVanillaInterestRateSwap {

    // compute next n non-contingent events
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<ContractEvent>();

        // purchase
        if (!CommonUtils.isNull(model.getAs("purchaseDate"))) {
            events.add(EventFactory.createEvent(model.getAs("purchaseDate"), EventType.PRD, model.getAs("currency"), new POF_PRD_FXOUT(), new STF_PRD_SWPPV(), model.getAs("contractID")));
        }

        // initial exchange
        events.add(EventFactory.createEvent(model.getAs("initialExchangeDate"), EventType.IED, model.getAs("currency"), new POF_IED_SWPPV(), new STF_IED_SWPPV(), model.getAs("contractID")));
        
        // principal redemption
        events.add(EventFactory.createEvent(model.getAs("maturityDate"), EventType.MD, model.getAs("currency"), new POF_MD_SWPPV(), new STF_MD_SWPPV(), model.getAs("contractID")));
        
        // interest payment events
        if (CommonUtils.isNull(model.getAs("deliverySettlement")) || model.getAs("deliverySettlement").equals(DeliverySettlement.D)) {
            // in case of physical delivery (delivery of individual cash flows)
            // interest payment schedule
            Set<LocalDateTime> interestSchedule = ScheduleFactory.createSchedule(model.getAs("cycleAnchorDateOfInterestPayment"),
                    model.getAs("maturityDate"),
                    model.getAs("cycleOfInterestPayment"),
                    model.getAs("endOfMonthConvention"));
            // fixed rate events
            events.addAll(EventFactory.createEvents(interestSchedule, EventType.IPFX, model.getAs("currency"), new POF_IPFix_SWPPV(), new STF_IPFix_SWPPV(), model.getAs("businessDayConvention"), model.getAs("contractID")));
            // floating rate events
            events.addAll(EventFactory.createEvents(interestSchedule, EventType.IPFL, model.getAs("currency"), new POF_IPFloat_SWPPV(), new STF_IPFloat_SWPPV(), model.getAs("businessDayConvention"), model.getAs("contractID")));
        } else {
            // in case of cash delivery (cash settlement)
            events.addAll(EventFactory.createEvents(ScheduleFactory.createSchedule(model.getAs("cycleAnchorDateOfInterestPayment"),
                    model.getAs("maturityDate"),
                    model.getAs("cycleOfInterestPayment"),
                    model.getAs("endOfMonthConvention")),
                    EventType.IP, model.getAs("currency"), new POF_IP_SWPPV(), new STF_IP_SWPPV(), model.getAs("businessDayConvention"), model.getAs("contractID")));

        }

        // rate reset
        events.addAll(EventFactory.createEvents(ScheduleFactory.createSchedule(model.getAs("cycleAnchorDateOfRateReset"), model.getAs("maturityDate"),
                model.getAs("cycleOfRateReset"), model.getAs("endOfMonthConvention"), false),
                EventType.RR, model.getAs("currency"), new POF_RR_PAM(), new STF_RR_SWPPV(), model.getAs("businessDayConvention"), model.getAs("contractID")));

        // termination
        if (!CommonUtils.isNull(model.getAs("terminationDate"))) {
            ContractEvent termination = EventFactory.createEvent(
                    model.getAs("terminationDate"),
                    EventType.TD,
                    model.getAs("currency"),
                    new POF_TD_FXOUT(),
                    new STF_TD_SWPPV(),
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

        // remove pre-purchase events if purchase date set
        if(!CommonUtils.isNull(model.getAs("purchaseDate"))) {
            events.removeIf(e -> !e.eventType().equals(EventType.AD) && e.compareTo(EventFactory.createEvent(model.getAs("purchaseDate"), EventType.PRD, model.getAs("currency"), null, null, model.getAs("contractID"))) == -1);
        }
        // return evaluated events
        return events;
    }

    // initialize state space per status date
    private static StateSpace initStateSpace(ContractModelProvider model) throws AttributeConversionException {
        StateSpace states = new StateSpace();
        states.notionalScalingMultiplier = 1;
        states.statusDate = model.getAs("statusDate");
        if (!model.<LocalDateTime>getAs("initialExchangeDate").isAfter(model.getAs("statusDate"))) {
            states.notionalPrincipal = ContractRoleConvention.roleSign(model.getAs("contractRole"))*model.<Double>getAs("notionalPrincipal");
            states.nominalInterestRate = model.getAs("nominalInterestRate");
            states.nominalInterestRate2 = model.getAs("nominalInterestRate2");
            states.accruedInterest = ContractRoleConvention.roleSign(model.getAs("contractRole"))*model.<Double>getAs("accruedInterest");
            states.accruedInterest2 = ContractRoleConvention.roleSign(model.getAs("contractRole"))*model.<Double>getAs("accruedInterest2");
            states.lastInterestPeriod = 0.0;
        }
        return states;
    }
}
