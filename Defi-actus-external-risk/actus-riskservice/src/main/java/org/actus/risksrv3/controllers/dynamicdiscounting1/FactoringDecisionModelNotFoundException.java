package org.actus.risksrv3.controllers.dynamicdiscounting1;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FactoringDecisionModelNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public FactoringDecisionModelNotFoundException(String riskFactorId) {
        super("FactoringDecisionModel not found for riskFactorId: " + riskFactorId);
    }
}
