package cc.mallet.configuration;

import org.apache.commons.configuration.ConfigurationException;

import cc.mallet.util.LDALoggingUtils;
import cc.mallet.util.LDANullLogger;

public class ParsedLDAConfiguration extends SubConfig implements Configuration, LDAConfiguration {

	private static final long serialVersionUID = 1L;

	LDALoggingUtils logger;

	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getLoggingUtil()
	 */
	@Override
	public LDALoggingUtils getLoggingUtil() {
		if(logger==null) { logger = new LDANullLogger(); }
		return logger;
	}

	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#setLoggingUtil(utils.LoggingUtils)
	 */
	@Override
	public void setLoggingUtil(LDALoggingUtils logger) {
		this.logger = logger;
	}

	public ParsedLDAConfiguration(LDACommandLineParser cp) throws ConfigurationException {
		super(cp.getConfigFn());
		setDefaultListDelimiter(',');
		commandlineParser = cp;
		whereAmI = cp.getConfigFn();
	}

	public ParsedLDAConfiguration(String path) throws ConfigurationException {
		super(path);
		whereAmI = path;
		setDefaultListDelimiter(',');
	}
		
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getDatasetFilename()
	 */
	@Override
	public String getDatasetFilename() {
		return getStringProperty("dataset");
	}

	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getTestDatasetFilename()
	 */
	@Override
	public String getTestDatasetFilename() {
		return getStringProperty("test_dataset");
	}

	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getScheme()
	 */
	@Override
	public String getScheme() {
		return getStringProperty("scheme");
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getNoTopics(int)
	 */
	@Override
	public Integer getNoTopics(int defaultValue) {
		return getInteger("topics",defaultValue);
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getAlpha(double)
	 */
	@Override
	public Double getAlpha(double defaultValue) {
		return getDouble("alpha",defaultValue);
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getBeta(double)
	 */
	@Override
	public Double getBeta(double defaultValue) {
		return getDouble("beta",defaultValue);
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getNoIterations(int)
	 */
	@Override
	public Integer getNoIterations(int defaultValue) {
		return getInteger("iterations",defaultValue);
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getNoBatches(int)
	 */
	@Override
	public Integer getNoBatches(int defaultValue) {
		return getInteger("batches",defaultValue);
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getRareThreshold(int)
	 */
	@Override
	public Integer getRareThreshold(int defaultValue) {
		return getInteger("rare_threshold",defaultValue);
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getTopicInterval(int)
	 */
	@Override
	public Integer getTopicInterval(int defaultValue) {
		return getInteger("topic_interval",defaultValue);
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getStartDiagnostic(int)
	 */
	@Override
	public Integer getStartDiagnostic(int defaultValue) {
		return getInteger("start_diagnostic",defaultValue);
	}

	/**
	 * @param seedDefault
	 * @return the seed set in the config file or the LSB of the current time if set to -1
	 */
	@Override
	public int getSeed(int seedDefault) {
		int seed = getInteger("seed",seedDefault);
		if(seed==0) {seed=(int)System.currentTimeMillis();};
		return seed;
	}

	@Override
	public boolean getDebug() {
		String key = "debug";
		return getBooleanProperty(key);
	}

	@Override
	public boolean getPrintPhi() {
		String key = "print_phi";
		return getBooleanProperty(key);
	}

	@Override
	public boolean getMeasureTiming() {
		String key = "measure_timing";
		return getBooleanProperty(key);
	}

	@Override
	public void setNoTopics(int newValue) {
		throw new UnsupportedOperationException("Cannot set 'no topics' on ParsedLDAConfiguration. Create a SimpleLDAConfiguration from it and modity that if needed");
	}

	@Override
	public int getResultSize(int resultsSizeDefault) {
		return getInteger("results_size",resultsSizeDefault);
	}

	@Override
	public String getDocumentBatchBuildingScheme(String batchBuildSchemeDefault) {
		String configProperty = getStringProperty("batch_building_scheme");
		return (configProperty == null) ? batchBuildSchemeDefault : configProperty;
	}

	@Override
	public String getTopicBatchBuildingScheme(String batchBuildSchemeDefault) {
		String configProperty = getStringProperty("topic_batch_building_scheme");
		return (configProperty == null) ? batchBuildSchemeDefault : configProperty;
	}

	@Override
	public double getDocPercentageSplitSize() {
		return getDouble("percentage_split_size_doc",1.0);
	}
	
	@Override
	public double getTopicPercentageSplitSize() {
		return getDouble("percentage_split_size_topic",1.0);
	}

	@Override
	public Integer getNoTopicBatches(int defaultValue) {
		return getInteger("topic_batches",defaultValue);
	}

	@Override
	public String getTopicIndexBuildingScheme(String topicIndexBuildSchemeDefault) {
		String configProperty = getStringProperty("topic_index_building_scheme");
		return (configProperty == null) ? topicIndexBuildSchemeDefault : configProperty;
	}

	@Override
	public int getInstabilityPeriod(int defaultValue) {
		return getInteger("instability_period",defaultValue);
	}

	@Override
	public double[] getFixedSplitSizeDoc() {
		return getDoubleArrayProperty("fixed_split_size_doc");
	}

	@Override
	public int getFullPhiPeriod(int defaultValue) {
		return getInteger("full_phi_period",defaultValue);	}

	@Override
	public String[] getSubTopicIndexBuilders(int i) {
		return getStringArrayProperty("sub_topic_index_builders");
	}

	@Override
	public double topTokensToSample(double defaultValue) {
		return getDouble("percent_top_tokens",defaultValue);
	}

	@Override
	public int[] getPrintNDocsInterval() {
		int [] defaultVal = {-1};
		return getIntArrayProperty("print_ndocs_interval", defaultVal);
	}

	@Override
	public int getPrintNDocs() {
		return getInteger("print_ndocs_cnt",0);
	}

	@Override
	public int[] getPrintNTopWordsInterval() {
		int [] defaultVal = {-1};
		return getIntArrayProperty("print_ntopwords_interval", defaultVal);
	}

	@Override
	public int getPrintNTopWords() {
		return getInteger("print_ntopwords_cnt",0);
	}

	@Override
	public int getProportionalTopicIndexBuilderSkipStep() {
		return getInteger("proportional_ib_skip_step",1);
	}

	@Override
	public boolean logTypeTopicDensity(boolean logTypeTopicDensityDefault) {
		String key = "log_type_topic_density";
		return getBooleanProperty(key);
	}

	@Override
	public boolean logDocumentDensity(boolean logDocumentDensityDefault) {
		String key = "log_document_density";
		return getBooleanProperty(key);
	}

	@Override
	public String getExperimentOutputDirectory(String defaultDir) {
		String dir = getStringProperty("experiment_out_dir");
		if(dir != null && dir.endsWith("/")) dir = dir.substring(0,dir.length()-1);
		return (dir == null) ? defaultDir : dir;
	}

	@Override
	public double getVariableSelectionPrior(double vsPriorDefault) {
		return getDouble("variable_selection_prior",vsPriorDefault);
	}

	@Override
	public boolean logPhiDensity(String logPhiDensityDefault) {
		String key = "log_phi_density";
		return getBooleanProperty(key);
	}

	@Override
	public String getTopicPriorFilename() {
		return getStringProperty("topic_prior_filename");
	}
	
	@Override
	public String getDocumentPriorFilename() {
		return getStringProperty("document_prior_filename");
	}
	
	@Override
	public String getStoplistFilename(String defaultStoplist) {
		String stoplistFn = getStringProperty("stoplist");
		if(stoplistFn==null || stoplistFn.length()==0) {
			stoplistFn = defaultStoplist;
		}
		return stoplistFn;
	}

	@Override
	public boolean keepNumbers() {
		String key = "keep_numbers";
		return getBooleanProperty(key);
	}

	@Override
	public boolean saveDocumentTopicMeans() {
		String key = "save_doc_topic_means";
		return getBooleanProperty(key);
	}

	@Override
	public String getDocumentTopicMeansOutputFilename() {
		return getStringProperty("doc_topic_mean_filename");
	}
	
	@Override
	public boolean saveDocumentTopicDiagnostics() {
		String key = "save_doc_topic_diagnostics";
		return getBooleanProperty(key);
	}

	@Override
	public String getDocumentTopicDiagnosticsOutputFilename() {
		return getStringProperty("doc_topic_diagnostics_filename");
	}


	@Override
	public String getPhiMeansOutputFilename() {
		return getStringProperty("phi_mean_filename");
	}

	@Override
	public boolean savePhiMeans(boolean defaultVal) {
		String key = "save_phi_mean";
		Object prop = super.getProperty(translateKey(key));
		// Hack because of inconsistent naming
		if(prop==null) {
			key = "save_phi_means";
			prop = super.getProperty(translateKey(key));
		}
		if(prop==null) return defaultVal;
		return getBooleanProperty(key);
	}

	@Override
	public int getPhiBurnInPercent(int phiBurnInDefault) {
		return getInteger("phi_mean_burnin", phiBurnInDefault);
	}

	@Override
	public int getPhiMeanThin(int phiMeanThinDefault) {
		return getInteger("phi_mean_thin", phiMeanThinDefault);
	}
	
	@Override
	public Integer getTfIdfVocabSize(int defaultValue) {
		return getInteger("tfidf_vocab_size",defaultValue);
	}

	@Override
	public int getNrTopWords(int defaltNr) {
		return getInteger("nr_top_words",defaltNr);
	}

	@Override
	public int getMaxDocumentBufferSize(int defaltSize) {
		return getInteger("max_doc_buf_size",defaltSize);
	}

	@Override
	public boolean getKeepConnectingPunctuation(boolean defaultKeepConnectingPunctuation) {
		String key = "keep_connecting_punctuation";
		Object prop = super.getProperty(translateKey(key));
		if(prop==null) return defaultKeepConnectingPunctuation;
		return getBooleanProperty(key);
	}
	
	@Override
	public boolean saveVocabulary(boolean defaultValue) {
		String key = "save_vocabulary";
		Object prop = super.getProperty(translateKey(key));
		if(prop==null) return defaultValue;
		return getBooleanProperty(key);
	}
	
	@Override
	public String getVocabularyFilename() {
		return getStringProperty("vocabulary_filename");
	}

	@Override
	public boolean saveTermFrequencies(boolean defaultValue) {
		String key = "save_term_frequencies";
		Object prop = super.getProperty(translateKey(key));
		if(prop==null) return defaultValue;
		return getBooleanProperty(key);
	}

	@Override
	public String getTermFrequencyFilename() {
		return getStringProperty("term_frequencies_filename");
	}

	@Override
	public boolean saveDocLengths(boolean defaultValue) {
		String key = "save_doc_lengths";
		Object prop = super.getProperty(translateKey(key));
		if(prop==null) return defaultValue;
		return getBooleanProperty(key);
	}

	@Override
	public String getDocLengthsFilename() {
		return getStringProperty("doc_lengths_filename");
	}

	@Override
	public double getLambda(double defaultValue) {
		return getDouble("lambda",defaultValue);
	}
	
	@Override
	public String getDocumentTopicThetaOutputFilename() {
		return getStringProperty("doc_topic_theta_filename");
	}

	@Override
	public boolean saveDocumentThetaEstimate() {
		String key = "save_doc_theta_estimate";
		return getBooleanProperty(key);
	}

	@Override
	public String getDirichletSamplerBuilderClass(String samplerBuilderClassName) {
		String samplerName = getStringProperty("sparse_dirichlet_sampler_builder_name");
		if(samplerName==null || samplerName.length()==0) {
			samplerName = samplerBuilderClassName;
		}
		return samplerName;
	}

	@Override
	public int getAliasPoissonThreshold(int aliasPoissonDefaultThreshold) {
		return getInteger("alias_poisson_threshold",aliasPoissonDefaultThreshold);
	}

	@Override
	public String getFileRegex(String string) {
		String ext = getStringProperty("file_regex");
		return (ext == null || ext.length() == 0) ? string : ext;
	}

	@Override
	public Integer getHyperparamOptimInterval(int defaultValue) {
		return getInteger("hyperparam_optim_interval",defaultValue);
	}

	@Override
	public boolean useSymmetricAlpha(boolean defaultAlpha) {
		String key = "symmetric_alpha";
		Boolean symAlpha = getBooleanPropertyOrNull(key);
		return symAlpha == null ? defaultAlpha : symAlpha;
	}
	
	@Override
	public double getHDPGamma(double gammaDefault) {
		return getDouble("hdp_gamma",gammaDefault);
	}
	
	@Override
	public int getHDPNrStartTopics(int defaultValue) {
		return getInteger("hdp_nr_start_topics",defaultValue);
	}

	@Override
	public boolean logTokensPerTopic(boolean logTokensPerTopic) {
		String key = "log_tokens_per_topic";
		return getBooleanProperty(key);
	}

	@Override
	public int getDocumentSamplerSplitLimit(int documentSamplerSplitLimitDefault) {
		return getInteger("document_sampler_split_limit",documentSamplerSplitLimitDefault);
	}

	@Override
	public double getHDPKPercentile(double hdpKPercentile) {
		return getDouble("hdp_k_percentile",hdpKPercentile);
	}
	
	@Override
	public boolean saveCorpus(boolean defaultValue) {
		String key = "save_corpus";
		Object prop = super.getProperty(translateKey(key));
		if(prop==null) return defaultValue;
		return getBooleanProperty(key);
	}
	
	@Override
	public String getCorpusFilename() {
		return getStringProperty("corpus_filename");
	}

	@Override
	public boolean logTopicIndicators(boolean logTypeTopicDensityDefault) {
		String key = "log_topic_indicators";
		return getBooleanProperty(key);
	}
	
	public SimpleLDAConfiguration simpleLDAConfiguration() {
		
		double [] fixedSplitSizeDoc = new double [0];
		try {
			fixedSplitSizeDoc = getFixedSplitSizeDoc();
		} catch (Exception e) {
		} 
		
		SimpleLDAConfiguration conf = new SimpleLDAConfiguration(
				getLoggingUtil(), 
				getScheme(), 
				getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT), 
				getAlpha(LDAConfiguration.ALPHA_DEFAULT), 
				getBeta(LDAConfiguration.BETA_DEFAULT),
				getLambda(LDAConfiguration.LAMBDA_DEFAULT), 
				getNoIterations(LDAConfiguration.NO_ITER_DEFAULT), 
				getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT), 
				getNoTopicBatches(LDAConfiguration.NO_TOPIC_BATCHES_DEFAULT), 
				getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD),
				getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT), 
				getTopicInterval(LDAConfiguration.TOPIC_INTER_DEFAULT), 
				getStartDiagnostic(LDAConfiguration.START_DIAG_DEFAULT), 
				getSeed(LDAConfiguration.SEED_DEFAULT), 
				getDebug(),
				getDatasetFilename(), 
				getTestDatasetFilename(), 
				getDocumentBatchBuildingScheme(LDAConfiguration.BATCH_BUILD_SCHEME_DEFAULT), 
				getDocPercentageSplitSize(),
				getTopicPercentageSplitSize(), 
				getResultSize(LDAConfiguration.RESULTS_SIZE_DEFAULT), 
				getFullPhiPeriod(5), 
				topTokensToSample(100),
				getTopicPriorFilename(),
				getDocumentPriorFilename(),
				getTopicIndexBuildingScheme(LDAConfiguration.TOPIC_INDEX_BUILD_SCHEME_DEFAULT), 
				getTopicBatchBuildingScheme(LDAConfiguration.TOPIC_BATCH_BUILD_SCHEME_DEFAULT),
				getInstabilityPeriod(100), 
				fixedSplitSizeDoc,
				getProportionalTopicIndexBuilderSkipStep(), 
				savePhiMeans(LDAConfiguration.SAVE_PHI_MEAN_DEFAULT), 
				getPhiBurnInPercent(LDAConfiguration.PHI_BURN_IN_DEFAULT),
				getDocLengthsFilename(), 
				saveDocLengths(false), 
				getTermFrequencyFilename(),
				saveTermFrequencies(false), 
				getVocabularyFilename(), 
				saveVocabulary(false), 
				getCorpusFilename(),
				saveCorpus(false), 
				getPrintPhi(), 
				getMeasureTiming(), 
				logTokensPerTopic(false),
				logTypeTopicDensity(false), 
				logDocumentDensity(false), 
				getExperimentOutputDirectory("tmp"),
				logPhiDensity("false"), 
				keepNumbers(), 
				saveDocumentTopicMeans(), 
				saveDocumentThetaEstimate(), 
				getDocumentTopicMeansOutputFilename(),
				getDocumentTopicThetaOutputFilename(),
				saveDocumentTopicDiagnostics(), 
				getDocumentTopicDiagnosticsOutputFilename(),
				getPhiMeansOutputFilename(), 
				getKeepConnectingPunctuation(LDAConfiguration.KEEP_CONNECTING_PUNCTUATION), 
				getStoplistFilename("stoplist.txt"), 
				getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT),
				getMaxDocumentBufferSize(LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT), 
				getPhiMeanThin(LDAConfiguration.PHI_THIN_DEFAULT), 
				getDirichletSamplerBuilderClass(LDAConfiguration.SPARSE_DIRICHLET_SAMPLER_BULDER_DEFAULT),
				getAliasPoissonThreshold(LDAConfiguration.ALIAS_POISSON_DEFAULT_THRESHOLD), 
				getFileRegex(LDAConfiguration.FILE_REGEX_DEFAULT), 
				getHyperparamOptimInterval(LDAConfiguration.HYPERPARAM_OPTIM_INTERVAL_DEFAULT), 
				useSymmetricAlpha(LDAConfiguration.SYMMETRIC_ALPHA_DEFAULT),
				getHDPGamma(LDAConfiguration.HDP_GAMMA_DEFAULT), 
				getHDPNrStartTopics(LDAConfiguration.HDP_START_TOPICS_DEFAULT), 
				getDocumentSamplerSplitLimit(LDAConfiguration.DOCUMENT_SAMPLER_SPLIT_LIMIT_DEFAULT), 
				getHDPKPercentile(LDAConfiguration.HDP_K_PERCENTILE),
				logTopicIndicators(false),
				getSamplerClass(LDAConfiguration.MODEL_DEFAULT),
				getOriginalDatasetFilename(),
				noPreprocess(),
				saveSampler(false),
				getSavedSamplerDirectory(LDAConfiguration.STORED_SAMPLER_DIR_DEFAULT),
				getIterationCallbackClass(LDAConfiguration.MODEL_CALLBACK_DEFAULT)
				);
		
		return conf;
	}
	
	@Override
	public String getSamplerClass(String modelDefault) {
		String configProperty = getStringProperty("sampler_class");
		return (configProperty == null) ? modelDefault : configProperty;
	}

	@Override
	public String getIterationCallbackClass(String modelDefault) {
		String configProperty = getStringProperty("iteration_callback_class");
		return (configProperty == null) ? modelDefault : configProperty;
	}

	@Override
	public String getOriginalDatasetFilename() {
		return getStringProperty("original_dataset");
	}

	@Override
	public boolean noPreprocess() {
		String key = "no_preprocess";
		return getBooleanProperty(key);
	}

	@Override
	public boolean saveSampler(boolean b) {
		String key = "save_sampler";
		return getBooleanProperty(key);
	}

	@Override
	public String getSavedSamplerDirectory(String default_dir) {
		String configProperty = getStringProperty("saved_sampler_dir");
		return (configProperty == null) ? default_dir : configProperty;
	}

}
