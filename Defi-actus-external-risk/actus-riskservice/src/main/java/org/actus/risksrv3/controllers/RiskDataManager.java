package org.actus.risksrv3.controllers;
import  org.actus.risksrv3.models.ReferenceIndex;
import  org.actus.risksrv3.models.Scenario;
import  org.actus.risksrv3.models.TwoDimensionalPrepaymentModelData;
import  org.actus.risksrv3.models.TwoDimensionalDepositTrxModelData;
import  org.actus.risksrv3.models.CollateralLTVModelData;
import  org.actus.risksrv3.models.BufferLTVModelData;
// ====== STABLECOIN MODEL IMPORTS ======
import  org.actus.risksrv3.models.stablecoin.BackingRatioModelData;
import  org.actus.risksrv3.models.stablecoin.RedemptionPressureModelData;
import  org.actus.risksrv3.models.stablecoin.MaturityLadderModelData;
import  org.actus.risksrv3.models.stablecoin.AssetQualityModelData;
import  org.actus.risksrv3.models.stablecoin.ConcentrationDriftModelData;
import  org.actus.risksrv3.models.stablecoin.ComplianceDriftModelData;
import  org.actus.risksrv3.models.stablecoin.EarlyWarningModelData;
import  org.actus.risksrv3.models.stablecoin.ContinuousAttestationModelData;
// ====== END STABLECOIN MODEL IMPORTS ======
// ====== HYBRID TREASURY MODEL IMPORTS ======
import  org.actus.risksrv3.models.hybridtreasury1.AllocationDriftModelData;
import  org.actus.risksrv3.models.hybridtreasury1.LiquidityBufferModelData;
import  org.actus.risksrv3.models.hybridtreasury1.PegStressModelData;
import  org.actus.risksrv3.models.hybridtreasury1.RegulatoryDeRiskModelData;
import  org.actus.risksrv3.models.hybridtreasury1.YieldArbitrageModelData;
import  org.actus.risksrv3.models.hybridtreasury1.CashConversionCycleModelData;
import  org.actus.risksrv3.models.hybridtreasury1.FairValueComplianceModelData;
import  org.actus.risksrv3.models.hybridtreasury1.IntegratedStressModelData;
import  org.actus.risksrv3.models.hybridtreasury1.ScheduledCashFlowModelData;
// ====== END HYBRID TREASURY MODEL IMPORTS ======
// ====== SUPPLY CHAIN TARIFF MODEL IMPORTS ======
import  org.actus.risksrv3.models.supplychaintariff1.TariffSpreadModelData;
import  org.actus.risksrv3.models.supplychaintariff1.WorkingCapitalStressModelData;
import  org.actus.risksrv3.models.supplychaintariff1.HedgeEffectivenessModelData;
import  org.actus.risksrv3.models.supplychaintariff1.RevenueElasticityModelData;
import  org.actus.risksrv3.models.supplychaintariff1.FXTariffCorrelationModelData;
import  org.actus.risksrv3.models.supplychaintariff1.PortCongestionModelData;
// ====== END SUPPLY CHAIN TARIFF MODEL IMPORTS ======
// ====== DEFI LIQUIDATION MODEL IMPORTS ======
import  org.actus.risksrv3.models.defiliquidation1.HealthFactorModelData;
import  org.actus.risksrv3.models.defiliquidation1.CollateralVelocityModelData;
import  org.actus.risksrv3.models.defiliquidation1.CollateralRebalancingModelData;
import  org.actus.risksrv3.models.defiliquidation1.CorrelationRiskModelData;
import  org.actus.risksrv3.models.defiliquidation1.CascadeProbabilityModelData;
import  org.actus.risksrv3.models.defiliquidation1.GasOptimizationModelData;
import  org.actus.risksrv3.models.defiliquidation1.InvoiceMaturityModelData;
// ====== END DEFI LIQUIDATION MODEL IMPORTS ======
// ====== DYNAMIC DISCOUNTING MODEL IMPORTS ======
import  org.actus.risksrv3.models.dynamicdiscounting1.EarlySettlementModelData;
import  org.actus.risksrv3.models.dynamicdiscounting1.PenaltyAccrualModelData;
import  org.actus.risksrv3.models.dynamicdiscounting1.OptimalPaymentTimingModelData;
import  org.actus.risksrv3.models.dynamicdiscounting1.SupplierUrgencyModelData;
import  org.actus.risksrv3.models.dynamicdiscounting1.FactoringDecisionModelData;
import  org.actus.risksrv3.models.dynamicdiscounting1.CashPoolOptimizationModelData;
// ====== END DYNAMIC DISCOUNTING MODEL IMPORTS ======
import  org.actus.risksrv3.repository.ReferenceIndexStore;
import  org.actus.risksrv3.repository.ScenarioStore;
import  org.actus.risksrv3.repository.TwoDimensionalPrepaymentModelStore;
import  org.actus.risksrv3.repository.TwoDimensionalDepositTrxModelStore;
import  org.actus.risksrv3.repository.CollateralLTVModelStore;
import  org.actus.risksrv3.repository.BufferLTVModelStore;
// ====== STABLECOIN STORE IMPORTS ======
import  org.actus.risksrv3.repository.stablecoin.BackingRatioModelStore;
import  org.actus.risksrv3.repository.stablecoin.RedemptionPressureModelStore;
import  org.actus.risksrv3.repository.stablecoin.MaturityLadderModelStore;
import  org.actus.risksrv3.repository.stablecoin.AssetQualityModelStore;
import  org.actus.risksrv3.repository.stablecoin.ConcentrationDriftModelStore;
import  org.actus.risksrv3.repository.stablecoin.ComplianceDriftModelStore;
import  org.actus.risksrv3.repository.stablecoin.EarlyWarningModelStore;
import  org.actus.risksrv3.repository.stablecoin.ContinuousAttestationModelStore;
// ====== END STABLECOIN STORE IMPORTS ======
// ====== HYBRID TREASURY STORE IMPORTS ======
import  org.actus.risksrv3.repository.hybridtreasury1.AllocationDriftModelStore;
import  org.actus.risksrv3.repository.hybridtreasury1.LiquidityBufferModelStore;
import  org.actus.risksrv3.repository.hybridtreasury1.PegStressModelStore;
import  org.actus.risksrv3.repository.hybridtreasury1.RegulatoryDeRiskModelStore;
import  org.actus.risksrv3.repository.hybridtreasury1.YieldArbitrageModelStore;
import  org.actus.risksrv3.repository.hybridtreasury1.CashConversionCycleModelStore;
import  org.actus.risksrv3.repository.hybridtreasury1.FairValueComplianceModelStore;
import  org.actus.risksrv3.repository.hybridtreasury1.IntegratedStressModelStore;
import  org.actus.risksrv3.repository.hybridtreasury1.ScheduledCashFlowModelStore;
// ====== END HYBRID TREASURY STORE IMPORTS ======
// ====== SUPPLY CHAIN TARIFF STORE IMPORTS ======
import  org.actus.risksrv3.repository.supplychaintariff1.TariffSpreadModelStore;
import  org.actus.risksrv3.repository.supplychaintariff1.WorkingCapitalStressModelStore;
import  org.actus.risksrv3.repository.supplychaintariff1.HedgeEffectivenessModelStore;
import  org.actus.risksrv3.repository.supplychaintariff1.RevenueElasticityModelStore;
import  org.actus.risksrv3.repository.supplychaintariff1.FXTariffCorrelationModelStore;
import  org.actus.risksrv3.repository.supplychaintariff1.PortCongestionModelStore;
// ====== END SUPPLY CHAIN TARIFF STORE IMPORTS ======
// ====== DEFI LIQUIDATION STORE IMPORTS ======
import  org.actus.risksrv3.repository.defiliquidation1.HealthFactorModelStore;
import  org.actus.risksrv3.repository.defiliquidation1.CollateralVelocityModelStore;
import  org.actus.risksrv3.repository.defiliquidation1.CollateralRebalancingModelStore;
import  org.actus.risksrv3.repository.defiliquidation1.CorrelationRiskModelStore;
import  org.actus.risksrv3.repository.defiliquidation1.CascadeProbabilityModelStore;
import  org.actus.risksrv3.repository.defiliquidation1.GasOptimizationModelStore;
import  org.actus.risksrv3.repository.defiliquidation1.InvoiceMaturityModelStore;
// ====== END DEFI LIQUIDATION STORE IMPORTS ======
// ====== DYNAMIC DISCOUNTING STORE IMPORTS ======
import  org.actus.risksrv3.repository.dynamicdiscounting1.EarlySettlementModelStore;
import  org.actus.risksrv3.repository.dynamicdiscounting1.PenaltyAccrualModelStore;
import  org.actus.risksrv3.repository.dynamicdiscounting1.OptimalPaymentTimingModelStore;
import  org.actus.risksrv3.repository.dynamicdiscounting1.SupplierUrgencyModelStore;
import  org.actus.risksrv3.repository.dynamicdiscounting1.FactoringDecisionModelStore;
import  org.actus.risksrv3.repository.dynamicdiscounting1.CashPoolOptimizationModelStore;
// ====== END DYNAMIC DISCOUNTING STORE IMPORTS ======
import  org.springframework.beans.factory.annotation.Autowired;
import  org.springframework.beans.factory.annotation.Value;
import  org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

//Annotation 
@RestController
public class RiskDataManager {
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
	@Autowired
	private BufferLTVModelStore bufferLTVModelStore;

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
	@Autowired
	private ScheduledCashFlowModelStore scheduledCashFlowModelStore;
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
	
	private
	@Value("${spring.data.mongodb.host}")
	String mongodbHost;
	
	private
	@Value("${spring.data.mongodb.port}")
	Integer mongodbPort;
	
	// demonstrate access to application properties 
	@GetMapping("/propertiesMongoHost")
	public String doPropertiesMongoHost ( ) {
	    return ( "Value of mongodbHost = " + mongodbHost + "\n");
	}
	
	@GetMapping("/propertiesMongoPort")
	public String doPropertiesMongoPort ( ) {
		return ( "Value of mongodbPort = " + mongodbPort + "\n");
	}
	
	@PostMapping("/addReferenceIndex")
    public String saveReferenceIndex(@RequestBody ReferenceIndex referenceIndex){
        referenceIndexStore.save(referenceIndex);      
        return "ReferenceIndex added Successfully\n";
    }	
	// Path parameter id is ReferenceIndexID  i.e. riskFactorType == "ReferenceIndex" in any descriptor 
    @DeleteMapping("/deleteReferenceIndex/{id}")
    public String deleteReferenceIndex(@PathVariable String id){
        referenceIndexStore.deleteById(id);     
        return "ReferenceIndex deleted Successfully\n";
    }   
    @GetMapping("/findReferenceIndex/{id}")
    public Optional<ReferenceIndex> findReferenceIndex(@PathVariable String id) {
    	Optional<ReferenceIndex> rx = referenceIndexStore.findById(id);
    	return rx;
    }
	
    @GetMapping("/findAllReferenceIndexes")
    public List<ReferenceIndex> getReferenceIndexes() {      
    	return referenceIndexStore.findAll();
    }
    
    // id is here a ScenarioID  i.e. riskFactorType == "Scenario" in any descriptor
	@PostMapping("/addScenario")
    public String saveScenario(@RequestBody Scenario scenario){
        scenarioStore.save(scenario);      
        return "Scenario added Successfully\n";
    }	
	// id is here a ScenarioID i.e. riskFactorType == "Scenario" in any descriptor
    @DeleteMapping("/deleteScenario/{id}")
    public String deleteScenario(@PathVariable String id){
        scenarioStore.deleteById(id);      
        return "Scenario Deleted Successfully\n";
    }
    @GetMapping("/findScenario/{id}")
    public Optional<Scenario> findScenario(@PathVariable String id) {
    	 return  scenarioStore.findById(id);
    }
    
    @GetMapping("/findAllScenarios")
    public List<Scenario> getScenarios() {
        return scenarioStore.findAll();
    }
    
    // Path Parameter id is here a TwoParameterPrepaymentModelID 
    //i.e.  a String riskFactorID  with associated riskFactorType == "TwoDimensionalPrepaymentModel" 
	@PostMapping("/addTwoDimensionalPrepaymentModel")
    public String saveTwoDimensionalPrepaymentModelData(
    		@RequestBody TwoDimensionalPrepaymentModelData twoDimensionalPrepaymentModelData){
        twoDimensionalPrepaymentModelStore.save(twoDimensionalPrepaymentModelData);      
        return "TwoDimensionalPrepayment model added successfully\n";
    }	
	// id is a TwoDimensionalPrepaymentModelID 
    @DeleteMapping("/deleteTwoDimensionalPrepaymentModel/{id}")
    public String deleteTwoDimensionalPrepaymentModel(@PathVariable String id){
        twoDimensionalPrepaymentModelStore.deleteById(id);      
        return "TwoDimensionalPrepaymentModel deleted Successfully\n";
    }
    @GetMapping("/findTwoDimensionalPrepaymentModel/{id}")
    public Optional<TwoDimensionalPrepaymentModelData> findTwoDimensionalPrepaymentModelData(@PathVariable String id) {
    	 return  twoDimensionalPrepaymentModelStore.findById(id);
    }
    
    @GetMapping("/findAllTwoDimensionalPrepaymentModels")
    public List<TwoDimensionalPrepaymentModelData> getTwoDimensionalPrepaymentModels() {
        return twoDimensionalPrepaymentModelStore.findAll();
    }
    
    // Path Parameter id is here a TwoParameterDepositTrxModelID 
    //i.e.  a String riskFactorID with associated riskFactorType == "TwoDimensionalDepositTrxModel" 
	@PostMapping("/addTwoDimensionalDepositTrxModel")
    public String saveTwoDimensionalDepositTrxModelData(
    		@RequestBody TwoDimensionalDepositTrxModelData twoDimensionalDepositTrxModelData){
        twoDimensionalDepositTrxModelStore.save(twoDimensionalDepositTrxModelData);      
        return "TwoDimensionalDepositTrx model added successfully\n";
    }
	// id is a TwoDimensionalDepositTrxModelID 
    @DeleteMapping("/deleteTwoDimensionalDepositTrxModel/{id}")
    public String deleteTwoDimensionalDepositTrxModel(@PathVariable String id){
        twoDimensionalDepositTrxModelStore.deleteById(id);      
        return "TwoDimensionalDepositTrx model deleted Successfully\n";
    }
    @GetMapping("/findTwoDimensionalDepositTrxModel/{id}")
    public Optional<TwoDimensionalDepositTrxModelData> findTwoDimensionalDepositTrxModelData(@PathVariable String id) {
    	 return  twoDimensionalDepositTrxModelStore.findById(id);
    }
    
    @GetMapping("/findAllTwoDimensionalDepositTrxModels")
    public List<TwoDimensionalDepositTrxModelData> getTwoDimensionalDepositTrxModels() {
        return twoDimensionalDepositTrxModelStore.findAll();
    }

    // =========================================================================
    // CollateralLTVModel CRUD endpoints
    // =========================================================================

    @PostMapping("/addCollateralLTVModel")
    public String saveCollateralLTVModelData(
    		@RequestBody CollateralLTVModelData collateralLTVModelData){
        collateralLTVModelStore.save(collateralLTVModelData);
        return "CollateralLTVModel added successfully\n";
    }

    @DeleteMapping("/deleteCollateralLTVModel/{id}")
    public String deleteCollateralLTVModel(@PathVariable String id){
        collateralLTVModelStore.deleteById(id);
        return "CollateralLTVModel deleted Successfully\n";
    }

    @GetMapping("/findCollateralLTVModel/{id}")
    public Optional<CollateralLTVModelData> findCollateralLTVModelData(@PathVariable String id) {
        return collateralLTVModelStore.findById(id);
    }

    @GetMapping("/findAllCollateralLTVModels")
    public List<CollateralLTVModelData> getCollateralLTVModels() {
        return collateralLTVModelStore.findAll();
    }

    // =========================================================================
    // BufferLTVModel CRUD endpoints - OPTION B (Buffer-First Strategy)
    // =========================================================================

    @PostMapping("/addBufferLTVModel")
    public String saveBufferLTVModelData(
    		@RequestBody BufferLTVModelData bufferLTVModelData){
        bufferLTVModelStore.save(bufferLTVModelData);
        return "BufferLTVModel added successfully\n";
    }

    @DeleteMapping("/deleteBufferLTVModel/{id}")
    public String deleteBufferLTVModel(@PathVariable String id){
        bufferLTVModelStore.deleteById(id);
        return "BufferLTVModel deleted Successfully\n";
    }

    @GetMapping("/findBufferLTVModel/{id}")
    public Optional<BufferLTVModelData> findBufferLTVModelData(@PathVariable String id) {
        return bufferLTVModelStore.findById(id);
    }

    @GetMapping("/findAllBufferLTVModels")
    public List<BufferLTVModelData> getBufferLTVModels() {
        return bufferLTVModelStore.findAll();
    }

    // =========================================================================
    // STABLECOIN BEHAVIORAL MODEL CRUD ENDPOINTS (8 models × 4 endpoints = 32)
    // riskFactorType values in scenario descriptors:
    //   "BackingRatioModel", "RedemptionPressureModel", "MaturityLadderModel",
    //   "AssetQualityModel", "ConcentrationDriftModel", "ComplianceDriftModel",
    //   "EarlyWarningModel", "ContinuousAttestationModel"
    // =========================================================================

    // --- 6.1 BackingRatioModel ---
    @PostMapping("/addBackingRatioModel")
    public String saveBackingRatioModelData(
            @RequestBody BackingRatioModelData data) {
        backingRatioModelStore.save(data);
        return "BackingRatioModel added successfully\n";
    }
    @DeleteMapping("/deleteBackingRatioModel/{id}")
    public String deleteBackingRatioModel(@PathVariable String id) {
        backingRatioModelStore.deleteById(id);
        return "BackingRatioModel deleted successfully\n";
    }
    @GetMapping("/findBackingRatioModel/{id}")
    public Optional<BackingRatioModelData> findBackingRatioModelData(@PathVariable String id) {
        return backingRatioModelStore.findById(id);
    }
    @GetMapping("/findAllBackingRatioModels")
    public List<BackingRatioModelData> getBackingRatioModels() {
        return backingRatioModelStore.findAll();
    }

    // --- 6.2 RedemptionPressureModel ---
    @PostMapping("/addRedemptionPressureModel")
    public String saveRedemptionPressureModelData(
            @RequestBody RedemptionPressureModelData data) {
        redemptionPressureModelStore.save(data);
        return "RedemptionPressureModel added successfully\n";
    }
    @DeleteMapping("/deleteRedemptionPressureModel/{id}")
    public String deleteRedemptionPressureModel(@PathVariable String id) {
        redemptionPressureModelStore.deleteById(id);
        return "RedemptionPressureModel deleted successfully\n";
    }
    @GetMapping("/findRedemptionPressureModel/{id}")
    public Optional<RedemptionPressureModelData> findRedemptionPressureModelData(@PathVariable String id) {
        return redemptionPressureModelStore.findById(id);
    }
    @GetMapping("/findAllRedemptionPressureModels")
    public List<RedemptionPressureModelData> getRedemptionPressureModels() {
        return redemptionPressureModelStore.findAll();
    }

    // --- 6.3 MaturityLadderModel ---
    @PostMapping("/addMaturityLadderModel")
    public String saveMaturityLadderModelData(
            @RequestBody MaturityLadderModelData data) {
        maturityLadderModelStore.save(data);
        return "MaturityLadderModel added successfully\n";
    }
    @DeleteMapping("/deleteMaturityLadderModel/{id}")
    public String deleteMaturityLadderModel(@PathVariable String id) {
        maturityLadderModelStore.deleteById(id);
        return "MaturityLadderModel deleted successfully\n";
    }
    @GetMapping("/findMaturityLadderModel/{id}")
    public Optional<MaturityLadderModelData> findMaturityLadderModelData(@PathVariable String id) {
        return maturityLadderModelStore.findById(id);
    }
    @GetMapping("/findAllMaturityLadderModels")
    public List<MaturityLadderModelData> getMaturityLadderModels() {
        return maturityLadderModelStore.findAll();
    }

    // --- 6.4 AssetQualityModel ---
    @PostMapping("/addAssetQualityModel")
    public String saveAssetQualityModelData(
            @RequestBody AssetQualityModelData data) {
        assetQualityModelStore.save(data);
        return "AssetQualityModel added successfully\n";
    }
    @DeleteMapping("/deleteAssetQualityModel/{id}")
    public String deleteAssetQualityModel(@PathVariable String id) {
        assetQualityModelStore.deleteById(id);
        return "AssetQualityModel deleted successfully\n";
    }
    @GetMapping("/findAssetQualityModel/{id}")
    public Optional<AssetQualityModelData> findAssetQualityModelData(@PathVariable String id) {
        return assetQualityModelStore.findById(id);
    }
    @GetMapping("/findAllAssetQualityModels")
    public List<AssetQualityModelData> getAssetQualityModels() {
        return assetQualityModelStore.findAll();
    }

    // --- 6.5 ConcentrationDriftModel ---
    @PostMapping("/addConcentrationDriftModel")
    public String saveConcentrationDriftModelData(
            @RequestBody ConcentrationDriftModelData data) {
        concentrationDriftModelStore.save(data);
        return "ConcentrationDriftModel added successfully\n";
    }
    @DeleteMapping("/deleteConcentrationDriftModel/{id}")
    public String deleteConcentrationDriftModel(@PathVariable String id) {
        concentrationDriftModelStore.deleteById(id);
        return "ConcentrationDriftModel deleted successfully\n";
    }
    @GetMapping("/findConcentrationDriftModel/{id}")
    public Optional<ConcentrationDriftModelData> findConcentrationDriftModelData(@PathVariable String id) {
        return concentrationDriftModelStore.findById(id);
    }
    @GetMapping("/findAllConcentrationDriftModels")
    public List<ConcentrationDriftModelData> getConcentrationDriftModels() {
        return concentrationDriftModelStore.findAll();
    }

    // --- 6.6 ComplianceDriftModel ---
    @PostMapping("/addComplianceDriftModel")
    public String saveComplianceDriftModelData(
            @RequestBody ComplianceDriftModelData data) {
        complianceDriftModelStore.save(data);
        return "ComplianceDriftModel added successfully\n";
    }
    @DeleteMapping("/deleteComplianceDriftModel/{id}")
    public String deleteComplianceDriftModel(@PathVariable String id) {
        complianceDriftModelStore.deleteById(id);
        return "ComplianceDriftModel deleted successfully\n";
    }
    @GetMapping("/findComplianceDriftModel/{id}")
    public Optional<ComplianceDriftModelData> findComplianceDriftModelData(@PathVariable String id) {
        return complianceDriftModelStore.findById(id);
    }
    @GetMapping("/findAllComplianceDriftModels")
    public List<ComplianceDriftModelData> getComplianceDriftModels() {
        return complianceDriftModelStore.findAll();
    }

    // --- 6.7 EarlyWarningModel ---
    @PostMapping("/addEarlyWarningModel")
    public String saveEarlyWarningModelData(
            @RequestBody EarlyWarningModelData data) {
        earlyWarningModelStore.save(data);
        return "EarlyWarningModel added successfully\n";
    }
    @DeleteMapping("/deleteEarlyWarningModel/{id}")
    public String deleteEarlyWarningModel(@PathVariable String id) {
        earlyWarningModelStore.deleteById(id);
        return "EarlyWarningModel deleted successfully\n";
    }
    @GetMapping("/findEarlyWarningModel/{id}")
    public Optional<EarlyWarningModelData> findEarlyWarningModelData(@PathVariable String id) {
        return earlyWarningModelStore.findById(id);
    }
    @GetMapping("/findAllEarlyWarningModels")
    public List<EarlyWarningModelData> getEarlyWarningModels() {
        return earlyWarningModelStore.findAll();
    }

    // --- 6.8 ContinuousAttestationModel ---
    @PostMapping("/addContinuousAttestationModel")
    public String saveContinuousAttestationModelData(
            @RequestBody ContinuousAttestationModelData data) {
        continuousAttestationModelStore.save(data);
        return "ContinuousAttestationModel added successfully\n";
    }
    @DeleteMapping("/deleteContinuousAttestationModel/{id}")
    public String deleteContinuousAttestationModel(@PathVariable String id) {
        continuousAttestationModelStore.deleteById(id);
        return "ContinuousAttestationModel deleted successfully\n";
    }
    @GetMapping("/findContinuousAttestationModel/{id}")
    public Optional<ContinuousAttestationModelData> findContinuousAttestationModelData(@PathVariable String id) {
        return continuousAttestationModelStore.findById(id);
    }
    @GetMapping("/findAllContinuousAttestationModels")
    public List<ContinuousAttestationModelData> getContinuousAttestationModels() {
        return continuousAttestationModelStore.findAll();
    }

    // =========================================================================
    // HYBRID TREASURY BEHAVIORAL MODEL CRUD ENDPOINTS (8 models × 4 = 32)
    // riskFactorType values in scenario descriptors:
    //   "AllocationDriftModel", "LiquidityBufferModel", "PegStressModel",
    //   "RegulatoryDeRiskModel", "YieldArbitrageModel", "CashConversionCycleModel",
    //   "FairValueComplianceModel", "IntegratedStressModel"
    // =========================================================================

    // --- 5.1 AllocationDriftModel ---
    @PostMapping("/addAllocationDriftModel")
    public String saveAllocationDriftModelData(
            @RequestBody AllocationDriftModelData data) {
        allocationDriftModelStore.save(data);
        return "AllocationDriftModel added successfully\n";
    }
    @DeleteMapping("/deleteAllocationDriftModel/{id}")
    public String deleteAllocationDriftModel(@PathVariable String id) {
        allocationDriftModelStore.deleteById(id);
        return "AllocationDriftModel deleted successfully\n";
    }
    @GetMapping("/findAllocationDriftModel/{id}")
    public Optional<AllocationDriftModelData> findAllocationDriftModelData(@PathVariable String id) {
        return allocationDriftModelStore.findById(id);
    }
    @GetMapping("/findAllAllocationDriftModels")
    public List<AllocationDriftModelData> getAllocationDriftModels() {
        return allocationDriftModelStore.findAll();
    }

    // --- 5.2 LiquidityBufferModel ---
    @PostMapping("/addLiquidityBufferModel")
    public String saveLiquidityBufferModelData(
            @RequestBody LiquidityBufferModelData data) {
        liquidityBufferModelStore.save(data);
        return "LiquidityBufferModel added successfully\n";
    }
    @DeleteMapping("/deleteLiquidityBufferModel/{id}")
    public String deleteLiquidityBufferModel(@PathVariable String id) {
        liquidityBufferModelStore.deleteById(id);
        return "LiquidityBufferModel deleted successfully\n";
    }
    @GetMapping("/findLiquidityBufferModel/{id}")
    public Optional<LiquidityBufferModelData> findLiquidityBufferModelData(@PathVariable String id) {
        return liquidityBufferModelStore.findById(id);
    }
    @GetMapping("/findAllLiquidityBufferModels")
    public List<LiquidityBufferModelData> getLiquidityBufferModels() {
        return liquidityBufferModelStore.findAll();
    }

    // --- 5.3 PegStressModel ---
    @PostMapping("/addPegStressModel")
    public String savePegStressModelData(
            @RequestBody PegStressModelData data) {
        pegStressModelStore.save(data);
        return "PegStressModel added successfully\n";
    }
    @DeleteMapping("/deletePegStressModel/{id}")
    public String deletePegStressModel(@PathVariable String id) {
        pegStressModelStore.deleteById(id);
        return "PegStressModel deleted successfully\n";
    }
    @GetMapping("/findPegStressModel/{id}")
    public Optional<PegStressModelData> findPegStressModelData(@PathVariable String id) {
        return pegStressModelStore.findById(id);
    }
    @GetMapping("/findAllPegStressModels")
    public List<PegStressModelData> getPegStressModels() {
        return pegStressModelStore.findAll();
    }

    // --- 5.4 RegulatoryDeRiskModel ---
    @PostMapping("/addRegulatoryDeRiskModel")
    public String saveRegulatoryDeRiskModelData(
            @RequestBody RegulatoryDeRiskModelData data) {
        regulatoryDeRiskModelStore.save(data);
        return "RegulatoryDeRiskModel added successfully\n";
    }
    @DeleteMapping("/deleteRegulatoryDeRiskModel/{id}")
    public String deleteRegulatoryDeRiskModel(@PathVariable String id) {
        regulatoryDeRiskModelStore.deleteById(id);
        return "RegulatoryDeRiskModel deleted successfully\n";
    }
    @GetMapping("/findRegulatoryDeRiskModel/{id}")
    public Optional<RegulatoryDeRiskModelData> findRegulatoryDeRiskModelData(@PathVariable String id) {
        return regulatoryDeRiskModelStore.findById(id);
    }
    @GetMapping("/findAllRegulatoryDeRiskModels")
    public List<RegulatoryDeRiskModelData> getRegulatoryDeRiskModels() {
        return regulatoryDeRiskModelStore.findAll();
    }

    // --- 5.5 YieldArbitrageModel ---
    @PostMapping("/addYieldArbitrageModel")
    public String saveYieldArbitrageModelData(
            @RequestBody YieldArbitrageModelData data) {
        yieldArbitrageModelStore.save(data);
        return "YieldArbitrageModel added successfully\n";
    }
    @DeleteMapping("/deleteYieldArbitrageModel/{id}")
    public String deleteYieldArbitrageModel(@PathVariable String id) {
        yieldArbitrageModelStore.deleteById(id);
        return "YieldArbitrageModel deleted successfully\n";
    }
    @GetMapping("/findYieldArbitrageModel/{id}")
    public Optional<YieldArbitrageModelData> findYieldArbitrageModelData(@PathVariable String id) {
        return yieldArbitrageModelStore.findById(id);
    }
    @GetMapping("/findAllYieldArbitrageModels")
    public List<YieldArbitrageModelData> getYieldArbitrageModels() {
        return yieldArbitrageModelStore.findAll();
    }

    // --- 5.6 CashConversionCycleModel ---
    @PostMapping("/addCashConversionCycleModel")
    public String saveCashConversionCycleModelData(
            @RequestBody CashConversionCycleModelData data) {
        cashConversionCycleModelStore.save(data);
        return "CashConversionCycleModel added successfully\n";
    }
    @DeleteMapping("/deleteCashConversionCycleModel/{id}")
    public String deleteCashConversionCycleModel(@PathVariable String id) {
        cashConversionCycleModelStore.deleteById(id);
        return "CashConversionCycleModel deleted successfully\n";
    }
    @GetMapping("/findCashConversionCycleModel/{id}")
    public Optional<CashConversionCycleModelData> findCashConversionCycleModelData(@PathVariable String id) {
        return cashConversionCycleModelStore.findById(id);
    }
    @GetMapping("/findAllCashConversionCycleModels")
    public List<CashConversionCycleModelData> getCashConversionCycleModels() {
        return cashConversionCycleModelStore.findAll();
    }

    // --- 5.7 FairValueComplianceModel ---
    @PostMapping("/addFairValueComplianceModel")
    public String saveFairValueComplianceModelData(
            @RequestBody FairValueComplianceModelData data) {
        fairValueComplianceModelStore.save(data);
        return "FairValueComplianceModel added successfully\n";
    }
    @DeleteMapping("/deleteFairValueComplianceModel/{id}")
    public String deleteFairValueComplianceModel(@PathVariable String id) {
        fairValueComplianceModelStore.deleteById(id);
        return "FairValueComplianceModel deleted successfully\n";
    }
    @GetMapping("/findFairValueComplianceModel/{id}")
    public Optional<FairValueComplianceModelData> findFairValueComplianceModelData(@PathVariable String id) {
        return fairValueComplianceModelStore.findById(id);
    }
    @GetMapping("/findAllFairValueComplianceModels")
    public List<FairValueComplianceModelData> getFairValueComplianceModels() {
        return fairValueComplianceModelStore.findAll();
    }

    // --- 5.8 IntegratedStressModel ---
    @PostMapping("/addIntegratedStressModel")
    public String saveIntegratedStressModelData(
            @RequestBody IntegratedStressModelData data) {
        integratedStressModelStore.save(data);
        return "IntegratedStressModel added successfully\n";
    }
    @DeleteMapping("/deleteIntegratedStressModel/{id}")
    public String deleteIntegratedStressModel(@PathVariable String id) {
        integratedStressModelStore.deleteById(id);
        return "IntegratedStressModel deleted successfully\n";
    }
    @GetMapping("/findIntegratedStressModel/{id}")
    public Optional<IntegratedStressModelData> findIntegratedStressModelData(@PathVariable String id) {
        return integratedStressModelStore.findById(id);
    }
    @GetMapping("/findAllIntegratedStressModels")
    public List<IntegratedStressModelData> getIntegratedStressModels() {
        return integratedStressModelStore.findAll();
    }

    // --- 5.9 ScheduledCashFlowModel ---
    // Routes deterministic T-Bill / CLM IP-MD cash flows into CASH-PAM as
    // PP signals, enabling CASH-PAM to function as a central cash pool ledger.
    // riskFactorType in scenario descriptor: "ScheduledCashFlowModel"
    @PostMapping("/addScheduledCashFlowModel")
    public String saveScheduledCashFlowModelData(
            @RequestBody ScheduledCashFlowModelData data) {
        scheduledCashFlowModelStore.save(data);
        return "ScheduledCashFlowModel added successfully\n";
    }
    @DeleteMapping("/deleteScheduledCashFlowModel/{id}")
    public String deleteScheduledCashFlowModel(@PathVariable String id) {
        scheduledCashFlowModelStore.deleteById(id);
        return "ScheduledCashFlowModel deleted successfully\n";
    }
    @GetMapping("/findScheduledCashFlowModel/{id}")
    public Optional<ScheduledCashFlowModelData> findScheduledCashFlowModelData(@PathVariable String id) {
        return scheduledCashFlowModelStore.findById(id);
    }
    @GetMapping("/findAllScheduledCashFlowModels")
    public List<ScheduledCashFlowModelData> getScheduledCashFlowModels() {
        return scheduledCashFlowModelStore.findAll();
    }

    // =========================================================================
    // SUPPLY CHAIN TARIFF BEHAVIORAL MODEL CRUD ENDPOINTS (6 models × 4 = 24)
    // riskFactorType values in scenario descriptors:
    //   "TariffSpreadModel", "WorkingCapitalStressModel",
    //   "HedgeEffectivenessModel", "RevenueElasticityModel",
    //   "FXTariffCorrelationModel", "PortCongestionModel"
    //
    // GTAP Armington elasticity-based models for India-US tariff corridor.
    // References:
    //   Hertel (1997), Armington (1969), Corong et al. (2017),
    //   Ahmad, Montgomery & Schreiber (2020), GTAP Database v11.
    // =========================================================================

    // --- 7.1 TariffSpreadModel ---
    @PostMapping("/addTariffSpreadModel")
    public String saveTariffSpreadModelData(
            @RequestBody TariffSpreadModelData data) {
        tariffSpreadModelStore.save(data);
        return "TariffSpreadModel added successfully\n";
    }
    @DeleteMapping("/deleteTariffSpreadModel/{id}")
    public String deleteTariffSpreadModel(@PathVariable String id) {
        tariffSpreadModelStore.deleteById(id);
        return "TariffSpreadModel deleted successfully\n";
    }
    @GetMapping("/findTariffSpreadModel/{id}")
    public Optional<TariffSpreadModelData> findTariffSpreadModelData(@PathVariable String id) {
        return tariffSpreadModelStore.findById(id);
    }
    @GetMapping("/findAllTariffSpreadModels")
    public List<TariffSpreadModelData> getTariffSpreadModels() {
        return tariffSpreadModelStore.findAll();
    }

    // --- 7.2 WorkingCapitalStressModel ---
    @PostMapping("/addWorkingCapitalStressModel")
    public String saveWorkingCapitalStressModelData(
            @RequestBody WorkingCapitalStressModelData data) {
        workingCapitalStressModelStore.save(data);
        return "WorkingCapitalStressModel added successfully\n";
    }
    @DeleteMapping("/deleteWorkingCapitalStressModel/{id}")
    public String deleteWorkingCapitalStressModel(@PathVariable String id) {
        workingCapitalStressModelStore.deleteById(id);
        return "WorkingCapitalStressModel deleted successfully\n";
    }
    @GetMapping("/findWorkingCapitalStressModel/{id}")
    public Optional<WorkingCapitalStressModelData> findWorkingCapitalStressModelData(@PathVariable String id) {
        return workingCapitalStressModelStore.findById(id);
    }
    @GetMapping("/findAllWorkingCapitalStressModels")
    public List<WorkingCapitalStressModelData> getWorkingCapitalStressModels() {
        return workingCapitalStressModelStore.findAll();
    }

    // --- 7.3 HedgeEffectivenessModel ---
    @PostMapping("/addHedgeEffectivenessModel")
    public String saveHedgeEffectivenessModelData(
            @RequestBody HedgeEffectivenessModelData data) {
        hedgeEffectivenessModelStore.save(data);
        return "HedgeEffectivenessModel added successfully\n";
    }
    @DeleteMapping("/deleteHedgeEffectivenessModel/{id}")
    public String deleteHedgeEffectivenessModel(@PathVariable String id) {
        hedgeEffectivenessModelStore.deleteById(id);
        return "HedgeEffectivenessModel deleted successfully\n";
    }
    @GetMapping("/findHedgeEffectivenessModel/{id}")
    public Optional<HedgeEffectivenessModelData> findHedgeEffectivenessModelData(@PathVariable String id) {
        return hedgeEffectivenessModelStore.findById(id);
    }
    @GetMapping("/findAllHedgeEffectivenessModels")
    public List<HedgeEffectivenessModelData> getHedgeEffectivenessModels() {
        return hedgeEffectivenessModelStore.findAll();
    }

    // --- 7.4 RevenueElasticityModel ---
    @PostMapping("/addRevenueElasticityModel")
    public String saveRevenueElasticityModelData(
            @RequestBody RevenueElasticityModelData data) {
        revenueElasticityModelStore.save(data);
        return "RevenueElasticityModel added successfully\n";
    }
    @DeleteMapping("/deleteRevenueElasticityModel/{id}")
    public String deleteRevenueElasticityModel(@PathVariable String id) {
        revenueElasticityModelStore.deleteById(id);
        return "RevenueElasticityModel deleted successfully\n";
    }
    @GetMapping("/findRevenueElasticityModel/{id}")
    public Optional<RevenueElasticityModelData> findRevenueElasticityModelData(@PathVariable String id) {
        return revenueElasticityModelStore.findById(id);
    }
    @GetMapping("/findAllRevenueElasticityModels")
    public List<RevenueElasticityModelData> getRevenueElasticityModels() {
        return revenueElasticityModelStore.findAll();
    }

    // --- 7.5 FXTariffCorrelationModel ---
    @PostMapping("/addFXTariffCorrelationModel")
    public String saveFXTariffCorrelationModelData(
            @RequestBody FXTariffCorrelationModelData data) {
        fxTariffCorrelationModelStore.save(data);
        return "FXTariffCorrelationModel added successfully\n";
    }
    @DeleteMapping("/deleteFXTariffCorrelationModel/{id}")
    public String deleteFXTariffCorrelationModel(@PathVariable String id) {
        fxTariffCorrelationModelStore.deleteById(id);
        return "FXTariffCorrelationModel deleted successfully\n";
    }
    @GetMapping("/findFXTariffCorrelationModel/{id}")
    public Optional<FXTariffCorrelationModelData> findFXTariffCorrelationModelData(@PathVariable String id) {
        return fxTariffCorrelationModelStore.findById(id);
    }
    @GetMapping("/findAllFXTariffCorrelationModels")
    public List<FXTariffCorrelationModelData> getFXTariffCorrelationModels() {
        return fxTariffCorrelationModelStore.findAll();
    }

    // --- 7.6 PortCongestionModel ---
    @PostMapping("/addPortCongestionModel")
    public String savePortCongestionModelData(
            @RequestBody PortCongestionModelData data) {
        portCongestionModelStore.save(data);
        return "PortCongestionModel added successfully\n";
    }
    @DeleteMapping("/deletePortCongestionModel/{id}")
    public String deletePortCongestionModel(@PathVariable String id) {
        portCongestionModelStore.deleteById(id);
        return "PortCongestionModel deleted successfully\n";
    }
    @GetMapping("/findPortCongestionModel/{id}")
    public Optional<PortCongestionModelData> findPortCongestionModelData(@PathVariable String id) {
        return portCongestionModelStore.findById(id);
    }
    @GetMapping("/findAllPortCongestionModels")
    public List<PortCongestionModelData> getPortCongestionModels() {
        return portCongestionModelStore.findAll();
    }

    // =========================================================================
    // DEFI LIQUIDATION BEHAVIORAL MODEL CRUD ENDPOINTS (7 models × 4 = 28)
    // riskFactorType values in scenario descriptors:
    //   "HealthFactorModel", "CollateralVelocityModel",
    //   "CollateralRebalancingModel", "CorrelationRiskModel",
    //   "CascadeProbabilityModel", "GasOptimizationModel",
    //   "InvoiceMaturityModel"
    //
    // CHRONOS-SHIELD DeFi liquidation behavioral risk models.
    // References:
    //   Lehar & Parlour (2022) BIS WP 1062,
    //   Heimbach & Huang (2024) BIS WP 1171,
    //   Sadeghi (2025) gas/liquidation dynamics.
    // =========================================================================

    // --- 8.1 HealthFactorModel ---
    @PostMapping("/addHealthFactorModel")
    public String saveHealthFactorModelData(
            @RequestBody HealthFactorModelData data) {
        healthFactorModelStore.save(data);
        return "HealthFactorModel added successfully\n";
    }
    @DeleteMapping("/deleteHealthFactorModel/{id}")
    public String deleteHealthFactorModel(@PathVariable String id) {
        healthFactorModelStore.deleteById(id);
        return "HealthFactorModel deleted successfully\n";
    }
    @GetMapping("/findHealthFactorModel/{id}")
    public Optional<HealthFactorModelData> findHealthFactorModelData(@PathVariable String id) {
        return healthFactorModelStore.findById(id);
    }
    @GetMapping("/findAllHealthFactorModels")
    public List<HealthFactorModelData> getHealthFactorModels() {
        return healthFactorModelStore.findAll();
    }

    // --- 8.2 CollateralVelocityModel ---
    @PostMapping("/addCollateralVelocityModel")
    public String saveCollateralVelocityModelData(
            @RequestBody CollateralVelocityModelData data) {
        collateralVelocityModelStore.save(data);
        return "CollateralVelocityModel added successfully\n";
    }
    @DeleteMapping("/deleteCollateralVelocityModel/{id}")
    public String deleteCollateralVelocityModel(@PathVariable String id) {
        collateralVelocityModelStore.deleteById(id);
        return "CollateralVelocityModel deleted successfully\n";
    }
    @GetMapping("/findCollateralVelocityModel/{id}")
    public Optional<CollateralVelocityModelData> findCollateralVelocityModelData(@PathVariable String id) {
        return collateralVelocityModelStore.findById(id);
    }
    @GetMapping("/findAllCollateralVelocityModels")
    public List<CollateralVelocityModelData> getCollateralVelocityModels() {
        return collateralVelocityModelStore.findAll();
    }

    // --- 8.3 CollateralRebalancingModel ---
    @PostMapping("/addCollateralRebalancingModel")
    public String saveCollateralRebalancingModelData(
            @RequestBody CollateralRebalancingModelData data) {
        collateralRebalancingModelStore.save(data);
        return "CollateralRebalancingModel added successfully\n";
    }
    @DeleteMapping("/deleteCollateralRebalancingModel/{id}")
    public String deleteCollateralRebalancingModel(@PathVariable String id) {
        collateralRebalancingModelStore.deleteById(id);
        return "CollateralRebalancingModel deleted successfully\n";
    }
    @GetMapping("/findCollateralRebalancingModel/{id}")
    public Optional<CollateralRebalancingModelData> findCollateralRebalancingModelData(@PathVariable String id) {
        return collateralRebalancingModelStore.findById(id);
    }
    @GetMapping("/findAllCollateralRebalancingModels")
    public List<CollateralRebalancingModelData> getCollateralRebalancingModels() {
        return collateralRebalancingModelStore.findAll();
    }

    // --- 8.4 CorrelationRiskModel ---
    @PostMapping("/addCorrelationRiskModel")
    public String saveCorrelationRiskModelData(
            @RequestBody CorrelationRiskModelData data) {
        correlationRiskModelStore.save(data);
        return "CorrelationRiskModel added successfully\n";
    }
    @DeleteMapping("/deleteCorrelationRiskModel/{id}")
    public String deleteCorrelationRiskModel(@PathVariable String id) {
        correlationRiskModelStore.deleteById(id);
        return "CorrelationRiskModel deleted successfully\n";
    }
    @GetMapping("/findCorrelationRiskModel/{id}")
    public Optional<CorrelationRiskModelData> findCorrelationRiskModelData(@PathVariable String id) {
        return correlationRiskModelStore.findById(id);
    }
    @GetMapping("/findAllCorrelationRiskModels")
    public List<CorrelationRiskModelData> getCorrelationRiskModels() {
        return correlationRiskModelStore.findAll();
    }

    // --- 8.5 CascadeProbabilityModel ---
    @PostMapping("/addCascadeProbabilityModel")
    public String saveCascadeProbabilityModelData(
            @RequestBody CascadeProbabilityModelData data) {
        cascadeProbabilityModelStore.save(data);
        return "CascadeProbabilityModel added successfully\n";
    }
    @DeleteMapping("/deleteCascadeProbabilityModel/{id}")
    public String deleteCascadeProbabilityModel(@PathVariable String id) {
        cascadeProbabilityModelStore.deleteById(id);
        return "CascadeProbabilityModel deleted successfully\n";
    }
    @GetMapping("/findCascadeProbabilityModel/{id}")
    public Optional<CascadeProbabilityModelData> findCascadeProbabilityModelData(@PathVariable String id) {
        return cascadeProbabilityModelStore.findById(id);
    }
    @GetMapping("/findAllCascadeProbabilityModels")
    public List<CascadeProbabilityModelData> getCascadeProbabilityModels() {
        return cascadeProbabilityModelStore.findAll();
    }

    // --- 8.6 GasOptimizationModel ---
    @PostMapping("/addGasOptimizationModel")
    public String saveGasOptimizationModelData(
            @RequestBody GasOptimizationModelData data) {
        gasOptimizationModelStore.save(data);
        return "GasOptimizationModel added successfully\n";
    }
    @DeleteMapping("/deleteGasOptimizationModel/{id}")
    public String deleteGasOptimizationModel(@PathVariable String id) {
        gasOptimizationModelStore.deleteById(id);
        return "GasOptimizationModel deleted successfully\n";
    }
    @GetMapping("/findGasOptimizationModel/{id}")
    public Optional<GasOptimizationModelData> findGasOptimizationModelData(@PathVariable String id) {
        return gasOptimizationModelStore.findById(id);
    }
    @GetMapping("/findAllGasOptimizationModels")
    public List<GasOptimizationModelData> getGasOptimizationModels() {
        return gasOptimizationModelStore.findAll();
    }

    // --- 8.7 InvoiceMaturityModel ---
    @PostMapping("/addInvoiceMaturityModel")
    public String saveInvoiceMaturityModelData(
            @RequestBody InvoiceMaturityModelData data) {
        invoiceMaturityModelStore.save(data);
        return "InvoiceMaturityModel added successfully\n";
    }
    @DeleteMapping("/deleteInvoiceMaturityModel/{id}")
    public String deleteInvoiceMaturityModel(@PathVariable String id) {
        invoiceMaturityModelStore.deleteById(id);
        return "InvoiceMaturityModel deleted successfully\n";
    }
    @GetMapping("/findInvoiceMaturityModel/{id}")
    public Optional<InvoiceMaturityModelData> findInvoiceMaturityModelData(@PathVariable String id) {
        return invoiceMaturityModelStore.findById(id);
    }
    @GetMapping("/findAllInvoiceMaturityModels")
    public List<InvoiceMaturityModelData> getInvoiceMaturityModels() {
        return invoiceMaturityModelStore.findAll();
    }

    // ====== DYNAMIC DISCOUNTING ENDPOINTS ======

    // --- 9.1 EarlySettlementModel ---
    @PostMapping("/addEarlySettlementModel")
    public String saveEarlySettlementModelData(@RequestBody EarlySettlementModelData data) {
        earlySettlementModelStore.save(data); return "EarlySettlementModel added successfully\n";
    }
    @DeleteMapping("/deleteEarlySettlementModel/{id}")
    public String deleteEarlySettlementModel(@PathVariable String id) {
        earlySettlementModelStore.deleteById(id); return "EarlySettlementModel deleted successfully\n";
    }
    @GetMapping("/findEarlySettlementModel/{id}")
    public Optional<EarlySettlementModelData> findEarlySettlementModelData(@PathVariable String id) {
        return earlySettlementModelStore.findById(id);
    }
    @GetMapping("/findAllEarlySettlementModels")
    public List<EarlySettlementModelData> getEarlySettlementModels() {
        return earlySettlementModelStore.findAll();
    }

    // --- 9.2 PenaltyAccrualModel ---
    @PostMapping("/addPenaltyAccrualModel")
    public String savePenaltyAccrualModelData(@RequestBody PenaltyAccrualModelData data) {
        penaltyAccrualModelStore.save(data); return "PenaltyAccrualModel added successfully\n";
    }
    @DeleteMapping("/deletePenaltyAccrualModel/{id}")
    public String deletePenaltyAccrualModel(@PathVariable String id) {
        penaltyAccrualModelStore.deleteById(id); return "PenaltyAccrualModel deleted successfully\n";
    }
    @GetMapping("/findPenaltyAccrualModel/{id}")
    public Optional<PenaltyAccrualModelData> findPenaltyAccrualModelData(@PathVariable String id) {
        return penaltyAccrualModelStore.findById(id);
    }
    @GetMapping("/findAllPenaltyAccrualModels")
    public List<PenaltyAccrualModelData> getPenaltyAccrualModels() {
        return penaltyAccrualModelStore.findAll();
    }

    // --- 9.3 OptimalPaymentTimingModel ---
    @PostMapping("/addOptimalPaymentTimingModel")
    public String saveOptimalPaymentTimingModelData(@RequestBody OptimalPaymentTimingModelData data) {
        optimalPaymentTimingModelStore.save(data); return "OptimalPaymentTimingModel added successfully\n";
    }
    @DeleteMapping("/deleteOptimalPaymentTimingModel/{id}")
    public String deleteOptimalPaymentTimingModel(@PathVariable String id) {
        optimalPaymentTimingModelStore.deleteById(id); return "OptimalPaymentTimingModel deleted successfully\n";
    }
    @GetMapping("/findOptimalPaymentTimingModel/{id}")
    public Optional<OptimalPaymentTimingModelData> findOptimalPaymentTimingModelData(@PathVariable String id) {
        return optimalPaymentTimingModelStore.findById(id);
    }
    @GetMapping("/findAllOptimalPaymentTimingModels")
    public List<OptimalPaymentTimingModelData> getOptimalPaymentTimingModels() {
        return optimalPaymentTimingModelStore.findAll();
    }

    // --- 9.4 SupplierUrgencyModel ---
    @PostMapping("/addSupplierUrgencyModel")
    public String saveSupplierUrgencyModelData(@RequestBody SupplierUrgencyModelData data) {
        supplierUrgencyModelStore.save(data); return "SupplierUrgencyModel added successfully\n";
    }
    @DeleteMapping("/deleteSupplierUrgencyModel/{id}")
    public String deleteSupplierUrgencyModel(@PathVariable String id) {
        supplierUrgencyModelStore.deleteById(id); return "SupplierUrgencyModel deleted successfully\n";
    }
    @GetMapping("/findSupplierUrgencyModel/{id}")
    public Optional<SupplierUrgencyModelData> findSupplierUrgencyModelData(@PathVariable String id) {
        return supplierUrgencyModelStore.findById(id);
    }
    @GetMapping("/findAllSupplierUrgencyModels")
    public List<SupplierUrgencyModelData> getSupplierUrgencyModels() {
        return supplierUrgencyModelStore.findAll();
    }

    // --- 9.5 FactoringDecisionModel ---
    @PostMapping("/addFactoringDecisionModel")
    public String saveFactoringDecisionModelData(@RequestBody FactoringDecisionModelData data) {
        factoringDecisionModelStore.save(data); return "FactoringDecisionModel added successfully\n";
    }
    @DeleteMapping("/deleteFactoringDecisionModel/{id}")
    public String deleteFactoringDecisionModel(@PathVariable String id) {
        factoringDecisionModelStore.deleteById(id); return "FactoringDecisionModel deleted successfully\n";
    }
    @GetMapping("/findFactoringDecisionModel/{id}")
    public Optional<FactoringDecisionModelData> findFactoringDecisionModelData(@PathVariable String id) {
        return factoringDecisionModelStore.findById(id);
    }
    @GetMapping("/findAllFactoringDecisionModels")
    public List<FactoringDecisionModelData> getFactoringDecisionModels() {
        return factoringDecisionModelStore.findAll();
    }

    // --- 9.6 CashPoolOptimizationModel ---
    @PostMapping("/addCashPoolOptimizationModel")
    public String saveCashPoolOptimizationModelData(@RequestBody CashPoolOptimizationModelData data) {
        cashPoolOptimizationModelStore.save(data); return "CashPoolOptimizationModel added successfully\n";
    }
    @DeleteMapping("/deleteCashPoolOptimizationModel/{id}")
    public String deleteCashPoolOptimizationModel(@PathVariable String id) {
        cashPoolOptimizationModelStore.deleteById(id); return "CashPoolOptimizationModel deleted successfully\n";
    }
    @GetMapping("/findCashPoolOptimizationModel/{id}")
    public Optional<CashPoolOptimizationModelData> findCashPoolOptimizationModelData(@PathVariable String id) {
        return cashPoolOptimizationModelStore.findById(id);
    }
    @GetMapping("/findAllCashPoolOptimizationModels")
    public List<CashPoolOptimizationModelData> getCashPoolOptimizationModels() {
        return cashPoolOptimizationModelStore.findAll();
    }
    // ====== END DYNAMIC DISCOUNTING ENDPOINTS ======
}
