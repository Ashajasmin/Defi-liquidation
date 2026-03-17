/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.contracts;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.actus.attributes.ContractModel;
import org.actus.events.ContractEvent;
import org.actus.events.EventFactory;
import org.actus.functions.pam.POF_AD_PAM;
import org.actus.functions.pam.STF_AD_PAM;
import org.actus.testutils.ContractTestUtils;
import org.actus.testutils.DataObserver;
import org.actus.testutils.ObservedDataSet;
import org.actus.testutils.ResultSet;
import org.actus.testutils.TestData;
import org.actus.types.EventType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class AD0Test {
	@TestFactory
	public Stream<DynamicTest> test() {
		String testFile = "./src/test/resources/actus/actus-tests-ad0.json";

		// read tests from file
		Map<String, TestData> tests = ContractTestUtils.readTests(testFile);

		// get ids of tests
		Set<String> testIds = tests.keySet();

		// go through test-id and perform test
		// Note: skipping tests with currently unsupported features
		// ann09: PRANX=IED and PRNXT=null -> cannot add PRF event at PRANX-1D
		return testIds.stream().filter(testId -> !Arrays.asList("ann09").contains(testId)).map(testId -> {
			// extract test for test ID
			TestData test = tests.get(testId);

			// create market model from data
			List<ObservedDataSet> dataObserved = new ArrayList<ObservedDataSet>(test.getDataObserved().values());
			DataObserver observer = ContractTestUtils.createObserver(dataObserved);

			// create contract model from data
			ContractModel terms = ContractTestUtils.createModel(tests.get(testId).getTerms());

			// compute and evaluate schedule
			LocalDateTime to = "".equals(test.getto()) ? terms.getAs("maturityDate")
					: LocalDateTime.parse(test.getto());
			ArrayList<ContractEvent> schedule = ContractType.schedule(to, terms);

			LocalDate localDate = LocalDate.parse("2013-03-31", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			LocalDateTime ad0 = localDate.atTime(23, 59, 59);
			ContractEvent adEvent = EventFactory.createEvent(ad0, EventType.AD, terms.getAs("currency"),
					new POF_AD_PAM(), new STF_AD_PAM(), terms.getAs("contractID"));
			schedule.add(0, adEvent);
			schedule.removeIf(e -> (e.eventType().toString().matches("RR|RRY|SC")
					&& (e.eventTime().isBefore(adEvent.eventTime()))));

			schedule = ContractType.apply(schedule, terms, observer);

			// extract test results
			List<ResultSet> expectedResults = test.getResults();
			expectedResults.forEach(ResultSet::setValues);

			// transform schedule to event list and return
			List<ResultSet> computedResults = new ArrayList<>();
			ResultSet sampleFields;
			int i = 0;
			for (ContractEvent event : schedule) {
				try {
					sampleFields = expectedResults.get(i);
					i++;
				} catch (IndexOutOfBoundsException e) {
					sampleFields = expectedResults.get(i - 1);
				}
				ResultSet result = new ResultSet();
				result.setRequiredValues(sampleFields.getValues(), event.getAllStates());
				computedResults.add(result);
			}

			// round results to available precision
			computedResults.forEach(result -> result.roundTo(10));
			expectedResults.forEach(result -> result.roundTo(10));

			// create dynamic test
			return DynamicTest.dynamicTest("Test: " + testId,
					() -> Assertions.assertArrayEquals(expectedResults.toArray(), computedResults.toArray()));
		});
	}
}