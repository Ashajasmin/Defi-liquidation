package org.actus.risksrv3.controllers.stablecoin;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * BackingRatioModelNotFoundException
 *
 * Thrown when a scenario descriptor references a BackingRatioModel
 * risk factor ID that does not exist in MongoDB.
 * Maps to HTTP 404.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class BackingRatioModelNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BackingRatioModelNotFoundException(String riskFactorId) {
        super("BackingRatioModel not found for riskFactorId: " + riskFactorId);
    }
}
