package com.spokentech.speechdown.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.UnsupportedAudioFileException;
import junit.framework.TestCase;

import edu.cmu.sphinx.decoder.Decoder;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.decoder.pruner.SimplePruner;
import edu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer;
import edu.cmu.sphinx.decoder.search.ActiveListFactory;
import edu.cmu.sphinx.decoder.search.PartitionActiveListFactory;
import edu.cmu.sphinx.decoder.search.SearchManager;
import edu.cmu.sphinx.decoder.search.SimpleActiveListManager;
import edu.cmu.sphinx.decoder.search.WordActiveListFactory;
import edu.cmu.sphinx.decoder.search.WordPruningBreadthFirstSearchManager;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.NonSpeechDataFilter;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifier;
import edu.cmu.sphinx.frontend.endpoint.SpeechMarker;
import edu.cmu.sphinx.frontend.feature.BatchCMN;
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor;
import edu.cmu.sphinx.frontend.feature.FeatureTransform;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;

import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.instrumentation.MemoryTracker;
import edu.cmu.sphinx.instrumentation.Monitor;
import edu.cmu.sphinx.instrumentation.SpeedTracker;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;

import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.FastDictionary;

import edu.cmu.sphinx.linguist.language.ngram.large.LargeTrigramModel;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.LogMath;


public class TranscriberConstructorConfigTest  extends TestCase    {

	public void startup() {}
	protected void setUp() {
		//gson = new Gson();
	}
	
	 
	private static double wordInsertionProbability = .1;
	private static double silenceInsertionProbability = 1;
    private static double outOfGrammarBranchProbability = 1e-20 ;//1e-50 ;//1e-20 ;
    private static double phoneInsertionProbability =  1e-10 ; //1e-20
    private static float languageWeight = 8.0f;

    private static int absoluteBeamWidth = -1;
	private static double relativeBeamWidth = 1e-90;
	private static double relativeWordBeamWidth = 1e-30;
	private static URL wordDictUrl;
	private static URL fillerDictUrl;
	private static UnitManager unitManager = new UnitManager();
	private static Dictionary dictionary;


	private static URL modelLocation;
	private static Sphinx3Loader modelLoader;
	private static LogMath logMath = new LogMath(1.0001f, true);
	private static TiedStateAcousticModel model;
	private static Linguist linguist;
	private static SearchManager searchManager;
	private static SimplePruner pruner = new SimplePruner();
	private static ThreadedAcousticScorer scorer;
	private static FrontEnd frontEnd;
	private static List<DataProcessor> pipeline = new ArrayList<DataProcessor>();
	private static Decoder decoder;
	private static Recognizer recognizer;
	private static List<Monitor> monitors = new ArrayList<Monitor>();
//	private AudioFileDataSource audioFileDataSource = new AudioFileDataSource(3200, null);
	
	

		// language models
		//private static String lm = "file:///C:/work/models/lm/lm_giga_64k_vp_3gram.arpa.DMP";
		//private static String lm= "file:///C:/work/models/lm/hub4_language_model.arpaformat.DMP";
		private static String lm= "file:///C:/work/models/lm/wsj5kc.Z.DMP";
			
			
		//Acoustic Models
		//String amLoc ="resource:/WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz";
		//String amaDloc="";
		//String amDef="mdef";


		//String amLoc ="file:///C:/work/models/am/mcmodel.cd_cont_4000";
		//String amaDloc="/";
		//String amDef="mdef";
	

		private static String amLoc ="file:///C:/work/models/am/voxforge-en-0.4/model_parameters/voxforge_en_sphinx.cd_cont_5000";
		private static String amDloc="/";
		private static String amDef="mdef";

		//Dictionaries
		private static String dict ="file:///c:/work/models/dict/cmudict.0.7a";
		private static String fillerDict ="file:///c:/work/models/dict/mcmodel.filler";
		private static AudioFileDataSource audioFileDataSource;

		
		public void testTranscription() throws IOException, UnsupportedAudioFileException {
		        URL audioURL;
		        
		        configure();
    
		       // audioURL = new URL("file:///c:/work/speechcloud/etc/test/transcriber/gtd.wav");
		        audioURL = new URL("file:///c:/work/speechcloud/etc/test/transcriber/gulf.wav");

		        // configure the audio input for the recognizer
		        audioFileDataSource.setAudioFile(audioURL, null);
		      
		        recognizer.allocate();


		        // Loop until last utterance in the audio file has been decoded, in which case the recognizer will return null.
		        Result result;
			    System.out.println("Entering loop");
		        while ((result = recognizer.recognize())!= null) {
		        	    System.out.println("In loop");
		                String resultText = result.getBestResultNoFiller();
		                System.out.println(resultText);
		        }
		    }
		
	
	public static void configure() throws MalformedURLException {

        // configure the audio input for the recognizer

		try {
			modelLoader = new Sphinx3Loader(amLoc, amDef, amDloc, logMath, unitManager, 0, 1e-7f, 0.0001f, true);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		// Dictionary
		wordDictUrl = new URL(dict);
		fillerDictUrl = new URL(fillerDict);
		dictionary = new FastDictionary(wordDictUrl, fillerDictUrl, new ArrayList<URL>(), false, "<sil>", false, false, unitManager);
		
	    model = new TiedStateAcousticModel(modelLoader, unitManager, true);
		LargeTrigramModel ltm = new LargeTrigramModel("DMP", 
				        new URL(lm), 
						null, 100000, 
						50000, false, 
						3, logMath, dictionary,
						false, languageWeight, 
						wordInsertionProbability, 0.5f, false);

	        
            linguist = new LexTreeLinguist(model, logMath, unitManager,
                    ltm, dictionary, 
                    false, true, 
                    wordInsertionProbability, silenceInsertionProbability,
                    0.02, 1.0, 
                    languageWeight, false, true,  
                    1.0f, 0);

            audioFileDataSource = new AudioFileDataSource(3200,null);
    		pipeline.add(audioFileDataSource);
    		//pipeline.add(new DataDumper());
    		pipeline.add(new DataBlocker(10));
    		pipeline.add(new SpeechClassifier(10, 0.003f, 10, 0));
    		pipeline.add(new SpeechMarker(200, 500, 100, 50, 100, 13.0));
    		pipeline.add(new NonSpeechDataFilter());
    		pipeline.add(new Preemphasizer(0.97));
    		pipeline.add(new RaisedCosineWindower(0.46, 25.625f, 10.0f));
    		pipeline.add(new DiscreteFourierTransform(-1, false));
				//pipeline.add(new MelFrequencyFilterBank(200.0, 3500.0, 31));  //8khz
				//pipeline.add(new DiscreteCosineTransform(31, 13)); //8khz

			pipeline.add(new MelFrequencyFilterBank(130.0, 6800.0, 40));
			pipeline.add(new DiscreteCosineTransform(40, 13));
    		pipeline.add(new BatchCMN());
    		pipeline.add(new DeltasFeatureExtractor(3));

			pipeline.add(new FeatureTransform(modelLoader));
   		
    		frontEnd = new FrontEnd(pipeline);
    		scorer = new ThreadedAcousticScorer(frontEnd, null, 10, true, 0, Thread.NORM_PRIORITY);
    		
    		ArrayList<ActiveListFactory> list = new ArrayList<ActiveListFactory>();
    		list.add(new PartitionActiveListFactory(absoluteBeamWidth, relativeBeamWidth, logMath));
    		// was 21, 1e-25
    		list.add(new WordActiveListFactory(21, 1e-25, logMath, 0, 1));
    		list.add(new WordActiveListFactory(21, 1e-25, logMath, 0, 1));
    		// abs was 25000
    		list.add(new PartitionActiveListFactory(25000, relativeBeamWidth, logMath));
    		list.add(new PartitionActiveListFactory(25000, relativeBeamWidth, logMath));
    		list.add(new PartitionActiveListFactory(25000, relativeBeamWidth, logMath));

    		SimpleActiveListManager activeListManager = new SimpleActiveListManager(list, false);

    		searchManager = new WordPruningBreadthFirstSearchManager(logMath, linguist, pruner,
    				scorer, activeListManager,
    				false, relativeBeamWidth,
    				0,
    				false, true,
    				100, 1.7f,
    				true);

    		decoder = new Decoder(searchManager, false, false, new ArrayList<ResultListener>(), 100000);
    		recognizer = new Recognizer(decoder, monitors);
    		monitors.add(new MemoryTracker(recognizer, true, false));
    		monitors.add(new SpeedTracker(recognizer, frontEnd, true, false, false, true));

	}

}