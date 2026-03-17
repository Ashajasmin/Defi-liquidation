package org.actus.risksrv3.controllers.supplychaintariff1;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class HedgeEffectivenessModelNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public HedgeEffectivenessModelNotFoundException(String riskFactorId) {
        super("HedgeEffectivenessModel not found for riskFactorId: " + riskFactorId);
    }
}
