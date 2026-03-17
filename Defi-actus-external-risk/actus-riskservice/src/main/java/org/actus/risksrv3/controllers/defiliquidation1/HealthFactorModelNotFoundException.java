package org.actus.risksrv3.controllers.defiliquidation1;

public class HealthFactorModelNotFoundException extends RuntimeException {
    public HealthFactorModelNotFoundException(String id) {
        super("HealthFactorModel not found: " + id);
    }
}
