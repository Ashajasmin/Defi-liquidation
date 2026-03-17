package org.actus.risksrv3.controllers.stablecoin;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RedemptionPressureModelNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RedemptionPressureModelNotFoundException(String riskFactorId) {
        super("RedemptionPressureModel not found for riskFactorId: " + riskFactorId);
    }
}
