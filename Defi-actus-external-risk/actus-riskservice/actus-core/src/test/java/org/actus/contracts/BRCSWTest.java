package org.actus.contracts;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.actus.attributes.ContractModel;
import org.actus.events.ContractEvent;
import org.actus.testutils.ContractTestUtils;
import org.actus.testutils.DataObserver;
import org.actus.testutils.ObservedDataSet;
import org.actus.testutils.ResultSet;
import org.actus.testutils.TestData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;


public class BRCSWTest {
	
    @TestFactory
    public Stream<DynamicTest> test() {
        String testFile = "./src/test/resources/actus/actus-tests-bcs.json";

        // read tests from file
        Map<String, TestData> tests = ContractTestUtils.readTests(testFile);

        // get ids of tests
        Set<String> testIds = tests.keySet();

        // go through test-id and perform test
        return testIds.stream().map(testId -> {
//        	String testId = "brcsw01A";
            // extract test for test ID
            TestData test = tests.get(testId);

//            System.out.println();
//            System.err.println("testId :"+testId);
            
            // create market model from data
            List<ObservedDataSet> dataObserved = new ArrayList<ObservedDataSet>(test.getDataObserved().values());
            DataObserver observer = ContractTestUtils.createObserver(dataObserved);

            // create contract model from data
            ContractModel terms = ContractTestUtils.createModel(tests.get(testId).getTerms());

            // compute and evaluate schedule
            ArrayList<ContractEvent> schedule = BoundaryControlledSwitch.schedule(LocalDateTime.parse(test.getto()), terms);
            try{
                schedule = BoundaryControlledSwitch.apply(schedule, terms, observer);
            }catch(Exception e){ 
//            	e.printStackTrace();
            }
            
//            System.err.println("EventDate		EventType	EventValue	ContractID		NotionalPrincipal	ExerciseAmount");
//            for(ContractEvent event : schedule){
//            	System.out.println(event.eventTime()+"	"+event.eventType()+"		"+event.payoff()+"		"+event.getContractID()+"		"+event.getAllStates().get("notionalPrincipal")+"			"+event.getAllStates().get("exerciseAmount"));
//            }
            
                   

            // extract test results
            List<ResultSet> expectedResults = test.getResults();
            expectedResults.forEach(ResultSet::setValues);

            // transform schedule to event list and return
            List<ResultSet> computedResults = new ArrayList<>();
            ResultSet sampleFields;
            int i = 0;
            for(ContractEvent event : schedule){
                try{
                    sampleFields = expectedResults.get(i);
                    i++;
                }catch (IndexOutOfBoundsException e) {
                    sampleFields = expectedResults.get(i-1);
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
