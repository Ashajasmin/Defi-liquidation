package org.actus.risksrv3.models.supplychaintariff1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * RevenueElasticityModelData
 *
 * MongoDB document for RevenueElasticityModel configuration.
 * Collection: revenueElasticityModels
 *
 * Domain 2 — Model 7.4: Revenue Elasticity
 * References: Armington (1969), GTAP Armington elasticities by sector
 *
 * Not all tariff increases pass through equally to revenue.
 * Uses product-specific elasticity parameters to compute revenue
 * impact from tariff changes. Feeds into WorkingCapitalStressModel.
 *
 * JSON example:
 * {
 *   "riskFactorId": "re_ind01",
 *   "tariffIndexMOC": "TARIFF_INDEX",
 *   "productElasticity": 2.8,
 *   "baseRevenue": 10000000.0,
 *   "passThrough": 0.6,
 *   "revenueFloorFraction": 0.5,
 *   "monitoringEventTimes": ["2025-06-01T00:00:00", ...]
 * }
 */
@Document(collection = "revenueElasticityModels")
public class RevenueElasticityModelData {

    @Id
    private String riskFactorId;

    private String tariffIndexMOC;
    private double productElasticity;
    private double baseRevenue;
    private double passThrough;
    private double revenueFloorFraction;
    private List<String> monitoringEventTimes;

    public RevenueElasticityModelData() {
    }

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getTariffIndexMOC() { return tariffIndexMOC; }
    public void setTariffIndexMOC(String tariffIndexMOC) { this.tariffIndexMOC = tariffIndexMOC; }

    public double getProductElasticity() { return productElasticity; }
    public void setProductElasticity(double productElasticity) { this.productElasticity = productElasticity; }

    public double getBaseRevenue() { return baseRevenue; }
    public void setBaseRevenue(double baseRevenue) { this.baseRevenue = baseRevenue; }

    public double getPassThrough() { return passThrough; }
    public void setPassThrough(double passThrough) { this.passThrough = passThrough; }

    public double getRevenueFloorFraction() { return revenueFloorFraction; }
    public void setRevenueFloorFraction(double revenueFloorFraction) { this.revenueFloorFraction = revenueFloorFraction; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
