package org.actus.risksrv3.controllers.defiliquidation1;

public class GasOptimizationModelNotFoundException extends RuntimeException {
    public GasOptimizationModelNotFoundException(String id) {
        super("GasOptimizationModel not found: " + id);
    }
}
