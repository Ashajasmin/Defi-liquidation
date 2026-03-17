package org.actus.risksrv3.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * CollateralLTVModelNotFoundException
 *
 * Thrown by RiskObservationHandler when a scenario descriptor references a
 * CollateralLTVModel risk factor ID that does not exist in MongoDB.
 *
 * Spring's @ResponseStatus annotation maps this exception to HTTP 404 so that
 * Postman / webapp callers receive a clear error response instead of a 500.
 *
 * Usage in RiskObservationHandler:
 * <pre>{@code
 *   Optional<CollateralLTVModelData> ocltv =
 *       this.collateralLTVModelStore.findById(rfxid);
 *
 *   if (ocltv.isPresent()) {
 *       CollateralLTVModel cltv = new CollateralLTVModel(
 *           rfxid, ocltv.get(), this.currentMarketModel);
 *       currentBehaviorModel.add(rfxid, cltv);
 *   } else {
 *       throw new CollateralLTVModelNotFoundException(rfxid);
 *   }
 * }</pre>
 *
 * Place this file in:
 *   actus-riskservice/src/main/java/org/actus/risksrv3/controllers/
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class CollateralLTVModelNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param riskFactorId  The ID that could not be found in the repository.
     */
    public CollateralLTVModelNotFoundException(String riskFactorId) {
        super("CollateralLTVModel not found for riskFactorId: " + riskFactorId);
    }
}