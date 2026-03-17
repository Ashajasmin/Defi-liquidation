/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.externals;

import org.actus.attributes.ContractModelProvider;
import org.actus.events.ContractEvent;
import org.actus.states.StateSpace;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A representation of an external Risk Factor Observer
 * <p>
 * Generally, the payoff of financial instruments is evaluated in the context of an environment that
 * embodies the dynamics (and therewith future states) of all relevant stochastic risk factors - or what
 * we call the Risk Factor Observer. Thus, method {@code ContractType.lifecycle} produces
 * a series of Risk Factor state contingent {@link ContractEvent}s. An external {@code RiskFactorModelProvider} 
 * implements this dynamics and allows to retrieve the state of all defined risk factors identified
 * through a unique {@code id} at any future time.
 * <p>
 * Note, all sorts of external variables on which the payoff of a {@link ContractType} is conditioned
 * must be implemented in the {@code RiskFactorModelProvider}. In particular, any source of market risk,
 * credit risk, or behavioral risk needs to be represented therein and identifiable through a unique ID.
 * Examples of such risk factors are the reference market rate linked to through contract attribute
 * {@code MarketObjectCodeOfRateReset}, the default indicator of a legal entity linked to e.g. through
 * attribute {@code LegalEntityIDCounterparty}, or the market value of an underlying linked to through
 * attribute {@code ContractID} of the underlying.
 * <p>
 * @see <a href="https://www.actusfrf.org">ACTUS Website</a>
 */
public abstract interface RiskFactorModelProvider {
  
  /**
   * Returns the set of unique risk factor IDs
   * 
   * @return set of unique risk factor IDs
   */
  public Set<String> keys();

  /**
   * Returns the set of event times for a particular risk factor
   *
   * The default implementation returns an empty set of events.
   *
   * @param attributes the attributes of the contract evaluating the events
   * @return set of non-scheduled (contingent) contract events
   */
  default public Set<ContractEvent> events(ContractModelProvider attributes) {
    return new HashSet<ContractEvent>();
  }
  
  /**
   * Returns the state of a particular risk factor at a future time
   * 
   * @param id identifier of the risk factor
   * @param time future time for which to return the risk factor's state
   * @param states the inner states of the contract as per @code{time} argument of the method
   * @param attributes the attributes of the contract evaluating the risk factor state
   * @return double the state of the risk factor
   */
  public double stateAt(String id, LocalDateTime time, StateSpace states, ContractModelProvider attributes, boolean isMarket);

}
