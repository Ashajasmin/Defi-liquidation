package org.actus.risksrv3.models.dynamicdiscounting1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * DynamicDiscountingModelData — MongoDB document for dynamic discounting
 * behavioral model configuration.
 *
 * Models supply-chain invoice dynamic discounting using the ACTUS PAM contract
 * with negative interest rate and daily rate reset (cycleOfRateReset = P1DL1).
 * The discount decays linearly from discountRateStart to 0 over the invoice term.
 *
 * The DISCOUNT_RATE_INDEX reference index provides the daily discount rate curve.
 * At any monitoring time, the model reads nominalAccrued (negative, representing
 * the cumulative discount) and returns the early-payment prepayment fraction.
 *
 * ACTUS modeling approach (from design iterations):
 *   - contractType: PAM
 *   - contractRole: RPA
 *   - nominalInterestRate: discountRateStart (negative, e.g. -0.02)
 *   - cycleOfRateReset: P1DL1 (daily)
 *   - marketObjectCodeOfRateReset: DISCOUNT_RATE_INDEX
 *   - dayCountConvention: AA (Actual/Actual)
 *
 * The stateAt() method returns a prepayment fraction (0.0 to 1.0) representing
 * the proportion of the invoice to prepay, which the ACTUS engine uses to trigger
 * a PP (Prepayment) event on the underlying PAM contract.
 */
@Document(collection = "dynamicDiscountingModels")
public class DynamicDiscountingModelData {
    @Id
    private String riskFactorId;
    private String discountRateIndexMOC;        // MOC for the DISCOUNT_RATE_INDEX reference index
    private double invoiceFaceValue;             // notionalPrincipal of the invoice PAM
    private double discountRateStart;            // initial annual discount rate (negative, e.g. -0.02 = 2%)
    private String invoiceDate;                  // initialExchangeDate (ISO datetime)
    private String maturityDate;                 // maturityDate (ISO datetime)
    private String dayCountConvention;           // e.g. "AA" for Actual/Actual
    private String supplierID;                   // counterpartyID
    private String buyerID;                      // creatorID
    private double earlyPaymentThreshold;        // min discount % to trigger early payment (e.g. 0.005 = 0.5%)
    private List<String> monitoringEventTimes;   // daily monitoring times for behavioral callouts

    public DynamicDiscountingModelData() {}

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String v) { this.riskFactorId = v; }
    public String getDiscountRateIndexMOC() { return discountRateIndexMOC; }
    public void setDiscountRateIndexMOC(String v) { this.discountRateIndexMOC = v; }
    public double getInvoiceFaceValue() { return invoiceFaceValue; }
    public void setInvoiceFaceValue(double v) { this.invoiceFaceValue = v; }
    public double getDiscountRateStart() { return discountRateStart; }
    public void setDiscountRateStart(double v) { this.discountRateStart = v; }
    public String getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(String v) { this.invoiceDate = v; }
    public String getMaturityDate() { return maturityDate; }
    public void setMaturityDate(String v) { this.maturityDate = v; }
    public String getDayCountConvention() { return dayCountConvention; }
    public void setDayCountConvention(String v) { this.dayCountConvention = v; }
    public String getSupplierID() { return supplierID; }
    public void setSupplierID(String v) { this.supplierID = v; }
    public String getBuyerID() { return buyerID; }
    public void setBuyerID(String v) { this.buyerID = v; }
    public double getEarlyPaymentThreshold() { return earlyPaymentThreshold; }
    public void setEarlyPaymentThreshold(double v) { this.earlyPaymentThreshold = v; }
    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> v) { this.monitoringEventTimes = v; }
}
