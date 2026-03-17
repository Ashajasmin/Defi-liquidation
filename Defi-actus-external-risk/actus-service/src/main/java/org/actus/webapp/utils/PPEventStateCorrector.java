package org.actus.webapp.utils;

import org.actus.webapp.models.Event;
import java.util.List;
import java.util.Map;

/**
 * Post-processes PP (Prepayment) events to correct nominalValue field
 * for contracts using buffer-first prepayment strategies.
 * 
 * BACKGROUND:
 * ACTUS-core captures event states BEFORE the state transition function (STF) runs.
 * For PP events, this means nominalValue reflects the principal BEFORE prepayment,
 * not AFTER. This is correct for traditional contracts but creates visualization
 * issues for DeFi buffer-first strategies where we need to show the reduced balance.
 * 
 * SOLUTION:
 * Post-process events to recalculate nominalValue = nominalBefore - payoff
 * 
 * BACKWARD COMPATIBILITY:
 * Only processes contracts with enablePPStateCorrection=true attribute.
 * All existing contracts work unchanged.
 * 
 * @author ACTUS Risk Service Extension Team
 * @version 1.0
 * @since 2026-03-13
 */
public class PPEventStateCorrector {
    
    /**
     * Corrects nominalValue in PP events if contract enables correction.
     * 
     * Algorithm:
     * 1. Check if enablePPStateCorrection=true in contract attributes
     * 2. If disabled, return events unchanged (backward compatibility)
     * 3. Track running notional principal across all events
     * 4. For each PP event with payoff > 0:
     *    - Calculate nominalAfter = nominalBefore - payoff
     *    - Update this PP event's nominalValue
     *    - Update subsequent RR events with reduced nominal
     * 
     * @param events List of events from ACTUS simulation
     * @param contractAttributes Contract configuration map
     * @return Corrected event list (or unchanged if feature disabled)
     */
    public static List<Event> correctPPStates(
        List<Event> events, 
        Map<String, Object> contractAttributes) {
        
        // STEP 1: Check if correction is enabled for this contract
        Boolean enableCorrection = (Boolean) contractAttributes.get("enablePPStateCorrection");
        
        if (enableCorrection == null || !enableCorrection) {
            // Feature disabled - return unchanged (backward compatibility)
            return events;
        }
        
        // STEP 2: Apply correction logic
        return applyCorrectionLogic(events);
    }
    
    /**
     * Core correction algorithm.
     * 
     * Tracks the "true" notional principal across events and updates
     * nominalValue fields to reflect post-prepayment state.
     * 
     * @param events List of events to process
     * @return Events with corrected nominalValue fields
     */
    private static List<Event> applyCorrectionLogic(List<Event> events) {
        
        double runningNotional = -1.0;  // Track actual notional principal
        
        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            String eventType = event.getType();
            
            // Initialize running notional from IED (Initial Exchange Date)
            if ("IED".equals(eventType)) {
                runningNotional = event.getNominalValue();
                continue;
            }
            
            // Process PP (Prepayment) events
            if ("PP".equals(eventType)) {
                double payoff = event.getPayoff();
                
                if (payoff > 0) {
                    // PP event with actual prepayment
                    double notionalBefore = runningNotional;
                    double notionalAfter = notionalBefore - payoff;
                    
                    // CORRECT this PP event's nominalValue
                    event.setNominalValue(notionalAfter);
                    
                    // Update running notional for subsequent events
                    runningNotional = notionalAfter;
                    
                } else {
                    // PP event with zero payoff (no action)
                    // Update with current running notional for consistency
                    event.setNominalValue(runningNotional);
                }
                continue;
            }
            
            // Update RR (Rate Reset) and IP (Interest Payment) events
            // These should reflect the current notional after any PP events
            if ("RR".equals(eventType) || "IP".equals(eventType)) {
                if (runningNotional > 0) {
                    event.setNominalValue(runningNotional);
                }
            }
            
            // Note: Other event types (MD, FP, SC, etc.) are not modified
            // as they have their own state transition logic
        }
        
        return events;
    }
}
