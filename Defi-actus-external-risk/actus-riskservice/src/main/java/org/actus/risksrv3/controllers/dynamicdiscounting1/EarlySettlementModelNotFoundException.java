package org.actus.risksrv3.controllers.dynamicdiscounting1;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EarlySettlementModelNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public EarlySettlementModelNotFoundException(String riskFactorId) {
        super("EarlySettlementModel not found for riskFactorId: " + riskFactorId);
    }
}
