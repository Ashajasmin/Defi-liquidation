package org.actus.risksrv3.controllers.defiliquidation1;

public class CollateralVelocityModelNotFoundException extends RuntimeException {
    public CollateralVelocityModelNotFoundException(String id) {
        super("CollateralVelocityModel not found: " + id);
    }
}
