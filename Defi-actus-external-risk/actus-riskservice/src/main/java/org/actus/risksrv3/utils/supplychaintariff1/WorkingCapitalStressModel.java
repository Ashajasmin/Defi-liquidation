package org.actus.risksrv3.utils.supplychaintariff1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.supplychaintariff1.WorkingCapitalStressModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * WorkingCapitalStressModel (Domain 2 — Model 7.2)
 *
 * Working Capital Stress for revolving credit (CLM) contracts.
 *
 * Tariff escalation reduces revenue, extends collection cycles (DSO),
 * increases inventory holding periods (DIO). Monitors TARIFF_INDEX,
 * REVENUE_INDEX, DSO_INDEX to compute expected working capital gap.
 *
 * Cash Conversion Cycle (CCC) = DSO + DIO - DPO
 * Working capital gap = CCC × (daily revenue)
 *
 * Returns: drawdown fraction (0.0 = no stress, 1.0 = full facility draw)
 */
public class WorkingCapitalStressModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String tariffIndexMOC;
    private final String revenueIndexMOC;
    private final String dsoIndexMOC;
    private final double baseDSO;
    private final double baseDIO;
    private final double baseDPO;
    private final double tariffDSOSensitivity;
    private final double tariffDIOSensitivity;
    private final double maxDrawdownFraction;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public WorkingCapitalStressModel(String riskFactorId,
                                     WorkingCapitalStressModelData data,
                                     MultiMarketRiskModel marketModel) {
        this.riskFactorId          = riskFactorId;
        this.tariffIndexMOC        = data.getTariffIndexMOC();
        this.revenueIndexMOC       = data.getRevenueIndexMOC();
        this.dsoIndexMOC           = data.getDsoIndexMOC();
        this.baseDSO               = data.getBaseDSO();
        this.baseDIO               = data.getBaseDIO();
        this.baseDPO               = data.getBaseDPO();
        this.tariffDSOSensitivity  = data.getTariffDSOSensitivity();
        this.tariffDIOSensitivity  = data.getTariffDIOSensitivity();
        this.maxDrawdownFraction   = data.getMaxDrawdownFraction();
        this.monitoringEventTimes  = data.getMonitoringEventTimes();
        this.marketModel           = marketModel;
    }

    @Override
    public Set<String> keys() {
        Set<String> keys = new HashSet<>();
        keys.add(this.riskFactorId);
        return keys;
    }

    @Override
    public List<CalloutData> contractStart(ContractModel contract) {
        // PP-before-IED fix: filter out callouts before contract starts
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        List<CalloutData> callouts = new ArrayList<>();
        for (String eventTime : this.monitoringEventTimes) {
            if (ied != null) {
                LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);
                if (eventDateTime.isBefore(ied)) {
                    System.out.println("**** WorkingCapitalStressModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * Compute working capital stress drawdown fraction.
     *
     * @return drawdown fraction (0.0–1.0)
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double currentTariff = this.marketModel.stateAt(this.tariffIndexMOC, time);
        double revenueIndex = this.marketModel.stateAt(this.revenueIndexMOC, time);
        double dsoIndex = this.marketModel.stateAt(this.dsoIndexMOC, time);

        // Tariff stress extends DSO and DIO
        double stressedDSO = this.baseDSO + (this.tariffDSOSensitivity * currentTariff * this.baseDSO);
        double stressedDIO = this.baseDIO + (this.tariffDIOSensitivity * currentTariff * this.baseDIO);

        // CCC = DSO + DIO - DPO
        double baseCCC = this.baseDSO + this.baseDIO - this.baseDPO;
        double stressedCCC = stressedDSO + stressedDIO - this.baseDPO;

        // Gap ratio: how much CCC expanded relative to base
        double cccExpansion = (baseCCC > 0)
                ? (stressedCCC - baseCCC) / baseCCC
                : 0.0;

        // Revenue decline amplifies stress
        double revenueStress = Math.max(0.0, 1.0 - revenueIndex);

        // Combined drawdown = CCC expansion + revenue stress, capped
        double drawdown = Math.min(cccExpansion + revenueStress, this.maxDrawdownFraction);
        drawdown = Math.max(0.0, drawdown);

        System.out.println("**** WorkingCapitalStressModel: time=" + time
                + " tariff=" + String.format("%.4f", currentTariff)
                + " DSO=" + String.format("%.1f→%.1f", this.baseDSO, stressedDSO)
                + " CCC=" + String.format("%.1f→%.1f", baseCCC, stressedCCC)
                + " drawdown=" + String.format("%.4f", drawdown));

        return drawdown;
    }
}
