package org.actus.functions.bcs;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;
import org.actus.types.ContractReference;
import org.actus.types.ReferenceRole;

public class STF_ME_BCS implements StateTransitionFunction {

	@Override
	public StateSpace eval(LocalDateTime time, StateSpace states, ContractModelProvider model,
			RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {

		if(states.boundaryMonitoringFlag) {
			ContractReference contractReference = model.<ArrayList<ContractReference>>getAs("contractStructure").stream()
					.filter((e -> e.referenceRole.equals(ReferenceRole.externalReferenceIndex))).findFirst().get();

			double cbv = riskFactorModel.stateAt((String)contractReference.getObject(), time, states, model,true);
	        
	        if(model.getAs("boundaryDirection").equals("DECR") && cbv <= model.<Double>getAs("boundaryValue") 
	        		|| model.getAs("boundaryDirection").equals("INCR") && cbv >= model.<Double>getAs("boundaryValue")) {
	        	
	        	// Update state space
	        	states.boundaryMonitoringFlag = false;
	        	states.boundaryCrossedFlag = true;
	            states.statusDate = time;
	        }

		}

		// Return post-event-states
        return StateSpace.copyStateSpace(states);
	}

}
