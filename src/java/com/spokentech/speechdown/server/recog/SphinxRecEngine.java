package com.spokentech.speechdown.server.recog;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.common.RecognitionResult;
import com.spokentech.speechdown.common.SpeechEventListener;
import com.spokentech.speechdown.common.sphinx.AudioStreamDataSource;
import com.spokentech.speechdown.common.sphinx.IdentityStage;
import com.spokentech.speechdown.common.sphinx.InsertSpeechSignalStage;
import com.spokentech.speechdown.common.sphinx.SpeechDataMonitor;
import com.spokentech.speechdown.common.sphinx.WavWriter;
import com.spokentech.speechdown.server.util.pool.AbstractPoolableObject;

import edu.cmu.sphinx.decoder.ResultListener;

import edu.cmu.sphinx.decoder.scorer.AbstractScorer;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.NonSpeechDataFilter;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifier;
import edu.cmu.sphinx.frontend.endpoint.SpeechMarker;
import edu.cmu.sphinx.frontend.feature.BatchCMN;
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor;
import edu.cmu.sphinx.frontend.feature.LDA;
import edu.cmu.sphinx.frontend.feature.LiveCMN;
import edu.cmu.sphinx.frontend.filter.Dither;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.jsapi.JSGFGrammar;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.recognizer.Recognizer.State;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;


public class SphinxRecEngine extends AbstractPoolableObject implements RecEngine {

    private static Logger _logger = Logger.getLogger(SphinxRecEngine.class);
    
	public static final short WAITING_FOR_SPEECH = 0;
	public static final short SPEECH_IN_PROGRESS = 1;
	public static final short COMPLETE = 2;
	protected volatile short _state = COMPLETE;

	private Sphinx3Loader _loader;
    private AbstractScorer  _scorer;

	private FrontEnd fe = null;
    private Recognizer _recognizer;
    private JSGFGrammar _jsgfGrammar;
    private boolean hotword = false;       
	private GrammarManager _grammarManager;
	private ConfigurationManager _cm;
	private int _id;
	

	private String recordingFilePath;
	private boolean recordingEnabled;
    private FileWriter outTextFile;

	protected /*static*/ Timer _timer = new Timer();
	protected TimerTask _noInputTimeoutTask;	
	protected StreamDataSource _dataSource = null;
	
	//front end elements
	DataBlocker dataBlocker ;
	SpeechClassifier speechClassifier;
	SpeechMarker speechMarker;
	NonSpeechDataFilter nonSpeechDataFilter;
	SpeechDataMonitor speechDataMonitor ;
	SpeechEventListener listener;
	InsertSpeechSignalStage insertSpeechSignalStage;
	IdentityStage identityStage ;
	Preemphasizer preemphasizer;
	Dither dither;
	RaisedCosineWindower raisedCosineWindower ;
	DiscreteFourierTransform discreteFourierTransform ;
	MelFrequencyFilterBank melFrequencyFilterBank ;
	DiscreteCosineTransform discreteCosineTransform ;
	BatchCMN batchCmn ;
	LiveCMN liveCmn;
	LDA lda;
	DeltasFeatureExtractor deltasFeatureExtractor;
	private WavWriter recorder; 
	
    public SphinxRecEngine(ConfigurationManager cm, GrammarManager grammarManager,String prefixId, int id, String recordingFilePath, boolean recordingEnabled) throws IOException, PropertyException, InstantiationException {

    	_logger.info("Creating a recognizer "+prefixId +"recognizer"+id);
    	_recognizer = (Recognizer) cm.lookup(prefixId+"recognizer"+id);
       
    	//SAL (comment)
    	_recognizer.allocate();


	    _jsgfGrammar = (JSGFGrammar) cm.lookup("grammar");
	    _grammarManager = grammarManager;

    	_loader = (Sphinx3Loader) cm.lookup("loader");
        _cm = cm;
        _id = id;
		_scorer = (AbstractScorer)_cm.lookup(prefixId+"scorer"+id);


        MyStateListener stateListener =  new MyStateListener();
        MyResultListener resultListener = new MyResultListener();
		_recognizer.addResultListener(resultListener);

		_recognizer.addStateListener(stateListener);


    	this.recordingEnabled = recordingEnabled;
    	
    	Date dateNow = new Date ();
        SimpleDateFormat dateformatMMDDYYYY = new SimpleDateFormat("MMddyyyy");
 
        StringBuilder nowMMDDYYYY = new StringBuilder( dateformatMMDDYYYY.format( dateNow ) );
    	this.recordingFilePath = recordingFilePath+"/"+nowMMDDYYYY+"-";
    	
    	
    	// todo:  should be a single filerwriter (but then it must be threadsafe), else each recognizer maybe should have its own file...
    	// Not really sure of what happens when there is a many Filewriters with the same filename
    	if (recordingEnabled) {
		    try {
		    	outTextFile = new FileWriter(recordingFilePath+"/"+nowMMDDYYYY+"-"+id+".txt",true);
		    } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    }
    	}

    	//create frontend elements (to be assembled into frontends at run time)
    	createFrontEndElements();
    	
    	
    }
    




	//Recognize using language mode
    public RecognitionResult recognize(InputStream as, String mimeType, int sampleRate, boolean bigEndian, 
    	                               int bytesPerValue, Encoding encoding, boolean doEndpointing, boolean cmnBatch) {
    	long startTime= System.currentTimeMillis();
		_logger.info("Using recognizer # "+_id+ ", time: "+startTime);
	    //SAL
		//_recognizer.allocate();


        //set the search manager in real time (to the one configured for language models)
        //_decoder.setSearchManager(_lmSearchManager);
        
	    Result r = doRecognize(as, mimeType, sampleRate, bigEndian, bytesPerValue, encoding, doEndpointing, cmnBatch);

	    _logger.info("Result: " + (r != null ? r.getBestFinalResultNoFiller() : null));
	    //SAL
	    //_recognizer.deallocate();
	    
	    if (r != null) {
	       RecognitionResult results = new RecognitionResult(r.getBestResultNoFiller());
	       return results;
	    } else {
	    	return null;
	    }
    }
	//recognize using a grammar
	public RecognitionResult recognize(InputStream as, String mimeType, String grammar, int sampleRate, 
			                           boolean bigEndian, int bytesPerValue, Encoding encoding, boolean doEndpointing, boolean cmnBatch) {
    	long startTime= System.currentTimeMillis();
		_logger.info("Using recognizer # "+_id+ ", time: "+startTime);
	    //SAL
		//_recognizer.allocate();
        _logger.debug("After allocate" + System.currentTimeMillis());
        
        //set the search manager in real time (to the one configured for jsgf grammars)
        //_decoder.setSearchManager(_jsgfSearchManager);
		//_jsgfSearchManager.allocate();
		
		GrammarLocation grammarLocation = null;
	    try {
	        grammarLocation = _grammarManager.saveGrammar(grammar);
	    } catch (IOException e) {
	        _logger.debug(e, e);
	    }
        
        _logger.debug("After save grammar" + System.currentTimeMillis());
    
	    try {
	        loadJSGF(_jsgfGrammar, grammarLocation);
        } catch (GrammarException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
        _logger.debug("After load grammar" + System.currentTimeMillis());
 
	    Result r = doRecognize(as, mimeType, sampleRate, bigEndian, bytesPerValue, encoding,doEndpointing,  cmnBatch);
	    
	    RecognitionResult results = new RecognitionResult(r, _jsgfGrammar.getRuleGrammar());
	    _logger.info("Result: " + (results != null ? results.getText() : null));
	    //SAL
	    //_recognizer.deallocate();
	    return results;
    }

	private Result doRecognize(InputStream as, String mimeType, int sampleRate, boolean bigEndian,
            int bytesPerValue, Encoding encoding, boolean doEndpointing, boolean cmnBatch) {
	    //TODO: Timers for recog timeout
		
		
		//setup the recorder
		// reuse this rather than construct a new one each time
		if (recordingEnabled) {
			recorder.setBigEndian(bigEndian);
			recorder.setBitsPerSample(bytesPerValue*8);
			recorder.setSampleRate(sampleRate);
			recorder.setSigned(true);
		}
	
	    // configure the audio input for the recognizer

		 String parts[] = mimeType.split("/");
		 if (parts[1].equals("x-s4audio")) {
			 _dataSource = new S4DataStreamDataSource();
			 fe = createAudioFrontend(doEndpointing,cmnBatch,(DataProcessor)_dataSource);
		 } else if (parts[1].equals("x-s4feature")) {
			 _dataSource = new S4DataStreamDataSource();
			 fe = createFeatureFrontend((DataProcessor)_dataSource);
			 if (doEndpointing) {
				 _logger.warn("Endpointing not supported for feature streams");
			 }
		 } else if (parts[1].equals("x-wav")) {
			 _dataSource = new AudioStreamDataSource();
			 fe = createAudioFrontend(doEndpointing,cmnBatch,(DataProcessor)_dataSource);
		 } else {
			 _logger.warn("Unrecognized mime type: "+mimeType + " Trying to process as audio/x-wav");
			 _dataSource = new AudioStreamDataSource();
			 fe = createAudioFrontend(doEndpointing,cmnBatch,(DataProcessor)_dataSource);
		 }
		 if (doEndpointing) {
			//TODO: start the timer
			//_noInputTimeoutTask = new TimerTask(30000);
		 }

		 
	     _logger.debug("-----> "+mimeType+ " "+parts[1]);

	     //set the first stage of the front end
		 fe.setDataSource((DataProcessor) _dataSource);

		 
		 // set the front end in the scorer in realtime
		 _scorer.setFrontEnd(fe);
	     
		 _dataSource.setInputStream(as, "ws-audiostream", sampleRate, bigEndian, bytesPerValue,encoding);
	    
		_logger.info("After setting the input stream " + System.currentTimeMillis());
	    
		long  start = System.nanoTime();
	    // decode the audio file.
	    //_logger.debug("Decoding " + audioFileURL);
		Result r = _recognizer.recognize();
		long stop = System.nanoTime();
		long wall = (stop - start)/1000000;
		long streamLen = _dataSource.getLengthInMs();
		double ratio = (double)wall/(double)streamLen;
		_logger.info(ratio+ "  Wall time "+ wall+ " stream length "+ streamLen);
		if (recordingEnabled) {
			logResults(r, recorder.getWavName());
		}
		
		fe=null;
	    return r;
    }
    
	public void stopAudioTransfer() {
		 try {
		        _dataSource.closeDataStream();
	     } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
	     }
	}
	
	
	@Override
    public String transcribe(InputStream as, String mimeType, int sampleRate, boolean bigEndian,
            int bytesPerValue, Encoding encoding, PrintWriter out,HttpServletResponse response) {
		
		_logger.info("Using recognizer # "+_id);
	    //SAL
		//_recognizer.allocate();
        _logger.debug("After allocate" + System.currentTimeMillis());
        

	    String r = doTranscribe(as, mimeType, sampleRate, bigEndian, bytesPerValue, encoding, out, response);

	    //SAL
	    //_recognizer.deallocate();
	    return r;

    }
	
	private String doTranscribe(InputStream as, String mimeType, int sampleRate, boolean bigEndian,
            int bytesPerValue, Encoding encoding, PrintWriter out,HttpServletResponse response) {
	    //TODO: Timers for recog timeout
	
		
		FrontEnd fe = null;
	    // configure the audio input for the recognizer

		 String parts[] = mimeType.split("/");
		 if (parts[1].equals("x-s4audio")) {
			 _dataSource = new S4DataStreamDataSource();
			 fe = createAudioFrontend(true,false,(DataProcessor) _dataSource);
		 } else if (parts[1].equals("x-s4feature")) {
			 _logger.warn("Feature mode Endpointing not for continuous recognition mode");
			 _dataSource = new S4DataStreamDataSource();
		 } else if (parts[1].equals("x-wav")) {
			 _dataSource = new AudioStreamDataSource();
			 fe = createAudioFrontend(true,false,(DataProcessor) _dataSource);
		 } else {
			 _logger.warn("Unrecognized mime type: "+mimeType + " Trying to process as audio/x-wav");
			 _dataSource = new AudioStreamDataSource();
			 fe = createAudioFrontend(true,false,(DataProcessor) _dataSource);
		 }
	     _logger.debug("-----> "+mimeType+ " "+parts[1]);
	     //set the first stage of the front end
		 fe.setDataSource((DataProcessor) _dataSource);
		 
		 // set the front end in the scorer in realtime
		 _scorer.setFrontEnd(fe);
		 
		 _dataSource.setInputStream(as, "ws-audiostream", sampleRate, bigEndian, bytesPerValue,encoding);
	    
		_logger.debug("After setting the input stream" + System.currentTimeMillis());
	    
	    // decode the audio file.
	    //_logger.debug("Decoding " + audioFileURL);
	    //List<Result> resultList = new ArrayList<Result>();
		String totalResult ="";

		Result result;
        while ((result = _recognizer.recognize())!= null) {

            String resultText = result.getBestResultNoFiller();
            _logger.info(resultText);
            out.println(resultText);
            totalResult = totalResult+resultText;

            //chunked encoded, so flush for realtime streaming
            if (response != null)
	            try {
	                response.flushBuffer();
                } catch (IOException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
                }
            //resultList.add(result);
    
        }
        fe = null;
	    return totalResult;
    }
	
	
	/* (non-Javadoc)
     * @see com.spokentech.speechdown.server.recog.RecEngine#recognize(javax.sound.sampled.AudioInputStream, java.lang.String)
     */
	//recognize using a grammar (the SOAP method) 
	//TODO: Re-write
	public RecognitionResult recognize(AudioInputStream as, String grammar) {
	    return null;
	}
    
	
	   /**
	  * TODOC
	  * @param grammarLocation
	  * @throws IOException
	  * @throws GrammarException
	  */
 public synchronized void loadJSGF(JSGFGrammar jsgfGrammar, GrammarLocation grammarLocation) throws IOException, GrammarException {
     jsgfGrammar.setBaseURL(grammarLocation.getBaseURL());
     try {
         jsgfGrammar.loadJSGF(grammarLocation.getGrammarName());
         _logger.debug("loadJSGF(): completed successfully.");
     } catch (com.sun.speech.engine.recognition.TokenMgrError e) {
         _logger.debug("loadJSGF(): encountered exception: " + e.getClass().getName(), e); // com.sun.speech.engine.recognition.TokenMgrError!!!
         String message = e.getMessage();
         /*if (message.indexOf("speech") < 0) {
             throw e;
         }*/
         // else assume caused by GrammarException
         // TODO: edu.cmu.sphinx.jsapi.JSGFGrammar.loadJSGF() should be updated not to swallow GrammarException
         throw new GrammarException(message);
     }
 }
 
 private Result waitForResult(boolean hotword) {
     Result result = null;
     
     _logger.debug("The hotword flag is: "+hotword);
     //if hotword mode, run recognize until a match occurs
     if (hotword) {
         RecognitionResult rr = new RecognitionResult();
         boolean inGrammarResult = false;
         while (!inGrammarResult) {
              result = _recognizer.recognize();

              if (result == null) {
                  _logger.debug("result is null");
              } else {
                  _logger.debug("result is:"+result.toString());
              }
              rr.setNewResult(result, _jsgfGrammar.getRuleGrammar());
              _logger.debug("Rec result: "+rr.toString());
              _logger.debug("text:"+rr.getText()+" matches:"+rr.getRuleMatches()+" oog flag:"+rr.isOutOfGrammar());
              if( (!rr.getRuleMatches().isEmpty()) && (!rr.isOutOfGrammar())) {
                  inGrammarResult = true;
              }
         }
      
     //if not hotword, just run recognize once
     } else {
          result = _recognizer.recognize();
     }
     return result;

 }
 


    /* (non-Javadoc)
     * @see org.speechforge.cairo.util.pool.PoolableObject#activate()
     */
    @Override
    public synchronized void activate() {
        _logger.info("SphinxRecEngine activating...");
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.util.pool.PoolableObject#passivate()
     */
    @Override
    public synchronized void passivate() {
        _logger.info("SphinxRecEngine passivating...");
        //stopProcessing();
        //_recogListener = null;
    }


    /**
     * TODOC
     * @param text
     * @param ruleName
     * @return
     * @throws GrammarException
     */
    public synchronized RuleParse parse(String text, String ruleName) throws GrammarException {
        
        RuleGrammar ruleGrammar = _jsgfGrammar.getRuleGrammar();
        return ruleGrammar.parse(text, ruleName);
    }


    /**
     * @return the hotword
     */
    public boolean isHotword() {
        return hotword;
    }

    /**
     * @param hotword the hotword to set
     */
    public void setHotword(boolean hotword) {
        this.hotword = hotword;
    }

    
    private class MyStateListener implements StateListener {

		public void statusChanged(State  arg0) {
	        _logger.debug("Recognizer Status changed to "+arg0.toString() +" "+System.currentTimeMillis());
	        
        }

		public void newProperties(PropertySheet arg0) throws PropertyException {
	        _logger.debug("StateListener New properties called");
	        
        }

    	
    }
    private class MyResultListener implements ResultListener {

		public void newResult(Result arg0) {
			_logger.info("best final result: "+arg0.getBestFinalResultNoFiller());
			_logger.info("best pronuciation: "+arg0.getBestPronunciationResult());
			_logger.info("Frame "+arg0.getStartFrame()+ " to "+arg0.getEndFrame()+"("+arg0.getFrameNumber()+")");
	        
        }

		public void newProperties(PropertySheet arg0) throws PropertyException {
	        _logger.info("ResultListener New properties called");
	        
        }
    	
    }
    /**
   	 * The Class Listener.
   	 */
   	protected class Listener implements SpeechEventListener {


	        @Override
	        public void speechStarted() {
	            _logger.debug("speechStarted()");

	            synchronized (SphinxRecEngine.this) {
	                if (_state == WAITING_FOR_SPEECH) {
	                    _state = SPEECH_IN_PROGRESS;
	                }
	                if (_noInputTimeoutTask != null) {
	                    _noInputTimeoutTask.cancel();
	                    _noInputTimeoutTask = null;
	                }
	            }
	        }
	        
        	public void speechEnded() {
	            _logger.debug("speechEnded()");
	            synchronized (SphinxRecEngine.this) {
	            	stopAudioTransfer();
	            	_state = COMPLETE;
	            }

	        }

        	public void noInputTimeout() {
	            _logger.debug("no input timeout()");
	            synchronized (SphinxRecEngine.this) {
	            	stopAudioTransfer();
	            	_state = COMPLETE;
	            }	   

	        }

			@Override
            public void recognitionComplete(RecognitionResult rr) {
	            // TODO Auto-generated method stub
	            
            }

	    }

    /**
     * The Class NoInputTimeoutTask.
     */
    public class NoInputTimeoutTask extends TimerTask {

        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            synchronized (SphinxRecEngine.this) {
                _noInputTimeoutTask = null;
                if (_state == WAITING_FOR_SPEECH) {
                    _state = COMPLETE;
                    stopAudioTransfer();

                }
            }
        }
        
    }
    
    
    
    private FrontEnd createFeatureFrontend(DataProcessor dataSource) {
    	   ArrayList<DataProcessor> components = new ArrayList <DataProcessor>();
    	   //components.add (new IdentityStage ());
     	   components.add(dataSource);
    	   FrontEnd fe = new FrontEnd (components);
    	   return fe;   	
    }

	
	
    private void createFrontEndElements() {

    	// create all the components that may be needed for the front end ahead of time. 
    	// to save time at recognition requests time.
    	dataBlocker = new DataBlocker(10);
    	speechClassifier = new SpeechClassifier(10,0.003,10.0,0.0);
    	speechMarker = new SpeechMarker(200,200,100,50,100);
    	nonSpeechDataFilter = new NonSpeechDataFilter();
    	speechDataMonitor  = new SpeechDataMonitor();
    	listener = new Listener();
    	insertSpeechSignalStage = new InsertSpeechSignalStage();
    	identityStage = new IdentityStage();
    	preemphasizer = new Preemphasizer(0.97);
    	dither = new Dither();
    	raisedCosineWindower = new RaisedCosineWindower(0.46,(float)25.625,(float)10.0);
    	discreteFourierTransform = new DiscreteFourierTransform(-1,false);
    	melFrequencyFilterBank = new MelFrequencyFilterBank((double)133.0,(double)3500.0,31);
    	discreteCosineTransform = new DiscreteCosineTransform(40,13);
    	batchCmn = new BatchCMN();
    	liveCmn = new LiveCMN(12,500,800);
    	deltasFeatureExtractor = new DeltasFeatureExtractor(3);
    	lda = new LDA(_loader);
    	
		boolean isCompletePath = false;
		int bitsPerSample = 16;
		boolean isSigned = true;
		boolean captureUtts = true;
		boolean bigEndian = false;
		recorder = new WavWriter(recordingFilePath,isCompletePath,bitsPerSample,bigEndian,isSigned,captureUtts);
    }
    
    
    private FrontEnd createAudioFrontend(boolean endpointing, boolean batchCMN, DataProcessor dataSource) {
    	
 	   ArrayList<DataProcessor> components = new ArrayList <DataProcessor>();
 	   components.add(dataSource);
	   components.add (dataBlocker);
	   if (endpointing) {
		  components.add (speechClassifier);
	      components.add (speechMarker);
	      components.add (nonSpeechDataFilter);
	   //} else {
		
		//   components.add(insertSpeechSignalStage);
	   }
	   _logger.info("Endpointing is: "+endpointing);
	   //always add the monitor and listener
	   components.add (speechDataMonitor);
	   speechDataMonitor.setSpeechEventListener(listener);
	    
	   //this is just for logging debug messages.
	   components.add(identityStage);
	   if (recordingEnabled && (recorder != null)) {
	      components.add(recorder);
	   }
	   components.add (preemphasizer);
	   components.add (dither);
	   components.add (raisedCosineWindower);
	   components.add (discreteFourierTransform);
	   components.add (melFrequencyFilterBank);

	   components.add (discreteCosineTransform);
	   if (batchCMN) {
	      components.add (batchCmn);
	   } else {
	      components.add (liveCmn);
	   }
	   
	   components.add (deltasFeatureExtractor);
	   components.add (lda);
	   	   
       //for (DataProcessor dp : components) {
       //   _logger.debug(dp);
       //}
	   
	   FrontEnd fe = new FrontEnd (components);
	   return fe;   
    }

	public void logResults(Result r,String wavName) {
		try {
			String results = null;
			if (r == null) {
				results ="null result";
			} else {
				results = r.getBestFinalResultNoFiller();
			}
	        outTextFile.write(results+","+wavName+"\n");
	        outTextFile.flush();
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
           
    }


}
