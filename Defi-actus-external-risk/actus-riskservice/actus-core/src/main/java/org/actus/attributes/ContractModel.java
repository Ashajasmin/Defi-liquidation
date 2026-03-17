/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.attributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.actus.AttributeConversionException;
import org.actus.ContractTypeUnknownException;
import org.actus.contracts.ContractType;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.time.calendar.BusinessDayCalendarProvider;
import org.actus.time.calendar.MondayToFridayCalendar;
import org.actus.time.calendar.MondayToFridayWithHolidaysCalendar;
import org.actus.time.calendar.NoHolidaysCalendar;
import org.actus.types.BusinessDayConventionEnum;
import org.actus.types.ContractPerformance;
import org.actus.types.ContractReference;
import org.actus.types.ContractRole;
import org.actus.types.ContractTypeEnum;
import org.actus.types.CreditEventTypeCovered;
import org.actus.types.CyclePointOfInterestPayment;
import org.actus.types.CyclePointOfRateReset;
import org.actus.types.DeliverySettlement;
import org.actus.types.EndOfMonthConventionEnum;
import org.actus.types.FeeBasis;
import org.actus.types.GuaranteedExposure;
import org.actus.types.InterestCalculationBase;
import org.actus.types.OptionType;
import org.actus.types.PenaltyType;
import org.actus.types.ReferenceRole;
import org.actus.types.ScalingEffect;
import org.actus.types.Seniority;
import org.actus.util.CommonUtils;

/**
 * A data structure representing the set of ACTUS contract attributes
 * <p>
 * This is a simple implementation of the {@link ContractModelProvider} interface representing
 * a generic data structure for the various ACTUS attributes parametrizing a {@link ContractType}.
 * Method {@code parse} allows parsing the attributes from an input {@code String} representation
 * to the internal data types.
 * <p>
 * Note, an ACTUS {@link ContractType} can deal with any data structure implementing the
 * {@link ContractModelProvider} interface. Thus, depending on the system ACTUS is embedded in,
 * more efficient data structures and parsing methods are possible.
 *
 * @see <a href="https://www.actusfrf.org/data-dictionary">ACTUS Data Dictionary</a>
 */
public class ContractModel implements ContractModelProvider {
    private Map<String, Object> attributes;

    /**
     * Constructor
     * <p>
     * The map provided as the constructor argument is expected to contain <key,value> pairs
     * of attributes using ACTUS attribute names (in long form) and data types of values
     * as per official ACTUS data dictionary.
     */
    public ContractModel(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * Create a new contract model from a java map
     * <p>
     * The map provided as the method argument is expected to contain <key,value> pairs
     * of attributes using ACTUS attribute names (in long form) and data types of values
     * as per official ACTUS data dictionary.
     *
     * @param attributes a java map of attributes as per ACTUS data dictionary
     * @return an instance of ContractModel containing the attributes provided with the method argument
     */
    public static ContractModel of(Map<String, Object> attributes) {
        return new ContractModel(attributes);
    }


    /**
     * Parse the attributes from external String-representation to internal, attribute-specific data types
     * <p>
     * For the {@link ContractType} indicated in attribute "contractType" the method goes through the list
     * of supported attributes and tries to parse these to their respective data type as indicated in the
     * ACTUS data dictionary ({@linktourl https://www.actusfrf.org/data-dictionary}).
     * <p>
     * For all attributes mandatory to a certain "contractType" the method expects a not-{@code null} return value
     * of method {@code get} of the {@code Map<String,String>} method parameter. For non-mandatory attributes, a
     * {@code null} return value is allowed and treated as that the attribute is not specified. Some attributes may
     * be mandatory conditional to the value of other attributes. Be referred to the ACTUS data dictionary
     * for details.
     *
     * @param contractAttributes an external, raw (String) data representation of the set of attributes
     * @return an instance of ContractModel containing the attributes provided with the method argument
     * @throws AttributeConversionException if an attribute cannot be parsed to its data type
     */
    public static ContractModel parse(Map<String, Object> contractAttributes) {
        HashMap<String, Object> map = new HashMap<>();
        Set<LocalDate> holidays = getHolidays(contractAttributes);
        if(contractAttributes.get("contractStructure")!=null){
            Map<String, Object> attributes = contractAttributes;
            List<ContractReference> contractStructure = new ArrayList<>();
            // parse all attributes known to the respective contract type
            try {
                switch (ContractTypeEnum.valueOf((String)attributes.get("contractType"))) {

                    case SWAPS:
                        // parse attributes (Swap) attributes
                        map.put("contractID", attributes.get("contractID"));
                        map.put("statusDate", LocalDateTime.parse((String)attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf((String)attributes.get("contractRole")));
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("currency", attributes.get("currency"));
                        map.put("purchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse((String)attributes.get("purchaseDate")));
                        map.put("priceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtPurchaseDate")));
                        map.put("terminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse((String)attributes.get("terminationDate")));
                        map.put("priceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtTerminationDate")));
                        map.put("deliverySettlement", DeliverySettlement.valueOf((String)attributes.get("deliverySettlement")));
                        map.put("contractType", ContractTypeEnum.valueOf((String)attributes.get("contractType")));
                        // parse child attributes
                        ((List<Map<String,Object>>)attributes.get("contractStructure")).forEach(e->contractStructure.add(new ContractReference(e, (ContractRole)map.get("contractRole"))));
                        map.put("contractStructure", contractStructure);

                        break;

                    case CAPFL:
                        // parse attributes (CapFloor) attributes
                        map.put("contractType", ContractTypeEnum.CAPFL);
                        map.put("statusDate", LocalDateTime.parse((String)attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf((String)attributes.get("contractRole")));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("currency", attributes.get("currency"));
                        map.put("purchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse((String)attributes.get("purchaseDate")));
                        map.put("priceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtPurchaseDate")));
                        map.put("terminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse((String)attributes.get("terminationDate")));
                        map.put("priceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtTerminationDate")));
                        map.put("lifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble((String)attributes.get("lifeCap")));
                        map.put("lifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble((String)attributes.get("lifeFloor")));

                        // parse underlying attributes
                        ((List<Map<String,Object>>)attributes.get("contractStructure")).forEach(e->contractStructure.add(new ContractReference(e, (ContractRole)map.get("contractRole"))));
                        map.put("contractStructure", contractStructure);
                        break;

                    case OPTNS:
                        map.put("maturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse((String)attributes.get("maturityDate"))));
                        map.put("calendar",
                        	    (!CommonUtils.isNull(attributes.get("calendar")))
                        	        ? (attributes.get("calendar").equals("MF")
                        	            ? new MondayToFridayCalendar()
                        	            : (attributes.get("calendar").equals("MFH")
                        	                ? new MondayToFridayWithHolidaysCalendar(holidays)
                        	                : new NoHolidaysCalendar()) )
                        	        : new NoHolidaysCalendar()
                        	);
                        map.put("businessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf((String)attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("endOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf((String)attributes.get("endOfMonthConvention")));
                        map.put("contractType", ContractTypeEnum.valueOf((String)attributes.get("contractType")));
                        map.put("statusDate", LocalDateTime.parse((String)attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf((String)attributes.get("contractRole")));
                        map.put("creatorID", attributes.get("creatorID"));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        //TODO: Only needed for underlying MarketObjectCode values in Exercise-Date/Amount calc ?
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("contractPerformance", (CommonUtils.isNull(attributes.get("contractPerformance")) ? ContractPerformance.PF : ContractPerformance.valueOf((String)attributes.get("contractPerformance"))));
                        map.put("seniority", !CommonUtils.isNull(attributes.get("seniority")) ? Seniority.valueOf((String)attributes.get("seniority")): null);
                        map.put("nonPerformingDate", !CommonUtils.isNull(attributes.get("nonPerformingDates")) ? LocalDateTime.parse((String)attributes.get("nonPerformingDates")): null);
                        map.put("prepaymentPeriod", attributes.get("prepaymentPeriod"));
                        map.put("gracePeriod", attributes.get("gracePeriod"));
                        map.put("delinquencyPeriod", attributes.get("delinquencyPeriod"));
                        map.put("delinquencyRate", !CommonUtils.isNull(attributes.get("delinquencyRate")) ? Double.parseDouble((String)attributes.get("delinquencyRate")) : 0.0);
                        map.put("guaranteedExposure", (CommonUtils.isNull(attributes.get("guaranteedExposure")) ? null : GuaranteedExposure.valueOf((String)attributes.get("guaranteedExposure"))));
                        map.put("coverageOfCreditEnhancement", (CommonUtils.isNull(attributes.get("coverageOfCreditEnhancement")) ? 1.0 : Double.parseDouble((String)attributes.get("coverageOfCreditEnhancement"))));
                        map.put("creditEventTypeCovered", (CommonUtils.isNull(attributes.get("creditEventTypeCovered")) ? new ArrayList<CreditEventTypeCovered>().add(CreditEventTypeCovered.DF) : Arrays.stream(((String)attributes.get("creditEventTypeCovered")).replaceAll("\\[", "").replaceAll("]", "").trim().split(",")).map(CreditEventTypeCovered::valueOf).toArray(CreditEventTypeCovered[]::new)));
                        map.put("cycleAnchorDateOfDividend", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfDividend")) ? null : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfDividend"))));
                        map.put("cycleOfDividend", attributes.get("cycleOfDividend"));
                        map.put("nextDividendPaymentAmount", !CommonUtils.isNull(attributes.get("nextDividendPaymentAmount")) ? Double.parseDouble((String)attributes.get("nextDividendPaymentAmount")) : 0.0);
                        map.put("exDividendDate", (CommonUtils.isNull(attributes.get("exDividendDate")) ? null : LocalDateTime.parse((String)attributes.get("exDividendDate"))));
                        map.put("cycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee")) ? null : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfFee"))));
                        map.put("cycleOfFee", attributes.get("cycleOfFee"));
                        map.put("feeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf((String)attributes.get("feeBasis")));
                        map.put("feeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeRate")));
                        map.put("feeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeAccrued")));
                        map.put("cycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse((String)attributes.get("initialExchangeDate"))) : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("arrayCycleAnchorDateOfInterestPayment", attributes.get("arrayCycleAnchorDateOfInterestPayment"));
                        map.put("cycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("arrayCycleOfInterestPayment", attributes.get("arrayCycleOfInterestPayment"));
                        map.put("nominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble((String)attributes.get("nominalInterestRate")));
                        map.put("exerciseAmount", !(CommonUtils.isNull(attributes.get("exerciseAmount"))) ? Double.parseDouble((String)attributes.get("exerciseAmount")): 0.0);
                        map.put("purchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse((String)attributes.get("purchaseDate")));
                        map.put("settlementPeriod", (CommonUtils.isNull(attributes.get("settlementPeriod"))) ? "P0D" : attributes.get("settlementPeriod"));
                        map.put("exerciseDate", (CommonUtils.isNull(attributes.get("exerciseDate")) ? null : LocalDateTime.parse((String)attributes.get("exerciseDate"))));
                        map.put("priceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtPurchaseDate")));
                        map.put("optionType", OptionType.valueOf((String)attributes.get("optionType")));
                        map.put("optionStrike1", Double.parseDouble((String)attributes.get("optionStrike1")));
                        map.put("currency", attributes.get("currency"));

                        // parse underlying attributes
                        ((List<Map<String,Object>>)attributes.get("contractStructure")).forEach(e->contractStructure.add(new ContractReference(e, (ContractRole)map.get("contractRole"))));
                        map.put("contractStructure", contractStructure);
                        break;
                        
                    case BCS:
                        map.put("maturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse((String)attributes.get("maturityDate"))));
                        map.put("calendar",
                        	    (!CommonUtils.isNull(attributes.get("calendar")))
                        	        ? (attributes.get("calendar").equals("MF")
                        	            ? new MondayToFridayCalendar()
                        	            : (attributes.get("calendar").equals("MFH")
                        	                ? new MondayToFridayWithHolidaysCalendar(holidays)
                        	                : new NoHolidaysCalendar()) )
                        	        : new NoHolidaysCalendar()
                        	);
                        map.put("businessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf((String)attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("endOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf((String)attributes.get("endOfMonthConvention")));
                        map.put("contractType", ContractTypeEnum.valueOf((String)attributes.get("contractType")));
                        map.put("statusDate", LocalDateTime.parse((String)attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf((String)attributes.get("contractRole")));
                        map.put("creatorID", attributes.get("creatorID"));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        //TODO: Only needed for underlying MarketObjectCode values in Exercise-Date/Amount calc ?
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("contractPerformance", (CommonUtils.isNull(attributes.get("contractPerformance")) ? ContractPerformance.PF : ContractPerformance.valueOf((String)attributes.get("contractPerformance"))));
                        map.put("seniority", !CommonUtils.isNull(attributes.get("seniority")) ? Seniority.valueOf((String)attributes.get("seniority")): null);
                        map.put("nonPerformingDate", !CommonUtils.isNull(attributes.get("nonPerformingDates")) ? LocalDateTime.parse((String)attributes.get("nonPerformingDates")): null);
                        map.put("prepaymentPeriod", attributes.get("prepaymentPeriod"));
                        map.put("gracePeriod", attributes.get("gracePeriod"));
                        map.put("delinquencyPeriod", attributes.get("delinquencyPeriod"));
                        map.put("delinquencyRate", !CommonUtils.isNull(attributes.get("delinquencyRate")) ? Double.parseDouble((String)attributes.get("delinquencyRate")) : 0.0);
                        map.put("guaranteedExposure", (CommonUtils.isNull(attributes.get("guaranteedExposure")) ? null : GuaranteedExposure.valueOf((String)attributes.get("guaranteedExposure"))));
                        map.put("coverageOfCreditEnhancement", (CommonUtils.isNull(attributes.get("coverageOfCreditEnhancement")) ? 1.0 : Double.parseDouble((String)attributes.get("coverageOfCreditEnhancement"))));
                        map.put("creditEventTypeCovered", (CommonUtils.isNull(attributes.get("creditEventTypeCovered")) ? new ArrayList<CreditEventTypeCovered>().add(CreditEventTypeCovered.DF) : Arrays.stream(((String)attributes.get("creditEventTypeCovered")).replaceAll("\\[", "").replaceAll("]", "").trim().split(",")).map(CreditEventTypeCovered::valueOf).toArray(CreditEventTypeCovered[]::new)));
                        map.put("cycleAnchorDateOfDividend", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfDividend")) ? null : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfDividend"))));
                        map.put("cycleOfDividend", attributes.get("cycleOfDividend"));
                        map.put("nextDividendPaymentAmount", !CommonUtils.isNull(attributes.get("nextDividendPaymentAmount")) ? Double.parseDouble((String)attributes.get("nextDividendPaymentAmount")) : 0.0);
                        map.put("exDividendDate", (CommonUtils.isNull(attributes.get("exDividendDate")) ? null : LocalDateTime.parse((String)attributes.get("exDividendDate"))));
                        map.put("cycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee")) ? null : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfFee"))));
                        map.put("cycleOfFee", attributes.get("cycleOfFee"));
                        map.put("feeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf((String)attributes.get("feeBasis")));
                        map.put("feeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeRate")));
                        map.put("feeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeAccrued")));
                        map.put("cycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse((String)attributes.get("initialExchangeDate"))) : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("arrayCycleAnchorDateOfInterestPayment", attributes.get("arrayCycleAnchorDateOfInterestPayment"));
                        map.put("cycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("arrayCycleOfInterestPayment", attributes.get("arrayCycleOfInterestPayment"));
                        map.put("nominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble((String)attributes.get("nominalInterestRate")));
                        map.put("exerciseAmount", !(CommonUtils.isNull(attributes.get("exerciseAmount"))) ? Double.parseDouble((String)attributes.get("exerciseAmount")): 0.0);
                        map.put("purchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse((String)attributes.get("purchaseDate")));
                        map.put("settlementPeriod", (CommonUtils.isNull(attributes.get("settlementPeriod"))) ? "P0D" : attributes.get("settlementPeriod"));
                        map.put("exerciseDate", (CommonUtils.isNull(attributes.get("exerciseDate")) ? null : LocalDateTime.parse((String)attributes.get("exerciseDate"))));
                        map.put("priceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtPurchaseDate")));
                        map.put("currency", attributes.get("currency"));
                        map.put("boundaryValue", (CommonUtils.isNull(attributes.get("boundaryValue"))) ? 0.0 : Double.parseDouble((String)attributes.get("boundaryValue")));
                        map.put("boundaryDirection", attributes.get("boundaryDirection"));
                        map.put("boundaryEffect", attributes.get("boundaryEffect"));
                        map.put("boundaryLegInitiallyActive", (CommonUtils.isNull(attributes.get("boundaryLegInitiallyActive"))) ? null : ReferenceRole.valueOf((String)attributes.get("boundaryLegInitiallyActive")));
                        map.put("boundaryMonitoringAnchorDate", (CommonUtils.isNull(attributes.get("boundaryMonitoringAnchorDate"))) ? map.get("purchaseDate") : LocalDateTime.parse((String)attributes.get("boundaryMonitoringAnchorDate")));
                        map.put("boundaryMonitoringEndDate", (CommonUtils.isNull(attributes.get("boundaryMonitoringEndDate"))) ? map.get("maturityDate") : LocalDateTime.parse((String)attributes.get("boundaryMonitoringEndDate")));
                        map.put("boundaryMonitoringCycle", attributes.get("boundaryMonitoringCycle"));
                        map.put("boundaryCrossedFlag", attributes.get("boundaryCrossedFlag"));


                        // parse underlying attributes
                        ((List<Map<String,Object>>)attributes.get("contractStructure")).forEach(e->contractStructure.add(new ContractReference(e, (ContractRole)map.get("contractRole"))));
                        map.put("contractStructure", contractStructure);
                        break;
                        
                    case FUTUR:
                        map.put("maturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse((String)attributes.get("maturityDate"))));
                        map.put("calendar",
                        	    (!CommonUtils.isNull(attributes.get("calendar")))
                        	        ? (attributes.get("calendar").equals("MF")
                        	            ? new MondayToFridayCalendar()
                        	            : (attributes.get("calendar").equals("MFH")
                        	                ? new MondayToFridayWithHolidaysCalendar(holidays)
                        	                : new NoHolidaysCalendar()) )
                        	        : new NoHolidaysCalendar()
                        	);
                        map.put("businessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf((String)attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("endOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf((String)attributes.get("endOfMonthConvention")));
                        map.put("contractType", ContractTypeEnum.valueOf((String)attributes.get("contractType")));
                        map.put("statusDate", LocalDateTime.parse((String)attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf((String)attributes.get("contractRole")));
                        map.put("creatorID", attributes.get("creatorID"));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("contractPerformance", (CommonUtils.isNull(attributes.get("contractPerformance")) ? ContractPerformance.PF : ContractPerformance.valueOf((String)attributes.get("contractPerformance"))));
                        map.put("seniority", !CommonUtils.isNull(attributes.get("seniority")) ? Seniority.valueOf((String)attributes.get("seniority")): null);
                        map.put("nonPerformingDate", !CommonUtils.isNull(attributes.get("nonPerformingDates")) ? LocalDateTime.parse((String)attributes.get("nonPerformingDates")): null);
                        map.put("prepaymentPeriod", attributes.get("prepaymentPeriod"));
                        map.put("gracePeriod", attributes.get("gracePeriod"));
                        map.put("delinquencyPeriod", attributes.get("delinquencyPeriod"));
                        map.put("delinquencyRate", !CommonUtils.isNull(attributes.get("delinquencyRate")) ? Double.parseDouble((String)attributes.get("delinquencyRate")) : 0.0);
                        map.put("guaranteedExposure", (CommonUtils.isNull(attributes.get("guaranteedExposure")) ? null : GuaranteedExposure.valueOf((String)attributes.get("guaranteedExposure"))));
                        map.put("coverageOfCreditEnhancement", (CommonUtils.isNull(attributes.get("coverageOfCreditEnhancement")) ? 1.0 : Double.parseDouble((String)attributes.get("coverageOfCreditEnhancement"))));
                        map.put("creditEventTypeCovered", (CommonUtils.isNull(attributes.get("creditEventTypeCovered")) ? new ArrayList<CreditEventTypeCovered>().add(CreditEventTypeCovered.DF) : Arrays.stream(((String)attributes.get("creditEventTypeCovered")).replaceAll("\\[", "").replaceAll("]", "").trim().split(",")).map(CreditEventTypeCovered::valueOf).toArray(CreditEventTypeCovered[]::new)));
                        map.put("cycleAnchorDateOfDividend", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfDividend")) ? null : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfDividend"))));
                        map.put("cycleOfDividend", attributes.get("cycleOfDividend"));
                        map.put("nextDividendPaymentAmount", !CommonUtils.isNull(attributes.get("nextDividendPaymentAmount")) ? Double.parseDouble((String)attributes.get("nextDividendPaymentAmount")) : 0.0);
                        map.put("exDividendDate", (CommonUtils.isNull(attributes.get("exDividendDate")) ? null : LocalDateTime.parse((String)attributes.get("exDividendDate"))));
                        map.put("cycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee")) ? null : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfFee"))));
                        map.put("cycleOfFee", attributes.get("cycleOfFee"));
                        map.put("feeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf((String)attributes.get("feeBasis")));
                        map.put("feeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeRate")));
                        map.put("feeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeAccrued")));
                        map.put("cycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse((String)attributes.get("initialExchangeDate"))) : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("arrayCycleAnchorDateOfInterestPayment", attributes.get("arrayCycleAnchorDateOfInterestPayment"));
                        map.put("cycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("arrayCycleOfInterestPayment", attributes.get("arrayCycleOfInterestPayment"));
                        map.put("nominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble((String)attributes.get("nominalInterestRate")));
                        map.put("accruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble((String)attributes.get("accruedInterest")));
                        map.put("futuresPrice", Double.parseDouble((String)attributes.get("futuresPrice")));
                        map.put("exerciseAmount", !(CommonUtils.isNull(attributes.get("exerciseAmount"))) ? Double.parseDouble((String)attributes.get("exerciseAmount")): 0.0);
                        map.put("purchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse((String)attributes.get("purchaseDate")));
                        map.put("settlementPeriod", (CommonUtils.isNull(attributes.get("settlementPeriod"))) ? "P0D" : attributes.get("settlementPeriod"));
                        map.put("exerciseDate", (CommonUtils.isNull(attributes.get("exerciseDate")) ? null : LocalDateTime.parse((String)attributes.get("exerciseDate"))));
                        map.put("priceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtPurchaseDate")));
                        map.put("currency", attributes.get("currency"));

                        // parse underlying attributes
                        ((List<Map<String,Object>>)attributes.get("contractStructure")).forEach(e->contractStructure.add(new ContractReference(e, (ContractRole)map.get("contractRole"))));
                        map.put("contractStructure", contractStructure);
                        break;
                    case CEG:
                    	map.put("calendar",
                        	    (!CommonUtils.isNull(attributes.get("calendar")))
                        	        ? (attributes.get("calendar").equals("MF")
                        	            ? new MondayToFridayCalendar()
                        	            : (attributes.get("calendar").equals("MFH")
                        	                ? new MondayToFridayWithHolidaysCalendar(holidays)
                        	                : new NoHolidaysCalendar()) )
                        	        : new NoHolidaysCalendar()
                        	);
                        map.put("businessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf((String)attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("endOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf((String)attributes.get("endOfMonthConvention")));
                        map.put("contractType", ContractTypeEnum.valueOf((String)attributes.get("contractType")));
                        map.put("statusDate", LocalDateTime.parse((String)attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf((String)attributes.get("contractRole")));
                        map.put("creatorID", attributes.get("creatorID"));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("contractPerformance", (CommonUtils.isNull(attributes.get("contractPerformance")) ? ContractPerformance.PF : ContractPerformance.valueOf((String)attributes.get("contractPerformance"))));
                        map.put("nonPerformingDate", !CommonUtils.isNull(attributes.get("nonPerformingDates")) ? LocalDateTime.parse((String)attributes.get("nonPerformingDates")): null);
                        map.put("gracePeriod", attributes.get("gracePeriod"));
                        map.put("delinquencyPeriod", attributes.get("delinquencyPeriod"));
                        map.put("delinquencyRate", !CommonUtils.isNull(attributes.get("delinquencyRate")) ? Double.parseDouble((String)attributes.get("delinquencyRate")) : 0.0);
                        map.put("guaranteedExposure", (CommonUtils.isNull(attributes.get("guaranteedExposure")) ? GuaranteedExposure.NO : GuaranteedExposure.valueOf((String)attributes.get("guaranteedExposure"))));
                        map.put("coverageOfCreditEnhancement", (CommonUtils.isNull(attributes.get("coverageOfCreditEnhancement")) ? 1.0 : Double.parseDouble((String)attributes.get("coverageOfCreditEnhancement"))));
                        map.put("creditEventTypeCovered", (CommonUtils.isNull(attributes.get("creditEventTypeCovered")) ? new ArrayList<CreditEventTypeCovered>().add(CreditEventTypeCovered.DF) : Arrays.stream(((String)attributes.get("creditEventTypeCovered")).replaceAll("\\[", "").replaceAll("]", "").trim().split(",")).map(CreditEventTypeCovered::valueOf).toArray(CreditEventTypeCovered[]::new)));
                        map.put("cycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee")) ? null : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfFee"))));
                        map.put("cycleOfFee", attributes.get("cycleOfFee"));
                        map.put("feeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf((String)attributes.get("feeBasis")));
                        map.put("feeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeRate")));
                        map.put("feeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeAccrued")));
                        map.put("dayCountConvention", (CommonUtils.isNull(attributes.get("dayCountConvention"))) ? null : new DayCountCalculator(attributes.get("dayCountConvention").toString(), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("currency", attributes.get("currency"));
                        map.put("maturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse((String)attributes.get("maturityDate"))));
                        map.put("notionalPrincipal", CommonUtils.isNull(attributes.get("notionalPrincipal")) ? null : Double.parseDouble((String)attributes.get("notionalPrincipal")));
                        map.put("purchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse((String)attributes.get("purchaseDate")));
                        map.put("priceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtPurchaseDate")));
                        map.put("terminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse((String)attributes.get("terminationDate")));
                        map.put("priceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtTerminationDate")));
                        map.put("exerciseDate", (CommonUtils.isNull(attributes.get("exerciseDate")) ? null : LocalDateTime.parse((String)attributes.get("exerciseDate"))));
                        map.put("exerciseAmount", !(CommonUtils.isNull(attributes.get("exerciseAmount"))) ? Double.parseDouble((String)attributes.get("exerciseAmount")): 0.0);
                        map.put("settlementPeriod", (CommonUtils.isNull(attributes.get("settlementPeriod"))) ? "P0D" : attributes.get("settlementPeriod"));
                         
                        // parse underlying attributes
                        ((List<Map<String,Object>>)attributes.get("contractStructure")).forEach(e->contractStructure.add(new ContractReference(e, (ContractRole)map.get("contractRole"))));
                        map.put("contractStructure", contractStructure);
                        break;

                    case CEC:
                    	map.put("calendar",
                        	    (!CommonUtils.isNull(attributes.get("calendar")))
                        	        ? (attributes.get("calendar").equals("MF")
                        	            ? new MondayToFridayCalendar()
                        	            : (attributes.get("calendar").equals("MFH")
                        	                ? new MondayToFridayWithHolidaysCalendar(holidays)
                        	                : new NoHolidaysCalendar()) )
                        	        : new NoHolidaysCalendar()
                        	);
                        map.put("businessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf((String)attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("endOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf((String)attributes.get("endOfMonthConvention")));
                        map.put("contractType", ContractTypeEnum.valueOf((String)attributes.get("contractType")));
                        map.put("statusDate", LocalDateTime.parse((String)attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf((String)attributes.get("contractRole")));
                        map.put("creatorID", attributes.get("creatorID"));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("guaranteedExposure", (CommonUtils.isNull(attributes.get("guaranteedExposure")) ? GuaranteedExposure.NO : GuaranteedExposure.valueOf((String)attributes.get("guaranteedExposure"))));
                        map.put("coverageOfCreditEnhancement", (CommonUtils.isNull(attributes.get("coverageOfCreditEnhancement")) ? 1.0 : Double.parseDouble((String)attributes.get("coverageOfCreditEnhancement"))));
                        map.put("creditEventTypeCovered", (CommonUtils.isNull(attributes.get("creditEventTypeCovered")) ? new ArrayList<CreditEventTypeCovered>().add(CreditEventTypeCovered.DF) : Arrays.stream(((String)attributes.get("creditEventTypeCovered")).replaceAll("\\[", "").replaceAll("]", "").trim().split(",")).map(CreditEventTypeCovered::valueOf).toArray(CreditEventTypeCovered[]::new)));
                        map.put("currency", attributes.get("currency"));
                        map.put("exerciseDate", (CommonUtils.isNull(attributes.get("exerciseDate")) ? null : LocalDateTime.parse((String)attributes.get("exerciseDate"))));
                        map.put("exerciseAmount", !(CommonUtils.isNull(attributes.get("exerciseAmount"))) ? Double.parseDouble((String)attributes.get("exerciseAmount")): 0.0);
                        map.put("settlementPeriod", (CommonUtils.isNull(attributes.get("settlementPeriod"))) ? "P0D" : attributes.get("settlementPeriod"));
                         
                        // parse underlying attributes
                        ((List<Map<String,Object>>)attributes.get("contractStructure")).forEach(e->contractStructure.add(new ContractReference(e, (ContractRole)map.get("contractRole"))));
                        map.put("contractStructure", contractStructure);
                        break;

                    default:
                        throw new ContractTypeUnknownException();
                }
            } catch (Exception e) {
                throw new AttributeConversionException();
            }
        } else{
            Map<String,String> attributes = contractAttributes.entrySet().stream().collect(Collectors.toMap(e->e.getKey(),e->e.getValue().toString()));
            // parse all attributes known to the respective contract type
            try {
                map.put("contractID", attributes.get("contractID"));
                switch (ContractTypeEnum.valueOf(attributes.get("contractType"))) {
                    case PAM:
                    	map.put("calendar",
                        	    (!CommonUtils.isNull(attributes.get("calendar")))
                        	        ? (attributes.get("calendar").equals("MF")
                        	            ? new MondayToFridayCalendar()
                        	            : (attributes.get("calendar").equals("MFH")
                        	                ? new MondayToFridayWithHolidaysCalendar(holidays)
                        	                : new NoHolidaysCalendar()) )
                        	        : new NoHolidaysCalendar()
                        	);
                        map.put("businessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("endOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("contractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("statusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("contractRole", (!CommonUtils.isNull(attributes.get("contractRole"))) ? ContractRole.valueOf(attributes.get("contractRole")) : null);
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("cycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee"))) ? ((CommonUtils.isNull(attributes.get("cycleOfFee"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfFee")));
                        map.put("cycleOfFee", attributes.get("cycleOfFee"));
                        map.put("feeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf(attributes.get("feeBasis")));
                        map.put("feeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble(attributes.get("feeRate")));
                        map.put("feeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble(attributes.get("feeAccrued")));
                        map.put("cycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("cycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("nominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("dayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("accruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest")));
                        map.put("capitalizationEndDate", (CommonUtils.isNull(attributes.get("capitalizationEndDate"))) ? null : LocalDateTime.parse(attributes.get("capitalizationEndDate")));
                        map.put("cyclePointOfInterestPayment", CommonUtils.isNull(attributes.get("cyclePointOfInterestPayment")) ? null : CyclePointOfInterestPayment.valueOf(attributes.get("cyclePointOfInterestPayment")));
                        map.put("currency", attributes.get("currency"));
                        map.put("initialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("premiumDiscountAtIED", (CommonUtils.isNull(attributes.get("premiumDiscountAtIED"))) ? 0.0 : Double.parseDouble(attributes.get("premiumDiscountAtIED")));
                        map.put("notionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("purchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("priceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("terminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("priceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("marketObjectCodeOfScalingIndex", attributes.get("marketObjectCodeOfScalingIndex"));
                        map.put("scalingIndexAtContractDealDate", (CommonUtils.isNull(attributes.get("scalingIndexAtContractDealDate"))) ? 0.0 : Double.parseDouble(attributes.get("scalingIndexAtContractDealDate")));
                        map.put("notionalScalingMultiplier", (CommonUtils.isNull(attributes.get("notionalScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("notionalScalingMultiplier")));
                        map.put("interestScalingMultiplier", (CommonUtils.isNull(attributes.get("interestScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("interestScalingMultiplier")));
                        map.put("cycleAnchorDateOfScalingIndex", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfScalingIndex"))) ? ((CommonUtils.isNull(attributes.get("cycleOfScalingIndex"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfScalingIndex")));
                        map.put("cycleOfScalingIndex", attributes.get("cycleOfScalingIndex"));
                        map.put("scalingEffect", CommonUtils.isNull(attributes.get("scalingEffect")) ? ScalingEffect.OOO : ScalingEffect.valueOf(attributes.get("scalingEffect")));
                        // TODO: review prepayment mechanism and attributes
                        map.put("cycleAnchorDateOfOptionality", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfOptionality"))) ? ((CommonUtils.isNull(attributes.get("cycleOfOptionality"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfOptionality")));
                        map.put("cycleOfOptionality", attributes.get("cycleOfOptionality"));
                        map.put("penaltyType", (CommonUtils.isNull(attributes.get("penaltyType"))) ? PenaltyType.valueOf("N") : PenaltyType.valueOf(attributes.get("penaltyType")));
                        map.put("penaltyRate", (CommonUtils.isNull(attributes.get("penaltyRate"))) ? 0.0 : Double.parseDouble(attributes.get("penaltyRate")));
                        map.put("objectCodeOfPrepaymentModel", attributes.get("objectCodeOfPrepaymentModel"));
                        map.put("cycleAnchorDateOfRateReset", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfRateReset"))) ? ((CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfRateReset")));
                        map.put("cycleOfRateReset", attributes.get("cycleOfRateReset"));
                        map.put("rateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("marketObjectCodeOfRateReset", attributes.get("marketObjectCodeOfRateReset"));
                        map.put("lifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("lifeCap")));
                        map.put("lifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("lifeFloor")));
                        map.put("periodCap", (CommonUtils.isNull(attributes.get("periodCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("periodCap")));
                        map.put("periodFloor", (CommonUtils.isNull(attributes.get("periodFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("periodFloor")));
                        map.put("cyclePointOfRateReset", CommonUtils.isNull(attributes.get("cyclePointOfRateReset")) ? null : CyclePointOfRateReset.valueOf(attributes.get("cyclePointOfRateReset")));
                        map.put("fixingPeriod", attributes.get("fixingPeriod"));
                        map.put("nextResetRate", (CommonUtils.isNull(attributes.get("nextResetRate"))) ? null : Double.parseDouble(attributes.get("nextResetRate")));
                        map.put("rateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));
                        map.put("maturityDate", LocalDateTime.parse(attributes.get("maturityDate")));

                        break; // nothing else to do for PAM
                    case LAM:
                    	map.put("calendar",
                        	    (!CommonUtils.isNull(attributes.get("calendar")))
                        	        ? (attributes.get("calendar").equals("MF")
                        	            ? new MondayToFridayCalendar()
                        	            : (attributes.get("calendar").equals("MFH")
                        	                ? new MondayToFridayWithHolidaysCalendar(holidays)
                        	                : new NoHolidaysCalendar()) )
                        	        : new NoHolidaysCalendar()
                        	);
                        map.put("businessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("endOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("contractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("statusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("cycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee"))) ? ((CommonUtils.isNull(attributes.get("cycleOfFee"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfFee")));
                        map.put("cycleOfFee", attributes.get("cycleOfFee"));
                        map.put("feeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf(attributes.get("feeBasis")));
                        map.put("feeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble(attributes.get("feeRate")));
                        map.put("feeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble(attributes.get("feeAccrued")));
                        map.put("cycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("cycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("nominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("dayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("accruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest")));
                        map.put("capitalizationEndDate", (CommonUtils.isNull(attributes.get("capitalizationEndDate"))) ? null : LocalDateTime.parse(attributes.get("capitalizationEndDate")));
                        map.put("cyclePointOfRateReset", CommonUtils.isNull(attributes.get("cyclePointOfRateReset")) ? null : map.get("cyclePointOfInterestPayment") == CyclePointOfInterestPayment.B ? CyclePointOfRateReset.E : CyclePointOfRateReset.valueOf(attributes.get("cyclePointOfRateReset")));
                        map.put("currency", attributes.get("currency"));
                        map.put("initialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("premiumDiscountAtIED", (CommonUtils.isNull(attributes.get("premiumDiscountAtIED"))) ? 0.0 : Double.parseDouble(attributes.get("premiumDiscountAtIED")));
                        map.put("notionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("purchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("priceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("terminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("priceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("marketObjectCodeOfScalingIndex", attributes.get("marketObjectCodeOfScalingIndex"));
                        map.put("scalingIndexAtContractDealDate", (CommonUtils.isNull(attributes.get("scalingIndexAtContractDealDate"))) ? 0.0 : Double.parseDouble(attributes.get("scalingIndexAtContractDealDate")));
                        map.put("notionalScalingMultiplier", (CommonUtils.isNull(attributes.get("notionalScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("notionalScalingMultiplier")));
                        map.put("interestScalingMultiplier", (CommonUtils.isNull(attributes.get("interestScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("interestScalingMultiplier")));
                        map.put("cycleAnchorDateOfScalingIndex", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfScalingIndex"))) ? ((CommonUtils.isNull(attributes.get("cycleOfScalingIndex"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfScalingIndex")));
                        map.put("cycleOfScalingIndex", attributes.get("cycleOfScalingIndex"));
                        map.put("scalingEffect", CommonUtils.isNull(attributes.get("scalingEffect")) ? ScalingEffect.OOO : ScalingEffect.valueOf(attributes.get("scalingEffect")));
                        // TODO: review prepayment mechanism and attributes
                        map.put("cycleAnchorDateOfOptionality", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfOptionality"))) ? ((CommonUtils.isNull(attributes.get("cycleOfOptionality"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfOptionality")));
                        map.put("cycleOfOptionality", attributes.get("cycleOfOptionality"));
                        map.put("penaltyType", (CommonUtils.isNull(attributes.get("penaltyType"))) ? PenaltyType.valueOf("N") : PenaltyType.valueOf(attributes.get("penaltyType")));
                        map.put("penaltyRate", (CommonUtils.isNull(attributes.get("penaltyRate"))) ? 0.0 : Double.parseDouble(attributes.get("penaltyRate")));
                        map.put("objectCodeOfPrepaymentModel", attributes.get("objectCodeOfPrepaymentModel"));
                        map.put("cycleAnchorDateOfRateReset", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfRateReset"))) ? ((CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfRateReset")));
                        map.put("cycleOfRateReset", attributes.get("cycleOfRateReset"));
                        map.put("rateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("marketObjectCodeOfRateReset", attributes.get("marketObjectCodeOfRateReset"));
                        map.put("lifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("lifeCap")));
                        map.put("lifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("lifeFloor")));
                        map.put("periodCap", (CommonUtils.isNull(attributes.get("periodCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("periodCap")));
                        map.put("periodFloor", (CommonUtils.isNull(attributes.get("periodFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("periodFloor")));
                        map.put("fixingPeriod", attributes.get("fixingPeriod"));
                        map.put("nextResetRate", (CommonUtils.isNull(attributes.get("nextResetRate"))) ? null : Double.parseDouble(attributes.get("nextResetRate")));
                        map.put("rateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));
                        map.put("maturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse(attributes.get("maturityDate"))));
                        map.put("cycleAnchorDateOfInterestCalculationBase", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestCalculationBase"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestCalculationBase"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestCalculationBase")));
                        map.put("cycleOfInterestCalculationBase", attributes.get("cycleOfInterestCalculationBase"));
                        map.put("interestCalculationBase", CommonUtils.isNull(attributes.get("interestCalculationBase")) ? null : InterestCalculationBase.valueOf(attributes.get("interestCalculationBase")));
                        map.put("interestCalculationBaseAmount", (CommonUtils.isNull(attributes.get("interestCalculationBaseAmount"))) ? 0.0 : Double.parseDouble(attributes.get("interestCalculationBaseAmount")));
                        map.put("cycleAnchorDateOfPrincipalRedemption", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfPrincipalRedemption"))) ? LocalDateTime.parse(attributes.get("initialExchangeDate")) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfPrincipalRedemption")));
                        map.put("cycleOfPrincipalRedemption", attributes.get("cycleOfPrincipalRedemption"));
                        map.put("nextPrincipalRedemptionPayment", (CommonUtils.isNull(attributes.get("nextPrincipalRedemptionPayment"))) ? null : Double.parseDouble(attributes.get("nextPrincipalRedemptionPayment")));

                    case NAM:
                    	map.put("calendar",
                        	    (!CommonUtils.isNull(attributes.get("calendar")))
                        	        ? (attributes.get("calendar").equals("MF")
                        	            ? new MondayToFridayCalendar()
                        	            : (attributes.get("calendar").equals("MFH")
                        	                ? new MondayToFridayWithHolidaysCalendar(holidays)
                        	                : new NoHolidaysCalendar()) )
                        	        : new NoHolidaysCalendar()
                        	);
                        map.put("businessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("endOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("contractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("statusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("cycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee"))) ? ((CommonUtils.isNull(attributes.get("cycleOfFee"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfFee")));
                        map.put("cycleOfFee", attributes.get("cycleOfFee"));
                        map.put("feeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf(attributes.get("feeBasis")));
                        map.put("feeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble(attributes.get("feeRate")));
                        map.put("feeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble(attributes.get("feeAccrued")));
                        map.put("cycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("cycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("nominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("dayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("accruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest")));
                        map.put("capitalizationEndDate", (CommonUtils.isNull(attributes.get("capitalizationEndDate"))) ? null : LocalDateTime.parse(attributes.get("capitalizationEndDate")));
                        map.put("cyclePointOfRateReset", CommonUtils.isNull(attributes.get("cyclePointOfRateReset")) ? null : map.get("cyclePointOfInterestPayment") == CyclePointOfInterestPayment.B ? CyclePointOfRateReset.E : CyclePointOfRateReset.valueOf(attributes.get("cyclePointOfRateReset")));
                        map.put("currency", attributes.get("currency"));
                        map.put("initialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("premiumDiscountAtIED", (CommonUtils.isNull(attributes.get("premiumDiscountAtIED"))) ? 0.0 : Double.parseDouble(attributes.get("premiumDiscountAtIED")));
                        map.put("notionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("purchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("priceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("terminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("priceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("marketObjectCodeOfScalingIndex", attributes.get("marketObjectCodeOfScalingIndex"));
                        map.put("scalingIndexAtContractDealDate", (CommonUtils.isNull(attributes.get("scalingIndexAtContractDealDate"))) ? 0.0 : Double.parseDouble(attributes.get("scalingIndexAtContractDealDate")));
                        map.put("notionalScalingMultiplier", (CommonUtils.isNull(attributes.get("notionalScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("notionalScalingMultiplier")));
                        map.put("interestScalingMultiplier", (CommonUtils.isNull(attributes.get("interestScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("interestScalingMultiplier")));
                        map.put("cycleAnchorDateOfScalingIndex", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfScalingIndex"))) ? ((CommonUtils.isNull(attributes.get("cycleOfScalingIndex"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfScalingIndex")));
                        map.put("cycleOfScalingIndex", attributes.get("cycleOfScalingIndex"));
                        map.put("scalingEffect", CommonUtils.isNull(attributes.get("scalingEffect")) ? ScalingEffect.OOO : ScalingEffect.valueOf(attributes.get("scalingEffect")));
                        // TODO: review prepayment mechanism and attributes
                        map.put("cycleAnchorDateOfOptionality", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfOptionality"))) ? ((CommonUtils.isNull(attributes.get("cycleOfOptionality"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfOptionality")));
                        map.put("cycleOfOptionality", attributes.get("cycleOfOptionality"));
                        map.put("penaltyType", (CommonUtils.isNull(attributes.get("penaltyType"))) ? PenaltyType.valueOf("N") : PenaltyType.valueOf(attributes.get("penaltyType")));
                        map.put("penaltyRate", (CommonUtils.isNull(attributes.get("penaltyRate"))) ? 0.0 : Double.parseDouble(attributes.get("penaltyRate")));
                        map.put("objectCodeOfPrepaymentModel", attributes.get("objectCodeOfPrepaymentModel"));
                        map.put("cycleAnchorDateOfRateReset", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfRateReset"))) ? ((CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfRateReset")));
                        map.put("cycleOfRateReset", attributes.get("cycleOfRateReset"));
                        map.put("rateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("marketObjectCodeOfRateReset", attributes.get("marketObjectCodeOfRateReset"));
                        map.put("lifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("lifeCap")));
                        map.put("lifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("lifeFloor")));
                        map.put("periodCap", (CommonUtils.isNull(attributes.get("periodCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("periodCap")));
                        map.put("periodFloor", (CommonUtils.isNull(attributes.get("periodFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("periodFloor")));
                        map.put("fixingPeriod", attributes.get("fixingPeriod"));
                        map.put("nextResetRate", (CommonUtils.isNull(attributes.get("nextResetRate"))) ? null : Double.parseDouble(attributes.get("nextResetRate")));
                        map.put("rateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));

                        // present for LAM, NAM, ANN but not PAM
                        map.put("maturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse(attributes.get("maturityDate"))));
                        map.put("cycleAnchorDateOfInterestCalculationBase", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestCalculationBase"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestCalculationBase"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestCalculationBase")));
                        map.put("cycleOfInterestCalculationBase", attributes.get("cycleOfInterestCalculationBase"));
                        map.put("interestCalculationBase", CommonUtils.isNull(attributes.get("interestCalculationBase")) ? InterestCalculationBase.NT : InterestCalculationBase.valueOf(attributes.get("interestCalculationBase")));
                        map.put("interestCalculationBaseAmount", (CommonUtils.isNull(attributes.get("interestCalculationBaseAmount"))) ? 0.0 : Double.parseDouble(attributes.get("interestCalculationBaseAmount")));
                        map.put("cycleAnchorDateOfPrincipalRedemption", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfPrincipalRedemption"))) ? LocalDateTime.parse(attributes.get("initialExchangeDate")) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfPrincipalRedemption")));
                        map.put("cycleOfPrincipalRedemption", attributes.get("cycleOfPrincipalRedemption"));
                        map.put("nextPrincipalRedemptionPayment", (CommonUtils.isNull(attributes.get("nextPrincipalRedemptionPayment"))) ? null : Double.parseDouble(attributes.get("nextPrincipalRedemptionPayment")));

                    case ANN: // almost identical with LAM, NAM, ANN
                    	map.put("calendar",
                        	    (!CommonUtils.isNull(attributes.get("calendar")))
                        	        ? (attributes.get("calendar").equals("MF")
                        	            ? new MondayToFridayCalendar()
                        	            : (attributes.get("calendar").equals("MFH")
                        	                ? new MondayToFridayWithHolidaysCalendar(holidays)
                        	                : new NoHolidaysCalendar()) )
                        	        : new NoHolidaysCalendar()
                        	);
                        map.put("businessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("endOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("contractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("statusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("cycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee"))) ? ((CommonUtils.isNull(attributes.get("cycleOfFee"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfFee")));
                        map.put("cycleOfFee", attributes.get("cycleOfFee"));
                        map.put("feeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf(attributes.get("feeBasis")));
                        map.put("feeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble(attributes.get("feeRate")));
                        map.put("feeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble(attributes.get("feeAccrued")));
                        map.put("cycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("cycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("nominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("dayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("accruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest")));
                        map.put("capitalizationEndDate", (CommonUtils.isNull(attributes.get("capitalizationEndDate"))) ? null : LocalDateTime.parse(attributes.get("capitalizationEndDate")));
                        map.put("cyclePointOfRateReset", CommonUtils.isNull(attributes.get("cyclePointOfRateReset")) ? null : map.get("cyclePointOfInterestPayment") == CyclePointOfInterestPayment.B ? CyclePointOfRateReset.E : CyclePointOfRateReset.valueOf(attributes.get("cyclePointOfRateReset")));
                        map.put("currency", attributes.get("currency"));
                        map.put("initialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("premiumDiscountAtIED", (CommonUtils.isNull(attributes.get("premiumDiscountAtIED"))) ? 0.0 : Double.parseDouble(attributes.get("premiumDiscountAtIED")));
                        map.put("notionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("purchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("priceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("terminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("priceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("marketObjectCodeOfScalingIndex", attributes.get("marketObjectCodeOfScalingIndex"));
                        map.put("scalingIndexAtContractDealDate", (CommonUtils.isNull(attributes.get("scalingIndexAtContractDealDate"))) ? 0.0 : Double.parseDouble(attributes.get("scalingIndexAtContractDealDate")));
                        map.put("notionalScalingMultiplier", (CommonUtils.isNull(attributes.get("notionalScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("notionalScalingMultiplier")));
                        map.put("interestScalingMultiplier", (CommonUtils.isNull(attributes.get("interestScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("interestScalingMultiplier")));
                        map.put("cycleAnchorDateOfScalingIndex", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfScalingIndex"))) ? ((CommonUtils.isNull(attributes.get("cycleOfScalingIndex"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfScalingIndex")));
                        map.put("cycleOfScalingIndex", attributes.get("cycleOfScalingIndex"));
                        map.put("scalingEffect", CommonUtils.isNull(attributes.get("scalingEffect")) ? ScalingEffect.OOO : ScalingEffect.valueOf(attributes.get("scalingEffect")));
                        // TODO: review prepayment mechanism and attributes
                        map.put("cycleAnchorDateOfOptionality", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfOptionality"))) ? ((CommonUtils.isNull(attributes.get("cycleOfOptionality"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfOptionality")));
                        map.put("cycleOfOptionality", attributes.get("cycleOfOptionality"));
                        map.put("penaltyType", (CommonUtils.isNull(attributes.get("penaltyType"))) ? PenaltyType.valueOf("N") : PenaltyType.valueOf(attributes.get("penaltyType")));
                        map.put("penaltyRate", (CommonUtils.isNull(attributes.get("penaltyRate"))) ? 0.0 : Double.parseDouble(attributes.get("penaltyRate")));
                        map.put("objectCodeOfPrepaymentModel", attributes.get("objectCodeOfPrepaymentModel"));
                        map.put("cycleAnchorDateOfRateReset", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfRateReset"))) ? ((CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfRateReset")));
                        map.put("cycleOfRateReset", attributes.get("cycleOfRateReset"));
                        map.put("rateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("marketObjectCodeOfRateReset", attributes.get("marketObjectCodeOfRateReset"));
                        map.put("lifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("lifeCap")));
                        map.put("lifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("lifeFloor")));
                        map.put("periodCap", (CommonUtils.isNull(attributes.get("periodCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("periodCap")));
                        map.put("periodFloor", (CommonUtils.isNull(attributes.get("periodFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("periodFloor")));
                        map.put("fixingPeriod", attributes.get("fixingPeriod"));
                        map.put("nextResetRate", (CommonUtils.isNull(attributes.get("nextResetRate"))) ? null : Double.parseDouble(attributes.get("nextResetRate")));
                        map.put("rateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));

                        // present for LAM, NAM, ANN but not PAM
                        map.put("maturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse(attributes.get("maturityDate"))));
                        map.put("cycleAnchorDateOfInterestCalculationBase", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestCalculationBase"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestCalculationBase"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestCalculationBase")));
                        map.put("cycleOfInterestCalculationBase", attributes.get("cycleOfInterestCalculationBase"));
                        map.put("interestCalculationBase", CommonUtils.isNull(attributes.get("interestCalculationBase")) ? InterestCalculationBase.NT : InterestCalculationBase.valueOf(attributes.get("interestCalculationBase")));
                        map.put("interestCalculationBaseAmount", (CommonUtils.isNull(attributes.get("interestCalculationBaseAmount"))) ? 0.0 : Double.parseDouble(attributes.get("interestCalculationBaseAmount")));
                        map.put("cycleAnchorDateOfPrincipalRedemption", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfPrincipalRedemption"))) ? LocalDateTime.parse(attributes.get("initialExchangeDate")) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfPrincipalRedemption")));
                        map.put("cycleOfPrincipalRedemption", attributes.get("cycleOfPrincipalRedemption"));
                        map.put("nextPrincipalRedemptionPayment", (CommonUtils.isNull(attributes.get("nextPrincipalRedemptionPayment"))) ? null : Double.parseDouble(attributes.get("nextPrincipalRedemptionPayment")));

                        // present for ANN but not for LAM, NAM
                        map.put("amortizationDate", (CommonUtils.isNull(attributes.get("amortizationDate")) ? null : LocalDateTime.parse(attributes.get("amortizationDate"))));

                        break;
                    case CLM:
                    	map.put("calendar",
                        	    (!CommonUtils.isNull(attributes.get("calendar")))
                        	        ? (attributes.get("calendar").equals("MF")
                        	            ? new MondayToFridayCalendar()
                        	            : (attributes.get("calendar").equals("MFH")
                        	                ? new MondayToFridayWithHolidaysCalendar(holidays)
                        	                : new NoHolidaysCalendar()) )
                        	        : new NoHolidaysCalendar()
                        	);
                        map.put("businessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("endOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("contractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("statusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("cycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee"))) ? ((CommonUtils.isNull(attributes.get("cycleOfFee"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfFee")));
                        map.put("cycleOfFee", attributes.get("cycleOfFee"));
                        map.put("feeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf(attributes.get("feeBasis")));
                        map.put("feeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble(attributes.get("feeRate")));
                        map.put("feeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble(attributes.get("feeAccrued")));
                        map.put("cycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("cycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("nominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("dayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("accruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest")));
                        map.put("currency", attributes.get("currency"));
                        map.put("initialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("notionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("maturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse(attributes.get("maturityDate"))));
                        map.put("xDayNotice", attributes.get("xDayNotice"));
                        map.put("cycleAnchorDateOfRateReset", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfRateReset"))) ? ((CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfRateReset")));
                        map.put("cycleOfRateReset", attributes.get("cycleOfRateReset"));
                        map.put("rateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("marketObjectCodeOfRateReset", attributes.get("marketObjectCodeOfRateReset"));
                        map.put("fixingPeriod", attributes.get("fixingPeriod"));
                        map.put("nextResetRate", (CommonUtils.isNull(attributes.get("nextResetRate"))) ? null : Double.parseDouble(attributes.get("nextResetRate")));
                        map.put("rateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));
                        map.put("lifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("lifeCap")));
                        map.put("lifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("lifeFloor")));
                        map.put("periodCap", (CommonUtils.isNull(attributes.get("periodCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("periodCap")));
                        map.put("periodFloor", (CommonUtils.isNull(attributes.get("periodFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("periodFloor")));
                        break;
                    case UMP:
                    	map.put("calendar",
                        	    (!CommonUtils.isNull(attributes.get("calendar")))
                        	        ? (attributes.get("calendar").equals("MF")
                        	            ? new MondayToFridayCalendar()
                        	            : (attributes.get("calendar").equals("MFH")
                        	                ? new MondayToFridayWithHolidaysCalendar(holidays)
                        	                : new NoHolidaysCalendar()) )
                        	        : new NoHolidaysCalendar()
                        	);
                        map.put("businessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("endOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("contractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("statusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("cycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee"))) ? ((CommonUtils.isNull(attributes.get("cycleOfFee"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfFee")));
                        map.put("cycleOfFee", attributes.get("cycleOfFee"));
                        map.put("feeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf(attributes.get("feeBasis")));
                        map.put("feeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble(attributes.get("feeRate")));
                        map.put("feeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble(attributes.get("feeAccrued")));
                        map.put("cycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("cycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("nominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("dayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("accruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest")));
                        map.put("currency", attributes.get("currency"));
                        map.put("initialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("notionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("terminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("priceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("xDayNotice", attributes.get("xDayNotice"));
                        map.put("maximumPenaltyFreeDisbursement", (CommonUtils.isNull(attributes.get("maximumPenaltyFreeDisbursement"))) ? attributes.get("notionalPrincipal") : attributes.get("maximumPenaltyFreeDisbursement"));
                        map.put("cycleAnchorDateOfRateReset", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfRateReset"))) ? ((CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfRateReset")));
                        map.put("cycleOfRateReset", attributes.get("cycleOfRateReset"));
                        map.put("rateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("marketObjectCodeOfRateReset", attributes.get("marketObjectCodeOfRateReset"));
                        map.put("fixingPeriod", attributes.get("fixingPeriod"));
                        map.put("nextResetRate", (CommonUtils.isNull(attributes.get("nextResetRate"))) ? null : Double.parseDouble(attributes.get("nextResetRate")));
                        map.put("rateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));
		            	map.put("lifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("lifeCap"))) ;
			            map.put("lifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("lifeFloor"))) ;
                        map.put("periodCap", (CommonUtils.isNull(attributes.get("periodCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("periodCap"))) ;
                     	map.put("periodFloor", (CommonUtils.isNull(attributes.get("periodFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("periodFloor"))) ;
                        break;
                    case CSH:

                        map.put("contractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("statusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("currency", attributes.get("currency"));
                        map.put("notionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));

                        break;
                    case COM: // almost identical with STK

                        map.put("contractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("statusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("currency", attributes.get("currency"));
                        map.put("quantity", (CommonUtils.isNull(attributes.get("quantity"))) ? 1 : Double.parseDouble(attributes.get("quantity")));
                        map.put("purchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("priceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("terminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("priceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("marketValueObserved", (CommonUtils.isNull(attributes.get("marketValueObserved"))) ? 0.0 : Double.parseDouble(attributes.get("marketValueObserved")));

                        break;
                    case STK:

                        map.put("contractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("statusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("currency", attributes.get("currency"));
                        map.put("quantity", (CommonUtils.isNull(attributes.get("quantity"))) ? 1 : Double.parseDouble(attributes.get("quantity")));
                        map.put("purchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("priceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("terminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("priceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("marketValueObserved", (CommonUtils.isNull(attributes.get("marketValueObserved"))) ? 0.0 : Double.parseDouble(attributes.get("marketValueObserved")));

                        // present for STK but not COM
                        map.put("calendar",
                        	    (!CommonUtils.isNull(attributes.get("calendar")))
                        	        ? (attributes.get("calendar").equals("MF")
                        	            ? new MondayToFridayCalendar()
                        	            : (attributes.get("calendar").equals("MFH")
                        	                ? new MondayToFridayWithHolidaysCalendar(holidays)
                        	                : new NoHolidaysCalendar()) )
                        	        : new NoHolidaysCalendar()
                        	);
                        map.put("businessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("endOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("cycleAnchorDateOfDividendPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfDividendPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfDividendPayment"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfDividendPayment")));
                        map.put("cycleOfDividendPayment", attributes.get("cycleOfDividendPayment"));
                        map.put("marketObjectCodeOfDividends", attributes.get("marketObjectCodeOfDividends"));
                        
						/*
						 * Fixed next dividend payment amount (DVNP). If provided and >= 0, the
						 * POF_DV_STK payoff function will use this value instead of market-observed
						 * dividend amounts. Implements ACTUS DVNP attribute.
						 */
                        map.put("nextDividendPaymentAmount", (CommonUtils.isNull(attributes.get("nextDividendPaymentAmount"))) ? null : Double.parseDouble(attributes.get("nextDividendPaymentAmount")));

                        break;
                    case FXOUT:
                    	map.put("calendar",
                        	    (!CommonUtils.isNull(attributes.get("calendar")))
                        	        ? (attributes.get("calendar").equals("MF")
                        	            ? new MondayToFridayCalendar()
                        	            : (attributes.get("calendar").equals("MFH")
                        	                ? new MondayToFridayWithHolidaysCalendar(holidays)
                        	                : new NoHolidaysCalendar()) )
                        	        : new NoHolidaysCalendar()
                        	);
                        map.put("businessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("endOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("contractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("statusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("currency", attributes.get("currency"));
                        map.put("currency2", attributes.get("currency2"));
                        map.put("maturityDate", LocalDateTime.parse(attributes.get("maturityDate")));
                        map.put("notionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("notionalPrincipal2", Double.parseDouble(attributes.get("notionalPrincipal2")));
                        map.put("purchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("priceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("terminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("priceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("deliverySettlement", CommonUtils.isNull(attributes.get("deliverySettlement")) ? null : DeliverySettlement.valueOf(attributes.get("deliverySettlement")));
                        map.put("settlementPeriod", (CommonUtils.isNull(attributes.get("settlementPeriod"))) ? "P0D" : attributes.get("settlementPeriod"));
    
                        break;
                    case SWPPV:
                        map.put("accruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest")));
                        map.put("accruedInterest2", (CommonUtils.isNull(attributes.get("accruedInterest2"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest2")));
                        map.put("calendar",
                        	    (!CommonUtils.isNull(attributes.get("calendar")))
                        	        ? (attributes.get("calendar").equals("MF")
                        	            ? new MondayToFridayCalendar()
                        	            : (attributes.get("calendar").equals("MFH")
                        	                ? new MondayToFridayWithHolidaysCalendar(holidays)
                        	                : new NoHolidaysCalendar()) )
                        	        : new NoHolidaysCalendar()
                        	);
                        map.put("businessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("endOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("contractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("statusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("creatorID", attributes.get("creatorID"));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("cycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("cycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("nominalInterestRate", Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("nominalInterestRate2", Double.parseDouble(attributes.get("nominalInterestRate2")));
                        map.put("dayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("currency", attributes.get("currency"));
                        map.put("initialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("maturityDate", LocalDateTime.parse(attributes.get("maturityDate")));
                        map.put("notionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("purchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("priceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("terminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("priceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("cycleAnchorDateOfRateReset", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfRateReset"))) ? ((CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfRateReset")));
                        map.put("cycleOfRateReset", (CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : attributes.get("cycleOfRateReset"));
                        map.put("rateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("marketObjectCodeOfRateReset", (CommonUtils.isNull(attributes.get("marketObjectCodeOfRateReset"))) ? null :attributes.get("marketObjectCodeOfRateReset"));
                        map.put("cyclePointOfRateReset", CommonUtils.isNull(attributes.get("cyclePointOfRateReset")) ? null : map.get("cyclePointOfInterestPayment") == CyclePointOfInterestPayment.B ? CyclePointOfRateReset.E : CyclePointOfRateReset.valueOf(attributes.get("cyclePointOfRateReset")));
                        map.put("fixingPeriod", (CommonUtils.isNull(attributes.get("fixingPeriod"))) ? null : attributes.get("fixingPeriod"));
                        map.put("nextResetRate", (CommonUtils.isNull(attributes.get("nextResetRate"))) ? null : Double.parseDouble(attributes.get("nextResetRate")));
                        map.put("rateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));
                        map.put("deliverySettlement", CommonUtils.isNull(attributes.get("deliverySettlement")) ? null : DeliverySettlement.valueOf(attributes.get("deliverySettlement")));
                        break;

                    case LAX:
                    	map.put("calendar",
                        	    (!CommonUtils.isNull(attributes.get("calendar")))
                        	        ? (attributes.get("calendar").equals("MF")
                        	            ? new MondayToFridayCalendar()
                        	            : (attributes.get("calendar").equals("MFH")
                        	                ? new MondayToFridayWithHolidaysCalendar(holidays)
                        	                : new NoHolidaysCalendar()) )
                        	        : new NoHolidaysCalendar()
                        	);
                        map.put("businessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("contractID", attributes.get("contractID"));
                        map.put("statusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("contractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("counterpartyID", attributes.get("counterpartyID"));
                        map.put("marketObjectCode", attributes.get("marketObjectCode"));
                        map.put("legalEntityIDRecordCreator", attributes.get("legalEntityIDRecordCreator"));
                        map.put("currency", attributes.get("currency"));
                        map.put("initialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("premiumDiscountAtIED", (CommonUtils.isNull(attributes.get("premiumDiscountAtIED"))) ? 0.0 : Double.parseDouble(attributes.get("premiumDiscountAtIED")));
                        map.put("maturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse(attributes.get("maturityDate"))));
                        map.put("notionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("arrayCycleAnchorDateOfPrincipalRedemption", attributes.get("arrayCycleAnchorDateOfPrincipalRedemption"));
                        map.put("arrayCycleOfPrincipalRedemption", attributes.get("arrayCycleOfPrincipalRedemption"));
                        map.put("arrayNextPrincipalRedemptionPayment", (CommonUtils.isNull(attributes.get("arrayNextPrincipalRedemptionPayment"))) ? 0 : attributes.get("arrayNextPrincipalRedemptionPayment"));
                        map.put("arrayIncreaseDecrease", attributes.get("arrayIncreaseDecrease"));
                        map.put("arrayCycleAnchorDateOfInterestPayment", attributes.get("arrayCycleAnchorDateOfInterestPayment"));
                        map.put("arrayCycleOfInterestPayment", attributes.get("arrayCycleOfInterestPayment"));
                        map.put("nominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("dayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("calendar")));
                        map.put("arrayCycleAnchorDateOfRateReset", attributes.get("arrayCycleAnchorDateOfRateReset"));
                        map.put("arrayCycleOfRateReset", attributes.get("arrayCycleOfRateReset"));
                        map.put("arrayRate", attributes.get("arrayRate"));
                        map.put("arrayFixedVariable", attributes.get("arrayFixedVariable"));
                        map.put("accruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest")));
                        map.put("feeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeAccrued")));
                        map.put("marketObjectCodeOfRateReset", attributes.get("marketObjectCodeOfRateReset"));
                        map.put("contractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("feeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble(attributes.get("feeRate")));
                        map.put("endOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("rateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));
                        map.put("rateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("periodCap", (CommonUtils.isNull(attributes.get("periodCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("periodCap")));
                        map.put("periodFloor", (CommonUtils.isNull(attributes.get("periodFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("periodFloor")));
                        map.put("lifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("lifeCap")));
                        map.put("lifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("lifeFloor")));
                        map.put("cycleAnchorDateOfInterestCalculationBase", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestCalculationBase"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestCalculationBase"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestCalculationBase")));
                        map.put("cycleOfInterestCalculationBase", attributes.get("cycleOfInterestCalculationBase"));
                        map.put("interestCalculationBase", CommonUtils.isNull(attributes.get("interestCalculationBase")) ? InterestCalculationBase.NT : InterestCalculationBase.valueOf(attributes.get("interestCalculationBase")));
                        map.put("interestCalculationBaseAmount", (CommonUtils.isNull(attributes.get("interestCalculationBaseAmount"))) ? 0.0 : Double.parseDouble(attributes.get("interestCalculationBaseAmount")));
                        map.put("cycleAnchorDateOfPrincipalRedemption", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfPrincipalRedemption"))) ? LocalDateTime.parse(attributes.get("initialExchangeDate")) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfPrincipalRedemption")));
                        break;
                    default:
                        throw new ContractTypeUnknownException();
                }
            } catch (Exception e) {
                throw new AttributeConversionException();
            }
        }
        return new ContractModel(map);
    }
    
    public static Set<LocalDate> getHolidays(Map<String, Object> contractAttributes){
    	
      	 Set<LocalDate> holidays = new HashSet<>();
           Object holidayObj = contractAttributes.get("holidays");
           if (holidayObj instanceof List<?>) {
               for (Object o : (List<?>) holidayObj) {
                   if (o instanceof String s) {
                       holidays.add(LocalDate.parse(s));
                   }
               }
           }
      	
   		return holidays;
      	
      }

    @Override
    public <T> T getAs(String name) {
        return (T) attributes.get(name);
    }

    public void addAttribute(String Key, Object value){
        attributes.put(Key,value);
    }
}
