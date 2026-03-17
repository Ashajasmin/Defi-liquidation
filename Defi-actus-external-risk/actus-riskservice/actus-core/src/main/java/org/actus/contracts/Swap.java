/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */

package org.actus.contracts;

import org.actus.AttributeConversionException;
import org.actus.attributes.ContractModel;
import org.actus.attributes.ContractModelProvider;
import org.actus.events.ContractEvent;
import org.actus.events.EventFactory;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.pam.POF_PRD_PAM;
import org.actus.functions.pam.STF_PRD_PAM;
import org.actus.functions.stk.STF_PRD_STK;
import org.actus.functions.stk.STF_TD_STK;
import org.actus.functions.swaps.*;
import org.actus.states.StateSpace;
import org.actus.types.DeliverySettlement;
import org.actus.types.EventType;
import org.actus.types.ReferenceRole;
import org.actus.util.CommonUtils;
import org.actus.types.ContractReference;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the Swap payoff algorithm
 *
 * @see <a href="https://www.actusfrf.org"></a>
 */
public final class Swap {

    // forward projection of the entire lifecycle of the contract
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<ContractEvent>();
        //create children event schedule
        ContractModel firstLegModel = (ContractModel)model.<List<ContractReference>>getAs("contractStructure").stream().filter(c-> ReferenceRole.FIL.equals(c.referenceRole)).collect(Collectors.toList()).get(0).getObject();
        ContractModel secondLegModel = (ContractModel)model.<List<ContractReference>>getAs("contractStructure").stream().filter(c-> ReferenceRole.SEL.equals(c.referenceRole)).collect(Collectors.toList()).get(0).getObject();
        ArrayList<ContractEvent> firstLegSchedule = new ArrayList<>();
        ArrayList<ContractEvent> secondLegSchedule = new ArrayList<>();
        firstLegSchedule = ContractType.schedule(firstLegModel.getAs("maturityDate"),firstLegModel);
        secondLegSchedule = ContractType.schedule(secondLegModel.getAs("maturityDate"), secondLegModel);
        events.addAll(firstLegSchedule);
        events.addAll(secondLegSchedule);

        // purchase
        if (!CommonUtils.isNull(model.getAs("purchaseDate"))) {
            events.add(EventFactory.createEvent(model.getAs("purchaseDate"), EventType.PRD, model.getAs("currency"), new POF_PRD_SWAPS(), new STF_PRD_SWAPS(), model.getAs("contractID")));
        }
        // termination
        if (!CommonUtils.isNull(model.getAs("terminationDate"))) {
            ContractEvent termination = EventFactory.createEvent(
                    model.getAs("terminationDate"),
                    EventType.TD,
                    model.getAs("currency"),
                    new POF_TD_SWAPS(),
                    new STF_TD_STK(),
                    model.getAs("contractID")
            );
            events.removeIf(e -> e.compareTo(termination) == 1); // remove all post-termination events
            events.add(termination);
        }

        // remove all pre-status date events
        events.removeIf(e -> e.compareTo(EventFactory.createEvent(model.getAs("statusDate"), EventType.AD, model.getAs("currency"), null, null, model.getAs("contractID"))) == -1);

        // remove all post to-date events
        events.removeIf(e -> e.compareTo(EventFactory.createEvent(to, EventType.AD, model.getAs("currency"), null, null, model.getAs("contractID"))) == 1);

        // return events
        return events;
    }

    // apply a set of events to the current state of a contract and return the post events state
    public static ArrayList<ContractEvent> apply(ArrayList<ContractEvent> events,
                                                 ContractModelProvider model,
                                                 RiskFactorModelProvider observer) throws AttributeConversionException {

        //sort first and second leg events and remove from parent schedule
        ContractModel firstLegModel = (ContractModel)model.<List<ContractReference>>getAs("contractStructure").stream().filter(c-> ReferenceRole.FIL.equals(c.referenceRole)).collect(Collectors.toList()).get(0).getObject();
        ContractModel secondLegModel = (ContractModel)model.<List<ContractReference>>getAs("contractStructure").stream().filter(c-> ReferenceRole.SEL.equals(c.referenceRole)).collect(Collectors.toList()).get(0).getObject();
        ArrayList<ContractEvent> firstLegSchedule = events.stream().filter(event -> firstLegModel.getAs("contractID").equals(event.getContractID())).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<ContractEvent> secondLegSchedule = events.stream().filter(event -> secondLegModel.getAs("contractID").equals(event.getContractID())).collect(Collectors.toCollection(ArrayList::new));
        events.removeAll(firstLegSchedule);
        events.removeAll(secondLegSchedule);

        // apply shedule of children
        List<ContractEvent> firstLegEvents = ContractType.apply(firstLegSchedule,firstLegModel,observer);
        List<ContractEvent> secondLegEvents = ContractType.apply(secondLegSchedule,secondLegModel,observer);

        //add netted and unnetted events back to collection
        if(DeliverySettlement.S.equals(model.getAs("deliverySettlement"))){
            events.addAll(filterAndNettCongruentEvents(firstLegEvents,secondLegEvents, model.getAs("contractID")));
        }else{
            events.addAll(firstLegEvents);
            events.addAll(secondLegEvents);
        }

        // evaluate parent-events and netting-events
        events.forEach(event -> {
            if(event.getContractID().equals(model.getAs("contractID"))){
                if(event.eventType().equals(EventType.PRD) || event.eventType().equals(EventType.TD)){
                    StateSpace parentState = new StateSpace();
                    ArrayList<ContractEvent> fLEventsAtTimepoint = firstLegEvents.stream().filter(e -> e.eventTime().isEqual(event.eventTime())).collect(Collectors.toCollection(ArrayList::new));
                    ArrayList<ContractEvent> sLEventsAtTimepoint = secondLegEvents.stream().filter(e -> e.eventTime().isEqual(event.eventTime())).collect(Collectors.toCollection(ArrayList::new));
                    double flIpac;
                    double slIpac;
                    if(fLEventsAtTimepoint.isEmpty()){
                        flIpac = 0.0;
                    }else{
                        flIpac = fLEventsAtTimepoint.stream().anyMatch(e -> EventType.IP.equals(e.eventType())) ? 0.0 : fLEventsAtTimepoint.stream().filter(e -> EventType.PR.equals(e.eventType())).collect(Collectors.toList()).get(0).states().accruedInterest;
                    }
                    if(sLEventsAtTimepoint.isEmpty()){
                        slIpac = 0.0;
                    } else{
                        slIpac = sLEventsAtTimepoint.stream().anyMatch(e -> EventType.IP.equals(e.eventType())) ? 0.0 : sLEventsAtTimepoint.stream().filter(e -> EventType.PR.equals(e.eventType())).collect(Collectors.toList()).get(0).states().accruedInterest;
                    }
                    parentState.accruedInterest = flIpac + slIpac;
                    event.eval(parentState, model, observer, null, null);
                }else{
                    event.eval(null, null, null, null, null);
                }
            }
        });

        if (!CommonUtils.isNull(model.getAs("purchaseDate"))) {
            ContractEvent purchase = EventFactory.createEvent(
                    model.getAs("purchaseDate"),
                    EventType.PRD,
                    model.getAs("currency"),
                    new POF_PRD_SWAPS(),
                    new STF_PRD_STK(),
                    model.getAs("contractID")
            );
            events.removeIf(e -> e.compareTo(purchase) == -1); // remove all pre-purchase events
        }
        Collections.sort(events);
        //
        return events;
    }

    private static StateSpace initStateSpace(ContractModelProvider model, ContractEvent eventAtT0) throws AttributeConversionException {
        ContractModel firstLegModel = (ContractModel)model.<List<ContractReference>>getAs("contractStructure").stream().filter(c-> ReferenceRole.FIL.equals(c.referenceRole)).collect(Collectors.toList()).get(0).getObject();
        ContractModel secondLegModel = (ContractModel)model.<List<ContractReference>>getAs("contractStructure").stream().filter(c-> ReferenceRole.SEL.equals(c.referenceRole)).collect(Collectors.toList()).get(0).getObject();

        StateSpace states = CommonUtils.isNull(eventAtT0.states().statusDate) ? new StateSpace() : eventAtT0.states();
        states.statusDate = model.getAs("statusDate");
        states.contractPerformance = !CommonUtils.isNull(model.getAs("contractPerformance")) ? model.getAs("contractPerformance") : null;
        states.maturityDate = firstLegModel.<LocalDateTime>getAs("maturityDate").isAfter(secondLegModel.<LocalDateTime>getAs("maturityDate")) ? firstLegModel.getAs("maturityDate") : secondLegModel.getAs("maturityDate");
        states.accruedInterest = eventAtT0.states().accruedInterest;

        // return the initialized state space
        return states;
    }

    // private method that filters all types of Congruents events and creates "netting events,
    private static List<ContractEvent> filterAndNettCongruentEvents(List<ContractEvent> firstLegEvents, List<ContractEvent> secondLegEvents, String parentContractID){
        // sort the events according to their time sequence
        Collections.sort(firstLegEvents);
        Collections.sort(secondLegEvents);

        List<ContractEvent> events = new ArrayList<>();

        List<ContractEvent> listFirstLegIED = firstLegEvents.stream().filter(event -> event.eventType().equals(EventType.IED)).collect(Collectors.toList());
        List<ContractEvent> listSecondLegIED = secondLegEvents.stream().filter(event -> event.eventType().equals(EventType.IED)).collect(Collectors.toList());
        netSingularEvent(parentContractID, events, listFirstLegIED, listSecondLegIED);

        List<ContractEvent> listFirstLegMD = firstLegEvents.stream().filter(event -> event.eventType().equals(EventType.MD)).collect(Collectors.toList());
        List<ContractEvent> listSecondLegMD = secondLegEvents.stream().filter(event -> event.eventType().equals(EventType.MD)).collect(Collectors.toList());
        netSingularEvent(parentContractID, events, listFirstLegMD, listSecondLegMD);

        List<ContractEvent> firstLegPR = firstLegEvents.stream().filter(event -> event.eventType().equals(EventType.PR)).collect(Collectors.toList());
        List<ContractEvent> secondLegPR = secondLegEvents.stream().filter(event -> event.eventType().equals(EventType.PR)).collect(Collectors.toList());
        events.removeAll(firstLegPR);events.removeAll(secondLegPR);
        events = netCongruentEvents(firstLegPR,secondLegPR,events, parentContractID);

        List<ContractEvent> firstLegIP = firstLegEvents.stream().filter(event -> event.eventType().equals(EventType.IP)).collect(Collectors.toList());
        List<ContractEvent> secondLegIP = secondLegEvents.stream().filter(event -> event.eventType().equals(EventType.IP)).collect(Collectors.toList());
        events.removeAll(firstLegIP);events.removeAll(secondLegIP);
        events = netCongruentEvents(firstLegIP,secondLegIP,events, parentContractID);

        return events;
    }

    private static void netSingularEvent(String parentContractID, List<ContractEvent> events, List<ContractEvent> listFirstLeg, List<ContractEvent> listSecondLeg) {
        if (!listFirstLeg.isEmpty() && !listSecondLeg.isEmpty()) {
            ContractEvent firstLegEvent = listFirstLeg.get(0);
            ContractEvent secondLegEvent = listSecondLeg.get(0);
            if (firstLegEvent.eventTime().isEqual(secondLegEvent.eventTime())) {
                events.remove(firstLegEvent);
                events.remove(secondLegEvent);
                events.add(nettingEvent(firstLegEvent, secondLegEvent, parentContractID));
            }
        }
    }

    //private method that filters congruent events and creates "netting" events of single types
    private static List<ContractEvent> netCongruentEvents(List<ContractEvent> firstLegEvents, List<ContractEvent> secondLegEvents, List<ContractEvent> events, String parentContractID){
        Iterator<ContractEvent> firstLegIt = firstLegEvents.iterator();
        Iterator<ContractEvent> secondLegIt = secondLegEvents.iterator();
        while(firstLegIt.hasNext() && secondLegIt.hasNext()) {
            ContractEvent tempFirstLegEvent = firstLegIt.next();
            while(secondLegIt.hasNext()){
                ContractEvent tempSecondLegEvent = secondLegIt.next();
                if(tempFirstLegEvent.eventTime().isEqual(tempSecondLegEvent.eventTime())){
                    events.add(nettingEvent(tempFirstLegEvent,tempSecondLegEvent, parentContractID));
                    secondLegIt.remove();
                    firstLegIt.remove();
                    break;
                }
            }
            secondLegIt = secondLegEvents.iterator();
        }
        events.addAll(firstLegEvents);events.addAll(secondLegEvents);
        return events;
    }
    // private method that allows creating a "netting" event from two events to be netted
    private static ContractEvent nettingEvent(ContractEvent e1, ContractEvent e2, String parentContractID) {
        ContractEvent netting = EventFactory.createEvent(e1.eventTime(), e1.eventType(), e1.currency(), new POF_NET_SWAPS(e1,e2), new STF_NET_SWAPS(e1,e2), parentContractID);
        return(netting);
    }

}
