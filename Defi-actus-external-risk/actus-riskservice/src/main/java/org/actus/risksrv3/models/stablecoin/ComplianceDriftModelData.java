package org.actus.risksrv3.models.stablecoin;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * ComplianceDriftModelData
 *
 * MongoDB document for ComplianceDriftModel configuration.
 * Collection: complianceDriftModels
 *
 * JSON example:
 * {
 *   "riskFactorId": "cp_sc01",
 *   "backingRatioMOC": "SC_BACKING_RATIO",
 *   "wamDaysMOC": "SC_WAM_DAYS",
 *   "hqlaScoreMOC": "SC_HQLA_SCORE",
 *   "attestationAgeMOC": "SC_ATTESTATION_AGE",
 *   "backingThreshold": 1.0,
 *   "wamMaxDays": 93.0,
 *   "hqlaMinScore": 100.0,
 *   "attestationMaxDays": 30.0,
 *   "monitoringEventTimes": ["2025-06-01T00:00:00", ...]
 * }
 */
@Document(collection = "complianceDriftModels")
public class ComplianceDriftModelData {

    @Id
    private String riskFactorId;

    private String backingRatioMOC;
    private String wamDaysMOC;
    private String hqlaScoreMOC;
    private String attestationAgeMOC;
    private double backingThreshold;
    private double wamMaxDays;
    private double hqlaMinScore;
    private double attestationMaxDays;
    private List<String> monitoringEventTimes;

    public ComplianceDriftModelData() {
    }

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getBackingRatioMOC() { return backingRatioMOC; }
    public void setBackingRatioMOC(String backingRatioMOC) { this.backingRatioMOC = backingRatioMOC; }

    public String getWamDaysMOC() { return wamDaysMOC; }
    public void setWamDaysMOC(String wamDaysMOC) { this.wamDaysMOC = wamDaysMOC; }

    public String getHqlaScoreMOC() { return hqlaScoreMOC; }
    public void setHqlaScoreMOC(String hqlaScoreMOC) { this.hqlaScoreMOC = hqlaScoreMOC; }

    public String getAttestationAgeMOC() { return attestationAgeMOC; }
    public void setAttestationAgeMOC(String attestationAgeMOC) { this.attestationAgeMOC = attestationAgeMOC; }

    public double getBackingThreshold() { return backingThreshold; }
    public void setBackingThreshold(double backingThreshold) { this.backingThreshold = backingThreshold; }

    public double getWamMaxDays() { return wamMaxDays; }
    public void setWamMaxDays(double wamMaxDays) { this.wamMaxDays = wamMaxDays; }

    public double getHqlaMinScore() { return hqlaMinScore; }
    public void setHqlaMinScore(double hqlaMinScore) { this.hqlaMinScore = hqlaMinScore; }

    public double getAttestationMaxDays() { return attestationMaxDays; }
    public void setAttestationMaxDays(double attestationMaxDays) { this.attestationMaxDays = attestationMaxDays; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
