/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.contracts;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;
import java.util.Set;

import org.actus.AttributeConversionException;
import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.conventions.endofmonth.EndOfMonthAdjuster;
import org.actus.events.ContractEvent;
import org.actus.events.EventFactory;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.StateTransitionFunction;
import org.actus.functions.lam.POF_IPCB_LAM;
import org.actus.functions.lam.POF_IP_LAM;
import org.actus.functions.lam.POF_PRD_LAM;
import org.actus.functions.lam.POF_PR_LAM;
import org.actus.functions.lam.POF_TD_LAM;
import org.actus.functions.lam.STF_FP_LAM;
import org.actus.functions.lam.STF_IED_LAM;
import org.actus.functions.lam.STF_IPCB_LAM;
import org.actus.functions.lam.STF_IPCI2_LAM;
import org.actus.functions.lam.STF_IPCI_LAM;
import org.actus.functions.lam.STF_MD_LAM;
import org.actus.functions.lam.STF_PR2_LAM;
import org.actus.functions.lam.STF_PRD_LAM;
import org.actus.functions.lam.STF_PR_LAM;
import org.actus.functions.lam.STF_RRF_LAM;
import org.actus.functions.lam.STF_RR_LAM;
import org.actus.functions.lam.STF_SC_LAM;
import org.actus.functions.pam.POF_FP_PAM;
import org.actus.functions.pam.POF_IED_PAM;
import org.actus.functions.pam.POF_IPCI_PAM;
import org.actus.functions.pam.POF_MD_PAM;
import org.actus.functions.pam.POF_RR_PAM;
import org.actus.functions.pam.POF_SC_PAM;
import org.actus.functions.pam.STF_IP_PAM;
import org.actus.functions.pam.STF_TD_PAM;
import org.actus.states.StateSpace;
import org.actus.time.ScheduleFactory;
import org.actus.types.EventType;
import org.actus.types.InterestCalculationBase;
import org.actus.util.CommonUtils;
import org.actus.util.CycleUtils;
import org.actus.util.PurchaseEventUtils;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import org.actus.util.RedemptionUtils;
import org.actus.util.TerminationEventUtils;

/**
 * Represents the Linear Amortizer payoff algorithm
 * 
 * @see <a href="https://www.actusfrf.org"></a>
 */
public final class LinearAmortizer {

    // compute next n non-contingent events
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<ContractEvent>();

        // determine maturity of the contract
        LocalDateTime maturity = maturity(model);

        // initial exchange
        events.add(EventFactory.createEvent(
                model.getAs("initialExchangeDate"),
                EventType.IED,
                model.getAs("currency"),
                new POF_IED_PAM(),
                new STF_IED_LAM(),
                model.getAs("contractID"))
        );

        // principal redemption schedule
        Set<LocalDateTime> prSchedule = ScheduleFactory.createSchedule(
                model.getAs("cycleAnchorDateOfPrincipalRedemption"),
                maturity,
                model.getAs("cycleOfPrincipalRedemption"),
                model.getAs("endOfMonthConvention"),
                false
        );
        // -> chose right state transition function depending on ipcb attributes
        StateTransitionFunction stf=(!CommonUtils.isNull(model.getAs("interestCalculationBase")) && model.getAs("interestCalculationBase").equals(InterestCalculationBase.NTL))? new STF_PR_LAM() : new STF_PR2_LAM();
        // regular principal redemption events
        events.addAll(EventFactory.createEvents(
                prSchedule,
                EventType.PR,
                model.getAs("currency"),
                new POF_PR_LAM(),
                stf,
                model.getAs("businessDayConvention"),
                model.getAs("contractID"))
        );

        events.add(EventFactory.createEvent(
	        maturity,
            EventType.MD,
            model.getAs("currency"),
            new POF_MD_PAM(),
            new STF_MD_LAM(),
            model.getAs("businessDayConvention"),
            model.getAs("contractID"))
        );

        // purchase
        if (!CommonUtils.isNull(model.getAs("purchaseDate"))) {
        	
        	// Insert IP at purchaseDate for clean-price PRD
        	ContractEvent ipAtPurchaseDate = PurchaseEventUtils.createAccruedInterestEventAtPurchaseLAM(model);
            events.add(ipAtPurchaseDate);
            
            events.add(EventFactory.createEvent(
                    model.getAs("purchaseDate"),
                    EventType.PRD,
                    model.getAs("currency"),
                    new POF_PRD_LAM(),
                    new STF_PRD_LAM(),
                    model.getAs("contractID"))
            );
        }

        // -> chose right state transition function for IPCI depending on ipcb attributes
        StateTransitionFunction stf_ipci=(!CommonUtils.isNull(model.getAs("interestCalculationBase")) && model.getAs("interestCalculationBase").equals(InterestCalculationBase.NTL))? new STF_IPCI_LAM() : new STF_IPCI2_LAM();
        // interest payment related
        if (!CommonUtils.isNull(model.getAs("cycleOfInterestPayment")) || !CommonUtils.isNull(model.getAs("cycleAnchorDateOfInterestPayment"))) {
            // raw interest payment events
            Set<ContractEvent> interestEvents = EventFactory.createEvents(
                    ScheduleFactory.createSchedule(
                            model.getAs("cycleAnchorDateOfInterestPayment"),
                            maturity,
                            model.getAs("cycleOfInterestPayment"),
                            model.getAs("endOfMonthConvention"),
                            true
                    ),
                    EventType.IP,
                    model.getAs("currency"),
                    new POF_IP_LAM(),
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
                                new POF_IPCI_PAM(), stf_ipci, 
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
                    e.fStateTrans(stf_ipci);
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
                    stf_ipci,
                    model.getAs("businessDayConvention"),
                    model.getAs("contractID"))
            );
        }

        // rate reset
        Set<ContractEvent> rateResetEvents = EventFactory.createEvents(
                ScheduleFactory.createSchedule(
                        model.<LocalDateTime>getAs("cycleAnchorDateOfRateReset"),
                        maturity,
                        model.getAs("cycleOfRateReset"),
                        model.getAs("endOfMonthConvention"),
                        false
                ),
                EventType.RR,
                model.getAs("currency"),
                new POF_RR_PAM(),
                new STF_RR_LAM(),
                model.getAs("businessDayConvention"),
                model.getAs("contractID")
        );

        // adapt fixed rate reset event
        if(!CommonUtils.isNull(model.getAs("nextResetRate"))) {
            ContractEvent fixedEvent = rateResetEvents.stream().sorted().filter(e -> e.compareTo(EventFactory.createEvent(model.getAs("statusDate"), EventType.AD, model.getAs("currency"), null, null, model.getAs("contractID"))) == 1).findFirst().get();
            fixedEvent.fStateTrans(new STF_RRF_LAM());
            fixedEvent.eventType(EventType.RRF);
            rateResetEvents.add(fixedEvent);
        }

        events.addAll(rateResetEvents);
        // fees (if specified)
        if (!CommonUtils.isNull(model.getAs("cycleOfFee"))) { 
            events.addAll(EventFactory.createEvents(
                    ScheduleFactory.createSchedule(
                            model.getAs("cycleAnchorDateOfFee"),
                            maturity,
                            model.getAs("cycleOfFee"),
                            model.getAs("endOfMonthConvention")
                    ),
                    EventType.FP,
                    model.getAs("currency"),
                    new POF_FP_PAM(),
                    new STF_FP_LAM(),
                    model.getAs("businessDayConvention"),
                    model.getAs("contractID"))
            );
        }
        // scaling (if specified)
        if (!CommonUtils.isNull(model.getAs("scalingEffect")) && (model.getAs("scalingEffect").toString().contains("I") || model.getAs("scalingEffect").toString().contains("N"))) {
            events.addAll(EventFactory.createEvents(
                    ScheduleFactory.createSchedule(
                            model.getAs("cycleAnchorDateOfScalingIndex"),
                            maturity,
                            model.getAs("cycleOfScalingIndex"),
                            model.getAs("endOfMonthConvention"),
                            false
                    ),
                    EventType.SC,
                    model.getAs("currency"),
                    new POF_SC_PAM(),
                    new STF_SC_LAM(),
                    model.getAs("businessDayConvention"),
                    model.getAs("contractID"))
            );
        }

        // interest calculation base (if specified)
        if (!CommonUtils.isNull(model.getAs("interestCalculationBase")) && model.getAs("interestCalculationBase").equals(InterestCalculationBase.NTL)) {
            events.addAll(EventFactory.createEvents(
                    ScheduleFactory.createSchedule(
                            model.getAs("cycleAnchorDateOfInterestCalculationBase"),
                            maturity,
                            model.getAs("cycleOfInterestCalculationBase"),
                            model.getAs("endOfMonthConvention"),
                            false
                    ),
                    EventType.IPCB,
                    model.getAs("currency"),
                    new POF_IPCB_LAM(),
                    new STF_IPCB_LAM(),
                    model.getAs("businessDayConvention"),
                    model.getAs("contractID"))
            );
        }

        // termination
        if (!CommonUtils.isNull(model.getAs("terminationDate"))) {
        	
        	// Insert IP at termination for clean-price TD
        	ContractEvent terminationAtIP = TerminationEventUtils.createAccruedInterestEventAtTerminationLAM(model);
            events.add(terminationAtIP);
            
            ContractEvent termination = EventFactory.createEvent(
                    model.getAs("terminationDate"),
                    EventType.TD, model.getAs("currency"),
                    new POF_TD_LAM(),
                    new STF_TD_PAM(),
                    model.getAs("contractID")
            );
            events.removeIf(e -> e.compareTo(termination) == 1); // remove all post-termination events
            events.add(termination);
        }

        // remove all pre-status date events
        events.removeIf(e -> e.compareTo(EventFactory.createEvent(model.getAs("statusDate"), EventType.AD, model.getAs("currency"), null, null, model.getAs("contractID"))) == -1);
        if(CommonUtils.isNull(to)){
            to = maturity;
        }

        // remove all post to-date events
        ContractEvent postDate = EventFactory.createEvent(to, EventType.AD, model.getAs("currency"), null, null, model.getAs("contractID"));
        events.removeIf(e -> e.compareTo(postDate)== 1);

        // sort the events in the payoff-list according to their time of occurence
        Collections.sort(events);

        return events;
    }

    // apply a set of events to the current state of a contract and return the post events state
    public static ArrayList<ContractEvent> apply(ArrayList<ContractEvent> events,
                                                 ContractModelProvider model,
                                                 RiskFactorModelProvider observer) throws AttributeConversionException {

        // initialize state space per status date
        StateSpace states = initStateSpace(model,maturity(model));

        // sort the events according to their time sequence
        Collections.sort(events);

        // apply events according to their time sequence to current state
		ListIterator<ContractEvent> eventIterator = events.listIterator();
        //while (( states.statusDate.isBefore(initialExchangeDate) || states.notionalPrincipal != 0.0) && eventIterator.hasNext()) {
        while (eventIterator.hasNext()) {
                ((ContractEvent) eventIterator.next()).eval(states, model, observer, model.getAs("dayCountConvention"),
                    model.getAs("businessDayConvention"));
        }
        
        // remove pre-purchase events if purchase date set
        if(!CommonUtils.isNull(model.getAs("purchaseDate"))) {
            events.removeIf(e -> !e.eventType().equals(EventType.AD) && e.compareTo(EventFactory.createEvent(model.getAs("purchaseDate"), EventType.PRD, model.getAs("currency"), null, null, model.getAs("contractID"))) == -1);
        }

        // return evaluated events
        return events;
    }

    private static LocalDateTime maturity(ContractModelProvider model) {
    	EndOfMonthAdjuster adjuster = null;
        // determine maturity of the contract
        LocalDateTime maturity = model.getAs("maturityDate");
        if (CommonUtils.isNull(maturity)) {
            LocalDateTime lastEvent;
            int remainingPeriods;
            if(model.<LocalDateTime>getAs("cycleAnchorDateOfPrincipalRedemption").isBefore(model.getAs("statusDate"))) {
                // last event not part of remaining periods
                Set<LocalDateTime> previousEvents = ScheduleFactory.createSchedule(model.getAs("cycleAnchorDateOfPrincipalRedemption"),model.getAs("statusDate"),
                        model.getAs("cycleOfPrincipalRedemption"), model.getAs("endOfMonthConvention"));
                previousEvents.removeIf( d -> d.isBefore(model.<LocalDateTime>getAs("statusDate").minus(CycleUtils.parsePeriod(model.getAs("cycleOfInterestPayment")))));
                previousEvents.remove(model.getAs("statusDate"));
                lastEvent = previousEvents.toArray(new LocalDateTime[1])[0];
                remainingPeriods = (int) Math.ceil(model.<Double>getAs("notionalPrincipal")/model.<Double>getAs("nextPrincipalRedemptionPayment"));
            } else {
                // last event also one of remaining periods
                lastEvent = model.getAs("cycleAnchorDateOfPrincipalRedemption");
                remainingPeriods = (int) Math.ceil(model.<Double>getAs("notionalPrincipal")/model.<Double>getAs("nextPrincipalRedemptionPayment"))-1;
            }
            String cycle = model.getAs("cycleOfPrincipalRedemption");
            adjuster = new EndOfMonthAdjuster(model.getAs("endOfMonthConvention"), lastEvent, cycle);
            TemporalAmount cycleAmount = CycleUtils.parseTemporalAmount(cycle);
            if (cycleAmount instanceof Duration) {
                maturity = adjuster.shift(lastEvent.plus(((Duration) cycleAmount).multipliedBy(remainingPeriods)));
            } else {
                maturity = adjuster.shift(lastEvent.plus(((Period) cycleAmount).multipliedBy(remainingPeriods)));
            }
        }
        return maturity;
    }

    private static StateSpace initStateSpace(ContractModelProvider model, LocalDateTime maturity) throws AttributeConversionException {
        StateSpace states = new StateSpace();

        // general states to be initialized
        states.maturityDate = maturity;

        if(model.<LocalDateTime>getAs("initialExchangeDate").isAfter(model.getAs("statusDate"))){
            states.notionalPrincipal = 0.0;
            states.nominalInterestRate = 0.0;
            states.interestCalculationBaseAmount = 0.0;
        }else{
            states.notionalPrincipal = ContractRoleConvention.roleSign(model.getAs("contractRole"))*model.<Double>getAs("notionalPrincipal");
            states.nominalInterestRate = model.getAs("nominalInterestRate");
            if(InterestCalculationBase.NT.equals(model.getAs("interestCalculationBase"))){
                states.interestCalculationBaseAmount = states.notionalPrincipal; // contractRole applied at notionalPrincipal init
            }else{
                states.interestCalculationBaseAmount = ContractRoleConvention.roleSign(model.getAs("contractRole")) * model.<Double>getAs("interestCalculationBaseAmount");
            }
        }

        if(CommonUtils.isNull(model.getAs("nominalInterestRate"))){
            states.accruedInterest = 0.0;
        } else if(!CommonUtils.isNull(model.getAs("accruedInterest"))){
            states.accruedInterest = model.getAs("accruedInterest");
        } else{
            DayCountCalculator dayCountCalculator = model.getAs("dayCountConvention");
            BusinessDayAdjuster businessDayAdjuster = model.getAs("businessDayConvention");
            //TODO: what is t- in this case ?
            //states.accruedInterest = dayCountCalculator.dayCountFraction()
        }

        if(CommonUtils.isNull(model.getAs("feeRate"))){
            states.feeAccrued = 0.0;
        } else if(!CommonUtils.isNull(model.getAs("feeAccrued"))){
            states.feeAccrued = model.getAs("feeAccrued");
        }//TODO: implement last two possible initialization

        states.notionalScalingMultiplier = model.getAs("notionalScalingMultiplier");
        states.interestScalingMultiplier = model.getAs("interestScalingMultiplier");

        states.contractPerformance = model.getAs("contractPerformance");
        states.statusDate = model.getAs("statusDate");

        // init next principal redemption payment amount (can be null!)
        if (CommonUtils.isNull(model.getAs("nextPrincipalRedemptionPayment"))) {
            /*LocalDateTime s ;
            LocalDateTime pranx = model.getAs("cycleAnchorDateOfPrincipalRedemption");
            LocalDateTime statusDate = model.getAs("statusDate");
            Period prcl = CycleUtils.parsePeriod(model.getAs("cycleOfPrincipalRedemption"));
            LocalDateTime ied = model.getAs("initialExchangeDate");

            if(!CommonUtils.isNull(pranx) && pranx.isAfter(statusDate)){
                s = pranx;
            } else if(CommonUtils.isNull(pranx) && ied.plus(prcl).isAfter(statusDate)){
                s = ied.plus(prcl);
            } else{
                Set<LocalDateTime> tPR = ScheduleFactory.createSchedule(
                        model.getAs("cycleAnchorDateOfPrincipalRedemption"),
                        maturity,
                        model.getAs("cycleOfPrincipalRedemption"),
                        model.getAs("endOfMonthConvention")
                );
                tPR.removeIf( d -> d.isAfter(statusDate));
                tPR.remove(statusDate);
                List<LocalDateTime> tPRlist = new ArrayList<>(tPR);
                Collections.sort(tPRlist);
                int lastIndex = tPRlist.size();
                s = tPRlist.get(lastIndex-1);
            }

            DayCountCalculator dayCounter  = model.getAs("dayCountConvention");
            BusinessDayAdjuster timeAdjuster = model.getAs("businessDayConvention");
            states.nextPrincipalRedemptionPayment =
                    model.<Double>getAs("notionalPrincipal")
                    * Math.pow(
                            Math.ceil(
                                    dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(s), timeAdjuster.shiftCalcTime(states.maturityDate))
                                    / dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(s), timeAdjuster.shiftCalcTime(s.plus(prcl)))
                            )
                    , -1.0)
            ;*/
            states.nextPrincipalRedemptionPayment = RedemptionUtils.redemptionAmount(model, states);
        } else {
            states.nextPrincipalRedemptionPayment = model.<Double>getAs("nextPrincipalRedemptionPayment");
        }

        // return the initialized state space
        return states;
    }

}
