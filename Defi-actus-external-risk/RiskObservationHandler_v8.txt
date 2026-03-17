package org.actus.risksrv3.controllers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.BatchStartInput;
import org.actus.risksrv3.models.BehaviorStateAtInput;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.MarketData;
import org.actus.risksrv3.models.OldScenario;
import org.actus.risksrv3.models.Scenario;
import org.actus.risksrv3.models.TwoDimensionalPrepaymentModelData;
import org.actus.risksrv3.models.TwoDimensionalDepositTrxModelData;
import org.actus.risksrv3.models.CollateralLTVModelData;
import org.actus.risksrv3.models.ReferenceIndex;
import org.actus.risksrv3.models.RiskFactorDescriptor;
import org.actus.risksrv3.models.ScenarioDescriptor;
import org.actus.risksrv3.models.StateAtInput;
// ====== STABLECOIN MODEL DATA IMPORTS ======
import org.actus.risksrv3.models.stablecoin.BackingRatioModelData;
import org.actus.risksrv3.models.stablecoin.RedemptionPressureModelData;
import org.actus.risksrv3.models.stablecoin.MaturityLadderModelData;
import org.actus.risksrv3.models.stablecoin.AssetQualityModelData;
import org.actus.risksrv3.models.stablecoin.ConcentrationDriftModelData;
import org.actus.risksrv3.models.stablecoin.ComplianceDriftModelData;
import org.actus.risksrv3.models.stablecoin.EarlyWarningModelData;
import org.actus.risksrv3.models.stablecoin.ContinuousAttestationModelData;
// ====== END STABLECOIN MODEL DATA IMPORTS ======
// ====== HYBRID TREASURY MODEL DATA IMPORTS ======
import org.actus.risksrv3.models.hybridtreasury1.AllocationDriftModelData;
import org.actus.risksrv3.models.hybridtreasury1.LiquidityBufferModelData;
import org.actus.risksrv3.models.hybridtreasury1.PegStressModelData;
import org.actus.risksrv3.models.hybridtreasury1.RegulatoryDeRiskModelData;
import org.actus.risksrv3.models.hybridtreasury1.YieldArbitrageModelData;
import org.actus.risksrv3.models.hybridtreasury1.CashConversionCycleModelData;
import org.actus.risksrv3.models.hybridtreasury1.FairValueComplianceModelData;
import org.actus.risksrv3.models.hybridtreasury1.IntegratedStressModelData;
// ====== END HYBRID TREASURY MODEL DATA IMPORTS ======
// ====== SUPPLY CHAIN TARIFF MODEL DATA IMPORTS ======
import org.actus.risksrv3.models.supplychaintariff1.TariffSpreadModelData;
import org.actus.risksrv3.models.supplychaintariff1.WorkingCapitalStressModelData;
import org.actus.risksrv3.models.supplychaintariff1.HedgeEffectivenessModelData;
import org.actus.risksrv3.models.supplychaintariff1.RevenueElasticityModelData;
import org.actus.risksrv3.models.supplychaintariff1.FXTariffCorrelationModelData;
import org.actus.risksrv3.models.supplychaintariff1.PortCongestionModelData;
// ====== END SUPPLY CHAIN TARIFF MODEL DATA IMPORTS ======
// ====== DEFI LIQUIDATION MODEL DATA IMPORTS ======
import org.actus.risksrv3.models.defiliquidation1.HealthFactorModelData;
import org.actus.risksrv3.models.defiliquidation1.CollateralVelocityModelData;
import org.actus.risksrv3.models.defiliquidation1.CollateralRebalancingModelData;
import org.actus.risksrv3.models.defiliquidation1.CorrelationRiskModelData;
import org.actus.risksrv3.models.defiliquidation1.CascadeProbabilityModelData;
import org.actus.risksrv3.models.defiliquidation1.GasOptimizationModelData;
import org.actus.risksrv3.models.defiliquidation1.InvoiceMaturityModelData;
// ====== END DEFI LIQUIDATION MODEL DATA IMPORTS ======
// ====== DYNAMIC DISCOUNTING MODEL DATA IMPORTS ======
import org.actus.risksrv3.models.dynamicdiscounting1.EarlySettlementModelData;
import org.actus.risksrv3.models.dynamicdiscounting1.PenaltyAccrualModelData;
import org.actus.risksrv3.models.dynamicdiscounting1.OptimalPaymentTimingModelData;
import org.actus.risksrv3.models.dynamicdiscounting1.SupplierUrgencyModelData;
import org.actus.risksrv3.models.dynamicdiscounting1.FactoringDecisionModelData;
import org.actus.risksrv3.models.dynamicdiscounting1.CashPoolOptimizationModelData;
// ====== END DYNAMIC DISCOUNTING MODEL DATA IMPORTS ======
import org.actus.risksrv3.repository.ReferenceIndexStore;
import org.actus.risksrv3.repository.ScenarioStore;
import org.actus.risksrv3.repository.TwoDimensionalPrepaymentModelStore;
import org.actus.risksrv3.repository.TwoDimensionalDepositTrxModelStore;
import org.actus.risksrv3.repository.CollateralLTVModelStore;
// ====== STABLECOIN STORE IMPORTS ======
import org.actus.risksrv3.repository.stablecoin.BackingRatioModelStore;
import org.actus.risksrv3.repository.stablecoin.RedemptionPressureModelStore;
import org.actus.risksrv3.repository.stablecoin.MaturityLadderModelStore;
import org.actus.risksrv3.repository.stablecoin.AssetQualityModelStore;
import org.actus.risksrv3.repository.stablecoin.ConcentrationDriftModelStore;
import org.actus.risksrv3.repository.stablecoin.ComplianceDriftModelStore;
import org.actus.risksrv3.repository.stablecoin.EarlyWarningModelStore;
import org.actus.risksrv3.repository.stablecoin.ContinuousAttestationModelStore;
// ====== END STABLECOIN STORE IMPORTS ======
// ====== HYBRID TREASURY STORE IMPORTS ======
import org.actus.risksrv3.repository.hybridtreasury1.AllocationDriftModelStore;
import org.actus.risksrv3.repository.hybridtreasury1.LiquidityBufferModelStore;
import org.actus.risksrv3.repository.hybridtreasury1.PegStressModelStore;
import org.actus.risksrv3.repository.hybridtreasury1.RegulatoryDeRiskModelStore;
import org.actus.risksrv3.repository.hybridtreasury1.YieldArbitrageModelStore;
import org.actus.risksrv3.repository.hybridtreasury1.CashConversionCycleModelStore;
import org.actus.risksrv3.repository.hybridtreasury1.FairValueComplianceModelStore;
import org.actus.risksrv3.repository.hybridtreasury1.IntegratedStressModelStore;
// ====== END HYBRID TREASURY STORE IMPORTS ======
// ====== SUPPLY CHAIN TARIFF STORE IMPORTS ======
import org.actus.risksrv3.repository.supplychaintariff1.TariffSpreadModelStore;
import org.actus.risksrv3.repository.supplychaintariff1.WorkingCapitalStressModelStore;
import org.actus.risksrv3.repository.supplychaintariff1.HedgeEffectivenessModelStore;
import org.actus.risksrv3.repository.supplychaintariff1.RevenueElasticityModelStore;
import org.actus.risksrv3.repository.supplychaintariff1.FXTariffCorrelationModelStore;
import org.actus.risksrv3.repository.supplychaintariff1.PortCongestionModelStore;
// ====== END SUPPLY CHAIN TARIFF STORE IMPORTS ======
// ====== DEFI LIQUIDATION STORE IMPORTS ======
import org.actus.risksrv3.repository.defiliquidation1.HealthFactorModelStore;
import org.actus.risksrv3.repository.defiliquidation1.CollateralVelocityModelStore;
import org.actus.risksrv3.repository.defiliquidation1.CollateralRebalancingModelStore;
import org.actus.risksrv3.repository.defiliquidation1.CorrelationRiskModelStore;
import org.actus.risksrv3.repository.defiliquidation1.CascadeProbabilityModelStore;
import org.actus.risksrv3.repository.defiliquidation1.GasOptimizationModelStore;
import org.actus.risksrv3.repository.defiliquidation1.InvoiceMaturityModelStore;
// ====== END DEFI LIQUIDATION STORE IMPORTS ======
// ====== DYNAMIC DISCOUNTING STORE IMPORTS ======
import org.actus.risksrv3.repository.dynamicdiscounting1.EarlySettlementModelStore;
import org.actus.risksrv3.repository.dynamicdiscounting1.PenaltyAccrualModelStore;
import org.actus.risksrv3.repository.dynamicdiscounting1.OptimalPaymentTimingModelStore;
import org.actus.risksrv3.repository.dynamicdiscounting1.SupplierUrgencyModelStore;
import org.actus.risksrv3.repository.dynamicdiscounting1.FactoringDecisionModelStore;
import org.actus.risksrv3.repository.dynamicdiscounting1.CashPoolOptimizationModelStore;
// ====== END DYNAMIC DISCOUNTING STORE IMPORTS ======
import org.actus.risksrv3.utils.MultiBehaviorRiskModel;
import org.actus.risksrv3.utils.MultiMarketRiskModel;
import org.actus.risksrv3.utils.TimeSeriesModel;
import org.actus.risksrv3.utils.TwoDimensionalPrepaymentModel;
import org.actus.risksrv3.utils.TwoDimensionalDepositTrxModel;
import org.actus.risksrv3.utils.CollateralLTVModel;
// ====== STABLECOIN MODEL UTIL IMPORTS ======
import org.actus.risksrv3.utils.stablecoin.BackingRatioModel;
import org.actus.risksrv3.utils.stablecoin.RedemptionPressureModel;
import org.actus.risksrv3.utils.stablecoin.MaturityLadderModel;
import org.actus.risksrv3.utils.stablecoin.AssetQualityModel;
import org.actus.risksrv3.utils.stablecoin.ConcentrationDriftModel;
import org.actus.risksrv3.utils.stablecoin.ComplianceDriftModel;
import org.actus.risksrv3.utils.stablecoin.EarlyWarningModel;
import org.actus.risksrv3.utils.stablecoin.ContinuousAttestationModel;
// ====== END STABLECOIN MODEL UTIL IMPORTS ======
// ====== HYBRID TREASURY MODEL UTIL IMPORTS ======
import org.actus.risksrv3.utils.hybridtreasury1.AllocationDriftModel;
import org.actus.risksrv3.utils.hybridtreasury1.LiquidityBufferModel;
import org.actus.risksrv3.utils.hybridtreasury1.PegStressModel;
import org.actus.risksrv3.utils.hybridtreasury1.RegulatoryDeRiskModel;
import org.actus.risksrv3.utils.hybridtreasury1.YieldArbitrageModel;
import org.actus.risksrv3.utils.hybridtreasury1.CashConversionCycleModel;
import org.actus.risksrv3.utils.hybridtreasury1.FairValueComplianceModel;
import org.actus.risksrv3.utils.hybridtreasury1.IntegratedStressModel;
// ====== END HYBRID TREASURY MODEL UTIL IMPORTS ======
// ====== SUPPLY CHAIN TARIFF MODEL UTIL IMPORTS ======
import org.actus.risksrv3.utils.supplychaintariff1.TariffSpreadModel;
import org.actus.risksrv3.utils.supplychaintariff1.WorkingCapitalStressModel;
import org.actus.risksrv3.utils.supplychaintariff1.HedgeEffectivenessModel;
import org.actus.risksrv3.utils.supplychaintariff1.RevenueElasticityModel;
import org.actus.risksrv3.utils.supplychaintariff1.FXTariffCorrelationModel;
import org.actus.risksrv3.utils.supplychaintariff1.PortCongestionModel;
// ====== END SUPPLY CHAIN TARIFF MODEL UTIL IMPORTS ======
// ====== DEFI LIQUIDATION MODEL UTIL IMPORTS ======
import org.actus.risksrv3.utils.defiliquidation1.HealthFactorModel;
import org.actus.risksrv3.utils.defiliquidation1.CollateralVelocityModel;
import org.actus.risksrv3.utils.defiliquidation1.CollateralRebalancingModel;
import org.actus.risksrv3.utils.defiliquidation1.CorrelationRiskModel;
import org.actus.risksrv3.utils.defiliquidation1.CascadeProbabilityModel;
import org.actus.risksrv3.utils.defiliquidation1.GasOptimizationModel;
import org.actus.risksrv3.utils.defiliquidation1.InvoiceMaturityModel;
// ====== END DEFI LIQUIDATION MODEL UTIL IMPORTS ======
// ====== DYNAMIC DISCOUNTING MODEL UTIL IMPORTS ======
import org.actus.risksrv3.utils.dynamicdiscounting1.EarlySettlementModel;
import org.actus.risksrv3.utils.dynamicdiscounting1.PenaltyAccrualModel;
import org.actus.risksrv3.utils.dynamicdiscounting1.OptimalPaymentTimingModel;
import org.actus.risksrv3.utils.dynamicdiscounting1.SupplierUrgencyModel;
import org.actus.risksrv3.utils.dynamicdiscounting1.FactoringDecisionModel;
import org.actus.risksrv3.utils.dynamicdiscounting1.CashPoolOptimizationModel;
// ====== END DYNAMIC DISCOUNTING MODEL UTIL IMPORTS ======
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

//actus-riskservice version of the RiskObservation processing 

//Annotation 
@RestController
public class RiskObservationHandler {
	@Autowired
	private ReferenceIndexStore referenceIndexStore;
	@Autowired
	private ScenarioStore scenarioStore;
	@Autowired
	private TwoDimensionalPrepaymentModelStore twoDimensionalPrepaymentModelStore;
	@Autowired
	private TwoDimensionalDepositTrxModelStore twoDimensionalDepositTrxModelStore;
	@Autowired
	private CollateralLTVModelStore collateralLTVModelStore;

	// ====== STABLECOIN MODEL STORES ======
	@Autowired
	private BackingRatioModelStore backingRatioModelStore;
	@Autowired
	private RedemptionPressureModelStore redemptionPressureModelStore;
	@Autowired
	private MaturityLadderModelStore maturityLadderModelStore;
	@Autowired
	private AssetQualityModelStore assetQualityModelStore;
	@Autowired
	private ConcentrationDriftModelStore concentrationDriftModelStore;
	@Autowired
	private ComplianceDriftModelStore complianceDriftModelStore;
	@Autowired
	private EarlyWarningModelStore earlyWarningModelStore;
	@Autowired
	private ContinuousAttestationModelStore continuousAttestationModelStore;
	// ====== END STABLECOIN MODEL STORES ======
	// ====== HYBRID TREASURY MODEL STORES ======
	@Autowired
	private AllocationDriftModelStore allocationDriftModelStore;
	@Autowired
	private LiquidityBufferModelStore liquidityBufferModelStore;
	@Autowired
	private PegStressModelStore pegStressModelStore;
	@Autowired
	private RegulatoryDeRiskModelStore regulatoryDeRiskModelStore;
	@Autowired
	private YieldArbitrageModelStore yieldArbitrageModelStore;
	@Autowired
	private CashConversionCycleModelStore cashConversionCycleModelStore;
	@Autowired
	private FairValueComplianceModelStore fairValueComplianceModelStore;
	@Autowired
	private IntegratedStressModelStore integratedStressModelStore;
	// ====== END HYBRID TREASURY MODEL STORES ======
	// ====== SUPPLY CHAIN TARIFF MODEL STORES ======
	@Autowired
	private TariffSpreadModelStore tariffSpreadModelStore;
	@Autowired
	private WorkingCapitalStressModelStore workingCapitalStressModelStore;
	@Autowired
	private HedgeEffectivenessModelStore hedgeEffectivenessModelStore;
	@Autowired
	private RevenueElasticityModelStore revenueElasticityModelStore;
	@Autowired
	private FXTariffCorrelationModelStore fxTariffCorrelationModelStore;
	@Autowired
	private PortCongestionModelStore portCongestionModelStore;
	// ====== END SUPPLY CHAIN TARIFF MODEL STORES ======
	// ====== DEFI LIQUIDATION MODEL STORES ======
	@Autowired
	private HealthFactorModelStore healthFactorModelStore;
	@Autowired
	private CollateralVelocityModelStore collateralVelocityModelStore;
	@Autowired
	private CollateralRebalancingModelStore collateralRebalancingModelStore;
	@Autowired
	private CorrelationRiskModelStore correlationRiskModelStore;
	@Autowired
	private CascadeProbabilityModelStore cascadeProbabilityModelStore;
	@Autowired
	private GasOptimizationModelStore gasOptimizationModelStore;
	@Autowired
	private InvoiceMaturityModelStore invoiceMaturityModelStore;
	// ====== END DEFI LIQUIDATION MODEL STORES ======
	// ====== DYNAMIC DISCOUNTING MODEL STORES ======
	@Autowired
	private EarlySettlementModelStore earlySettlementModelStore;
	@Autowired
	private PenaltyAccrualModelStore penaltyAccrualModelStore;
	@Autowired
	private OptimalPaymentTimingModelStore optimalPaymentTimingModelStore;
	@Autowired
	private SupplierUrgencyModelStore supplierUrgencyModelStore;
	@Autowired
	private FactoringDecisionModelStore factoringDecisionModelStore;
	@Autowired
	private CashPoolOptimizationModelStore cashPoolOptimizationModelStore;
	// ====== END DYNAMIC DISCOUNTING MODEL STORES ======

// local state attributes and objects 
// these are the state variables used for processing simulation requests 
	private String					currentScenarioID = null;
	private MultiMarketRiskModel    currentMarketModel;
	private MultiBehaviorRiskModel 	currentBehaviorModel;
	private HashSet<String>	        currentActivatedModels = new HashSet<String>();
	
// handler for /rf2/eventsBatch callout processing 	
	@GetMapping("/marketData/{scid}")
	MarketData  doMarketData (@PathVariable String scid) {
  	 	System.out.println("**** fnp200 entered /marketData/{scid} ; scid = " + scid);
		Optional<Scenario> oscn = this.scenarioStore.findById(scid); 
		List<ReferenceIndex>  rfxl = new ArrayList< ReferenceIndex> ();
		Scenario scn;
		if (oscn.isPresent()) 
			{  System.out.println("**** fnp201 found scenario" );
			   scn = oscn.get();
			   }
		else
			{ throw new ScenarioNotFoundException(scid);}
		List<RiskFactorDescriptor> rfdl = scn.getRiskFactorDescriptors();
		Set<String>  mocl  = new HashSet<> ();
		for (RiskFactorDescriptor rfd : rfdl ) {
			if (rfd.getRiskFactorType().equals("ReferenceIndex") ) {
				Optional<ReferenceIndex> orfx =  this.referenceIndexStore.findById(rfd.getRiskFactorID());
				ReferenceIndex rfx;
				if (orfx.isPresent()) 
					{ rfx = orfx.get(); 
					System.out.println("**** fnp202 found rfx ; rfxid = " + rfx.getRiskFactorID());
					}
				else
					{ throw new ReferenceIndexNotFoundException(rfd.getRiskFactorID());}
				rfxl.add(rfx);
				String moc = rfx.getMarketObjectCode();
				if (! mocl.contains(moc)) 
					{ mocl.add(moc); }
				else throw new DuplicateMOCTimeSeriesException(moc);
				}
			}
		 MarketData marketData = new MarketData(rfxl);
		 System.out.println("**** fnp203 returning marketData " + marketData.toString());
		 return  marketData;
	  }
	
// handlers for /rf2/scenarioSimulation initiated callout processing
	 
	  @PostMapping("/scenarioSimulationStart")
	  String doScenarioSimulationStart(@RequestBody ScenarioDescriptor scenarioDescriptor) {
		  currentScenarioID = scenarioDescriptor.getScenarioID();
		  // checkout the scenario using  scenarioID from the  input descriptor
		  Optional<Scenario> oscn = scenarioStore.findById(currentScenarioID);
		  Scenario scn;
		  if (oscn.isPresent()) {
			  scn = oscn.get(); 
			  System.out.println("**** fnp204 found scn ; scnid = " + scn.getScenarioID() + 
					  " descriptors= " + scn.getRiskFactorDescriptors().toString());
			 	}
		  else {
			  throw new ScenarioNotFoundException(currentScenarioID);
		  	  }

		  // Process the scenario to create MultiMarketRiskModel and multiBehaviorRiskModel
		  this.currentMarketModel = new MultiMarketRiskModel();
		  this.currentBehaviorModel = new MultiBehaviorRiskModel();
		  
		  // a scenario has a list of RiskFactorDescriptors
		  for (RiskFactorDescriptor rfd : scn.getRiskFactorDescriptors()) {
			  String rfxid = rfd.getRiskFactorID();
			  System.out.println("**** fnp2041 found rfid= " + rfxid + " rfd: " + rfd.toString() ); 
			  if (rfd.getRiskFactorType().equals("ReferenceIndex")) {
				  Optional<ReferenceIndex> orfx = referenceIndexStore.findById(rfxid);
				  ReferenceIndex rfx;
				  if (orfx.isPresent()) {
					  rfx = orfx.get();
					  System.out.println("**** fnp205 found rfx ; rfxid = " + rfxid);
					  this.currentMarketModel.add(rfx.getMarketObjectCode(), new TimeSeriesModel(rfx));	
				  }
				  else {
					  throw new ReferenceIndexNotFoundException(rfxid); 
					  }
			  }
			  else if (rfd.getRiskFactorType().equals("TwoDimensionalPrepaymentModel")) {
				  Optional<TwoDimensionalPrepaymentModelData> oppmd =
						  this.twoDimensionalPrepaymentModelStore.findById(rfxid);
				  TwoDimensionalPrepaymentModelData ppmd;
				  if (oppmd.isPresent()) {
					  ppmd = oppmd.get();
					  System.out.println("**** fnp206 found ppmd ; rfxid = " + rfxid);
					  TwoDimensionalPrepaymentModel ppm = 
								 new TwoDimensionalPrepaymentModel(rfxid, ppmd,this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, ppm);
				  }
				  else  {
					  throw new TwoDimensionalPrepaymentModelNotFoundException(rfxid);
				  }
			  }  
			  else if (rfd.getRiskFactorType().equals("TwoDimensionalDepositTrxModel")) {
				  Optional<TwoDimensionalDepositTrxModelData> odxmd =
						  this.twoDimensionalDepositTrxModelStore.findById(rfxid);
				  TwoDimensionalDepositTrxModelData dxmd;
				  if (odxmd.isPresent()) {
					  dxmd = odxmd.get();
					  System.out.println("**** fnp207 found dxmd ; rfxid = " + rfxid);
					  TwoDimensionalDepositTrxModel dxm = 
								 new TwoDimensionalDepositTrxModel(rfxid, dxmd);
					  currentBehaviorModel.add(rfxid, dxm);
				  }
				  else  {
					  throw new TwoDimensionalDepositTrxModelNotFoundException(rfxid);
				  }
			  }
			  // ================================================================
			  // NEW: CollateralLTVModel branch
			  // ================================================================
			  else if (rfd.getRiskFactorType().equals("CollateralLTVModel")) {
				  Optional<CollateralLTVModelData> ocltv =
						  this.collateralLTVModelStore.findById(rfxid);
				  if (ocltv.isPresent()) {
					  System.out.println("**** fnp210 found cltv ; rfxid = " + rfxid);
					  // currentMarketModel must already contain ETH_USD by this point
					  // (ReferenceIndex entries are processed first in the descriptor list)
					  CollateralLTVModel cltv =
							  new CollateralLTVModel(rfxid, ocltv.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, cltv);
				  }
				  else {
					  throw new CollateralLTVModelNotFoundException(rfxid);
				  }
			  }
			  // ================================================================
			  // STABLECOIN BEHAVIORAL MODELS (8 models)
			  // ================================================================
			  else if (rfd.getRiskFactorType().equals("BackingRatioModel")) {
				  Optional<BackingRatioModelData> odata =
						  this.backingRatioModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp220 found BackingRatioModel ; rfxid = " + rfxid);
					  BackingRatioModel mdl =
							  new BackingRatioModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.stablecoin.BackingRatioModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("RedemptionPressureModel")) {
				  Optional<RedemptionPressureModelData> odata =
						  this.redemptionPressureModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp221 found RedemptionPressureModel ; rfxid = " + rfxid);
					  RedemptionPressureModel mdl =
							  new RedemptionPressureModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.stablecoin.RedemptionPressureModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("MaturityLadderModel")) {
				  Optional<MaturityLadderModelData> odata =
						  this.maturityLadderModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp222 found MaturityLadderModel ; rfxid = " + rfxid);
					  MaturityLadderModel mdl =
							  new MaturityLadderModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.stablecoin.MaturityLadderModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("AssetQualityModel")) {
				  Optional<AssetQualityModelData> odata =
						  this.assetQualityModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp223 found AssetQualityModel ; rfxid = " + rfxid);
					  AssetQualityModel mdl =
							  new AssetQualityModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.stablecoin.AssetQualityModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("ConcentrationDriftModel")) {
				  Optional<ConcentrationDriftModelData> odata =
						  this.concentrationDriftModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp224 found ConcentrationDriftModel ; rfxid = " + rfxid);
					  ConcentrationDriftModel mdl =
							  new ConcentrationDriftModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.stablecoin.ConcentrationDriftModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("ComplianceDriftModel")) {
				  Optional<ComplianceDriftModelData> odata =
						  this.complianceDriftModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp225 found ComplianceDriftModel ; rfxid = " + rfxid);
					  ComplianceDriftModel mdl =
							  new ComplianceDriftModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.stablecoin.ComplianceDriftModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("EarlyWarningModel")) {
				  Optional<EarlyWarningModelData> odata =
						  this.earlyWarningModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp226 found EarlyWarningModel ; rfxid = " + rfxid);
					  EarlyWarningModel mdl =
							  new EarlyWarningModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.stablecoin.EarlyWarningModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("ContinuousAttestationModel")) {
				  Optional<ContinuousAttestationModelData> odata =
						  this.continuousAttestationModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp227 found ContinuousAttestationModel ; rfxid = " + rfxid);
					  ContinuousAttestationModel mdl =
							  new ContinuousAttestationModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.stablecoin.ContinuousAttestationModelNotFoundException(rfxid);
				  }
			  }
			  // ================================================================
			  // HYBRID TREASURY BEHAVIORAL MODELS (8 models)
			  // ================================================================
			  else if (rfd.getRiskFactorType().equals("AllocationDriftModel")) {
				  Optional<AllocationDriftModelData> odata =
						  this.allocationDriftModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp230 found AllocationDriftModel ; rfxid = " + rfxid);
					  AllocationDriftModel mdl =
							  new AllocationDriftModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.hybridtreasury1.AllocationDriftModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("LiquidityBufferModel")) {
				  Optional<LiquidityBufferModelData> odata =
						  this.liquidityBufferModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp231 found LiquidityBufferModel ; rfxid = " + rfxid);
					  LiquidityBufferModel mdl =
							  new LiquidityBufferModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.hybridtreasury1.LiquidityBufferModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("PegStressModel")) {
				  Optional<PegStressModelData> odata =
						  this.pegStressModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp232 found PegStressModel ; rfxid = " + rfxid);
					  PegStressModel mdl =
							  new PegStressModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.hybridtreasury1.PegStressModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("RegulatoryDeRiskModel")) {
				  Optional<RegulatoryDeRiskModelData> odata =
						  this.regulatoryDeRiskModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp233 found RegulatoryDeRiskModel ; rfxid = " + rfxid);
					  RegulatoryDeRiskModel mdl =
							  new RegulatoryDeRiskModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.hybridtreasury1.RegulatoryDeRiskModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("YieldArbitrageModel")) {
				  Optional<YieldArbitrageModelData> odata =
						  this.yieldArbitrageModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp234 found YieldArbitrageModel ; rfxid = " + rfxid);
					  YieldArbitrageModel mdl =
							  new YieldArbitrageModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.hybridtreasury1.YieldArbitrageModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("CashConversionCycleModel")) {
				  Optional<CashConversionCycleModelData> odata =
						  this.cashConversionCycleModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp235 found CashConversionCycleModel ; rfxid = " + rfxid);
					  CashConversionCycleModel mdl =
							  new CashConversionCycleModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.hybridtreasury1.CashConversionCycleModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("FairValueComplianceModel")) {
				  Optional<FairValueComplianceModelData> odata =
						  this.fairValueComplianceModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp236 found FairValueComplianceModel ; rfxid = " + rfxid);
					  FairValueComplianceModel mdl =
							  new FairValueComplianceModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.hybridtreasury1.FairValueComplianceModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("IntegratedStressModel")) {
				  Optional<IntegratedStressModelData> odata =
						  this.integratedStressModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp237 found IntegratedStressModel ; rfxid = " + rfxid);
					  IntegratedStressModel mdl =
							  new IntegratedStressModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.hybridtreasury1.IntegratedStressModelNotFoundException(rfxid);
				  }
			  }
			  // ================================================================
			  // SUPPLY CHAIN TARIFF BEHAVIORAL MODELS (6 models)
			  // GTAP Armington elasticity-based, India-US tariff corridor
			  // ================================================================
			  else if (rfd.getRiskFactorType().equals("TariffSpreadModel")) {
				  Optional<TariffSpreadModelData> odata =
						  this.tariffSpreadModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp240 found TariffSpreadModel ; rfxid = " + rfxid);
					  TariffSpreadModel mdl =
							  new TariffSpreadModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.supplychaintariff1.TariffSpreadModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("WorkingCapitalStressModel")) {
				  Optional<WorkingCapitalStressModelData> odata =
						  this.workingCapitalStressModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp241 found WorkingCapitalStressModel ; rfxid = " + rfxid);
					  WorkingCapitalStressModel mdl =
							  new WorkingCapitalStressModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.supplychaintariff1.WorkingCapitalStressModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("HedgeEffectivenessModel")) {
				  Optional<HedgeEffectivenessModelData> odata =
						  this.hedgeEffectivenessModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp242 found HedgeEffectivenessModel ; rfxid = " + rfxid);
					  HedgeEffectivenessModel mdl =
							  new HedgeEffectivenessModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.supplychaintariff1.HedgeEffectivenessModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("RevenueElasticityModel")) {
				  Optional<RevenueElasticityModelData> odata =
						  this.revenueElasticityModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp243 found RevenueElasticityModel ; rfxid = " + rfxid);
					  RevenueElasticityModel mdl =
							  new RevenueElasticityModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.supplychaintariff1.RevenueElasticityModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("FXTariffCorrelationModel")) {
				  Optional<FXTariffCorrelationModelData> odata =
						  this.fxTariffCorrelationModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp244 found FXTariffCorrelationModel ; rfxid = " + rfxid);
					  FXTariffCorrelationModel mdl =
							  new FXTariffCorrelationModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.supplychaintariff1.FXTariffCorrelationModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("PortCongestionModel")) {
				  Optional<PortCongestionModelData> odata =
						  this.portCongestionModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp245 found PortCongestionModel ; rfxid = " + rfxid);
					  PortCongestionModel mdl =
							  new PortCongestionModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.supplychaintariff1.PortCongestionModelNotFoundException(rfxid);
				  }
			  }
			  // ================================================================
			  // DEFI LIQUIDATION BEHAVIORAL MODELS (7 models)
			  // CHRONOS-SHIELD liquidation risk framework
			  // ================================================================
			  else if (rfd.getRiskFactorType().equals("HealthFactorModel")) {
				  Optional<HealthFactorModelData> odata =
						  this.healthFactorModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp250 found HealthFactorModel ; rfxid = " + rfxid);
					  HealthFactorModel mdl =
							  new HealthFactorModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.defiliquidation1.HealthFactorModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("CollateralVelocityModel")) {
				  Optional<CollateralVelocityModelData> odata =
						  this.collateralVelocityModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp251 found CollateralVelocityModel ; rfxid = " + rfxid);
					  CollateralVelocityModel mdl =
							  new CollateralVelocityModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.defiliquidation1.CollateralVelocityModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("CollateralRebalancingModel")) {
				  Optional<CollateralRebalancingModelData> odata =
						  this.collateralRebalancingModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp252 found CollateralRebalancingModel ; rfxid = " + rfxid);
					  CollateralRebalancingModel mdl =
							  new CollateralRebalancingModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.defiliquidation1.CollateralRebalancingModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("CorrelationRiskModel")) {
				  Optional<CorrelationRiskModelData> odata =
						  this.correlationRiskModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp253 found CorrelationRiskModel ; rfxid = " + rfxid);
					  CorrelationRiskModel mdl =
							  new CorrelationRiskModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.defiliquidation1.CorrelationRiskModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("CascadeProbabilityModel")) {
				  Optional<CascadeProbabilityModelData> odata =
						  this.cascadeProbabilityModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp254 found CascadeProbabilityModel ; rfxid = " + rfxid);
					  CascadeProbabilityModel mdl =
							  new CascadeProbabilityModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.defiliquidation1.CascadeProbabilityModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("GasOptimizationModel")) {
				  Optional<GasOptimizationModelData> odata =
						  this.gasOptimizationModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp255 found GasOptimizationModel ; rfxid = " + rfxid);
					  GasOptimizationModel mdl =
							  new GasOptimizationModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.defiliquidation1.GasOptimizationModelNotFoundException(rfxid);
				  }
			  }
			  else if (rfd.getRiskFactorType().equals("InvoiceMaturityModel")) {
				  Optional<InvoiceMaturityModelData> odata =
						  this.invoiceMaturityModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp256 found InvoiceMaturityModel ; rfxid = " + rfxid);
					  InvoiceMaturityModel mdl =
							  new InvoiceMaturityModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  }
				  else {
					  throw new org.actus.risksrv3.controllers.defiliquidation1.InvoiceMaturityModelNotFoundException(rfxid);
				  }
			  }
			  // ================================================================
			  // ================================================================
			  // DYNAMIC DISCOUNTING BEHAVIORAL MODELS (6 models)
			  // ================================================================
			  else if (rfd.getRiskFactorType().equals("EarlySettlement")) {
				  Optional<EarlySettlementModelData> odata = this.earlySettlementModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp260 found EarlySettlementModel ; rfxid = " + rfxid);
					  EarlySettlementModel mdl = new EarlySettlementModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  } else { throw new org.actus.risksrv3.controllers.dynamicdiscounting1.EarlySettlementModelNotFoundException(rfxid); }
			  }
			  else if (rfd.getRiskFactorType().equals("PenaltyAccrual")) {
				  Optional<PenaltyAccrualModelData> odata = this.penaltyAccrualModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp261 found PenaltyAccrualModel ; rfxid = " + rfxid);
					  PenaltyAccrualModel mdl = new PenaltyAccrualModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  } else { throw new org.actus.risksrv3.controllers.dynamicdiscounting1.PenaltyAccrualModelNotFoundException(rfxid); }
			  }
			  else if (rfd.getRiskFactorType().equals("OptimalPaymentTiming")) {
				  Optional<OptimalPaymentTimingModelData> odata = this.optimalPaymentTimingModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp262 found OptimalPaymentTimingModel ; rfxid = " + rfxid);
					  OptimalPaymentTimingModel mdl = new OptimalPaymentTimingModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  } else { throw new org.actus.risksrv3.controllers.dynamicdiscounting1.OptimalPaymentTimingModelNotFoundException(rfxid); }
			  }
			  else if (rfd.getRiskFactorType().equals("SupplierUrgency")) {
				  Optional<SupplierUrgencyModelData> odata = this.supplierUrgencyModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp263 found SupplierUrgencyModel ; rfxid = " + rfxid);
					  SupplierUrgencyModel mdl = new SupplierUrgencyModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  } else { throw new org.actus.risksrv3.controllers.dynamicdiscounting1.SupplierUrgencyModelNotFoundException(rfxid); }
			  }
			  else if (rfd.getRiskFactorType().equals("FactoringDecision")) {
				  Optional<FactoringDecisionModelData> odata = this.factoringDecisionModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp264 found FactoringDecisionModel ; rfxid = " + rfxid);
					  FactoringDecisionModel mdl = new FactoringDecisionModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  } else { throw new org.actus.risksrv3.controllers.dynamicdiscounting1.FactoringDecisionModelNotFoundException(rfxid); }
			  }
			  else if (rfd.getRiskFactorType().equals("CashPoolOptimization")) {
				  Optional<CashPoolOptimizationModelData> odata = this.cashPoolOptimizationModelStore.findById(rfxid);
				  if (odata.isPresent()) {
					  System.out.println("**** fnp265 found CashPoolOptimizationModel ; rfxid = " + rfxid);
					  CashPoolOptimizationModel mdl = new CashPoolOptimizationModel(rfxid, odata.get(), this.currentMarketModel);
					  currentBehaviorModel.add(rfxid, mdl);
				  } else { throw new org.actus.risksrv3.controllers.dynamicdiscounting1.CashPoolOptimizationModelNotFoundException(rfxid); }
			  }
			  // ====== END DYNAMIC DISCOUNTING ======
			  else {
				  System.out.println("**** fnp208 unrecognized rfType= " + rfd.getRiskFactorType() );
			  }
		  }	
		  this.currentActivatedModels.clear();
		  String outstr = "** CurrentMarketModel initialized for scenario "+ currentScenarioID + "\n";
		  outstr += "keys are: " + this.currentMarketModel.keys().toString();
		  outstr += "** CurrentBehaviorModel also initialized with keys: " + 
		  this.currentBehaviorModel.keys().toString() + "\n";
		  return outstr;
	  }	
	  
	  @PostMapping("/contractSimulationStart")
	  List<CalloutData> doContractSimulationStart(@RequestBody Map<String,Object> contract){	  		  
		  ContractModel contractModel = ContractModel.parse(contract);
		  
		  // the MultiBehaviorRiskModel will get list of models to activate from contractModel
		  // BUT we need to check here that all models referred to by the contract are in the scenario
		  this.currentActivatedModels.clear();
		  List<String> ppmdls  = contractModel.getAs("prepaymentModels");
		  List<String> dwmdls  = contractModel.getAs("depositTrxModels");
		  List<String> cltvmdls = contractModel.getAs("collateralModels");
		  List<String> scmdls   = contractModel.getAs("stablecoinModels");
		  
		  // combine all model lists
		  List<String> mdls = new ArrayList<String>() ;
		  if (ppmdls != null) 
			  mdls.addAll(ppmdls);
		  if (dwmdls != null)
			  mdls.addAll(dwmdls);
		  if (cltvmdls != null)
			  mdls.addAll(cltvmdls);
		  if (scmdls != null)
			  mdls.addAll(scmdls);
		  List<String> trmdls  = contractModel.getAs("treasuryModels");
		  if (trmdls != null)
			  mdls.addAll(trmdls);
		  List<String> tariffmdls = contractModel.getAs("tariffModels");
		  if (tariffmdls != null)
			  mdls.addAll(tariffmdls);
		  List<String> defimdls = contractModel.getAs("defiLiquidationModels");
		  if (defimdls != null)
			  mdls.addAll(defimdls);
		  List<String> discmdls = contractModel.getAs("discountingModels");
		  if (discmdls != null)
			  mdls.addAll(discmdls);
		  
		  List<CalloutData> observations = new ArrayList<CalloutData>();
		  for (String mdl : mdls) {
			  if ( currentBehaviorModel.keys().contains(mdl))  {
					  currentActivatedModels.add(mdl);
					  observations.addAll(currentBehaviorModel.modelContractStart(contractModel, mdl));
			  }
			  else
					  throw new RiskModelNotFoundException("*** modelID: " + mdl + " in scenario: " + currentScenarioID);
		  }
	      return observations;
	  }  	  

	  @PostMapping("/marketStateAt")
	  Double doMarketStateAt(@RequestBody StateAtInput stateAtInput) {
		  String id = stateAtInput.getId();
		  System.out.println("***fnp2071 looking for moc= " + id + " market Keys= " + currentMarketModel.keys().toString() );
		  LocalDateTime time = stateAtInput.getTime();	
		  Double dval = this.currentMarketModel.stateAt(id, time);
		  System.out.println("**** fnp207: /marketStateAt id = "+id+" time= "
		     + time.toString() + " scenario= " + this.currentScenarioID +
		     " value= " + dval.toString()); 
		  return dval;
	  }
	  
	  @PostMapping("/behaviorStateAt")
	  double doBehaviorStateAt(@RequestBody BehaviorStateAtInput behaviorStateAtInput) {
		  String mdlid = behaviorStateAtInput.getRiskFactorId();
		  System.out.println("**** fnp208: in  /behaviorStateAt id = "+ mdlid );
		  LocalDateTime time = behaviorStateAtInput.getTime();
		  StateSpace state = behaviorStateAtInput.getStates();
		  double dval = this.currentBehaviorModel.stateAt(mdlid, time, state);
		  System.out.println("**** fnp209: /behavior id = " + mdlid + " time= "
				     + time.toString() + " nominalInterest= " 
				     + state.nominalInterestRate +   " value= " + dval );
		  return dval;
	  }

	  @GetMapping("/marketKeys") 
	  HashSet<String> doMarketKeys() {	
		  Set<String> kset = this.currentMarketModel.keys();
		  HashSet<String> hks = new HashSet<String>();
		  for (String ks : kset) {
			  hks.add(ks);
		  }
		  return hks;
	  }
	  
	  @GetMapping("/activeScenario")
	  String doActiveScenario() {
		  String out;
		  if (currentScenarioID == null)  {
			  out = "No scenario currently active." ;
		  }
		  else { 
			  out = "Currently activeScenario: " + currentScenarioID + "\n" ;	
		  }
		  return out;	  
	  }
	  
	  @GetMapping("/currentBehaviorKeys")
	  HashSet<String> doCurrentBehaviorKeys(){
		  Set<String> kset = this.currentBehaviorModel.keys();
		  HashSet<String> hks = new HashSet<String>();
		  for (String ks :kset) {
			  hks.add(ks);
		  }
		  System.out.println("**** fnp214 in /currentBehaviorKeys");
		  return hks;
	  }
	 
	  @GetMapping("/activeBehaviorKeys")
	  HashSet<String> doActiveBehaviorKeys(){
	      return this.currentActivatedModels;
	  }
	  
}