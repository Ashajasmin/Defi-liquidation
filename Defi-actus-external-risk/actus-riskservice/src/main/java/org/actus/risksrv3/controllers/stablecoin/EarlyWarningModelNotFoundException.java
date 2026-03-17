package org.actus.risksrv3.controllers.stablecoin;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EarlyWarningModelNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EarlyWarningModelNotFoundException(String riskFactorId) {
        super("EarlyWarningModel not found for riskFactorId: " + riskFactorId);
    }
}
