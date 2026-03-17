package org.actus.risksrv3.models.hybridtreasury1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "cashConversionCycleModels")
public class CashConversionCycleModelData {

    @Id
    private String riskFactorId;

    private String discountAPRMOC;
    private String treasuryYieldMOC;
    private String availableDiscountsMOC;
    private double minArbitrageSpread;
    private List<String> monitoringEventTimes;

    public CashConversionCycleModelData() {}

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getDiscountAPRMOC() { return discountAPRMOC; }
    public void setDiscountAPRMOC(String discountAPRMOC) { this.discountAPRMOC = discountAPRMOC; }

    public String getTreasuryYieldMOC() { return treasuryYieldMOC; }
    public void setTreasuryYieldMOC(String treasuryYieldMOC) { this.treasuryYieldMOC = treasuryYieldMOC; }

    public String getAvailableDiscountsMOC() { return availableDiscountsMOC; }
    public void setAvailableDiscountsMOC(String availableDiscountsMOC) { this.availableDiscountsMOC = availableDiscountsMOC; }

    public double getMinArbitrageSpread() { return minArbitrageSpread; }
    public void setMinArbitrageSpread(double minArbitrageSpread) { this.minArbitrageSpread = minArbitrageSpread; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
