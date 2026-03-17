/* 
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.contracts;

import org.actus.AttributeConversionException;
import org.actus.ContractTypeUnknownException;
import org.actus.attributes.ContractModelProvider;
import org.actus.events.ContractEvent;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.states.StateSpace;
import org.actus.types.ContractTypeEnum;
 

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * A representation of an ACTUS Contract Type algorithm
 * <p>
 * ACTUS Contract Types represent classes of financial instrument payoff functions. This component 
 * is a representation of the algorithm mapping a set of attributes (cf. {@link ContractModelProvider}),
 * an external market model (cf. {@link RiskFactorModelProvider}) and analysis times 
 * (cf. {@code analysisTimes}) onto a vector of contingent contract events (cf. {@link ContractEvent}).
 * </p>
 * @see <a href="https://www.actusfrf.org">ACTUS Website</a>
 */
public final class ContractType {
    
    /**
     * Evaluates the next contract events within a certain time period
     * <p>
     *     The set of contract attributes are mapped to the stream of next contract events
     *     within a specified time period according
     *     to the legal logic of the respective Contract Type and contingent
     *     to the risk factor dynamics provided with the {@link RiskFactorModelProvider}.
     *     The contract's {@code StatusDate} is used as the reference time as from which
     *     the {@code within} period is evaluated.
     * </p>
     * <p>
     *     Note, the stream of the next non-contingent contract events matches the portion
     *     of the stream of the next contingent events up to the first contingent event.
     *     Further, for a contract with purely non-contingent events
     *     (e.g. a {@link PrincipalAtMaturity} without {@code RateReset}, {@code Scaling},
     *     {@code CreditDefault}, etc.) contingent and non-contingent event streams are
     *     the same.
     * </p>
     * <p>
     *     If the {@code ContractType} attribute cannot be resolved to an ACTUS Contract Type the method
     *     throws a {@link ContractTypeUnknownException}.
     * </p>
     *
     * @param to the time up to which the events are to be evaluated
     * @param model the model carrying the contract attributes
     * @return a list of contract events scheduled up to 'to'
     * @throws ContractTypeUnknownException if the provided ContractType field in the {@link ContractModelProvider} cannot be resolved
     * @throws AttributeConversionException if and attribute in {@link ContractModelProvider} cannot be converted to its target data type
     *
     */
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws ContractTypeUnknownException,AttributeConversionException {
        switch((ContractTypeEnum)model.getAs("contractType")) {
            case PAM:
                return PrincipalAtMaturity.schedule(to,model);
            case LAM:
                return LinearAmortizer.schedule(to,model);
            case NAM:
                return NegativeAmortizer.schedule(to,model);
            case ANN:
                return Annuity.schedule(to,model);
            case LAX:
                return ExoticLinearAmortizer.schedule(to, model);
            case CLM:
                return CallMoney.schedule(to,model);
            case UMP:
                return UndefinedMaturityProfile.schedule(to,model);
            case CSH:
                return Cash.schedule(to,model);
            case STK:
                return Stock.schedule(to,model);
            case COM:
                return Commodity.schedule(to,model);
            case FXOUT:
                return ForeignExchangeOutright.schedule(to,model);
            case SWPPV:
                return PlainVanillaInterestRateSwap.schedule(to,model);
            case SWAPS:
                return Swap.schedule(to,model);
            case CAPFL:
                return CapFloor.schedule(to, model);
            case OPTNS:
                return Option.schedule(to, model);
            case FUTUR:
                return Future.schedule(to, model);
            case CEG:
                return CreditEnhancementGuarantee.schedule(to, model);
            case CEC:
                return CreditEnhancementCollateral.schedule(to, model);
            case BCS:
                return BoundaryControlledSwitch.schedule(to, model);
            default:
                throw new ContractTypeUnknownException();
        }
    }

    /**
     * Applies a Set of contract events to the current state of the contract
     * <p>
     *     The {@code Set} of {@link ContractEvent}s is applied to the current contract
     *     state (i.e. as per attribute {@code StatusDate} in the {@link org.actus.attributes.ContractModel})
     *     in timely sequence of the provided events. The {@link StateSpace} carrying the
     *     contract's post-events state is returned.
     * </p>
     * <p>
     *     If the {@code ContractType} attribute cannot be resolved to an ACTUS Contract Type the method
     *     throws a {@link ContractTypeUnknownException}.
     * </p>
     * <p>
     *     Note, this method is not yet available for Contract Type {@link Swap} and
     *     throws a {@link ContractTypeUnknownException} if attempted to be evaluated for the same.
     * </p>
     *
     * @param events a list of contract events that should be applied in time sequence
     * @param model the model carrying the contract attributes
     * @param observer the observer for external events and data
     * @return the evaluated events and post-event contract states
     * @throws ContractTypeUnknownException if the provided ContractType field in the {@link ContractModelProvider} cannot be resolved
     * @throws AttributeConversionException if and attribute in {@link ContractModelProvider} cannot be converted to its target data type
     *
     */
    public static ArrayList<ContractEvent> apply(ArrayList<ContractEvent> events,
                                                 ContractModelProvider model,
                                                 RiskFactorModelProvider observer) throws ContractTypeUnknownException,AttributeConversionException {
        switch((ContractTypeEnum)model.getAs("contractType")) {
            case PAM:
                return PrincipalAtMaturity.apply(events,model,observer);
            case LAM:
                return LinearAmortizer.apply(events,model,observer);
            case NAM:
                return NegativeAmortizer.apply(events,model,observer);
            case ANN:
                return Annuity.apply(events,model,observer);
            case LAX:
                return ExoticLinearAmortizer.apply(events, model, observer);
            case CLM:
                return CallMoney.apply(events,model,observer);
            case UMP:
                return UndefinedMaturityProfile.apply(events,model,observer);
            case CSH:
                return Cash.apply(events,model,observer);
            case STK:
                return Stock.apply(events,model,observer);
            case COM:
                return Commodity.apply(events,model,observer);
            case FXOUT:
                return ForeignExchangeOutright.apply(events,model,observer);
            case SWPPV:
                return PlainVanillaInterestRateSwap.apply(events,model,observer);
            case SWAPS:
                return Swap.apply(events,model,observer);
            case CAPFL:
                return CapFloor.apply(events, model, observer);
            case OPTNS:
                return Option.apply(events, model, observer);
            case FUTUR:
                return Future.apply(events, model, observer);
            case CEG:
                return CreditEnhancementGuarantee.apply(events, model, observer);
            case CEC:
                return CreditEnhancementCollateral.apply(events, model, observer);
            case BCS:
                return BoundaryControlledSwitch.apply(events, model, observer);
            default:
                throw new ContractTypeUnknownException();
        }
    }
}
