package org.actus.contracts;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.actus.AttributeConversionException;
import org.actus.attributes.ContractModel;
import org.actus.attributes.ContractModelProvider;
import org.actus.events.ContractEvent;
import org.actus.events.EventFactory;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.bcs.POF_PRD_BCS;
import org.actus.functions.bcs.POF_TD_BCS;
import org.actus.functions.bcs.STF_ME_BCS;
import org.actus.functions.bcs.STF_TD_BCS;
import org.actus.functions.optns.POF_PRD_OPTNS;
import org.actus.functions.pam.POF_AD_PAM;
import org.actus.functions.pam.POF_IED_PAM;
import org.actus.functions.pam.STF_IED_PAM;
import org.actus.functions.stk.STF_PRD_STK;
import org.actus.states.StateSpace;
import org.actus.time.ScheduleFactory;
import org.actus.types.ContractReference;
import org.actus.types.ContractTypeEnum;
import org.actus.types.EventType;
import org.actus.types.ReferenceRole;
import org.actus.util.CommonUtils;

public final class BoundaryControlledSwitch {

	// Forward projection of the entire lifecycle of the contract
	public static ArrayList<ContractEvent> schedule(LocalDateTime to, ContractModelProvider model)
			throws AttributeConversionException {

		ArrayList<ContractEvent> events = new ArrayList<>();

		// Purchase date event of master contract
		if (!CommonUtils.isNull(model.getAs("purchaseDate"))) {
			events.add(EventFactory.createEvent(model.getAs("purchaseDate"), EventType.PRD, model.getAs("currency"),
					new POF_PRD_OPTNS(), new STF_PRD_STK(), model.getAs("contractID")));
		}

		// Raw monitoring events
		Set<ContractEvent> monitoringEvents = EventFactory.createEvents(
				ScheduleFactory.createSchedule(model.getAs("boundaryMonitoringAnchorDate"),
						model.getAs("boundaryMonitoringEndDate"), model.getAs("boundaryMonitoringCycle"),
						model.getAs("endOfMonthConvention"), true),
				EventType.ME, model.getAs("currency"), new POF_AD_PAM(), new STF_ME_BCS(),
				model.getAs("businessDayConvention"), model.getAs("contractID"));

		events.addAll(monitoringEvents);

		return events;
	}

	// Apply a set of events to the current state of a contract and return the post
	public static ArrayList<ContractEvent> apply(ArrayList<ContractEvent> events, ContractModelProvider model,
			RiskFactorModelProvider observer) throws AttributeConversionException {

		// Initialize state space per status date
		StateSpace states = initStateSpace(model);

		// Sort the events according to their time sequence
		Collections.sort(events);

		// Apply events according to their time sequence to current state
		events.forEach(e -> e.eval(states, model, observer, model.getAs("dayCountConvention"),
				model.getAs("businessDayConvention")));

		// Remove monitoring events
		events.removeIf(e -> e.eventType().equals(EventType.ME));

		// Activating child legs based on boundaryEffect
		if (states.boundaryCrossedFlag) {
			switch ((String) model.getAs("boundaryEffect")) {
			case "knockINFirstLeg":
				states.boundaryLeg1ActiveFlag = true;
				states.boundaryLeg2ActiveFlag = false;
				break;
			case "knockINSecondLeg":
				states.boundaryLeg2ActiveFlag = true;
				states.boundaryLeg1ActiveFlag = false;
				break;
			case "knockOUTCurrent":
				states.boundaryLeg1ActiveFlag = false;
				states.boundaryLeg2ActiveFlag = false;
				break;
			default:
				break;
			}
		}

		// First leg model
		ContractModel firstLegModel = (ContractModel) model.<List<ContractReference>>getAs("contractStructure").stream()
				.filter(c -> ReferenceRole.FIL.equals(c.referenceRole)).collect(Collectors.toList()).get(0).getObject();
		ArrayList<ContractEvent> firstLegSchedule = new ArrayList<>();
		
		// Second leg model
		List<ContractReference> secondLeg = model.<List<ContractReference>>getAs("contractStructure").stream()
				.filter(c -> ReferenceRole.SEL.equals(c.referenceRole)).collect(Collectors.toList());
		ArrayList<ContractEvent> secondLegSchedule = new ArrayList<>();
		ContractModel secondLegModel = secondLeg.isEmpty() ? null : (ContractModel) secondLeg.get(0).getObject();

		// Create children event schedule based on boundary conditions
		if (states.boundaryLeg1ActiveFlag) {

			firstLegSchedule = ContractType.schedule(firstLegModel.getAs("maturityDate"), firstLegModel);
			if (!firstLegModel.getAs("contractType").equals(ContractTypeEnum.PAM)) {
				firstLegSchedule
						.add(EventFactory.createEvent(states.statusDate, EventType.PRD, firstLegModel.getAs("currency"),
								new POF_PRD_BCS(), new STF_PRD_STK(), firstLegModel.getAs("contractID")));
			} else {
				firstLegSchedule.removeIf(e -> e.eventType().equals(EventType.IED));
				firstLegSchedule
						.add(EventFactory.createEvent(states.statusDate, EventType.IED, firstLegModel.getAs("currency"),
								new POF_IED_PAM(), new STF_IED_PAM(), firstLegModel.getAs("contractID")));
			}
			firstLegSchedule.removeIf(e -> e.eventTime().isBefore(states.statusDate));

			// Apply schedule of children
			List<ContractEvent> firstLegEvents = ContractType.apply(firstLegSchedule, firstLegModel, observer);
			events.addAll(firstLegEvents);

		} else if (!states.boundaryLeg1ActiveFlag
				&& !CommonUtils.isNull(model.<ReferenceRole>getAs("boundaryLegInitiallyActive"))
				&& model.<ReferenceRole>getAs("boundaryLegInitiallyActive").equals(ReferenceRole.FIL)) {

			firstLegSchedule = ContractType.schedule(firstLegModel.getAs("maturityDate"), firstLegModel);
			if (!firstLegModel.getAs("contractType").equals(ContractTypeEnum.PAM)) {
				firstLegSchedule.add(EventFactory.createEvent(model.getAs("purchaseDate"), EventType.PRD,
						firstLegModel.getAs("currency"), new POF_PRD_BCS(), new STF_PRD_STK(),
						firstLegModel.getAs("contractID")));
			}
			ContractEvent tdEvent = EventFactory.createEvent(states.statusDate, EventType.TD,
					firstLegModel.getAs("currency"), new POF_TD_BCS(), new STF_TD_BCS(),
					firstLegModel.getAs("contractID"));

			firstLegSchedule.removeIf(e -> e.compareTo(tdEvent) == 1);
			firstLegSchedule.add(tdEvent);

			// Apply schedule of children
			List<ContractEvent> firstLegEvents = ContractType.apply(firstLegSchedule, firstLegModel, observer);
			events.addAll(firstLegEvents);
		}

		if (states.boundaryLeg2ActiveFlag) {

			secondLegSchedule = ContractType.schedule(secondLegModel.getAs("maturityDate"), secondLegModel);
			if (!secondLegModel.getAs("contractType").equals(ContractTypeEnum.PAM)) {
				secondLegSchedule.add(
						EventFactory.createEvent(states.statusDate, EventType.PRD, secondLegModel.getAs("currency"),
								new POF_PRD_BCS(), new STF_PRD_STK(), secondLegModel.getAs("contractID")));
			} else {
				secondLegSchedule.removeIf(e -> e.eventType().equals(EventType.IED));
				secondLegSchedule.add(
						EventFactory.createEvent(states.statusDate, EventType.IED, secondLegModel.getAs("currency"),
								new POF_IED_PAM(), new STF_IED_PAM(), secondLegModel.getAs("contractID")));
			}
			firstLegSchedule.removeIf(e -> e.eventTime().isBefore(states.statusDate));
			
			// Apply schedule of children
			List<ContractEvent> secondLegEvents = ContractType.apply(secondLegSchedule, secondLegModel, observer);
			events.addAll(secondLegEvents);

		} else if (!states.boundaryLeg2ActiveFlag
				&& !CommonUtils.isNull(model.<ReferenceRole>getAs("boundaryLegInitiallyActive"))
				&& model.<ReferenceRole>getAs("boundaryLegInitiallyActive").equals(ReferenceRole.SEL)) {

			if (!secondLegModel.getAs("contractType").equals(ContractTypeEnum.PAM)) {
				secondLegSchedule.add(EventFactory.createEvent(model.getAs("purchaseDate"), EventType.PRD,
						secondLegModel.getAs("currency"), new POF_PRD_BCS(), new STF_PRD_STK(),
						secondLegModel.getAs("contractID")));
			}
			ContractEvent tdEvent = EventFactory.createEvent(states.statusDate, EventType.TD,
					secondLegModel.getAs("currency"), new POF_TD_BCS(), new STF_TD_BCS(),
					secondLegModel.getAs("contractID"));

			secondLegSchedule.removeIf(e -> e.compareTo(tdEvent) == 1);
			secondLegSchedule.add(tdEvent);

			secondLegSchedule = ContractType.schedule(secondLegModel.getAs("maturityDate"), secondLegModel);

			// Apply schedule of children
			List<ContractEvent> secondLegEvents = ContractType.apply(secondLegSchedule, secondLegModel, observer);
			events.addAll(secondLegEvents);
		}

		// Termination of master contract
		if (states.boundaryCrossedFlag && !model.getAs("boundaryEffect").equals("knockINFirstLeg")) {
			events.add(EventFactory.createEvent(states.statusDate, EventType.TD, model.getAs("currency"),
					new POF_TD_BCS(), new STF_TD_BCS(), model.getAs("contractID")));
		} else {
			events.add(EventFactory.createEvent(model.getAs("boundaryMonitoringEndDate"), EventType.TD,
					model.getAs("currency"), new POF_TD_BCS(), new STF_TD_BCS(), model.getAs("contractID")));
		}

		// Sort the events according to their time sequence
		Collections.sort(events);

		// Return post events states
		return events;
	}

	private static StateSpace initStateSpace(ContractModelProvider model) {
		StateSpace states = new StateSpace();

		// Initialize state variables
		states.statusDate = model.getAs("statusDate");
		states.contractPerformance = model.getAs("contractPerformance");
		states.boundaryCrossedFlag = false;
		states.boundaryMonitoringFlag = true;

		if (!CommonUtils.isNull(model.getAs("boundaryLegInitiallyActive"))) {
			switch (model.<ReferenceRole>getAs("boundaryLegInitiallyActive")) {
			case FIL:
				states.boundaryLeg1ActiveFlag = true;
				states.boundaryLeg2ActiveFlag = false;
				break;
			case SEL:
				states.boundaryLeg2ActiveFlag = true;
				states.boundaryLeg1ActiveFlag = false;
				break;
			default:
				states.boundaryLeg1ActiveFlag = false;
				states.boundaryLeg2ActiveFlag = false;
				break;

			}
		} else {
			states.boundaryLeg1ActiveFlag = false;
			states.boundaryLeg2ActiveFlag = false;
		}

		// Return the initialized state space
		return states;
	}

}
