/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.conventions.contractrole;

import org.actus.AttributeConversionException;
import org.actus.types.ContractRole;
import org.actus.util.CommonUtils;

/**
 * Convention determining the direction of cash flows based on attribute ContractRole
 * 
 */
public final class ContractRoleConvention {
    
    // this is a utility class
    private ContractRoleConvention() {
    }
    
    /**
     * Returns an integer indicating the direction of cash flows in the contract's pay-off
     * <p>
     * ACTUS contract attribute "contractRole" codifies the direction of cash flows in a
     * contract's pay-off. The pay-off functions treat the direction of cash flows as
     * an integer of +1 for cash in-flows (cash flowing from LegalEntityIDCounterparty to 
     * LegalEntityIDRecordCreator) and -1 for cash out-flows (cash flowing from 
     * LegalEntityIDRecordCreator to LegalEntityIDCounterparty). This method translates
     * value of attribute ContractRole to an integer +1 or -1 indicating the direction of
     * cash flows.
     * 
     * @param role the value of attribute ContractRole
     * @return an integer of +1 (for cash in-flows) and -1 (for cash out-flows)
     * @throws AttributeConversionException if the value of the method argument is invalid
     */
    public static int roleSign(ContractRole role) throws AttributeConversionException {
        int sign;
        if(CommonUtils.isNull(role)){
            throw new AttributeConversionException();
        }
        switch(role) {
            case RPA:
            case BUY:
            case RFL:
            case RF:
            sign = 1;
            break;
            case RPL:
            case SEL:
            case PFL:
            case PF:
            sign = -1;
            break;
            default:
                throw new AttributeConversionException();
        }
        return sign;
    }
}
