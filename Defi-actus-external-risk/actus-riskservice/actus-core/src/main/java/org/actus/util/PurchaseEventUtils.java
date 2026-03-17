package org.actus.util;

import org.actus.attributes.ContractModelProvider;
import org.actus.events.ContractEvent;
import org.actus.events.EventFactory;
import org.actus.functions.lam.POF_IP_At_PRD_LAM;
import org.actus.functions.pam.POF_IP_AT_PRD_PAM;
import org.actus.functions.pam.STF_IP_PAM;
import org.actus.types.EventType;

public class PurchaseEventUtils {
	
	 private PurchaseEventUtils() {
	        // Utility class – prevent instantiation
	    }

	    /**
	     * Creates an IP event at purchase to pay accrued interest separately,
	     * ensuring the PRD event reflects a clean price (principal only).
	     */
	    public static ContractEvent createAccruedInterestEventAtPurchaseLAM(ContractModelProvider model) {
	        return EventFactory.createEvent(
	                model.getAs("purchaseDate"),
	                EventType.IP,
	                model.getAs("currency"),
	                new POF_IP_At_PRD_LAM(),
	                new STF_IP_PAM(),
	                model.getAs("businessDayConvention"),
	                model.getAs("contractID")
	        );
	    }
	    
	    /**
	     * Creates an IP event at purchase to pay accrued interest separately,
	     * ensuring the PRD event reflects a clean price (principal only).
	     */
	    public static ContractEvent createAccruedInterestEventAtPurchasePAM(ContractModelProvider model) {
	        return EventFactory.createEvent(
	                model.getAs("purchaseDate"),
	                EventType.IP,
	                model.getAs("currency"),
	                new POF_IP_AT_PRD_PAM(),
	                new STF_IP_PAM(),
	                model.getAs("businessDayConvention"),
	                model.getAs("contractID")
	        );
	    }

}
