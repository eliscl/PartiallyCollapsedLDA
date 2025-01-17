package cc.mallet.topics.tui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import cc.mallet.configuration.ConfigFactory;
import cc.mallet.configuration.Configuration;
import cc.mallet.configuration.LDACommandLineParser;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.ModelFactory;
import cc.mallet.configuration.ParsedLDAConfiguration;
import cc.mallet.topics.HDPSamplerWithPhi;
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.topics.LDASamplerInitiable;
import cc.mallet.topics.LDASamplerWithCallback;
import cc.mallet.topics.LDASamplerWithPhi;
import cc.mallet.topics.TopicModelDiagnosticsPlain;
import cc.mallet.types.InstanceList;
import cc.mallet.util.EclipseDetector;
import cc.mallet.util.FileLoggingUtils;
import cc.mallet.util.LDALoggingUtils;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.TeeStream;
import cc.mallet.util.Timer;

public class ParallelLDA {
	public static String PROGRAM_NAME = "ParallelLDA";
	public static Thread.UncaughtExceptionHandler exHandler;
	protected static volatile boolean abort = false;
	protected static volatile boolean normalShutdown = false;
	static LDAGibbsSampler plda;
	public static PrintWriter pw;

	String BASE_DIR = LDAConfiguration.BASE_OUTPUT_DIR_DEFAULT;
	
	public static void main(String[] args) throws Exception {
		ParallelLDA plda = new ParallelLDA();
		plda.doSample(args);
	}

	private static LDAGibbsSampler getCurrentSampler() {
		return plda;
	}

	public void doSample(String[] args) throws Exception {
		if(args.length == 0) {
			System.out.println("\n" + PROGRAM_NAME + ": No args given, you should typically call it along the lines of: \n" 
					+ "java -cp PCPLDA-X.X.X.jar cc.mallet.topics.tui.ParallelLDA --run_cfg=src/main/resources/configuration/PLDAConfig.cfg\n" 
					+ "or\n" 
					+ "java -jar PCPLDA-X.X.X.jar -run_cfg=src/main/resources/configuration/PLDAConfig.cfg\n");
			System.exit(-1);
		}


		String [] argsWithoutRunningInEclipse = EclipseDetector.runningInEclipse(args);
		// If we are not running in eclipse we can install the abort functionality
		if(argsWithoutRunningInEclipse==null) {
			installAbortHandler();
		} else {
			// Else don't install it, but set args to be the one with "-runningInEclipse" removed
			args = argsWithoutRunningInEclipse;
		}

		exHandler = buildExceptionHandler();

		Thread.setDefaultUncaughtExceptionHandler(exHandler);

		System.out.println("We have: " + Runtime.getRuntime().availableProcessors() 
				+ " processors avaiable");
		String buildVer = FileLoggingUtils.getManifestInfo("Implementation-Build","PCPLDA");
		String implVer  = FileLoggingUtils.getManifestInfo("Implementation-Version", "PCPLDA");
		if(buildVer==null||implVer==null) {
			System.out.println("GIT info:" + FileLoggingUtils.getLatestCommit());
		} else {
			System.out.println("Build info:" 
					+ "Implementation-Build = " + buildVer + ", " 
					+ "Implementation-Version = " + implVer);
		}

		LDACommandLineParser cp = new LDACommandLineParser(args);

		// We have to create this temporary config because at this stage if we want to create a new config for each run
		ParsedLDAConfiguration tmpconfig = (ParsedLDAConfiguration) ConfigFactory.getMainConfiguration(cp);			

		int numberOfRuns = 1;
		try {
			numberOfRuns = tmpconfig.getInt("no_runs");
		} catch (NoSuchElementException e) {
		}
		System.out.println("Doing: " + numberOfRuns + " runs");
		// Reading in command line parameters		
		for (int i = 0; i < numberOfRuns; i++) {
			System.out.println("Starting run: " + i);

			LDAConfiguration config = (LDAConfiguration) ConfigFactory.getMainConfiguration(cp);
			LDALoggingUtils lu = new LoggingUtils();

			String [] configs = config.getSubConfigs();
			if(configs!=null && configs.length>0) {
				for(String conf : configs) {
					config.activateSubconfig(conf);
					String logSuitePath = getLogSuitePath(config);
					System.out.println("Logging to: " + logSuitePath);
					lu.checkAndCreateCurrentLogDir(logSuitePath);
					config.setLoggingUtil(lu);		
					lu.checkCreateAndSetSubLogDir(conf);
					doIteration(cp, config, lu, conf);
				}
			} else {
				String logSuitePath = getLogSuitePath(config);
				System.out.println("Logging to: " + logSuitePath);
				lu.checkAndCreateCurrentLogDir(logSuitePath);
				config.setLoggingUtil(lu);		

				System.out.println("No subconfigs defined, using 'default'...");
				String conf = "default";
				lu.checkCreateAndSetSubLogDir(conf);
				doIteration(cp, config, lu, conf);				
			}
			if(buildVer==null||implVer==null) {
				System.out.println("GIT info:" + FileLoggingUtils.getLatestCommit());
			} else {
				System.out.println("Build info:" 
						+ "Implementation-Build = " + buildVer + ", " 
						+ "Implementation-Version = " + implVer);
			}
			normalShutdown = true;
		}
	}

	private String getLogSuitePath(LDAConfiguration config) {
		String baseDir = config.getBaseOutputDirectory(BASE_DIR);
		if(baseDir.equals("")) {
			baseDir = BASE_DIR;
		} else {
			if(!baseDir.endsWith("/")) {
				baseDir += "/";
			}
		}
		
		String expDir = config.getExperimentOutputDirectory("");
		if(!expDir.equals("")) {
			if(!expDir.endsWith("/")) {
				expDir += "/";
			}
		}
		String logSuitePath = baseDir + expDir + "RunSuite" + FileLoggingUtils.getDateStamp();
		return logSuitePath;
	}

	void installAbortHandler() {
		final Thread mainThread = Thread.currentThread();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				int waitTimeout = 300;
				if(!normalShutdown) {
					System.err.println("Running shutdown hook: " + PROGRAM_NAME + " Aborted! Waiting max " 
							+ waitTimeout + "(s) for shudown...");
					abort = true;
					if(getCurrentSampler()!=null) {
						System.err.println("Calling sampler abort...");
						getCurrentSampler().abort();
						try {
							mainThread.join(waitTimeout * 1000);
						} catch (InterruptedException e) {
							System.err.println("Exception during Join..");
							e.printStackTrace();
						}
					}
				} 
			}
		});
	}

	boolean isContinuation(LDACommandLineParser cp) {
		return cp.isOptionSet("continue");
	}

	void doIteration(LDACommandLineParser cp, LDAConfiguration config, LDALoggingUtils lu, String conf)
			throws FileNotFoundException, IOException, Exception {
		int commonSeed = config.getSeed(LDAConfiguration.SEED_DEFAULT);

		File lgDir = lu.getLogDir();
		String logFile = lgDir.getAbsolutePath() + "/" + conf + "_console_output.txt"; 
		PrintStream logOut = new PrintStream(new FileOutputStream(logFile, true));

		PrintStream teeStdOut = new TeeStream(System.out, logOut);
		PrintStream teeStdErr = new TeeStream(System.err, logOut);

		System.setOut(teeStdOut);
		System.setErr(teeStdErr);

		System.out.println("Using Config: " + config.whereAmI());
		System.out.println("Running subconfig: " + conf);
		String dataset_fn = config.getDatasetFilename();
		System.out.println("# topics: " + config.getNoTopics(-1));
		System.out.println("Using dataset: " + dataset_fn);
		if(config.getTestDatasetFilename()!=null) {
			System.out.println("Using TEST dataset: " + config.getTestDatasetFilename());
		}
		String whichModel = config.getScheme();
		if(whichModel==null) {
			whichModel = ModelFactory.DEFAULT_MODEL;
			System.out.println("No model set, using '" + whichModel + "' as default...");
		}
		System.out.println("Scheme: " + whichModel);


		boolean continueSampling = isContinuation(cp);
		LDAGibbsSampler model = ModelFactory.createModel(config, whichModel);
		InstanceList instances = null;
		if(continueSampling) {
			System.out.println("Continuing sampling from previously stored model...");
			initSamplerFromSaved(config, model);
			instances = model.getDataset();
		} else {
			instances = LDAUtils.loadDataset(config, dataset_fn);
			instances.getAlphabet().stopGrowth();			
		}

		if(model==null) {
			System.out.println("No valid model selected ('" + whichModel + "' is not a recognized model), please select a valid model...");
			System.exit(-1);
		}
		System.out.println();
		plda = model;

		IterationListener iterListener = createIterationListener(config);

		if(model instanceof LDASamplerWithCallback && iterListener != null) {
			System.out.println("Setting callback...");
			((LDASamplerWithCallback)model).setIterationCallback(iterListener);
			//vis = new TopicMatrixPanel(900, 400, config.getNoTopics(-1) , 1);
		}

		model.setRandomSeed(commonSeed);
		if(config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT)>0) {
			System.out.println(String.format("Top TF-IDF threshold: %d", config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT)));
		} else {
			System.out.println(String.format("Rare word threshold: %d", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD)));
		}


		System.out.println("Vocabulary size: " + instances.getDataAlphabet().size() + "\n");
		System.out.println("Instance list is: " + instances.size());
		System.out.println("Loading data instances...");

		// Sets the frequent with which top words for each topic are printed
		//model.setShowTopicsInterval(config.getTopicInterval(LDAConfiguration.TOPIC_INTER_DEFAULT));
		System.out.println("Config seed:" + config.getSeed(LDAConfiguration.SEED_DEFAULT));
		System.out.println("Start seed: " + model.getStartSeed());
		// Imports the data into the model

		// If this is a continued sampling, train and test set is already added
		if(!continueSampling) {
			model.addInstances(instances);
			if(config.getTestDatasetFilename()!=null) {
				InstanceList testInstances = LDAUtils.loadDataset(config, config.getTestDatasetFilename(),instances.getAlphabet());
				model.addTestInstances(testInstances);
			}
		}

		System.out.println("Loaded " + model.getDataset().size() + " documents, with " + model.getCorpusSize() + " words in total.");
		System.out.println("Starting iterations (" + config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT) + " total).");
		System.out.println("_____________________________\n");

		// Runs the model
		System.out.println("Starting:" + new Date());
		Timer t = new Timer();
		t.start();
		model.sample(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));
		t.stop();
		System.out.println("Finished:" + new Date());

		int requestedWords = config.getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT);
		//System.out.println("Topic model diagnostics:");
		//System.out.println(tmd.toString());	

		if(config.saveSampler(false)) {
			String samplerFolder = config.getSavedSamplerDirectory(LDAConfiguration.STORED_SAMPLER_DIR_DEFAULT);
			LDAUtils.saveSampler(model, config, samplerFolder);
		}

		if(config.saveDocumentTopicMeans()) {
			String docTopicMeanFn = config.getDocumentTopicMeansOutputFilename();
			double [][] means = model.getZbar();
			PrintWriter dtmOut = config.getLoggingUtil().getLogPrinter(docTopicMeanFn);
			LDAUtils.writeASCIIDoubleMatrix(means, ",", dtmOut);
		}

		if(config.saveDocumentThetaEstimate()) {
			String docTopicThetaFn = config.getDocumentTopicThetaOutputFilename();
			double [][] means = model.getThetaEstimate();
			PrintWriter dtmOut = config.getLoggingUtil().getLogPrinter(docTopicThetaFn);
			LDAUtils.writeASCIIDoubleMatrix(means, ",", dtmOut);
		}

		if(model instanceof LDASamplerWithPhi) {
			LDASamplerWithPhi modelWithPhi = (LDASamplerWithPhi) model;
			if(config.savePhiMeans(LDAConfiguration.SAVE_PHI_MEAN_DEFAULT)) {
				String docTopicMeanFn = config.getPhiMeansOutputFilename();
				double [][] means = modelWithPhi.getPhiMeans();
				if(means!=null) {
					PrintWriter dtmOut = config.getLoggingUtil().getLogPrinter(docTopicMeanFn);
					LDAUtils.writeASCIIDoubleMatrix(means, ",",dtmOut);
				} else {
					System.err.println("WARNING: ParallelLDA: No Phi means where sampled, not saving Phi means! This is likely due to a combination of configuration settings of phi_mean_burnin, phi_mean_thin and save_phi_mean");
				}
				// No big point in saving Phi without the vocabulary
				String vocabFn = config.getVocabularyFilename();
				if(vocabFn==null || vocabFn.length()==0) { vocabFn = "phi_vocabulary.txt"; }
				String [] vobaculary = LDAUtils.extractVocabulaty(instances.getDataAlphabet());
				PrintWriter out = lu.getLogPrinter(vocabFn);
				for(String line : vobaculary) {
					out.println(line);
				}
				out.flush();
			}
		}

		if(config.saveVocabulary(false)) {
			String vocabFn = config.getVocabularyFilename();
			String [] vocabulary = LDAUtils.extractVocabulaty(instances.getDataAlphabet());
			PrintWriter out = lu.getLogPrinter(vocabFn);
			for(String line : vocabulary) {
				out.println(line);
			}
			out.flush();
		}

		if(config.saveCorpus(false)) {
			String corpusFn = config.getCorpusFilename();
			int [][] corpus = LDAUtils.extractCorpus(instances);
			LDAUtils.writeASCIIIntMatrix(corpus,lgDir.getAbsolutePath() + "/" + corpusFn, ",");
		}

		if(config.saveTermFrequencies(false)) {
			String termCntFn = config.getTermFrequencyFilename();
			int [] freqs = LDAUtils.extractTermCounts(instances);
			LDAUtils.writeIntArray(freqs, lgDir.getAbsolutePath() + "/" + termCntFn);
		}

		if(config.saveDocLengths(false)) {
			String docLensFn = config.getDocLengthsFilename();
			int [] freqs = LDAUtils.extractDocLength(instances);
			LDAUtils.writeIntArray(freqs, lgDir.getAbsolutePath() + "/" + docLensFn);

		}

		List<String> metadata = new ArrayList<String>();
		metadata.add("No. Topics: " + model.getNoTopics());
		metadata.add("Start Seed: " + model.getStartSeed());
		// Save stats for this run
		String baseDir = config.getBaseOutputDirectory(BASE_DIR);
		if(baseDir.equals("")) {
			baseDir = BASE_DIR;
		} else {
			if(!baseDir.endsWith("/")) {
				baseDir += "/";
			}
		}
		lu.dynamicLogRun(baseDir, t, cp, (Configuration) config, null, 
				ParallelLDA.class.getName(), "Convergence", "HEADING", "PLDA", 1, metadata);

		if(requestedWords>instances.getDataAlphabet().size()) {
			requestedWords = instances.getDataAlphabet().size();
		}

		PrintWriter out = new PrintWriter(lgDir.getAbsolutePath() + "/TopWords.txt");
		out.println(LDAUtils.formatTopWordsAsCsv(
				LDAUtils.getTopWords(requestedWords, 
						model.getAlphabet().size(), 
						model.getNoTopics(), 
						model.getTypeTopicMatrix(), 
						model.getAlphabet())));
		out.flush();
		out.close();

		out = new PrintWriter(lgDir.getAbsolutePath() + "/RelevanceWords.txt");
		out.println(LDAUtils.formatTopWordsAsCsv(
				LDAUtils.getTopRelevanceWords(requestedWords, 
						model.getAlphabet().size(), 
						model.getNoTopics(), 
						model.getTypeTopicMatrix(),  
						config.getBeta(LDAConfiguration.BETA_DEFAULT),
						config.getLambda(LDAConfiguration.LAMBDA_DEFAULT), 
						model.getAlphabet())));
		out.flush();
		out.close();

		System.out.println("Top words are: \n" + 
				LDAUtils.formatTopWords(LDAUtils.getTopWords(requestedWords, 
						model.getAlphabet().size(), 
						model.getNoTopics(), 
						model.getTypeTopicMatrix(), 
						model.getAlphabet())));
		System.out.println("Relevance words are: \n" + 
				LDAUtils.formatTopWords(LDAUtils.getTopRelevanceWords(requestedWords, 
						model.getAlphabet().size(), 
						model.getNoTopics(), 
						model.getTypeTopicMatrix(),  
						config.getBeta(LDAConfiguration.BETA_DEFAULT),
						config.getLambda(LDAConfiguration.LAMBDA_DEFAULT), 
						model.getAlphabet())));
		//				System.out.println("Salient words are: \n" + 
		//						LDAUtils.formatTopWords(LDAUtils.getTopSalientWords(20, 
		//								model.getAlphabet().size(), 
		//								model.getNoTopics(), 
		//								model.getTypeTopicMatrix(),  
		//								config.getBeta(LDAConfiguration.BETA_DEFAULT),
		//								model.getAlphabet())));
		//				System.out.println("KR1 re-weighted words are: \n" + 
		//						LDAUtils.formatTopWords(LDAUtils.getK1ReWeightedWords(20, 
		//								model.getAlphabet().size(), 
		//								model.getNoTopics(), 
		//								model.getTypeTopicMatrix(),  
		//								config.getBeta(LDAConfiguration.BETA_DEFAULT),
		//								model.getAlphabet())));

		if(model instanceof HDPSamplerWithPhi) {
			printHDPResults(model, lgDir);
		}

		if(config.saveDocumentTopicDiagnostics()) {
			TopicModelDiagnosticsPlain tmd = new TopicModelDiagnosticsPlain(model, requestedWords);
			String docTopicDiagFn = config.getDocumentTopicDiagnosticsOutputFilename();
			out = new PrintWriter(lgDir.getAbsolutePath() + "/" + docTopicDiagFn);
			out.println(tmd.topicsToCsv());
			out.flush();
			out.close();
		}

		System.out.println(new Date() + ": I am done!");
	}

	private void initSamplerFromSaved(LDAConfiguration config, LDAGibbsSampler model) {
		String storedDir = config.getSavedSamplerDirectory(LDAConfiguration.STORED_SAMPLER_DIR_DEFAULT);
		LDASamplerWithPhi newModel = LDAUtils.loadStoredSampler(config, storedDir);
		// Since the user asked us to continue using this sampler, we assume it is "initiable"
		LDASamplerInitiable toInit = (LDASamplerInitiable) model;			
		toInit.initFrom(newModel);
	}

	private UncaughtExceptionHandler buildExceptionHandler() {
		return new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				System.err.println(t + " throws exception: " + e);
				if(e instanceof java.io.EOFException) {
					// Ignore these errors since they seem to be due to a 
					// Java bug which throws an error eventhough the class 
					// loads, as below ...
					//                  Thread[main,5,main] throws exception: java.lang.NoClassDefFoundError: cc/mallet/topics/TopicModelDiagnosticsPlain
					//					java.lang.NoClassDefFoundError: cc/mallet/topics/TopicModelDiagnosticsPlain
					//					at cc.mallet.topics.tui.ParallelLDA.doSample(ParallelLDA.java:225)
					//					at cc.mallet.topics.tui.ParallelLDA.main(ParallelLDA.java:53)
					//				Caused by: java.lang.ClassNotFoundException: cc.mallet.topics.TopicModelDiagnosticsPlain
					//					at java.net.URLClassLoader$1.run(URLClassLoader.java:369)
					//					at java.net.URLClassLoader$1.run(URLClassLoader.java:361)
					//					at java.security.AccessController.doPrivileged(Native Method)
					//					at java.net.URLClassLoader.findClass(URLClassLoader.java:360)
					//					at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
					//					at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:308)
					//					at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
					//					... 2 more
					//				Caused by: java.io.EOFException: Unexpected end of ZLIB input stream
					//					at java.util.zip.ZipFile$ZipFileInflaterInputStream.fill(ZipFile.java:418)
					//					at java.util.zip.InflaterInputStream.read(InflaterInputStream.java:158)
					//					at java.util.jar.Manifest$FastInputStream.fill(Manifest.java:441)
					//					at java.util.jar.Manifest$FastInputStream.readLine(Manifest.java:375)
					//					at java.util.jar.Manifest$FastInputStream.readLine(Manifest.java:409)
					//					at java.util.jar.Manifest.read(Manifest.java:210)
					//					at java.util.jar.Manifest.<init>(Manifest.java:69)
					//					at java.util.jar.JarFile.getManifestFromReference(JarFile.java:191)
					//					at java.util.jar.JarFile.getManifest(JarFile.java:172)
					//					at sun.misc.URLClassPath$JarLoader$2.getManifest(URLClassPath.java:780)
					//					at java.net.URLClassLoader.defineClass(URLClassLoader.java:422)
					//					at java.net.URLClassLoader.access$100(URLClassLoader.java:73)
					//					at java.net.URLClassLoader$1.run(URLClassLoader.java:367)
					System.err.println("Ignoring it...");
				} else {
					e.printStackTrace();
					if(pw != null) {
						try {
							e.printStackTrace(pw);
							pw.close();
						} catch (Exception e1) {
							// Give up!
						}
					}
					System.err.println("Main thread Exiting.");
					System.exit(-1);
				}
			}
		};
	}

	private void printHDPResults(LDAGibbsSampler model, File lgDir) {
		HDPSamplerWithPhi modelWithPhi = (HDPSamplerWithPhi) model;
		System.out.println("Topic Occurence Count:");
		System.out.println(Arrays.toString(modelWithPhi.getTopicOcurrenceCount()));
		LDAUtils.writeIntArray(modelWithPhi.getTopicOcurrenceCount(), lgDir.getAbsolutePath() + "/TopicOccurenceCount.csv");
		System.out.println("Active topics:");
		List<Integer> activeTopicHistoryList = modelWithPhi.getActiveTopicHistory();
		System.out.println(activeTopicHistoryList);
		String historySubset = activeTopicHistoryList.toString().substring(1, activeTopicHistoryList.toString().length()-1);
		model.getConfiguration().getLoggingUtil().getLogPrinter("ActiveTopics.csv").println(historySubset);

		System.out.println("Active topics in data:");
		List<Integer> activeTopicInDataHistory = modelWithPhi.getActiveTopicInDataHistory();
		System.out.println(activeTopicInDataHistory);
		String subsetInData = activeTopicInDataHistory.toString().substring(1, activeTopicInDataHistory.toString().length()-1);
		model.getConfiguration().getLoggingUtil().getLogPrinter("ActiveTopicsInData.csv").println(subsetInData);

		int [] tokenAllocation = modelWithPhi.getTopicTotals();
		double [] tokenAllocationPercent = new double[tokenAllocation.length]; 
		double [] tokenAllocationCDF = new double[tokenAllocation.length];
		double tokenSum = 0;
		for(int idx = 0; idx < tokenAllocation.length; idx++) {
			tokenSum += tokenAllocation[idx];
		}
		List<String> precents = new ArrayList<>();
		List<String> cdfs = new ArrayList<>();
		for(int idx = 0; idx < tokenAllocation.length; idx++) {
			tokenAllocationPercent[idx] = tokenAllocation[idx] / tokenSum;
			if(idx==0) {
				tokenAllocationCDF[idx] = tokenAllocationPercent[idx]; 
			} else {
				tokenAllocationCDF[idx] = tokenAllocationPercent[idx] + tokenAllocationCDF[idx-1];
			}
			short offset = (short) Math.ceil(Math.log10(tokenAllocationPercent[idx]) + 1);
			short significantDigits = 4;

			precents.add(String.format("%,." + (significantDigits - offset) + "f", tokenAllocationPercent[idx]));
			cdfs.add(String.format("%,." + (significantDigits - offset) + "f", tokenAllocationCDF[idx]));
		}
		System.out.println("Topic Token Allocation Count (sum=" + tokenSum + "):");
		System.out.println(Arrays.toString(tokenAllocation));
		System.out.println("Topic Token Allocation Count (%):");
		System.out.println(precents);
		System.out.println("Topic Token Allocation CDF (%):");
		System.out.println(cdfs);
	}

	public static IterationListener createIterationListener(LDAConfiguration config) {
		return ModelFactory.getIterationCallback(config);
	}
}	
