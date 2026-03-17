package org.actus.risksrv3.models.dynamicdiscounting1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.Map;

@Document(collection = "factoringDecisionModels")
public class FactoringDecisionModelData {
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
    private double bankFactoringRateAnnualized;
    private double reverseFactoringRateAnnualized;
    private List<String> monitoringEventTimes;
    public FactoringDecisionModelData() {}
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
    public double getBankFactoringRateAnnualized() { return bankFactoringRateAnnualized; }
    public void setBankFactoringRateAnnualized(double v) { this.bankFactoringRateAnnualized = v; }
    public double getReverseFactoringRateAnnualized() { return reverseFactoringRateAnnualized; }
    public void setReverseFactoringRateAnnualized(double v) { this.reverseFactoringRateAnnualized = v; }
    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> v) { this.monitoringEventTimes = v; }
}
