package org.actus.risksrv3.controllers.hybridtreasury1;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ScheduledCashFlowModelNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public ScheduledCashFlowModelNotFoundException(String riskFactorId) {
        super("ScheduledCashFlowModel not found for riskFactorId: " + riskFactorId);
    }
}
