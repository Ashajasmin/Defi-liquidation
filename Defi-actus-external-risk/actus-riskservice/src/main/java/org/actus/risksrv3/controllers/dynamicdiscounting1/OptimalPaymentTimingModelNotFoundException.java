package org.actus.risksrv3.controllers.dynamicdiscounting1;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OptimalPaymentTimingModelNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public OptimalPaymentTimingModelNotFoundException(String riskFactorId) {
        super("OptimalPaymentTimingModel not found for riskFactorId: " + riskFactorId);
    }
}
