package org.actus.risksrv3.controllers.supplychaintariff1;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PortCongestionModelNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public PortCongestionModelNotFoundException(String riskFactorId) {
        super("PortCongestionModel not found for riskFactorId: " + riskFactorId);
    }
}
