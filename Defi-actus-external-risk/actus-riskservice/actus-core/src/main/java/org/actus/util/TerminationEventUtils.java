package org.actus.util;

import org.actus.attributes.ContractModelProvider;
import org.actus.events.ContractEvent;
import org.actus.events.EventFactory;
import org.actus.functions.lam.POF_IP_At_TD_LAM;
import org.actus.functions.pam.POF_IP_At_TD_PAM;
import org.actus.functions.pam.STF_IP_PAM;
import org.actus.types.EventType;

public class TerminationEventUtils {
	
	private TerminationEventUtils() {
        // Utility class – prevent instantiation
    }

    /**
     * Creates an IP event at termination to pay accrued interest separately,
     * ensuring the TD event reflects a clean price (principal only).
     */
    public static ContractEvent createAccruedInterestEventAtTerminationLAM(ContractModelProvider model) {
        return EventFactory.createEvent(
                model.getAs("terminationDate"),
                EventType.IP,
                model.getAs("currency"),
                new POF_IP_At_TD_LAM(),
                new STF_IP_PAM(),
                model.getAs("businessDayConvention"),
                model.getAs("contractID")
        );
    }
    
    /**
     * Creates an IP event at termination to pay accrued interest separately,
     * ensuring the TD event reflects a clean price (principal only).
     */
    public static ContractEvent createAccruedInterestEventAtTerminationPAM(ContractModelProvider model) {
        return EventFactory.createEvent(
                model.getAs("terminationDate"),
                EventType.IP,
                model.getAs("currency"),
                new POF_IP_At_TD_PAM(),
                new STF_IP_PAM(),
                model.getAs("businessDayConvention"),
                model.getAs("contractID")
        );
    }

}
