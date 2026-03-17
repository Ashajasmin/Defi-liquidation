/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.contracts;

import org.actus.AttributeConversionException;
import org.actus.attributes.ContractModel;
import org.actus.events.ContractEvent;
import org.actus.events.EventFactory;
import org.actus.states.StateSpace;
import org.actus.time.ScheduleFactory;
import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.pam.POF_AD_PAM;
import org.actus.functions.pam.STF_AD_PAM;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.time.LocalDateTime;

import org.actus.types.EndOfMonthConventionEnum;
import org.actus.types.EventType;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class ContractTypeTest {
    
    class MarketModel implements RiskFactorModelProvider {
        public Set<String> keys() {
            Set<String> keys = new HashSet<String>();
            return keys;
        }

        @Override
        public double stateAt(String id, LocalDateTime time,StateSpace states,ContractModelProvider contractAttributes, boolean isMarket) {
            return 0.0;    
        }

    }
    
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    @Test
    public void test_UnknownCT_exception() {
        thrown.expect(AttributeConversionException.class);
        // define attributes
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("contractType", "IDoNotExist");
        map.put("calendar", "NoHolidayCalendar");
        map.put("statusDate", "2016-01-01T00:00:00");
        map.put("contractRole", "RPA");
        map.put("legalEntityIDCounterparty", "CORP-XY");
        map.put("dayCountConvention", "AA");
        map.put("currency", "USD");
        map.put("initialExchangeDate", "2016-01-02T00:00:00");
        map.put("maturityDate", "2017-01-01T00:00:00");
        map.put("notionalPrincipal", "1000.0");
        // parse attributes
        ContractModel model = ContractModel.parse(map);

        // compute schedule
        ArrayList<ContractEvent> schedule = ContractType.schedule(model.getAs("maturityDate"),model);

        // add analysis events
        schedule.addAll(EventFactory.createEvents(
            ScheduleFactory.createSchedule(model.getAs("statusDate"),model.<LocalDateTime>getAs("statusDate").plusMonths(6),"P1ML1", EndOfMonthConventionEnum.SD),
            EventType.AD, model.getAs("currency"), new POF_AD_PAM(), new STF_AD_PAM(), model.getAs("contractID")));
    
        // define risk factor model
        MarketModel riskFactors = new MarketModel();

        // apply events
        ArrayList<ContractEvent> events = ContractType.apply(schedule,model,riskFactors);
    }
    
    @Test
    public void test_PAM_MandatoryAttributes() {
        thrown = ExpectedException.none();
        // define attributes
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("contractType", "PAM");
        map.put("calendar", "NoHolidayCalendar");
        map.put("statusDate", "2016-01-01T00:00:00");
        map.put("contractRole", "RPA");
        map.put("legalEntityIDCounterparty", "CORP-XY");
        map.put("dayCountConvention", "AA");
        map.put("currency", "USD");
        map.put("initialExchangeDate", "2016-01-02T00:00:00");
        map.put("maturityDate", "2017-01-01T00:00:00");
        map.put("notionalPrincipal", "1000.0");
        // parse attributes
        ContractModel model = ContractModel.parse(map);

        // compute schedule
        ArrayList<ContractEvent> schedule = ContractType.schedule(model.getAs("maturityDate"),model);

        // add analysis events
        schedule.addAll(EventFactory.createEvents(
            ScheduleFactory.createSchedule(model.getAs("statusDate"),model.<LocalDateTime>getAs("statusDate").plusMonths(6),"P1ML1",EndOfMonthConventionEnum.SD),
            EventType.AD, model.getAs("currency"), new POF_AD_PAM(), new STF_AD_PAM(), model.getAs("contractID")));
    
        // define risk factor model
        MarketModel riskFactors = new MarketModel();

        // apply events
        ArrayList<ContractEvent> events = ContractType.apply(schedule,model,riskFactors);
    }
    
    @Test
    public void test_PAM_withIP_withRR_withSC_withOP_withMultipleAnalysisTimes() {
        thrown = ExpectedException.none();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("contractType", "PAM");
        map.put("calendar", "NoHolidayCalendar");
        map.put("statusDate", "2016-01-01T00:00:00");
        map.put("contractRole", "RPA");
        map.put("legalEntityIDCounterparty", "CORP-XY");
        map.put("dayCountConvention", "AA");
        map.put("currency", "USD");
        map.put("initialExchangeDate", "2016-01-02T00:00:00");
        map.put("maturityDate", "2017-01-01T00:00:00");
        map.put("notionalPrincipal", "1000.0");
        map.put("nominalInterestRate","0.01");
        map.put("cycleOfInterestPayment","P3ML1");
        map.put("cycleOfRateReset","P3ML1");
        map.put("scalingEffect","INO");
        map.put("cycleOfScalingIndex","P3ML1");
        map.put("cycleAnchorDateOfOptionality","2016-06-01T00:00:00");
        // parse attributes
        ContractModel model = ContractModel.parse(map);

        // compute schedule
        ArrayList<ContractEvent> schedule = ContractType.schedule(model.getAs("maturityDate"),model);

        // add analysis events
        schedule.addAll(EventFactory.createEvents(
            ScheduleFactory.createSchedule(model.getAs("statusDate"),model.<LocalDateTime>getAs("statusDate").plusMonths(6),"P1ML1",EndOfMonthConventionEnum.SD),
            EventType.AD, model.getAs("currency"), new POF_AD_PAM(), new STF_AD_PAM(), model.getAs("contractID")));
    
        // define risk factor model
        MarketModel riskFactors = new MarketModel();

        // apply events
        ArrayList<ContractEvent> events = ContractType.apply(schedule,model,riskFactors);
    }
    
    @Test
    public void test_LAM_MandatoryAttributes_withMaturity() {
        thrown = ExpectedException.none();
        // define attributes
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("contractType", "LAM");
        map.put("calendar", "NoHolidayCalendar");
        map.put("statusDate", "2016-01-01T00:00:00");
        map.put("contractRole", "RPA");
        map.put("legalEntityIDCounterparty", "CORP-XY");
        map.put("dayCountConvention", "AA");
        map.put("currency", "USD");
        map.put("initialExchangeDate", "2016-01-02T00:00:00");
        map.put("cycleOfPrincipalRedemption", "P3ML1");
        map.put("maturityDate", "2017-01-01T00:00:00");
        map.put("notionalPrincipal", "1000.0");
        map.put("nominalInterestRate","0.01");
        // parse attributes
        ContractModel model = ContractModel.parse(map);

        // compute schedule
        ArrayList<ContractEvent> schedule = ContractType.schedule(model.getAs("maturityDate"),model);

        // add analysis events
        schedule.addAll(EventFactory.createEvents(
            ScheduleFactory.createSchedule(model.getAs("statusDate"),model.<LocalDateTime>getAs("statusDate").plusMonths(6),"P1ML1",EndOfMonthConventionEnum.SD),
            EventType.AD, model.getAs("currency"), new POF_AD_PAM(), new STF_AD_PAM(), model.getAs("contractID")));
    
        // define risk factor model
        MarketModel riskFactors = new MarketModel();

        // apply events
        ArrayList<ContractEvent> events = ContractType.apply(schedule,model,riskFactors);
    }
    
    @Test
    public void test_NAM_MandatoryAttributes() {
        thrown = ExpectedException.none();
        // define attributes
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("contractType", "NAM");
        map.put("calendar", "NoHolidayCalendar");
        map.put("statusDate", "2016-01-01T00:00:00");
        map.put("contractRole", "RPA");
        map.put("legalEntityIDCounterparty", "CORP-XY");
        map.put("dayCountConvention", "AA");
        map.put("currency", "USD");
        map.put("initialExchangeDate", "2016-01-02T00:00:00");
        map.put("cycleOfPrincipalRedemption", "P3ML1");
        map.put("nextPrincipalRedemptionPayment", "100.0");
        map.put("notionalPrincipal", "1000.0");
        map.put("nominalInterestRate","0.01");
        // parse attributes
        ContractModel model = ContractModel.parse(map);

        // compute schedule
        ArrayList<ContractEvent> schedule = ContractType.schedule(model.<LocalDateTime>getAs("initialExchangeDate").plusYears(5),model);

        // add analysis events
        schedule.addAll(EventFactory.createEvents(
            ScheduleFactory.createSchedule(model.getAs("statusDate"),model.<LocalDateTime>getAs("statusDate").plusMonths(6),"P1ML1",EndOfMonthConventionEnum.SD),
            EventType.AD, model.getAs("currency"), new POF_AD_PAM(), new STF_AD_PAM(), model.getAs("contractID")));
    
        // define risk factor model
        MarketModel riskFactors = new MarketModel();

        // apply events
        ArrayList<ContractEvent> events = ContractType.apply(schedule,model,riskFactors);
    }
    
    
    @Test
    public void test_ANN_MandatoryAttributes_withMaturity() {
        thrown = ExpectedException.none();
        // define attributes
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("contractType", "ANN");
        map.put("calendar", "NoHolidayCalendar");
        map.put("statusDate", "2016-01-01T00:00:00");
        map.put("contractRole", "RPA");
        map.put("legalEntityIDCounterparty", "CORP-XY");
        map.put("dayCountConvention", "AA");
        map.put("currency", "USD");
        map.put("initialExchangeDate", "2016-01-02T00:00:00");
        map.put("cycleOfPrincipalRedemption", "P3ML1");
        map.put("maturityDate", "2017-01-01T00:00:00");
        map.put("notionalPrincipal", "1000.0");
        map.put("nominalInterestRate","0.01");
        // parse attributes
        ContractModel model = ContractModel.parse(map);

        // compute schedule
        ArrayList<ContractEvent> schedule = ContractType.schedule(model.getAs("maturityDate"),model);

        // add analysis events
        schedule.addAll(EventFactory.createEvents(
            ScheduleFactory.createSchedule(model.getAs("statusDate"),model.<LocalDateTime>getAs("statusDate").plusMonths(6),"P1ML1",EndOfMonthConventionEnum.SD),
            EventType.AD, model.getAs("currency"), new POF_AD_PAM(), new STF_AD_PAM(), model.getAs("contractID")));
    
        // define risk factor model
        MarketModel riskFactors = new MarketModel();

        // apply events
        ArrayList<ContractEvent> events = ContractType.apply(schedule,model,riskFactors);
    }

    @Test
    public void test_CLM_MandatoryAttributes() {
        thrown = ExpectedException.none();
        // define attributes
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("contractType", "CLM");
        map.put("statusDate", "2016-01-01T00:00:00");
        map.put("contractRole", "RPA");
        map.put("legalEntityIDCounterparty", "CORP-XY");
        map.put("legalEntityIDCounterparty", "CORP-XY");
        map.put("nominalInterestRate", "0.01");
        map.put("dayCountConvention", "AA");
        map.put("currency", "USD");
        map.put("initialExchangeDate", "2016-01-02T00:00:00");
        map.put("notionalPrincipal", "1000.0");
        map.put("xDayNotice", "1M");
        // parse attributes
        ContractModel model = ContractModel.parse(map);

        // compute schedule
        ArrayList<ContractEvent> schedule = ContractType.schedule(model.<LocalDateTime>getAs("initialExchangeDate").plusYears(5),model);

        // add analysis events
        schedule.addAll(EventFactory.createEvents(
            ScheduleFactory.createSchedule(model.getAs("statusDate"),model.<LocalDateTime>getAs("statusDate").plusMonths(6),"P1ML1",EndOfMonthConventionEnum.SD),
            EventType.AD, model.getAs("currency"), new POF_AD_PAM(), new STF_AD_PAM(), model.getAs("contractID")));
    
        // define risk factor model
        MarketModel riskFactors = new MarketModel();

        // apply events
        ArrayList<ContractEvent> events = ContractType.apply(schedule,model,riskFactors);
    }

    @Test
    public void test_CSH_MandatoryAttributes() {
        thrown = ExpectedException.none();
        // define attributes
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("contractType", "CSH");
        map.put("statusDate", "2016-01-01T00:00:00");
        map.put("contractRole", "RPA");
        map.put("currency", "USD");
        map.put("notionalPrincipal", "1000.0");
        // parse attributes
        ContractModel model = ContractModel.parse(map);

        // compute schedule
        ArrayList<ContractEvent> schedule = ContractType.schedule(model.<LocalDateTime>getAs("statusDate").plusYears(5),model);

        // add analysis events
        schedule.addAll(EventFactory.createEvents(
            ScheduleFactory.createSchedule(model.getAs("statusDate"),model.<LocalDateTime>getAs("statusDate").plusMonths(6),"P1ML1",EndOfMonthConventionEnum.SD),
            EventType.AD, model.getAs("currency"), new POF_AD_PAM(), new STF_AD_PAM(), model.getAs("contractID")));
    
        // define risk factor model
        MarketModel riskFactors = new MarketModel();

        // apply events
        ArrayList<ContractEvent> events = ContractType.apply(schedule,model,riskFactors);
    }
    
    @Test
    public void test_STK_MandatoryAttributes() {
        thrown = ExpectedException.none();
        // define attributes
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("contractType", "STK");
        map.put("calendar", "NoHolidayCalendar");
        map.put("statusDate", "2016-01-01T00:00:00");
        map.put("contractRole", "RPA");
        map.put("legalEntityIDCounterparty", "CORP-XY");
        map.put("currency", "USD");
        // parse attributes
        ContractModel model = ContractModel.parse(map);

        // compute schedule
        ArrayList<ContractEvent> schedule = ContractType.schedule(model.<LocalDateTime>getAs("statusDate").plusYears(5),model);

        // add analysis events
        schedule.addAll(EventFactory.createEvents(
            ScheduleFactory.createSchedule(model.getAs("statusDate"),model.<LocalDateTime>getAs("statusDate").plusMonths(6),"P1ML1",EndOfMonthConventionEnum.SD),
            EventType.AD, model.getAs("currency"), new POF_AD_PAM(), new STF_AD_PAM(), model.getAs("contractID")));
    
        // define risk factor model
        MarketModel riskFactors = new MarketModel();

        // apply events
        ArrayList<ContractEvent> events = ContractType.apply(schedule,model,riskFactors);
    }
    
    @Test
    public void test_COM_MandatoryAttributes() {
        thrown = ExpectedException.none();
        // define attributes
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("contractType", "COM");
        map.put("statusDate", "2016-01-01T00:00:00");
        map.put("contractRole", "RPA");
        map.put("currency", "USD");
        // parse attributes
        ContractModel model = ContractModel.parse(map);

        // compute schedule
        ArrayList<ContractEvent> schedule = ContractType.schedule(model.<LocalDateTime>getAs("statusDate").plusYears(5),model);

        // add analysis events
        schedule.addAll(EventFactory.createEvents(
            ScheduleFactory.createSchedule(model.getAs("statusDate"),model.<LocalDateTime>getAs("statusDate").plusMonths(6),"P1ML1",EndOfMonthConventionEnum.SD),
            EventType.AD, model.getAs("currency"), new POF_AD_PAM(), new STF_AD_PAM(), model.getAs("contractID")));
    
        // define risk factor model
        MarketModel riskFactors = new MarketModel();

        // apply events
        ArrayList<ContractEvent> events = ContractType.apply(schedule,model,riskFactors);
    }
    
    @Test
    public void test_FXOUT_MandatoryAttributes() {
        thrown = ExpectedException.none();
        // define attributes
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("contractType", "FXOUT");
        map.put("statusDate", "2016-01-01T00:00:00");
        map.put("contractRole", "RPA");
        map.put("currency", "USD");
        map.put("currency2", "EUR");
        map.put("maturityDate", "2016-06-01T00:00:00");
        map.put("notionalPrincipal", "1000");
        map.put("notionalPrincipal2", "900");
        map.put("deliverySettlement", "D");
        // parse attributes
        ContractModel model = ContractModel.parse(map);

        // compute schedule
        ArrayList<ContractEvent> schedule = ContractType.schedule(model.getAs("maturityDate"),model);

        // add analysis events
        schedule.addAll(EventFactory.createEvents(
            ScheduleFactory.createSchedule(model.getAs("statusDate"),model.<LocalDateTime>getAs("statusDate").plusMonths(6),"P1ML1",EndOfMonthConventionEnum.SD),
            EventType.AD, model.getAs("currency"), new POF_AD_PAM(), new STF_AD_PAM(), model.getAs("contractID")));
    
        // define risk factor model
        MarketModel riskFactors = new MarketModel();

        // apply events
        ArrayList<ContractEvent> events = ContractType.apply(schedule,model,riskFactors);
    }

}
