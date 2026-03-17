/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.contracts;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.actus.AttributeConversionException;
import org.actus.attributes.ContractModel;
import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.events.ContractEvent;
import org.actus.events.EventFactory;
import org.actus.externals.RiskFactorModelProvider;

import org.actus.functions.PayOffFunction;
import org.actus.functions.StateTransitionFunction;
import org.actus.functions.lam.POF_IPCB_LAM;
import org.actus.functions.lam.POF_IP_LAM;
import org.actus.functions.lam.POF_PRD_LAM;
import org.actus.functions.lam.POF_TD_LAM;
import org.actus.functions.lam.STF_FP_LAM;
import org.actus.functions.lam.STF_IED_LAM;
import org.actus.functions.lam.STF_IPCB_LAM;
import org.actus.functions.lam.STF_IPCI2_LAM;
import org.actus.functions.lam.STF_IPCI_LAM;
import org.actus.functions.lam.STF_PRD_LAM;
import org.actus.functions.lam.STF_SC_LAM;
import org.actus.functions.lax.POF_PI_LAX;
import org.actus.functions.lax.POF_PR_LAX;
import org.actus.functions.lax.STF_PI_LAX;
import org.actus.functions.lax.STF_PI_LAX2;
import org.actus.functions.lax.STF_PR_LAX;
import org.actus.functions.lax.STF_PR_LAX2;
import org.actus.functions.lax.STF_RRF_LAX;
import org.actus.functions.lax.STF_RRY_LAM;
import org.actus.functions.lax.STF_RR_LAX;
import org.actus.functions.pam.POF_AD_PAM;
import org.actus.functions.pam.POF_FP_PAM;
import org.actus.functions.pam.POF_IED_PAM;
import org.actus.functions.pam.POF_IPCI_PAM;
import org.actus.functions.pam.POF_RR_PAM;
import org.actus.functions.pam.POF_SC_PAM;
import org.actus.functions.pam.STF_AD_PAM;
import org.actus.functions.pam.STF_IP_PAM;
import org.actus.functions.pam.STF_TD_PAM;
import org.actus.functions.lam.STF_MD_LAM;
import org.actus.functions.pam.POF_MD_PAM;

import org.actus.states.StateSpace;
import org.actus.time.ScheduleFactory;
import org.actus.types.EventType;
import org.actus.types.InterestCalculationBase;
import org.actus.util.CommonUtils;
import org.actus.util.CycleUtils;

/**
 * Represents the Exotic Linear Amortizer payoff algorithm
 * 
 * @see <a href="https://www.actusfrf.org"></a>
 */
public final class ExoticLinearAmortizer {

	// compute next n non-contingent events
	public static ArrayList<ContractEvent> schedule(LocalDateTime to, ContractModelProvider model)
			throws AttributeConversionException {
		ArrayList<ContractEvent> events = new ArrayList<ContractEvent>();

		// determine maturity of the contract
		LocalDateTime maturity = Objects.isNull(to) ? maturity(model) : to;

		// initial exchange
		events.add(EventFactory.createEvent(
				model.getAs("initialExchangeDate"),
				EventType.IED,
				model.getAs("currency"),
				new POF_IED_PAM(),
				new STF_IED_LAM(),
				model.getAs("contractID"))
		);
		
		// purchase event
		if (!CommonUtils.isNull(model.getAs("purchaseDate"))) {
			events.add(EventFactory.createEvent(
					model.getAs("purchaseDate"),
					EventType.PRD,
					model.getAs("currency"),
					new POF_PRD_LAM(),
					new STF_PRD_LAM(),
					model.getAs("contractID"))
			);
		}

		// create principal redemption schedule
		if (!CommonUtils.isNull(model.getAs("arrayCycleAnchorDateOfPrincipalRedemption"))) {

			// parse array-type attributes
			LocalDateTime[] prAnchor = Arrays.stream(model.getAs("arrayCycleAnchorDateOfPrincipalRedemption").toString().replaceAll("\\[", "").replaceAll("]","").split(","))
					.map(d -> LocalDateTime.parse(d.trim())).toArray(LocalDateTime[]::new);
			String[] prCycle = {};
			if (!CommonUtils.isNull(model.getAs("arrayCycleOfPrincipalRedemption"))) {
				prCycle = Arrays.stream(model.getAs("arrayCycleOfPrincipalRedemption").toString().replaceAll("\\[", "").replaceAll("]","").split(",")).map(String::trim).toArray(String[]::new);
			}
			String[] prPayment = Arrays.stream(model.getAs("arrayNextPrincipalRedemptionPayment").toString().replaceAll("\\[", "").replaceAll("]","").split(",")).map(d -> d).toArray(String[]::new);
			String[] prIncDec = Arrays.stream(model.getAs("arrayIncreaseDecrease").toString().replaceAll("\\[", "").replaceAll("]","").split(","))
					.map(String::trim).toArray(String[]::new);

			// create array-type schedule with respective increase/decrease features
			EventType prType;
			StateTransitionFunction prStf;
			PayOffFunction prPof;
			int prLen = prAnchor.length + 1;
			LocalDateTime[] prLocalDate = new LocalDateTime[prLen];
			prLocalDate[prLen - 1] = maturity;
			for (int i = 0; i < prAnchor.length; i++) {
				prLocalDate[i] = prAnchor[i];
			}
			for (int i = 0; i < prAnchor.length; i++) {
				
				if (prIncDec[i].trim().equalsIgnoreCase("DEC")) {
					prType = EventType.PR;
					prStf = (!CommonUtils.isNull(model.getAs("interestCalculationBase"))
							&& model.getAs("interestCalculationBase").equals(InterestCalculationBase.NTL)) ?
							new STF_PR_LAX(Double.parseDouble(prPayment[i])) : new STF_PR_LAX2(Double.parseDouble(prPayment[i]));
					prPof = new POF_PR_LAX(Double.parseDouble(prPayment[i]));
				} else {
					prType = EventType.PI;
					prStf = (!CommonUtils.isNull(model.getAs("interestCalculationBase"))
							&& model.getAs("interestCalculationBase").equals(InterestCalculationBase.NTL)) ?
							new STF_PI_LAX(Double.parseDouble(prPayment[i])) : new STF_PI_LAX2(Double.parseDouble(prPayment[i]));
					prPof = new POF_PI_LAX(Double.parseDouble(prPayment[i]));
				}
				events.addAll(EventFactory.createEvents(
						ScheduleFactory.createSchedule(
								prLocalDate[i],
								prLocalDate[i + 1],
								(prCycle.length>0)? prCycle[i] : null, model.getAs("endOfMonthConvention"),
								false
						),
						prType,
						model.getAs("currency"),
						prPof,
						prStf,
						model.getAs("businessDayConvention"),
						model.getAs("contractID"))
				);
			}
		}

		// add maturity event
		events.add(EventFactory.createEvent(
	        maturity,
            EventType.MD,
            model.getAs("currency"),
            new POF_MD_PAM(),
            new STF_MD_LAM(),
            model.getAs("businessDayConvention"),
            model.getAs("contractID"))
        );

		// create interest payment schedule
		if (!CommonUtils.isNull(model.getAs("arrayCycleAnchorDateOfInterestPayment"))) {

			// parse array-type attributes
			LocalDateTime[] ipAnchor = Arrays
					.asList(model.getAs("arrayCycleAnchorDateOfInterestPayment").toString().replaceAll("\\[", "").replaceAll("\\]","").split(",")).stream()
					.map(d -> LocalDateTime.parse(d.trim())).toArray(LocalDateTime[]::new);
			String[] ipCycle = {};
			if (!CommonUtils.isNull(model.getAs("arrayCycleOfInterestPayment"))) {
				ipCycle = Arrays.asList(model.getAs("arrayCycleOfInterestPayment").toString().replaceAll("\\[", "").replaceAll("\\]","").split(","))
					.stream().map(d -> d.trim()).toArray(String[]::new);
			}

			// raw interest payment events
			Set<ContractEvent> interestEvents = EventFactory.createEvents(
					ScheduleFactory.createArraySchedule(
							ipAnchor,
							maturity,
							(ipCycle.length>0)? ipCycle : null,
							model.getAs("endOfMonthConvention")
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
				
				// define ipci state-transition function
				StateTransitionFunction stf_ipci = (!CommonUtils.isNull(model.getAs("interestCalculationBase"))
						&& model.getAs("interestCalculationBase").equals(InterestCalculationBase.NTL)) ? new STF_IPCI_LAM() : new STF_IPCI2_LAM();
						
				// for all events with time <= IPCED && type == "IP" do
				// change type to IPCI and payoff/state-trans functions
				ContractEvent capitalizationEnd = EventFactory.createEvent(
						model.getAs("capitalizationEndDate"),
						EventType.IPCI,
						model.getAs("currency"),
						new POF_IPCI_PAM(),
						stf_ipci,
						model.getAs("businessDayConvention"),
						model.getAs("contractID")
				);
				interestEvents.forEach(e -> {
					if (e.eventType().equals(EventType.IP) && e.compareTo(capitalizationEnd) == -1) {
						e.eventType(EventType.IPCI);
						e.fPayOff(new POF_IPCI_PAM());
						e.fStateTrans(stf_ipci);
					}
				});
				
				// also, remove any IP event exactly at IPCED and replace with an IPCI event
				interestEvents.remove(EventFactory.createEvent(model.getAs("capitalizationEndDate"),
						EventType.IP, model.getAs("currency"), new POF_AD_PAM(), new STF_AD_PAM(),
						model.getAs("businessDayConvention"), model.getAs("contractID")));
			}
			events.addAll(interestEvents);
		} else 
			
			// if no interest schedule defined, still add a capitalization event
			if (!CommonUtils.isNull(model.getAs("capitalizationEndDate"))) {
			
				// define ipci state-transition function
				StateTransitionFunction stf_ipci = (!CommonUtils.isNull(model.getAs("interestCalculationBase"))
						&& model.getAs("interestCalculationBase").equals(InterestCalculationBase.NTL)) ? new STF_IPCI_LAM() : new STF_IPCI2_LAM();
						
				// add single event
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
				
		// create rate reset schedule
		if (!CommonUtils.isNull(model.getAs("arrayCycleAnchorDateOfRateReset"))) {
			
			// parse array-type attributes
			LocalDateTime[] rrAnchor = Arrays
					.asList(model.getAs("arrayCycleAnchorDateOfRateReset").toString().replaceAll("\\[", "").replaceAll("\\]","").split(",")).stream()
					.map(d -> LocalDateTime.parse(d.trim())).toArray(LocalDateTime[]::new);
			String[] rrCycle = {};
			if (!CommonUtils.isNull(model.getAs("arrayCycleOfRateReset"))) {
				rrCycle = Arrays.asList(model.getAs("arrayCycleOfRateReset").toString().replaceAll("\\[", "").replaceAll("\\]","").split(","))
					.stream().map(d -> d.trim()).toArray(String[]::new);
			}
			String[] rrRate = Arrays.asList(model.getAs("arrayRate").toString().replaceAll("\\[", "").replaceAll("\\]","").split(",")).stream().map(d -> d.trim())
					.toArray(String[]::new);
			String[] rrFidedVar = Arrays.asList(model.getAs("arrayFixedVariable").toString().replaceAll("\\[", "").replaceAll("\\]","").split(",")).stream()
					.map(d -> d.trim()).toArray(String[]::new);
			
			// create array-type schedule with fix/var features
			EventType rrType;
			StateTransitionFunction rrStf;
			Set<ContractEvent> rateResetEvents = null;
			int rrLen = rrAnchor.length + 1;
			LocalDateTime rrLocalDate[] = new LocalDateTime[rrLen];
			rrLocalDate[rrLen - 1] = maturity;
			for (int i = 0; i < rrAnchor.length; i++) {
				rrLocalDate[i] = rrAnchor[i];
			}
			for (int i = 0; i < rrAnchor.length; i++) {
				if (rrFidedVar[i].trim().equalsIgnoreCase("FIX")) {
					rrType = EventType.RRF;
					rrStf = new STF_RRF_LAX(Double.parseDouble(rrRate[i]));
				} else {
					rrType = EventType.RR;
					rrStf = new STF_RR_LAX(Double.parseDouble(rrRate[i]));
				}
				rateResetEvents = EventFactory.createEvents(
						ScheduleFactory.createSchedule(
								rrLocalDate[i],
								rrLocalDate[i + 1],
								(rrCycle.length>0)? rrCycle[i] : null,
								model.getAs("endOfMonthConvention"),
								false
						),
						rrType,
						model.getAs("currency"),
						new POF_RR_PAM(),
						rrStf,
						model.getAs("businessDayConvention"),
						model.getAs("contractID")
				);
				events.addAll(rateResetEvents);
			}
			
			// adjust for already fixed reset rates
			if (!CommonUtils.isNull(model.getAs("nextResetRate"))) {
				rateResetEvents.stream().sorted()
						.filter(e -> e.compareTo(EventFactory.createEvent(model.getAs("statusDate"),
								EventType.AD, model.getAs("currency"), null, null, model.getAs("contractID"))) == 1)
						.findFirst().get().fStateTrans(new STF_RRY_LAM());
				events.addAll(rateResetEvents);
			}	
		}
				
		// fee schedule
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
		if (!CommonUtils.isNull(model.getAs("scalingEffect")) && (model.getAs("scalingEffect").toString().contains("I")
				|| model.getAs("scalingEffect").toString().contains("N"))) {
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
		if (!CommonUtils.isNull(model.getAs("interestCalculationBase"))
				&& model.getAs("interestCalculationBase").equals(InterestCalculationBase.NTL)) {
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
		events.removeIf(e -> e.compareTo(EventFactory.createEvent(model.getAs("statusDate"), EventType.AD,model.getAs("currency"), null, null, model.getAs("contractID"))) == -1);

		// remove all post to-date events
        events.removeIf(e -> e.compareTo(EventFactory.createEvent(maturity, EventType.AD, model.getAs("currency"), null, null, model.getAs("contractID"))) == 1);

		// sort the events in the payoff-list according to their time of occurence
		Collections.sort(events);

		return events;
	}

	// apply a set of events to the current state of a contract and return the post
	// events state
	public static ArrayList<ContractEvent> apply(ArrayList<ContractEvent> events, ContractModelProvider model,
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

		// determine maturity of the contract
		LocalDateTime maturity = model.getAs("maturityDate");

		if(CommonUtils.isNull(maturity)) {
			DayCountCalculator dayCounter = model.getAs("dayCountConvention");
			BusinessDayAdjuster timeAdjuster = model.getAs("businessDayConvention");
			double notionalPrincipal = model.getAs("notionalPrincipal");
			ArrayList<LocalDateTime> prAnchor = Arrays.stream(model.getAs("arrayCycleAnchorDateOfPrincipalRedemption").toString().replaceAll("\\[", "").replaceAll("]", "").split(","))
					.map(s -> {
						s = s.trim();
						return LocalDateTime.parse(s);
					}).collect(Collectors.toCollection(ArrayList::new));

			Integer[] prIncDec = Arrays.stream(model.getAs("arrayIncreaseDecrease").toString().replaceAll("\\[", "").replaceAll("]", "").trim().split(","))
					.map(d -> {
						if (d.equals("INC")) {
							return 1;
						} else {
							return -1;
						}
					}).toArray(Integer[]::new);

			Double[] prPayment = Arrays.stream(model.getAs("arrayNextPrincipalRedemptionPayment").toString().replaceAll("\\[", "").replaceAll("]", "").trim().split(",")).map(Double::parseDouble).toArray(Double[]::new);

			if (Objects.isNull(model.getAs("arrayCycleOfPrincipalRedemption"))) {
				maturity = prAnchor.get(prAnchor.size()-1);
			} else {
				String[] prCycle = Arrays.stream(model.getAs("arrayCycleOfPrincipalRedemption").toString().replaceAll("\\[", "").replaceAll("]", "").split(",")).map(String::trim).toArray(String[]::new);
				LocalDateTime t = model.getAs("statusDate");
				if (prCycle.length > 1) {
					double sum = 0.0;
					int index = 0;
					int noOfPrEvents = 0;
					Set<LocalDateTime> prSchedule;
					do {
						prSchedule = ScheduleFactory.createSchedule(prAnchor.get(index), prAnchor.get(index + 1), prCycle[index], model.getAs("endOfMonthConvention"), false);
						noOfPrEvents = (prSchedule.size() * prPayment[index] * prIncDec[index]) + notionalPrincipal + sum >= 0 ? prSchedule.size() : (int) ((notionalPrincipal + sum) / prPayment[index]);
						sum += noOfPrEvents * prIncDec[index] * prPayment[index];
						//ARPRCL, ARPRANX and ARINDEC must be the same size
						if (prAnchor.size()-2 == index) {
							noOfPrEvents = Math.abs((int) Math.ceil((sum + notionalPrincipal) / prPayment[index+1]));
							t = prAnchor.get(index+1);
							for (int i = 0; i < noOfPrEvents-1; i++) {
							t = t.plus(CycleUtils.parseTemporalAmount(prCycle[index+1]));
							}
							sum += noOfPrEvents * prIncDec[index+1] * prPayment[index+1];
							} else {
							index++;
							for (int i = 0; i < noOfPrEvents; i++) {
							t = t.plus(CycleUtils.parseTemporalAmount(prCycle[index - 1]));
							}
						}
					} while ((sum + notionalPrincipal) > 0);
				} else {
					int noOfPrEvents = (int) Math.ceil(notionalPrincipal / prPayment[0]);
					t = prAnchor.get(0);
					for (int i = 0; i < noOfPrEvents-1; i++) {
					t = t.plus(CycleUtils.parseTemporalAmount(prCycle[0]));
					}
				}
				maturity = timeAdjuster.shiftEventTime(t);
			}
		}
		return maturity;
	}

	private static StateSpace initStateSpace(ContractModelProvider model, LocalDateTime maturity)
			throws AttributeConversionException {
		StateSpace states = new StateSpace();

		// general states to be initialized
		states.statusDate = model.getAs("statusDate");
		states.notionalScalingMultiplier = 1;
		states.interestScalingMultiplier = 1;
		
		if(model.<LocalDateTime>getAs("initialExchangeDate").isAfter(model.getAs("statusDate"))){
            states.notionalPrincipal = 0.0;
            states.nominalInterestRate = 0.0;
            states.interestCalculationBaseAmount = 0.0;
        }else{
			states.notionalPrincipal = ContractRoleConvention.roleSign(model.getAs("contractRole"))
					* model.<Double>getAs("notionalPrincipal");
			states.nominalInterestRate = model.getAs("nominalInterestRate");
			states.accruedInterest = ContractRoleConvention.roleSign(model.getAs("contractRole"))
					* model.<Double>getAs("accruedInterest");
			states.feeAccrued = model.getAs("feeAccrued");
			if(InterestCalculationBase.NT.equals(model.getAs("interestCalculationBase"))){
                states.interestCalculationBaseAmount = states.notionalPrincipal; // contractRole applied at notionalPrincipal init
            }else{
                states.interestCalculationBaseAmount = ContractRoleConvention.roleSign(model.getAs("contractRole")) * model.<Double>getAs("interestCalculationBaseAmount");
            }
		}
		return states;
	}

}
