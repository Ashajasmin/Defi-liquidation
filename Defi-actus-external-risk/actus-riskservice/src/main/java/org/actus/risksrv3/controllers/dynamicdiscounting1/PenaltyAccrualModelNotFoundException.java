package org.actus.risksrv3.controllers.dynamicdiscounting1;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PenaltyAccrualModelNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public PenaltyAccrualModelNotFoundException(String riskFactorId) {
        super("PenaltyAccrualModel not found for riskFactorId: " + riskFactorId);
    }
}
