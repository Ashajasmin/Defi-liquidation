package org.actus.risksrv3.models.defiliquidation1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "invoiceMaturityModels")
public class InvoiceMaturityModelData {
    @Id
    private String riskFactorId;
    private String collateralPriceMOC;
    private String invoicePaymentProbMOC;
    private double collateralQuantity;
    private double invoiceFaceValue;
    private String invoiceMaturityDate;
    private double overdueDegradationRate;
    private double creditDiscountRate;
    private double ltvThreshold;
    private double liquidationThreshold;
    private double ltvTarget;
    private List<String> monitoringEventTimes;

    public InvoiceMaturityModelData() {}
    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String v) { this.riskFactorId = v; }
    public String getCollateralPriceMOC() { return collateralPriceMOC; }
    public void setCollateralPriceMOC(String v) { this.collateralPriceMOC = v; }
    public String getInvoicePaymentProbMOC() { return invoicePaymentProbMOC; }
    public void setInvoicePaymentProbMOC(String v) { this.invoicePaymentProbMOC = v; }
    public double getCollateralQuantity() { return collateralQuantity; }
    public void setCollateralQuantity(double v) { this.collateralQuantity = v; }
    public double getInvoiceFaceValue() { return invoiceFaceValue; }
    public void setInvoiceFaceValue(double v) { this.invoiceFaceValue = v; }
    public String getInvoiceMaturityDate() { return invoiceMaturityDate; }
    public void setInvoiceMaturityDate(String v) { this.invoiceMaturityDate = v; }
    public double getOverdueDegradationRate() { return overdueDegradationRate; }
    public void setOverdueDegradationRate(double v) { this.overdueDegradationRate = v; }
    public double getCreditDiscountRate() { return creditDiscountRate; }
    public void setCreditDiscountRate(double v) { this.creditDiscountRate = v; }
    public double getLtvThreshold() { return ltvThreshold; }
    public void setLtvThreshold(double v) { this.ltvThreshold = v; }
    public double getLiquidationThreshold() { return liquidationThreshold; }
    public void setLiquidationThreshold(double v) { this.liquidationThreshold = v; }
    public double getLtvTarget() { return ltvTarget; }
    public void setLtvTarget(double v) { this.ltvTarget = v; }
    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> v) { this.monitoringEventTimes = v; }
}