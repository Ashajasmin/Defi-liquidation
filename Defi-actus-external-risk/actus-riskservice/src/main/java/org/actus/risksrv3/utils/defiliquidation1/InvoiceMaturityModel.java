package org.actus.risksrv3.utils.defiliquidation1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.defiliquidation1.InvoiceMaturityModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * InvoiceMaturityModel (Domain 1 — Model 2.8)
 *
 * For DeFi protocols accepting real-world assets (invoices, receivables) as
 * collateral alongside digital assets. Models maturity risk — the invoice
 * collateral's effective value depends on time-to-maturity and payment
 * probability, not just face value.
 *
 * Value curve logic:
 *   daysToMaturity > 30    → discount factor based on credit risk
 *   daysToMaturity 0-30    → approaching certainty (value increases)
 *   daysToMaturity < 0     → OVERDUE: value degrades rapidly (default risk)
 *
 * This model directly supports CHRONOS-SHIELD's Strategy E: Invoice Sacrifice
 * by tracking when invoice collateral value changes relative to volatile assets.
 *
 * ACTUS contract types modeled:
 *   - Invoice as PAM with maturityDate
 *   - Main loan as PAM with this behavioral model
 *
 * Data: INVOICE_PAYMENT_PROB reference index (probability of on-time payment)
 */
public class InvoiceMaturityModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String collateralPriceMOC;       // volatile asset price (ETH_USD)
    private final String invoicePaymentProbMOC;    // payment probability reference
    private final double collateralQuantity;        // ETH quantity
    private final double invoiceFaceValue;          // invoice face value in USD
    private final LocalDateTime invoiceMaturityDate;
    private final double overdueDegradationRate;    // daily value loss when overdue (e.g. 0.03)
    private final double creditDiscountRate;        // annualized discount rate for time value
    private final double ltvThreshold;
    private final double liquidationThreshold;
    private final double ltvTarget;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public InvoiceMaturityModel(String riskFactorId,
                                InvoiceMaturityModelData data,
                                MultiMarketRiskModel marketModel) {
        this.riskFactorId           = riskFactorId;
        this.collateralPriceMOC     = data.getCollateralPriceMOC();
        this.invoicePaymentProbMOC  = data.getInvoicePaymentProbMOC();
        this.collateralQuantity     = data.getCollateralQuantity();
        this.invoiceFaceValue       = data.getInvoiceFaceValue();
        this.invoiceMaturityDate    = LocalDateTime.parse(data.getInvoiceMaturityDate());
        this.overdueDegradationRate = data.getOverdueDegradationRate();
        this.creditDiscountRate     = data.getCreditDiscountRate();
        this.ltvThreshold           = data.getLtvThreshold();
        this.liquidationThreshold   = data.getLiquidationThreshold();
        this.ltvTarget              = data.getLtvTarget();
        this.monitoringEventTimes   = data.getMonitoringEventTimes();
        this.marketModel            = marketModel;
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
                    System.out.println("**** InvoiceMaturityModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double ethPrice = this.marketModel.stateAt(this.collateralPriceMOC, time);
        double paymentProb = this.marketModel.stateAt(this.invoicePaymentProbMOC, time);

        if (ethPrice <= 0.0) return 0.0;

        // Compute effective invoice value based on maturity distance
        long daysToMaturity = ChronoUnit.DAYS.between(time, invoiceMaturityDate);
        double effectiveInvoiceValue;

        if (daysToMaturity > 0) {
            // Time-value discount: PV = FV * paymentProb * exp(-r * t/365)
            double yearFraction = daysToMaturity / 365.0;
            double discountFactor = Math.exp(-creditDiscountRate * yearFraction);
            effectiveInvoiceValue = invoiceFaceValue * paymentProb * discountFactor;
        } else {
            // OVERDUE: degrade value daily
            long daysOverdue = Math.abs(daysToMaturity);
            double degradation = Math.pow(1.0 - overdueDegradationRate, daysOverdue);
            effectiveInvoiceValue = invoiceFaceValue * paymentProb * degradation;
        }

        // Total collateral = ETH value + effective invoice value
        double ethValue = collateralQuantity * ethPrice;
        double totalCollateral = ethValue + effectiveInvoiceValue;

        double debt = states.notionalPrincipal + states.accruedInterest;
        if (debt <= 0.0) return 0.0;

        double currentLTV = debt / totalCollateral;

        System.out.println("**** InvoiceMaturityModel: time=" + time
                + " daysToMaturity=" + daysToMaturity
                + " paymentProb=" + String.format("%.4f", paymentProb)
                + " effectiveInvoiceValue=" + String.format("%.2f", effectiveInvoiceValue)
                + " ethValue=" + String.format("%.2f", ethValue)
                + " totalCollateral=" + String.format("%.2f", totalCollateral)
                + " LTV=" + String.format("%.4f", currentLTV));

        if (currentLTV >= liquidationThreshold) {
            System.out.println("**** InvoiceMaturityModel: LIQUIDATION");
            return 1.0;
        } else if (currentLTV >= ltvThreshold) {
            double targetDebt = ltvTarget * totalCollateral;
            double repayFraction = (debt - targetDebt) / debt;

            // Check if invoice sacrifice is beneficial
            double ethOnlyLTV = debt / ethValue;
            if (ethOnlyLTV < ltvThreshold) {
                System.out.println("**** InvoiceMaturityModel: STRATEGY_E invoice_sacrifice viable"
                        + " ethOnlyLTV=" + String.format("%.4f", ethOnlyLTV));
            }
            return Math.max(0.0, Math.min(1.0, repayFraction));
        }
        return 0.0;
    }
}
