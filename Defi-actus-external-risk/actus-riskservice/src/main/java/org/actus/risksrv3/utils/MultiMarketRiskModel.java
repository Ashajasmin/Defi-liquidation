package org.actus.risksrv3.utils;

import java.util.Set;
import java.util.HashMap;
import java.time.LocalDateTime;


public class MultiMarketRiskModel implements MarketRiskModelProvider {
	
	HashMap<String,MarketRiskModelProvider> model = new HashMap<String,MarketRiskModelProvider>();
	
	public MultiMarketRiskModel() {
	}

	public Set<String> keys() {
		return model.keySet();
	}

	public void add(String symbol, MarketRiskModelProvider dimension) {
		model.put(symbol,dimension);
	}

	public boolean containsKey(String id) {
		return model.containsKey(id);
	}

	public double stateAt(String id, LocalDateTime time) {
		MarketRiskModelProvider provider = model.get(id);
		if (provider == null) {
			throw new IllegalArgumentException(
				"MarketObjectCode '" + id + "' not found in active scenario. "
				+ "Available MOCs: " + model.keySet()
				+ ". Add a ReferenceIndex with this marketObjectCode to the scenario.");
		}
		return provider.stateAt(id, time);
	}
}
