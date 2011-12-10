

package com.spokentech.speechdown.server;

import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.NonSpeechDataFilter;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifier;
import edu.cmu.sphinx.frontend.endpoint.SpeechMarker;
import edu.cmu.sphinx.frontend.feature.BatchCMN;
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor;
import edu.cmu.sphinx.frontend.feature.FeatureTransform;
import edu.cmu.sphinx.frontend.feature.LiveCMN;
import edu.cmu.sphinx.frontend.filter.Dither;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.FastDictionary;
import edu.cmu.sphinx.linguist.language.ngram.large.LargeTrigramModel;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.LogMath;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.spokentech.speechdown.server.util.pool.ModelPools;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import junit.framework.TestCase;


/** A simple example that shows how to transcribe a continuous audio file that has multiple utterances in it. */
public class TranscriberSpringConfigTest  extends TestCase implements BeanFactoryAware  {

	public void startup() {}
	protected void setUp() {
		//gson = new Gson();
	}



	
	//front end elements
	private static DataBlocker dataBlocker ;
	private static SpeechClassifier speechClassifier;
	private static SpeechMarker speechMarker;
	private static NonSpeechDataFilter nonSpeechDataFilter;
	//InsertSpeechSignalStage insertSpeechSignalStage;
	private static Preemphasizer preemphasizer;

	private static RaisedCosineWindower raisedCosineWindower ;
	private static DiscreteFourierTransform discreteFourierTransform ;
	private static MelFrequencyFilterBank melFrequencyFilterBank16k ;
	private static MelFrequencyFilterBank melFrequencyFilterBank8k ;
	private static DiscreteCosineTransform discreteCosineTransform8k ;
	private static DiscreteCosineTransform discreteCosineTransform16k ;
	private static BatchCMN batchCmn ;

	private static FeatureTransform lda;
	private static DeltasFeatureExtractor deltasFeatureExtractor;
	//private WavWriter recorder; 
	
	private static URL modelLocation;
	private static Sphinx3Loader modelLoader;
	private static LogMath logMath = new LogMath(1.0001f, true);
	private static TiedStateAcousticModel model;
	private static Linguist linguist;
	
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
	
	private static double wordInsertionProbability = .1;
	private static double silenceInsertionProbability = 1;

    private static float languageWeight = 8.0f;

	private static URL wordDictUrl;
	private static URL fillerDictUrl;
	private static UnitManager unitManager = new UnitManager();
	private static Dictionary dictionary;

	public void testGtd() throws IOException, UnsupportedAudioFileException {
        URL audioURL;
        

		//URL configURL = Transcriber.class.getResource("config.xml");
		//URL configURL = new URL("file:///c:/work/speechcloud/etc/test/transcriber/config.xml");
        //URL configURL = SpringTranscriber.class.getResource("speechserver-test.xml");
	    //context = new ClassPathXmlApplicationContext("file:///c:/work/speechcloud/etc/test/oog/speechserver-test.xml");
        
        Properties props = loadProperties("speech.properties");
        Enumeration ee = props.propertyNames();

        while (ee.hasMoreElements()) {
          String key = (String) ee.nextElement();
          System.out.println(key + " -- " + props.getProperty(key));
        }
      
	    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("speechserver.xml");

        Recognizer recognizer =  (Recognizer) context.getBean("recognizerLm");
            
		//create frontend elements (to be assembled into frontends at run time)
    	createFrontEndElements();
        //audioURL = new URL("file:///c:/work/speechcloud/etc/test/transcriber/gtd.wav");
        //audioURL = new URL("file:///c:/work/speechcloud/etc/test/transcriber/french2.wav");
        audioURL = new URL("file:///c:/work/speechcloud/etc/test/transcriber/gulf.wav");

    
        // configure the audio input for the recognizer
        AudioFileDataSource dataSource = (AudioFileDataSource) context.getBean("audioFileDataSource");
        dataSource.setAudioFile(audioURL, null);
        
        

        /*
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

        
        Linguist l = recognizer.getDecoder().getSearchManager().getLinguist() ;
        LexTreeLinguist ll = (LexTreeLinguist) l;

        ll.setDictionary(dictionary);
        l.setAcousticModel(model);
        ll.setLanguageModel(ltm);
*/
        
        //recognizer.getDecoder().getSearchManager().getLinguist().deallocate();
        //recognizer.getDecoder().getSearchManager().getLinguist().allocate();
        
        
        //recognizer.getDecoder().getSearchManager().setLinguist(linguist);
        
        ModelPools modelPool = (ModelPools) context.getBean("modelPool");
		AcousticModel am = modelPool.getAcoustic().get("english16").getModel();
		LargeTrigramModel trigramModel = modelPool.getLanguage().get("en");
		//AcousticModel am = modelPool.getAcoustic().get("mandarin16").getModel();
		//LargeTrigramModel trigramModel = modelPool.getLanguage().get("mandarin");
		
		Dictionary d = trigramModel.getDictionary();
		
        //liguist pool appraoch
        //LexTreeLinguist ling =  (LexTreeLinguist) context.getBean("frenchLexTreeLinguist");
        //LexTreeLinguist ling =  (LexTreeLinguist) context.getBean("englishLexTreeLinguist");
        //recognizer.getDecoder().getSearchManager().setLinguist(ling);
        //---------------------------------------------------------------------
        
        LexTreeLinguist ling = (LexTreeLinguist)recognizer.getDecoder().getSearchManager().getLinguist();		
//		ling.setDictionary(d);
//        ling.setLanguageModel(trigramModel);
		ling.setAcousticModel(am);
        
        
        modelLoader = (Sphinx3Loader) recognizer.getDecoder().getSearchManager().getLinguist().getAcousticModel().getLoader();        
        //FrontEnd fe = (FrontEnd) as.getFrontEnd();
        FrontEnd fe = createAudioFrontend((DataProcessor)dataSource,modelLoader,16000);
		//fe.setDataSource((DataProcessor) dataSource);
 	    // set the front end in the scorer in realtime
		recognizer.getDecoder().getSearchManager().getScorer().setFrontEnd(fe);

        //FeatureTransform ft = new FeatureTransform(loader);
        //AcousticScorer as = recognizer.getDecoder().getSearchManager().getScorer(); 
		//List<DataProcessor> l = fe.getElements();		
		//if (loader.getTransformMatrix() != null) {
		//		System.out.println("------> adding feat tran");
	   	//		l.add(ft);
	   	//	    recognizer.getDecoder().getSearchManager().getScorer().setFrontEnd(fe);
		//}
		//int i=0;
		//for (DataProcessor b: l) {
		//	DataProcessor x = l.get(i++);
		//	System.out.println(x);
		//}
        //System.out.println(audioURL.getPath());
        //System.out.println(ft);
        //System.out.println(loader);  

        /* allocate the resource necessary for the recognizer */
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
	@Override
	public void setBeanFactory(BeanFactory arg0) throws BeansException {
		// TODO Auto-generated method stub
		
	}
	
	
	 private static void createFrontEndElements() {
	    	
	    	//TODO: use spring rather than constructors here...
	    	/*There are the spring bean prototype names
	    	 * Could get the beanfactory and get them that way, 
	    	 * not sure what is best yet...
	    	speechClassifier
	        speechMarker
	        speechDataMonitor
	        nonSpeechDataFilter
	        identityStage
	        dataBlocker
	        insertSpeechSignal
	        recorder
	        preemphasizer
	        dither
	        windower
	        fft
	        melFilterBank16k
	        melFilterBank8k
	        dct
	        batchCMN
	        liveCMN
	        featureExtraction*/
	    	
	    	

	    	// create all the components that may be needed for the front end ahead of time. 
	    	// to save time at recognition requests time.
	    	dataBlocker = new DataBlocker(10);
	    	speechClassifier = new SpeechClassifier(10,0.003,10.0,0.0);
	    	speechMarker = new SpeechMarker(200,200,100,50,100,15);
	    	nonSpeechDataFilter = new NonSpeechDataFilter();

	    	//insertSpeechSignalStage = new InsertSpeechSignalStage();

	    	preemphasizer = new Preemphasizer(0.97);
	    	//dither = new Dither();
	    	raisedCosineWindower = new RaisedCosineWindower(0.46,(float)25.625,(float)10.0);
	    	discreteFourierTransform = new DiscreteFourierTransform(-1,false);
	    	melFrequencyFilterBank16k = new MelFrequencyFilterBank((double)130.0,(double)6800.0,40);
	    	melFrequencyFilterBank8k = new MelFrequencyFilterBank((double)200.0,(double)3500.0,31);
	    	discreteCosineTransform8k = new DiscreteCosineTransform(31,13);
	    	discreteCosineTransform16k = new DiscreteCosineTransform(40,13);
	    	batchCmn = new BatchCMN();
	    	//liveCmn = new LiveCMN(12,500,800);
	    	deltasFeatureExtractor = new DeltasFeatureExtractor(3);
			//recorder = new WavWriter(recordingFilePath,isCompletePath,bitsPerSample,bigEndian,isSigned,captureUtts);
	    }
	    
	    
	 private static FrontEnd createAudioFrontend(DataProcessor dataSource, Loader loader, int sampleRate) {

		 ArrayList<DataProcessor> components = new ArrayList <DataProcessor>();
		 components.add(dataSource);
		 components.add (dataBlocker);
		 components.add (speechClassifier);
		 components.add (speechMarker);
		 components.add (nonSpeechDataFilter);

		 components.add (preemphasizer);
		 //components.add (dither);
		 components.add (raisedCosineWindower);
		 components.add (discreteFourierTransform);
		 if (sampleRate == 16000) {
			 components.add (melFrequencyFilterBank16k);
			 components.add (discreteCosineTransform16k);
		 }else if (sampleRate == 8000) {
			 components.add (melFrequencyFilterBank8k);
			 components.add (discreteCosineTransform8k);
		 }else {
			 components.add (melFrequencyFilterBank8k);
			 components.add (discreteCosineTransform8k);
		 }

		 components.add (batchCmn);

		 components.add (deltasFeatureExtractor);

		 //TODO:  feature extractor is constructed on the fly, it needs the loader for the model.  Should not be very high overhead if
		 //it already loaded anyway.  Some models dont have a matrix. 
		 if (loader.getTransformMatrix() != null) {
		 System.out.println(loader.getTransformMatrix());
			 lda = new FeatureTransform(loader);
			 components.add (lda);
		 }
		 //for (DataProcessor dp : components) {
		 //   _logger.debug(dp);
		 //}
		 FrontEnd fe = new FrontEnd (components);
		 return fe;   
	 }
	 
	 
		public static Properties loadProperties (String name, ClassLoader loader) {
	        if (name == null)
	            throw new IllegalArgumentException ("null input: name");
	        
	        if (name.startsWith ("/"))
	            name = name.substring (1);  
	        Properties result = null;
	        try {
		        InputStream in = null;
		
		        if (loader == null) loader = ClassLoader.getSystemClassLoader ();                
		  
		        in = loader.getResourceAsStream (name);
		        if (in != null) {
		            result = new Properties ();
		            result.load (in); // Can throw IOException
		        }
	        } catch (Exception e) {
	        	e.printStackTrace();
			}
	      
	        return result;
	    }
	    
	    /**
	     * A convenience overload of {@link #loadProperties(String, ClassLoader)}
	     * that uses the current thread's context classloader.
	     */
	    public static Properties loadProperties (final String name) {
	        return loadProperties (name,
	            Thread.currentThread ().getContextClassLoader ());
	    }
	        

	 
	 
	 
}
