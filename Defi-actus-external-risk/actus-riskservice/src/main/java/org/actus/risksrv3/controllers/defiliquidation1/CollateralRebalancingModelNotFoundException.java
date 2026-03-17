package org.actus.risksrv3.controllers.defiliquidation1;

public class CollateralRebalancingModelNotFoundException extends RuntimeException {
    public CollateralRebalancingModelNotFoundException(String id) {
        super("CollateralRebalancingModel not found: " + id);
    }
}
