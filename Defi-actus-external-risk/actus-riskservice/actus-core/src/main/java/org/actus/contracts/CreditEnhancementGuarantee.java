/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.contracts;

import org.actus.AttributeConversionException;
import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.events.ContractEvent;
import org.actus.events.EventFactory;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.ceg.*;
import org.actus.functions.optns.*;
import org.actus.states.StateSpace;
import org.actus.time.ScheduleFactory;
import org.actus.types.ContractReference;
import org.actus.types.CreditEventTypeCovered;
import org.actus.types.EventType;
import org.actus.types.GuaranteedExposure;
import org.actus.types.ReferenceRole;
import org.actus.util.CommonUtils;
import org.actus.util.CycleUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the Credit Enhancement Guarantee contract algorithm
 *
 * @see <a https://www.actusfrf.org"></a>
 */
public class CreditEnhancementGuarantee {
    // forward projection of the entire lifecycle of the contract
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<>();

        // determine maturity date
        LocalDateTime maturity = maturity(model);
        
        // purchase
        if (!CommonUtils.isNull(model.getAs("purchaseDate"))) {
            events.add(EventFactory.createEvent(model.getAs("purchaseDate"), EventType.PRD, model.getAs("currency"), new POF_PRD_OPTNS(), new STF_PRD_CEG(), model.getAs("contractID")));
        }

        // fees (if specified)
        if(!(CommonUtils.isNull(model.getAs("feeRate")) || model.<Double>getAs("feeRate") == 0.0)){
            LocalDateTime startDate;
            LocalDateTime endDate;
            if(CommonUtils.isNull(model.getAs("cycleAnchorDateOfFee")) && CommonUtils.isNull(model.getAs("cycleOfFee"))){
                startDate = null;
            }else if(CommonUtils.isNull(model.getAs("cycleAnchorDateOfFee"))){
                startDate = model.<LocalDateTime>getAs("purchaseDate").plus(CycleUtils.parseTemporalAmount(model.getAs("cycleOfFee") + "L0"));
            }else{
                startDate = model.getAs("cycleAnchorDateOfFee");
            }
            if(CommonUtils.isNull(model.getAs("exerciseDate"))){
                endDate = maturity;
            }else {
                endDate = model.getAs("exerciseDate");
            }
            events.addAll(EventFactory.createEvents(
                    ScheduleFactory.createSchedule(
                            startDate,
                            endDate,
                            model.getAs("cycleOfFee"),
                            model.getAs("endOfMonthConvention"),
                            false),
                    EventType.FP,
                    model.getAs("currency"),
                    new POF_FP_CEG(),
                    new STF_FP_CEG(),
                    model.getAs("businessDayConvention"),
                    model.getAs("contractID"))
            );
        }

        // maturity
        if(CommonUtils.isNull(model.getAs("exerciseDate"))){
            events.add(EventFactory.createEvent(maturity, EventType.MD, model.getAs("currency"), new POF_MD_CEG(), new STF_MD_CEG(), model.getAs("contractID")));
        }

        //exercise
        if(!CommonUtils.isNull(model.getAs("exerciseDate"))){
            events.add(EventFactory.createEvent(model.getAs("exerciseDate"), EventType.XD, model.getAs("currency"), new POF_XD_OPTNS(), new STF_XD_CEG(), model.getAs("contractID")));
            events.add(EventFactory.createEvent(model.<LocalDateTime>getAs("exerciseDate").plus(CycleUtils.parseTemporalAmount(model.getAs("settlementPeriod"))), EventType.STD, model.getAs("currency"), new POF_STD_CEG(), new STF_STD_CEG(), model.getAs("businessDayConvention"), model.getAs("contractID")));
        }
        return events;
    }

    // apply a set of events to the current state of a contract and return the post events state
    public static ArrayList<ContractEvent> apply(ArrayList<ContractEvent> events,
                                                 ContractModelProvider model,
                                                 RiskFactorModelProvider observer) throws AttributeConversionException {
        // determine maturity date
        LocalDateTime maturity = maturity(model);
        events = addExternalXDEvent(model, events, observer, maturity);
        // initialize state space per status date
        StateSpace states = initStateSpace(model, observer, maturity);
        // sort the events according to their time sequence
        Collections.sort(events);

        // apply events according to their time sequence to current state
        events.forEach(e -> {
            e.eval(states, model, observer, model.getAs("dayCountConvention"), model.getAs("businessDayConvention"));
        });

        // return post events states
        return events;
    }

    // determine maturity of the contract
    private static LocalDateTime maturity(ContractModelProvider model) {
        LocalDateTime maturityDate = null;
        if(!CommonUtils.isNull(model.getAs("maturityDate"))){
            maturityDate = model.getAs("maturityDate");
        } else{
            List<ContractReference> coveredContractRefs = model.<List<ContractReference>>getAs("contractStructure").stream().filter(ref -> ref.referenceRole.equals(ReferenceRole.COVE)).collect(Collectors.toList());
            List<LocalDateTime> maturityDates = new ArrayList<>();
            coveredContractRefs.forEach(c -> {
                maturityDates.add(LocalDateTime.parse(c.getContractAttribute("maturityDate")));
            });
            Collections.sort(maturityDates);
            maturityDate = maturityDates.get(maturityDates.size()-1);
        }
        return maturityDate;
    }

    public static StateSpace initStateSpace(ContractModelProvider model, RiskFactorModelProvider observer, LocalDateTime maturity) throws AttributeConversionException {
        StateSpace states = new StateSpace();
        states.maturityDate = maturity;
        states.statusDate = model.getAs("statusDate");

        if(states.statusDate.isAfter(states.maturityDate)){
            states.notionalPrincipal = 0.0;
        }else if(!CommonUtils.isNull(model.<Double>getAs("notionalPrincipal"))){
            states.notionalPrincipal = model.<Double>getAs("coverageOfCreditEnhancement")
                    * ContractRoleConvention.roleSign(model.getAs("contractRole"))
                    * model.<Double>getAs("notionalPrincipal");
        } else{
            states.notionalPrincipal = CreditEnhancementGuarantee.calculateNotionalPrincipal(states,model,observer,states.statusDate);
        }

        if(CommonUtils.isNull(model.getAs("feeRate"))){
            states.feeAccrued = 0.0;
        } else if(!CommonUtils.isNull(model.getAs("feeAccrued"))){
            states.feeAccrued = model.getAs("feeAccrued");
        }//TODO: implement last two possible initialization

        states.exerciseAmount = model.getAs("exerciseAmount");
        states.exerciseDate = model.getAs("exerciseDate");
        // return the initialized state space
        return states;
    }

    public static Double calculateNotionalPrincipal(StateSpace states, ContractModelProvider model, RiskFactorModelProvider observer, LocalDateTime time){
        List<ContractReference> coveredContractRefs = model.<List<ContractReference>>getAs("contractStructure").stream().filter(ref -> ref.referenceRole.equals(ReferenceRole.COVE)).collect(Collectors.toList());
        List<StateSpace> statesAtTimePoint = coveredContractRefs.stream().map(c -> c.getStateSpaceAtTimepoint(time,observer)).collect(Collectors.toList());

        if(GuaranteedExposure.NO.equals(model.getAs("guaranteedExposure"))){
            states.notionalPrincipal =
                    model.<Double>getAs("coverageOfCreditEnhancement")
                            * ContractRoleConvention.roleSign(model.getAs("contractRole"))
                            * statesAtTimePoint.stream().map(s -> s.notionalPrincipal).reduce(0.0, Double::sum);
        }else if(GuaranteedExposure.NI.equals(model.getAs("guaranteedExposure"))){
            states.notionalPrincipal =
                    model.<Double>getAs("coverageOfCreditEnhancement")
                            * ContractRoleConvention.roleSign(model.getAs("contractRole"))
                            * (statesAtTimePoint.stream().map(s -> s.notionalPrincipal).reduce(0.0, Double::sum)
                            + statesAtTimePoint.stream().map(s -> s.accruedInterest).reduce(0.0, Double::sum))
            ;
        }else{
            List<String> marketObjectCodesOfUnderlying = coveredContractRefs.stream().map(c -> c.getContractAttribute("marketObjectCode")).collect(Collectors.toList());
            states.notionalPrincipal =
                    model.<Double>getAs("coverageOfCreditEnhancement")
                            * ContractRoleConvention.roleSign(model.getAs("contractRole"))
                            * marketObjectCodesOfUnderlying.stream().map(s -> observer.stateAt(s,time,states,model,true)).reduce(0.0, Double::sum);
        }
        return states.notionalPrincipal;
    }

    private static ArrayList<ContractEvent> addExternalXDEvent(ContractModelProvider model, ArrayList<ContractEvent> events, RiskFactorModelProvider observer, LocalDateTime maturity){
        List<String> contractIdentifiers = model.<List<ContractReference>>getAs("contractStructure").stream().map(c -> c.getContractAttribute("contractID")).collect(Collectors.toList());
        CreditEventTypeCovered creditEventTypeCovered = model.<CreditEventTypeCovered[]>getAs("creditEventTypeCovered")[0];
        // fetch observed events from external data observer
        Set<ContractEvent> observedEvents = observer.events(model);
        // filter relevant credit events:
        // - emitted by any of the covered contracts
        // - emitted before maturity of the guarantee
        // - credit event type that is actually covered under the guarantee
        List<ContractEvent> ceEvents = observedEvents.stream().filter(e -> contractIdentifiers.contains(e.getContractID()) && 
                                                                            !maturity.isBefore(e.eventTime()) &&
                                                                            e.states().contractPerformance.toString().equals(creditEventTypeCovered.toString())).collect(Collectors.toList());
        if(ceEvents.size() > 0 ){
            ContractEvent ceEvent = ceEvents.get(0);
            events = events.stream().filter(e -> e.eventType() != EventType.MD).collect(Collectors.toCollection(ArrayList::new));
            events.add(EventFactory.createEvent(ceEvent.eventTime(), EventType.XD, model.getAs("currency"), new POF_XD_OPTNS(), new STF_XD_CEG(), model.getAs("contractID")));
            ContractEvent std = EventFactory.createEvent(ceEvent.eventTime().plus(CycleUtils.parseTemporalAmount(model.getAs("settlementPeriod"))), EventType.STD, model.getAs("currency"), new POF_STD_CEG(), new STF_STD_CEG(), model.getAs("businessDayConvention"), model.getAs("contractID"));
            events.add(std);
        }
        return events;
    }
}
