package org.actus.risksrv3.controllers.supplychaintariff1;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * TariffSpreadModelNotFoundException
 *
 * Thrown when a scenario descriptor references a TariffSpreadModel
 * risk factor ID that does not exist in MongoDB.
 * Maps to HTTP 404.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class TariffSpreadModelNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TariffSpreadModelNotFoundException(String riskFactorId) {
        super("TariffSpreadModel not found for riskFactorId: " + riskFactorId);
    }
}
