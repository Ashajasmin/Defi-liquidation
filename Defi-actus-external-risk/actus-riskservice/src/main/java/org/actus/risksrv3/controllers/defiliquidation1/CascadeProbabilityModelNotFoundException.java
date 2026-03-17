package org.actus.risksrv3.controllers.defiliquidation1;

public class CascadeProbabilityModelNotFoundException extends RuntimeException {
    public CascadeProbabilityModelNotFoundException(String id) {
        super("CascadeProbabilityModel not found: " + id);
    }
}
