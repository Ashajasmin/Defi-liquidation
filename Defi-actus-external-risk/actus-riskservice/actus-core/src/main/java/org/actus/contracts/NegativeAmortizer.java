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
import org.actus.states.StateSpace;
import org.actus.events.EventFactory;
import org.actus.time.ScheduleFactory;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.types.EventType;
import org.actus.types.InterestCalculationBase;
import org.actus.util.CommonUtils;
import org.actus.util.CycleUtils;
import org.actus.util.PurchaseEventUtils;
import org.actus.util.TerminationEventUtils;
import org.actus.functions.pam.*;
import org.actus.functions.lam.*;
import org.actus.functions.nam.*;
import org.actus.functions.StateTransitionFunction;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the Negative Amortizer payoff algorithm
 * 
 * @see <a href="https://www.actusfrf.org"></a>
 */
public final class NegativeAmortizer {

    // compute next n non-contingent events
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<ContractEvent>();

        // determine maturity of the contract
        LocalDateTime maturity = maturity(model);

        // initial exchange
        events.add(EventFactory.createEvent(
                model.getAs("initialExchangeDate"),
                EventType.IED, model.getAs("currency"),
                new POF_IED_PAM(), new STF_IED_LAM(),
                model.getAs("contractID"))
        );

        // principal redemption schedule
        Set<LocalDateTime> prSchedule = ScheduleFactory.createSchedule(
                model.getAs("cycleAnchorDateOfPrincipalRedemption"),
                maturity,model.getAs("cycleOfPrincipalRedemption"),
                model.getAs("endOfMonthConvention"),
                false
        );

        // -> chose right state transition function depending on ipcb attributes
        StateTransitionFunction stf= !(InterestCalculationBase.NT.equals(model.<InterestCalculationBase>getAs("interestCalculationBase")))? new STF_PR_NAM() : new STF_PR2_NAM();

        // regular principal redemption events
        events.addAll(EventFactory.createEvents(
                prSchedule,
                EventType.PR,
                model.getAs("currency"),
                new POF_PR_NAM(),
                stf,
                model.getAs("businessDayConvention"),
                model.getAs("contractID"))
        );

        events.add(EventFactory.createEvent(
                maturity,
                EventType.MD,
                model.getAs("currency"),
                new POF_MD_PAM(),new STF_MD_LAM(),
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
            
            // Check if the cycle anchor dates and cycle periods for interest payments and principal payments are different
            if (!model.getAs("cycleAnchorDateOfInterestPayment").equals(model.getAs("cycleAnchorDateOfPrincipalRedemption"))
                    || !model.getAs("cycleOfInterestPayment").equals(model.getAs("cycleOfPrincipalRedemption")) ) {

                // Parse the period of principal redemption cycle
                TemporalAmount prcl = CycleUtils.parseTemporalAmount(model.getAs("cycleOfPrincipalRedemption"));

                // Calculate the next principal redemption date by subtracting the cycle period from the anchor date
                LocalDateTime pranxm = model.<LocalDateTime>getAs("cycleAnchorDateOfPrincipalRedemption").minus(prcl);

                // Remove any interest payment events that occur on or after the calculated next principal redemption date
                interestEvents.removeIf(e -> (e.eventType().toString().equals("IP")
                                        && (e.eventTime().isAfter(pranxm) || e.eventTime().equals(pranxm))));

                // Create a new interest payment event at the adjusted principal redemption date
                ContractEvent ipanxm = EventFactory.createEvent(pranxm, 
                        EventType.IP,
                        model.getAs("currency"),
                        new POF_IP_LAM(),
                        new STF_IP_PAM(),
                        model.getAs("businessDayConvention"), 
                        model.getAs("contractID"));

                // Add the new interest payment event to the list
                interestEvents.add(ipanxm);            

                // Generate new interest payment events based on the updated principal redemption schedule
                events.addAll( EventFactory.createEvents(
                        ScheduleFactory.createSchedule(
                                model.getAs("cycleAnchorDateOfPrincipalRedemption"),
                                maturity,
                                model.getAs("cycleOfPrincipalRedemption"),
                                model.getAs("endOfMonthConvention"),
                                true
                        ),
                        EventType.IP,
                        model.getAs("currency"),
                        new POF_IP_LAM(),
                        new STF_IP_PAM(),
                        model.getAs("businessDayConvention"),
                        model.getAs("contractID")
                ));
            }

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
        } else if (CommonUtils.isNull(model.getAs("cycleOfInterestPayment")) && CommonUtils.isNull(model.getAs("cycleAnchorDateOfInterestPayment"))) {
			
			//  If no IPCL or IPANX is provided, IP events are set to PR cycle
			 
			 // raw interest payment events
            Set<ContractEvent> interestEvents = EventFactory.createEvents(
                    ScheduleFactory.createSchedule(
                            model.getAs("cycleAnchorDateOfPrincipalRedemption"),
                            maturity,
                            model.getAs("cycleOfPrincipalRedemption"),
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
            events.addAll(interestEvents);
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
                            EventType.TD,
                            model.getAs("currency"),
                            new POF_TD_LAM(),
                            new STF_TD_PAM(),
                            model.getAs("contractID")
            );
            events.removeIf(e -> e.compareTo(termination) == 1); // remove all post-termination events
            events.add(termination);
        }

        // remove all pre-status date events
        events.removeIf(e -> e.compareTo(EventFactory.createEvent(model.getAs("statusDate"), EventType.AD, model.getAs("currency"), null,null, model.getAs("contractID"))) == -1);

        // remove all post to-date events
        if(CommonUtils.isNull(to)){
            to = maturity;
        }
        ContractEvent postDate = EventFactory.createEvent(to, EventType.AD, model.getAs("currency"), null, null, model.getAs("contractID"));
        events.removeIf(e -> e.compareTo(postDate) == 1);

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
        ListIterator eventIterator = events.listIterator();
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

    // determine maturity of the contract
    private static LocalDateTime maturity(ContractModelProvider model) {
        LocalDateTime maturity = model.getAs("maturityDate");
        if (CommonUtils.isNull(maturity)) {
            LocalDateTime t0 = model.getAs("statusDate");
            LocalDateTime pranx = model.getAs("cycleAnchorDateOfPrincipalRedemption");
            LocalDateTime ied = model.getAs("initialExchangeDate");
            TemporalAmount prcl = CycleUtils.parseTemporalAmount(model.getAs("cycleOfPrincipalRedemption"));
            LocalDateTime lastEvent;
            if(!CommonUtils.isNull(pranx) && (pranx.isEqual(t0) || pranx.isAfter(t0))) {
                lastEvent = pranx;
            } else if(ied.plus(prcl).isAfter(t0) || ied.plus(prcl).isEqual(t0)) {
                lastEvent = ied.plus(prcl);
            }else{
                Set<LocalDateTime> previousEvents = ScheduleFactory.createSchedule(
                        model.getAs("cycleAnchorDateOfPrincipalRedemption"),
                        model.getAs("statusDate"),
                        model.getAs("cycleOfPrincipalRedemption"),
                        model.getAs("endOfMonthConvention")
                    );
                previousEvents.removeIf( d -> d.isBefore(t0));
                previousEvents.remove(t0);
                List<LocalDateTime> prevEventsList = new ArrayList<>(previousEvents);
                Collections.sort(prevEventsList);
                lastEvent = prevEventsList.get(prevEventsList.size()-1);
            }
            double timeFromLastEventPlusOneCycle = model.<DayCountCalculator>getAs("dayCountConvention").dayCountFraction(lastEvent, lastEvent.plus(prcl));
            double redemptionPerCycle = model.<Double>getAs("nextPrincipalRedemptionPayment") - (timeFromLastEventPlusOneCycle * model.<Double>getAs("nominalInterestRate") * model.<Double>getAs("notionalPrincipal"));
            int remainingPeriods = (int) Math.ceil(model.<Double>getAs("notionalPrincipal") / redemptionPerCycle)-1;
            if (prcl instanceof Duration) {
                maturity = model.<BusinessDayAdjuster>getAs("businessDayConvention").shiftEventTime(lastEvent.plus(((Duration) prcl).multipliedBy(remainingPeriods)));
            } else {
                maturity = model.<BusinessDayAdjuster>getAs("businessDayConvention").shiftEventTime(lastEvent.plus(((Period) prcl).multipliedBy(remainingPeriods)));
            }
        }
        return maturity;
    }

    private static StateSpace initStateSpace(ContractModelProvider model) throws AttributeConversionException {
        StateSpace states = new StateSpace();

        states.notionalScalingMultiplier = model.getAs("notionalScalingMultiplier");
        states.interestScalingMultiplier = model.getAs("interestScalingMultiplier");

        states.contractPerformance = model.getAs("contractPerformance");
        states.statusDate = model.getAs("statusDate");
        states.nextPrincipalRedemptionPayment = model.<Double>getAs("nextPrincipalRedemptionPayment");

        if(model.<LocalDateTime>getAs("initialExchangeDate").isAfter(model.getAs("statusDate"))){
            states.notionalPrincipal = 0.0;
            states.nominalInterestRate = 0.0;
            states.interestCalculationBaseAmount = 0.0;
        }else{
            states.notionalPrincipal = ContractRoleConvention.roleSign(model.getAs("contractRole"))*model.<Double>getAs("notionalPrincipal");
            states.nominalInterestRate = model.getAs("nominalInterestRate");
            if(InterestCalculationBase.NT.equals(model.getAs("interestCalculationBase"))){
                states.interestCalculationBaseAmount = states.notionalPrincipal; // contractRole applied at notionalPrincipal initialization
            }else{
                states.interestCalculationBaseAmount = ContractRoleConvention.roleSign(model.getAs("contractRole")) * model.<Double>getAs("interestCalculationBaseAmount");
            }

        }

        if(CommonUtils.isNull(model.getAs("nominalInterestRate"))){
            states.accruedInterest = 0.0;
        } else if(!CommonUtils.isNull(model.getAs("accruedInterest"))){
            states.accruedInterest = ContractRoleConvention.roleSign(model.getAs("contractRole")) * model.<Double>getAs("accruedInterest");
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
