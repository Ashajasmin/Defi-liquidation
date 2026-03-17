/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.contracts;

import org.actus.AttributeConversionException;
import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.events.ContractEvent;
import org.actus.states.StateSpace;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.conventions.daycount.DayCountCalculator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents the Cash payoff algorithm
 * 
 * @see <a https://www.actusfrf.org"></a>
 */
public final class Cash {

    // compute next n non-contingent events
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        return new ArrayList<ContractEvent>();
    }

    // apply a set of events to the current state of a contract and return the post events state
    public static ArrayList<ContractEvent> apply(ArrayList<ContractEvent> events,
                                                 ContractModelProvider model,
                                                 RiskFactorModelProvider observer) throws AttributeConversionException {

        // initialize state space per status date
        StateSpace states = new StateSpace();
        states.statusDate = model.getAs("statusDate");
        states.notionalPrincipal = ContractRoleConvention.roleSign(model.getAs("contractRole")) * model.<Double>getAs("notionalPrincipal");

        // sort the events according to their time sequence
        Collections.sort(events);

        // apply events according to their time sequence to current state
        events.forEach(e -> e.eval(states, model, observer, new DayCountCalculator("AA", null), new BusinessDayAdjuster(null, null)));

        // return evaluated events
        return events;
    }

}
