package org.actus.risksrv3.controllers.defiliquidation1;

public class CorrelationRiskModelNotFoundException extends RuntimeException {
    public CorrelationRiskModelNotFoundException(String id) {
        super("CorrelationRiskModel not found: " + id);
    }
}
