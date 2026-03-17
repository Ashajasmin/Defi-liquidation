package org.actus.risksrv3.models.dynamicdiscounting1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.Map;

@Document(collection = "penaltyAccrualModels")
public class PenaltyAccrualModelData {
    @Id
    private String riskFactorId;
    private String dueDate;
    private String penaltyFunctionType;
    private double basePenaltyRate;
    private double penaltyGrowthLambda;
    private double penaltyPowerAlpha;
    private Map<Integer, Double> penaltyStepSchedule;
    private String penaltyRateMOC;
    private int penaltyHorizonDays;
    private List<String> monitoringEventTimes;
    public PenaltyAccrualModelData() {}
    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String v) { this.riskFactorId = v; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String v) { this.dueDate = v; }
    public String getPenaltyFunctionType() { return penaltyFunctionType; }
    public void setPenaltyFunctionType(String v) { this.penaltyFunctionType = v; }
    public double getBasePenaltyRate() { return basePenaltyRate; }
    public void setBasePenaltyRate(double v) { this.basePenaltyRate = v; }
    public double getPenaltyGrowthLambda() { return penaltyGrowthLambda; }
    public void setPenaltyGrowthLambda(double v) { this.penaltyGrowthLambda = v; }
    public double getPenaltyPowerAlpha() { return penaltyPowerAlpha; }
    public void setPenaltyPowerAlpha(double v) { this.penaltyPowerAlpha = v; }
    public Map<Integer, Double> getPenaltyStepSchedule() { return penaltyStepSchedule; }
    public void setPenaltyStepSchedule(Map<Integer, Double> v) { this.penaltyStepSchedule = v; }
    public String getPenaltyRateMOC() { return penaltyRateMOC; }
    public void setPenaltyRateMOC(String v) { this.penaltyRateMOC = v; }
    public int getPenaltyHorizonDays() { return penaltyHorizonDays; }
    public void setPenaltyHorizonDays(int v) { this.penaltyHorizonDays = v; }
    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> v) { this.monitoringEventTimes = v; }
}
