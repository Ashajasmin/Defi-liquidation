package org.actus.risksrv3.utils.hybridtreasury1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.hybridtreasury1.CashConversionCycleModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CashConversionCycleModel (Domain 4 — Treasury Model 5.6)
 *
 * Bridges treasury management and working capital optimization.
 * When discount APR > treasury yield + minArbitrageSpread, signals
 * T-bill redemption to capture supplier discount.
 *
 * ACTUS contract type: CSH/PAM (treasury position)
 * Market Object Codes consumed:
 *   DISCOUNT_APR, TREASURY_YIELD, AVAILABLE_DISCOUNTS
 */
public class CashConversionCycleModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String discountAPRMOC;
    private final String treasuryYieldMOC;
    private final String availableDiscountsMOC;
    private final double minArbitrageSpread;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public CashConversionCycleModel(String riskFactorId,
                                    CashConversionCycleModelData data,
                                    MultiMarketRiskModel marketModel) {
        this.riskFactorId         = riskFactorId;
        this.discountAPRMOC       = data.getDiscountAPRMOC();
        this.treasuryYieldMOC     = data.getTreasuryYieldMOC();
        this.availableDiscountsMOC = data.getAvailableDiscountsMOC();
        this.minArbitrageSpread   = data.getMinArbitrageSpread();
        this.monitoringEventTimes = data.getMonitoringEventTimes();
        this.marketModel          = marketModel;
    }

    @Override
    public Set<String> keys() {
        Set<String> keys = new HashSet<>();
        keys.add(this.riskFactorId);
        return keys;
    }

    @Override
    public List<CalloutData> contractStart(ContractModel contract) {
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        List<CalloutData> callouts = new ArrayList<>();
        for (String eventTime : this.monitoringEventTimes) {
            if (ied != null) { LocalDateTime edt = LocalDateTime.parse(eventTime); if (edt.isBefore(ied)) { System.out.println("**** CashConversionCycleModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")"); continue; } }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double discountAPR      = this.marketModel.stateAt(this.discountAPRMOC, time);
        double treasuryYield    = this.marketModel.stateAt(this.treasuryYieldMOC, time);
        double availableAmount  = this.marketModel.stateAt(this.availableDiscountsMOC, time);

        double spread = discountAPR - treasuryYield;

        System.out.println("**** CashConversionCycleModel: time=" + time
                + " discountAPR=" + String.format("%.4f", discountAPR)
                + " treasuryYield=" + String.format("%.4f", treasuryYield)
                + " spread=" + String.format("%.4f", spread)
                + " minSpread=" + String.format("%.4f", minArbitrageSpread)
                + " availableDiscounts=$" + String.format("%.2f", availableAmount));

        if (availableAmount <= 0.0) {
            return 0.0;
        }

        if (spread > this.minArbitrageSpread) {
            double signalStrength = Math.min(1.0, spread);
            System.out.println("**** CashConversionCycleModel: OPPORTUNITY"
                    + " spread=" + String.format("%.4f", spread)
                    + " → redeem T-bills $" + String.format("%.2f", availableAmount)
                    + " to capture " + String.format("%.2f%%", discountAPR * 100)
                    + " discount APR");
            return signalStrength;
        }

        return 0.0;
    }
}
