/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.events;

import org.actus.types.EventType;

/**
 * Component that represents the sequence of {@link ContractEvent}s at a certain time
 * <p>
 * Various {@link ContractEvent}s happen during the lifetime of a {@ContractType}. Some
 * events may happen at exactly the same point in time. In this case, the {@code EventSequence}
 * defines the sequence in which the events are being processed. Therefore, events are ordered
 * according to their time and processing order (i.e. event sequence) represented by a time
 * offset in the {@link EventSeries}. 
 * <p>
 * This component contains all definitions of event sequence time offsets which
 * are being used when ordering events in the {@link EventSeries}.
 */
public final class EventSequence {
  
  private EventSequence() {
    // do nothing used in static sense only
  }
  
  	/**
	 * Returns the upper bound on the time offsets for the event sequence
	 * 
	 */
  public static int upperBound() {
    return 900;
  }
  
  	/**
	 * Returns the time offset according to the event sequence for a particular event type
	 * 
	 * @param eventType the event type for which to return the time offset
	 * @return the time offset
	 */
  public static int timeOffset(EventType eventType) {
      int offset = 0;
switch (eventType) {
    case IED:
        offset = 20;
        break;
    case PR:
        offset = 30;
        break;
    case IP:
        offset = 40;
        break;
    case IPFX:
        offset = 40;
        break;
    case IPFL:
        offset = 45;
        break;
    case IPCI:
        offset = 40;
        break;
      case FP:
        offset = 60;
        break;
      case DV:
        offset = 70;
        break;
      case MR:
        offset = 80;
        break;
      case RRF:
        offset = 100;
        break;
      case RR:
        offset = 100;
        break;
     case PRF:
        offset = 105;
        break;
      case SC:
        offset = 110;
        break;
      case IPCB:
        offset = 120;
        break;
      case PRD:
        offset = 130;
        break;
      case TD:
        offset = 140;
        break;
      case MD:
        offset = 150;
        break;
    case XD:
        offset = 160;
        break;
    case STD:
        offset = 170;
        break;
    case AD:
        offset = 950;
        break;
    case ME:
        offset = 180;
        break;
    case AFD:
    	offset = 190;
    	break;
}
    return offset;
    }

}
