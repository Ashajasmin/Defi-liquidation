package org.actus.risksrv3.models.stablecoin;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * EarlyWarningModelData
 *
 * MongoDB document for EarlyWarningModel configuration.
 * Collection: earlyWarningModels
 *
 * JSON example:
 * {
 *   "riskFactorId": "ew_sc01",
 *   "curveImbalanceMOC": "SC_CURVE_IMBALANCE",
 *   "orderbookDeclineMOC": "SC_ORDERBOOK_DECLINE",
 *   "cexOutflowMOC": "SC_CEX_OUTFLOW_MULT",
 *   "sentimentZscoreMOC": "SC_SENTIMENT_ZSCORE",
 *   "monitoringEventTimes": ["2025-06-01T00:00:00", ...]
 * }
 */
@Document(collection = "earlyWarningModels")
public class EarlyWarningModelData {

    @Id
    private String riskFactorId;

    private String curveImbalanceMOC;
    private String orderbookDeclineMOC;
    private String cexOutflowMOC;
    private String sentimentZscoreMOC;
    private List<String> monitoringEventTimes;

    public EarlyWarningModelData() {
    }

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getCurveImbalanceMOC() { return curveImbalanceMOC; }
    public void setCurveImbalanceMOC(String curveImbalanceMOC) { this.curveImbalanceMOC = curveImbalanceMOC; }

    public String getOrderbookDeclineMOC() { return orderbookDeclineMOC; }
    public void setOrderbookDeclineMOC(String orderbookDeclineMOC) { this.orderbookDeclineMOC = orderbookDeclineMOC; }

    public String getCexOutflowMOC() { return cexOutflowMOC; }
    public void setCexOutflowMOC(String cexOutflowMOC) { this.cexOutflowMOC = cexOutflowMOC; }

    public String getSentimentZscoreMOC() { return sentimentZscoreMOC; }
    public void setSentimentZscoreMOC(String sentimentZscoreMOC) { this.sentimentZscoreMOC = sentimentZscoreMOC; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
