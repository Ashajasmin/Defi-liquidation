package org.actus.risksrv3.controllers.supplychaintariff1;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RevenueElasticityModelNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public RevenueElasticityModelNotFoundException(String riskFactorId) {
        super("RevenueElasticityModel not found for riskFactorId: " + riskFactorId);
    }
}
