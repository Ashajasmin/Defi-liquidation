/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.events;

import org.actus.functions.PayOffFunction;
import org.actus.functions.StateTransitionFunction;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.types.EventType;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Factory of {@link ContractEvent}s from a schedule of times
 * <p>
 * The {@code EventFactory} allows to convert plain schedule times to {@link ContractEvent}s.
 * <p>
 * The times provided are expected to be plain schedule-times, i.e. the output of 
 * {@link ScheduleFactory}, and thus unadjusted for the business day convention.
 * <p>
 * The class is a utility class with only static methods and no constructor, i.e. instances
 * of this class cannot be created but its methods only be accessed in a static way.  
 */
public final class EventFactory {

    // this is a utility class so no need for instantiation
	private EventFactory() {
    }
    
  /**
   * Create a single {@link ContractEvent}
   * 
   * @param scheduleTime the schedule time
   * @param eventType the event type
   * @param currency the event currency
   * @param payOff the event pay-off function
   * @param stateTrans the event state-transition function
   * @param contractID
   * @return
   */
    public static ContractEvent createEvent(LocalDateTime scheduleTime, EventType eventType, String currency, PayOffFunction payOff, StateTransitionFunction stateTrans, String contractID) {
        return new ContractEvent(scheduleTime, scheduleTime, eventType, currency, payOff, stateTrans, contractID);
    }
    
      /**
   * Create a single {@link ContractEvent} shifting the event time according to a business day convention
   * 
   * @param scheduleTime the schedule time
   * @param eventType the event type
   * @param currency the event currency
   * @param payOff the event pay-off function
   * @param stateTrans the event state-transition function
   * @param convention the business day convention to be used
   * @param contractID
       * @return
   */
    public static ContractEvent createEvent(LocalDateTime scheduleTime, EventType eventType, String currency, PayOffFunction payOff, StateTransitionFunction stateTrans, BusinessDayAdjuster convention, String contractID) {
        return new ContractEvent(scheduleTime, convention.shiftEventTime(scheduleTime), eventType, currency, payOff, stateTrans, contractID);
    }
    
  /**
   * Create a series of {@link ContractEvent}s from a times-schedule
   * 
   * @param eventSchedule an unordered set of schedule times
   * @param eventType the event type
   * @param currency the event currency
   * @param payOff the event pay-off function
   * @param stateTrans the event state-transition function
   * @param contractID
   * @return an unordered set of contract events
   */
    public static Set<ContractEvent> createEvents(Set<LocalDateTime> eventSchedule, EventType eventType, String currency, PayOffFunction payOff, StateTransitionFunction stateTrans, String contractID) {
        Set<ContractEvent> events = new HashSet<ContractEvent>(eventSchedule.size());
        Iterator<LocalDateTime> iterator = eventSchedule.iterator();
        LocalDateTime time;
        
        while(iterator.hasNext()) {
            time = iterator.next();
            events.add(new ContractEvent(time, time, eventType, currency, payOff, stateTrans, contractID));
        }
        
        return events;
    }
    
  /**
   * Create a series of {@link ContractEvent}s from a times-schedule shifting the event times according to a business day convention
   * 
   * @param eventSchedule an unordered set of schedule times
   * @param eventType the event type
   * @param currency the event currency
   * @param payOff the event pay-off function
   * @param stateTrans the event state-transition function
   * @param convention the business day convention to be used
   * @param contractID
   * @return an unordered set of contract events
   */
    public static Set<ContractEvent> createEvents(Set<LocalDateTime> eventSchedule, EventType eventType, String currency, PayOffFunction payOff, StateTransitionFunction stateTrans, BusinessDayAdjuster convention, String contractID) {
        Set<ContractEvent> events = new HashSet<ContractEvent>(eventSchedule.size());
        Iterator<LocalDateTime> iterator = eventSchedule.iterator();
        LocalDateTime time;
        
        while(iterator.hasNext()) {
            time = iterator.next();
            events.add(new ContractEvent(time, convention.shiftEventTime(time), eventType, currency, payOff, stateTrans, contractID));
        }
        
        return events;
    }
}
