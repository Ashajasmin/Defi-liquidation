/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.contracts;

import org.actus.AttributeConversionException;
import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.events.ContractEvent;
import org.actus.states.StateSpace;
import org.actus.events.EventFactory;
import org.actus.time.ScheduleFactory;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.types.EventType;
import org.actus.util.CommonUtils;
import org.actus.util.PurchaseEventUtils;
import org.actus.util.TerminationEventUtils;
import org.actus.functions.pam.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the Principal At Maturity payoff algorithm
 * 
 * @see <a href="https://www.actusfrf.org"></a>
 */
public final class PrincipalAtMaturity {

    // compute next events within period
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<ContractEvent>();

        // initial exchange
        events.add(EventFactory.createEvent(model.getAs("initialExchangeDate"), EventType.IED, model.getAs("currency"), new POF_IED_PAM(), new STF_IED_PAM(), model.getAs("contractID")));
        // principal redemption
        events.add(EventFactory.createEvent(model.getAs("maturityDate"), EventType.MD, model.getAs("currency"), new POF_MD_PAM(), new STF_MD_PAM(), model.getAs("contractID")));
        // purchase
        if (!CommonUtils.isNull(model.getAs("purchaseDate"))) {
        	// Insert IP at purchaseDate for clean-price PRD
        	ContractEvent ipAtPurchaseDate = PurchaseEventUtils.createAccruedInterestEventAtPurchasePAM(model);
            events.add(ipAtPurchaseDate);
            
            events.add(EventFactory.createEvent(model.getAs("purchaseDate"), EventType.PRD, model.getAs("currency"), new POF_PRD_PAM(), new STF_PRD_PAM(), model.getAs("contractID")));
        }
        // interest payment related
        if (!CommonUtils.isNull(model.getAs("nominalInterestRate")) && (!CommonUtils.isNull(model.getAs("cycleOfInterestPayment")) || !CommonUtils.isNull(model.getAs("cycleAnchorDateOfInterestPayment")))) {
            // raw interest payment events
            Set<ContractEvent> interestEvents = EventFactory.createEvents(
                    ScheduleFactory.createSchedule(
                            model.getAs("cycleAnchorDateOfInterestPayment"),
                            model.getAs("maturityDate"),
                            model.getAs("cycleOfInterestPayment"),
                            model.getAs("endOfMonthConvention"),
                            true
                    ),
                    EventType.IP,
                    model.getAs("currency"),
                    new POF_IP_PAM(),
                    new STF_IP_PAM(),
                    model.getAs("businessDayConvention"),
                    model.getAs("contractID")
            );
            // adapt if interest capitalization set
            if (!CommonUtils.isNull(model.getAs("capitalizationEndDate"))) {
                // remove IP and add capitalization event at IPCED instead
                ContractEvent capitalizationEnd = EventFactory.createEvent(model.getAs("capitalizationEndDate"), 
                                                        EventType.IPCI,
                                                        model.getAs("currency"),
                                                        new POF_IPCI_PAM(), new STF_IPCI_PAM(), 
                                                        model.getAs("businessDayConvention"), 
                                                        model.getAs("contractID"));
                interestEvents.removeIf(e -> e.eventType().equals(EventType.IP) && e.compareTo(capitalizationEnd) == 0);
                interestEvents.add(capitalizationEnd);

                // for all events with time <= IPCED && type == "IP" do
                // change type to IPCI and payoff/state-trans functions
                interestEvents.forEach(e -> {
                    if (e.eventType().equals(EventType.IP) && e.compareTo(capitalizationEnd) != 1) {
                        e.eventType(EventType.IPCI);
                        e.fPayOff(new POF_IPCI_PAM());
                        e.fStateTrans(new STF_IPCI_PAM());
                    }
                });
            }
            events.addAll(interestEvents);
            
        }else if(!CommonUtils.isNull(model.getAs("capitalizationEndDate"))) {
            // if no extra interest schedule set but capitalization end date, add single IPCI event
            events.add(EventFactory.createEvent(
                    model.getAs("capitalizationEndDate"),
                    EventType.IPCI,
                    model.getAs("currency"),
                    new POF_IPCI_PAM(),
                    new STF_IPCI_PAM(),
                    model.getAs("businessDayConvention"),
                    model.getAs("contractID")
            ));
        }else if (CommonUtils.isNull(model.getAs("cycleOfInterestPayment")) && CommonUtils.isNull(model.getAs("cycleAnchorDateOfInterestPayment"))) {
			
			 // raw interest payment events
           Set<ContractEvent> interestEvents = EventFactory.createEvents(
                   ScheduleFactory.createSchedule(
                           model.getAs("initialExchangeDate"),
                           model.getAs("maturityDate"),
                           null,
                           model.getAs("endOfMonthConvention"),
                           true
                   ),
                   EventType.IP,
                   model.getAs("currency"),
                   new POF_IP_PAM(),
                   new STF_IP_PAM(),
                   model.getAs("businessDayConvention"),
                   model.getAs("contractID")
           );
           events.addAll(interestEvents);
		}
        // rate reset
        Set<ContractEvent> rateResetEvents = EventFactory.createEvents(
                ScheduleFactory.createSchedule(
                        model.<LocalDateTime>getAs("cycleAnchorDateOfRateReset"),
                        model.getAs("maturityDate"),
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

        // add all rate reset events
        events.addAll(rateResetEvents);

        // fees (if specified)
        if (!CommonUtils.isNull(model.getAs("cycleOfFee"))) { 
        events.addAll(EventFactory.createEvents(
                ScheduleFactory.createSchedule(
                        model.getAs("cycleAnchorDateOfFee"),
                        model.getAs("maturityDate"),
                        model.getAs("cycleOfFee"),
                        model.getAs("endOfMonthConvention"),
                        true
                ),
                EventType.FP,
                model.getAs("currency"),
                new POF_FP_PAM(),
                new STF_FP_PAM(),
                model.getAs("businessDayConvention"),
                model.getAs("contractID")
        ));
        }
        // scaling (if specified)
        String scalingEffect = model.getAs("scalingEffect").toString();
        if (!CommonUtils.isNull(scalingEffect) && (scalingEffect.contains("I") || scalingEffect.contains("N"))) { 
        events.addAll(EventFactory.createEvents(
                ScheduleFactory.createSchedule(
                        model.getAs("cycleAnchorDateOfScalingIndex"),
                        model.getAs("maturityDate"),
                        model.getAs("cycleOfScalingIndex"),
                        model.getAs("endOfMonthConvention"),
                        false
                ),
                EventType.SC,
                model.getAs("currency"),
                new POF_SC_PAM(),
                new STF_SC_PAM(),
                model.getAs("businessDayConvention"),
                model.getAs("contractID")
        ));
        }
        // termination
        if (!CommonUtils.isNull(model.getAs("terminationDate"))) {
        	// Insert IP at termination for clean-price TD
        	ContractEvent terminationAtIP = TerminationEventUtils.createAccruedInterestEventAtTerminationPAM(model);
            events.add(terminationAtIP);
            
            ContractEvent termination = EventFactory.createEvent(
                    model.getAs("terminationDate"),
                    EventType.TD,
                    model.getAs("currency"),
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
        // remove pre-purchase events if purchase date set
        if(!CommonUtils.isNull(model.getAs("purchaseDate"))) {
            events.removeIf(e -> !e.eventType().equals(EventType.AD) && e.compareTo(EventFactory.createEvent(model.getAs("purchaseDate"), EventType.PRD, model.getAs("currency"), null, null, model.getAs("contractID"))) == -1);
        }
        // return evaluated events
        return events;
    }

    private static StateSpace initStateSpace(ContractModelProvider model) throws AttributeConversionException {
        StateSpace states = new StateSpace();
        states.notionalScalingMultiplier = model.getAs("notionalScalingMultiplier");
        states.interestScalingMultiplier = model.getAs("interestScalingMultiplier");

        states.contractPerformance = model.getAs("contractPerformance");
        states.statusDate = model.getAs("statusDate");

        if(model.<LocalDateTime>getAs("initialExchangeDate").isAfter(model.getAs("statusDate"))){
            states.notionalPrincipal = 0.0;
            states.nominalInterestRate = 0.0;
        }else{
            states.notionalPrincipal = ContractRoleConvention.roleSign(model.getAs("contractRole"))*model.<Double>getAs("notionalPrincipal");
            states.nominalInterestRate = model.getAs("nominalInterestRate");
        }

        if(CommonUtils.isNull(model.getAs("nominalInterestRate"))){
            states.accruedInterest = 0.0;
        } else if(!CommonUtils.isNull(model.getAs("accruedInterest"))){
            states.accruedInterest = model.getAs("accruedInterest");
        } else{
            DayCountCalculator dayCounter = model.getAs("dayCountConvention");
            BusinessDayAdjuster timeAdjuster = model.getAs("businessDayConvention");
            List<LocalDateTime> ipSchedule = new ArrayList<>(ScheduleFactory.createSchedule(
                    model.getAs("cycleAnchorDateOfInterestPayment"),
                    model.getAs("maturityDate"),
                    model.getAs("cycleOfInterestPayment"),
                    model.getAs("endOfMonthConvention"),
                    true
            ));
            Collections.sort(ipSchedule);
            List<LocalDateTime> dateEarlierThanT0 = ipSchedule.stream().filter(time -> time.isBefore(states.statusDate)).collect(Collectors.toList());
            LocalDateTime tMinus = dateEarlierThanT0.get(dateEarlierThanT0.size() -1);
            states.accruedInterest = dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(tMinus), timeAdjuster.shiftCalcTime(states.statusDate))
                    * states.notionalPrincipal
                    * states.nominalInterestRate;
        }

        if(CommonUtils.isNull(model.getAs("feeRate"))){
            states.feeAccrued = 0.0;
        } else if(!CommonUtils.isNull(model.getAs("feeAccrued"))){
            states.feeAccrued = model.getAs("feeAccrued");
        }//TODO: implement last two possible initialization


        // return the initialized state space
        return states;
    }

}
