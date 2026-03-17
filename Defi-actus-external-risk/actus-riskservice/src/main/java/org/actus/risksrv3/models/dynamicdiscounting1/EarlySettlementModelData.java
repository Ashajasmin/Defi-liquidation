package org.actus.risksrv3.models.dynamicdiscounting1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.Map;

@Document(collection = "earlySettlementModels")
public class EarlySettlementModelData {
    @Id
    private String riskFactorId;
    private String invoiceDate;
    private String dueDate;
    private double notionalAmount;
    private String discountFunctionType;
    private double maxDiscountRate;
    private double decayLambda;
    private double powerAlpha;
    private Map<Integer, Double> stepDiscountSchedule;
    private String customDiscountMOC;
    private double hurdleRateAnnualized;
    private String buyerCashMOC;
    private List<String> monitoringEventTimes;
    public EarlySettlementModelData() {}
    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String v) { this.riskFactorId = v; }
    public String getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(String v) { this.invoiceDate = v; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String v) { this.dueDate = v; }
    public double getNotionalAmount() { return notionalAmount; }
    public void setNotionalAmount(double v) { this.notionalAmount = v; }
    public String getDiscountFunctionType() { return discountFunctionType; }
    public void setDiscountFunctionType(String v) { this.discountFunctionType = v; }
    public double getMaxDiscountRate() { return maxDiscountRate; }
    public void setMaxDiscountRate(double v) { this.maxDiscountRate = v; }
    public double getDecayLambda() { return decayLambda; }
    public void setDecayLambda(double v) { this.decayLambda = v; }
    public double getPowerAlpha() { return powerAlpha; }
    public void setPowerAlpha(double v) { this.powerAlpha = v; }
    public Map<Integer, Double> getStepDiscountSchedule() { return stepDiscountSchedule; }
    public void setStepDiscountSchedule(Map<Integer, Double> v) { this.stepDiscountSchedule = v; }
    public String getCustomDiscountMOC() { return customDiscountMOC; }
    public void setCustomDiscountMOC(String v) { this.customDiscountMOC = v; }
    public double getHurdleRateAnnualized() { return hurdleRateAnnualized; }
    public void setHurdleRateAnnualized(double v) { this.hurdleRateAnnualized = v; }
    public String getBuyerCashMOC() { return buyerCashMOC; }
    public void setBuyerCashMOC(String v) { this.buyerCashMOC = v; }
    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> v) { this.monitoringEventTimes = v; }
}
