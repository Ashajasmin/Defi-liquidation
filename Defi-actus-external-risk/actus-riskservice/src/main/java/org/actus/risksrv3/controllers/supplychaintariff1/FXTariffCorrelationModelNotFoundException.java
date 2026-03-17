package org.actus.risksrv3.controllers.supplychaintariff1;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FXTariffCorrelationModelNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public FXTariffCorrelationModelNotFoundException(String riskFactorId) {
        super("FXTariffCorrelationModel not found for riskFactorId: " + riskFactorId);
    }
}
