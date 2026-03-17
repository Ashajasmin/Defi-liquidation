/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.functions;

import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.states.StateSpace;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.conventions.businessday.BusinessDayAdjuster;

import java.time.LocalDateTime;

/**
 * Component that updates the {@link StateSpace} for a specific {@link ContractEvent}
 * <p>
 * This component implements the state-transiton function for a specific {@link ContractEvent} 
 * (and {@link ContractType}). Upon evaluation of a {@link ContractEvent} this component
 * and {@link PayOffFunction} are evaluated through method {@code lifecycle}. Return
 * value of this component's {@code lifecycle} function gives the post-event contract states for
 * the respective {@link ContractEvent}.
 */
public abstract interface StateTransitionFunction {
    
    /**
     * Evaluate the function
     * 
     * @param time the schedule time of this particular event
     * @param states the current state of conract states
     * @param model the model containing parsed contract attributes
     * @param riskFactorModel an external market model
     * @param dayCounter the day count convention used to calculate day count fractions
     * @param timeAdjuster the business day convention used to shift the schedule time
     * @return an array of post-event states of numerical contract states
     */
    public StateSpace eval(LocalDateTime time, StateSpace states,
    ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster);
}
